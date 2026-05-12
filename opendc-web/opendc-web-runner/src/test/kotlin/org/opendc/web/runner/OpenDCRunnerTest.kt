/*
 * Copyright (c) 2026 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.web.runner

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.opendc.web.proto.JobState
import org.opendc.web.proto.runner.Job
import org.opendc.web.proto.runner.Report
import org.opendc.web.proto.runner.Scenario
import org.opendc.web.proto.runner.Topology
import java.io.IOException
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.ArrayDeque
import java.util.Queue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Test suite for [OpenDCRunner], focused on the polling loop and job-dispatch behaviour added by
 * the runner stability fix in commit 2e1c7474 (`pollOnce()` retry logic and the empty-topology
 * pre-condition in `JobAction`).
 */
class OpenDCRunnerTest {
    private lateinit var manager: JobManager

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        manager = mockk()
    }

    private fun makeRunner(
        pollInterval: Duration = Duration.ofMillis(10),
        heartbeatInterval: Duration = Duration.ofSeconds(60),
        jobTimeout: Duration = Duration.ofSeconds(60),
    ): OpenDCRunner =
        OpenDCRunner(
            manager = manager,
            tracePath = tempDir.toFile(),
            parallelism = 1,
            jobTimeout = jobTimeout,
            pollInterval = pollInterval,
            heartbeatInterval = heartbeatInterval,
            exportDir = tempDir.resolve("exports").toFile(),
        )

    private val emptyTopologyJob: Job =
        Job(
            1L,
            Scenario(
                1L,
                1,
                null,
                "test-scenario",
                null,
                Topology(1L, 1, "test-topology", emptyList(), Instant.now(), Instant.now()),
                null,
                null,
                null,
            ),
            JobState.PENDING,
            Instant.now(),
            null,
            null,
            0,
            null,
        )

    @Test
    fun testRunExitsCleanlyOnInterruption() {
        every { manager.findNext() } returns null

        val runner = makeRunner(pollInterval = Duration.ofSeconds(60))
        val thread = Thread(runner)
        thread.start()
        // Give the thread a moment to enter Thread.sleep(pollInterval)
        Thread.sleep(50)
        thread.interrupt()
        thread.join(10_000)

        assertFalse(thread.isAlive, "Runner thread should exit after interrupt")
    }

    @Test
    fun testPollKeepsRunningAfterIOException() {
        // Verifies the new pollOnce() behaviour: a transient IOException from JobManager.findNext()
        // is logged and the runner sleeps, rather than crashing the polling loop. The CountDownLatch
        // synchronises termination: once we observe at least 3 polls we interrupt the runner thread.
        val pollLatch = CountDownLatch(3)
        every { manager.findNext() } answers {
            pollLatch.countDown()
            throw IOException("transient")
        }

        val runner = makeRunner(pollInterval = Duration.ofMillis(1))
        val thread = Thread(runner)
        thread.start()
        try {
            assertTrue(
                pollLatch.await(5, TimeUnit.SECONDS),
                "Expected at least 3 poll attempts; IOException must not break the loop",
            )
        } finally {
            thread.interrupt()
            thread.join(TimeUnit.SECONDS.toMillis(10))
        }

        assertFalse(thread.isAlive, "Runner thread should exit after interrupt")
        verify(atLeast = 3) { manager.findNext() }
        verify(exactly = 0) { manager.claim(any()) }
    }

    @Test
    fun testPollSleepsWhenNoJobAvailable() {
        // findNext() returns null (drained queue) -> pollOnce() must return true so run() sleeps for
        // pollInterval. We let the runner make several polls, then interrupt to exit.
        val jobQueue: Queue<Job> = ArrayDeque()
        val pollLatch = CountDownLatch(3)
        every { manager.findNext() } answers {
            pollLatch.countDown()
            jobQueue.poll()
        }

        val runner = makeRunner(pollInterval = Duration.ofMillis(1))
        val thread = Thread(runner)
        thread.start()
        try {
            assertTrue(
                pollLatch.await(5, TimeUnit.SECONDS),
                "Expected at least 3 poll attempts when no job is available",
            )
        } finally {
            thread.interrupt()
            thread.join(TimeUnit.SECONDS.toMillis(10))
        }

        assertFalse(thread.isAlive, "Runner thread should exit after interrupt")
        verify(atLeast = 3) { manager.findNext() }
        verify(exactly = 0) { manager.claim(any()) }
    }

    @Test
    fun testPollImmediatelyAfterClaimFailure() {
        // pollOnce() returns false on a failed claim -> run() must NOT sleep before the next attempt.
        // Pre-load the queue with several jobs and use a long pollInterval (10 minutes): if the
        // runner slept between claim attempts, the latch wouldn't be satisfied within 5 seconds.
        val jobCount = 4
        val jobQueue: Queue<Job> = ArrayDeque<Job>().apply { repeat(jobCount) { add(emptyTopologyJob) } }
        every { manager.findNext() } answers { jobQueue.poll() }

        val claimLatch = CountDownLatch(jobCount)
        every { manager.claim(1L) } answers {
            claimLatch.countDown()
            false
        }

        val runner = makeRunner(pollInterval = Duration.ofMinutes(10))
        val thread = Thread(runner)
        thread.start()
        try {
            assertTrue(
                claimLatch.await(5, TimeUnit.SECONDS),
                "Expected $jobCount claim attempts within 5 seconds; pollOnce must not sleep on failed claim",
            )
        } finally {
            thread.interrupt()
            thread.join(TimeUnit.SECONDS.toMillis(10))
        }

        assertFalse(thread.isAlive, "Runner thread should exit after interrupt")
        verify(exactly = jobCount) { manager.claim(1L) }
    }

    @Test
    fun testJobActionFailsOnEmptyTopology() {
        // Verifies the require(topology.isNotEmpty()) check in JobAction.compute(): when a scenario's
        // topology has no hosts, the runner reports the failure via JobManager.fail() with an
        // IllegalArgumentException whose message names the empty topology.

        // findNext() drains a queue with a single empty-topology job; subsequent polls return null
        // so the runner stays parked in Thread.sleep(pollInterval) until we interrupt it.
        val jobQueue: Queue<Job> = ArrayDeque<Job>().apply { add(emptyTopologyJob) }
        every { manager.findNext() } answers { jobQueue.poll() }
        every { manager.claim(1L) } returns true
        every { manager.heartbeat(1L, any()) } returns true

        val failLatch = CountDownLatch(1)
        val capturedReport = AtomicReference<Report?>()

        every { manager.fail(any(), any(), any()) } answers {
            capturedReport.set(thirdArg<Report?>())
            failLatch.countDown()
        }

        val runner = makeRunner(pollInterval = Duration.ofSeconds(60))
        val thread = Thread(runner)
        thread.start()

        try {
            assertTrue(
                failLatch.await(10, TimeUnit.SECONDS),
                "Expected manager.fail to be invoked within 10 seconds",
            )
        } finally {
            thread.interrupt()
            thread.join(TimeUnit.SECONDS.toMillis(10))
        }
        assertFalse(thread.isAlive, "Runner thread should exit after interrupt")

        val report =
            capturedReport.get() ?: error("Expected a non-null Report to be captured by manager.fail")

        val errorInfo = report.error()
        assertNotNull(errorInfo, "Report should carry ErrorInfo for failed jobs")
        assertEquals("IllegalArgumentException", errorInfo.type())
        assertTrue(
            errorInfo.message().contains("test-topology"),
            "Error message should mention the empty topology by name; was: ${errorInfo.message()}",
        )

        verify(exactly = 1) { manager.fail(1L, any(), any()) }
        verify(exactly = 0) { manager.finish(any(), any(), any(), any(), any()) }
    }
}
