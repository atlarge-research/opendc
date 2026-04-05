/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.web.runner.internal

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.web.client.runner.JobResource
import org.opendc.web.client.runner.OpenDCRunnerClient
import org.opendc.web.proto.JobState
import org.opendc.web.proto.runner.Job

/**
 * Test suite for [JobManagerImpl].
 */
class JobManagerImplTest {
    private lateinit var client: OpenDCRunnerClient
    private lateinit var jobResource: JobResource
    private lateinit var manager: JobManagerImpl

    @BeforeEach
    fun setUp() {
        client = mockk()
        jobResource = mockk()
        every { client.jobs } returns jobResource
        manager = JobManagerImpl(client)
    }

    private fun makeJob(
        id: Long,
        state: JobState,
    ): Job =
        mockk {
            every { this@mockk.id } returns id
            every { this@mockk.state } returns state
        }

    @Test
    fun testFindNextReturnsNullWhenEmpty() {
        every { jobResource.queryPending() } returns emptyList()

        assertNull(manager.findNext())
    }

    @Test
    fun testFindNextReturnsFirstJob() {
        val job = makeJob(1L, JobState.PENDING)
        every { jobResource.queryPending() } returns listOf(job)

        val result = manager.findNext()
        assertEquals(job, result)
    }

    @Test
    fun testClaimSuccess() {
        every { jobResource.update(1L, Job.Update(JobState.CLAIMED, 0, null, null)) } returns makeJob(1L, JobState.CLAIMED)

        assertTrue(manager.claim(1L))
    }

    @Test
    fun testClaimFailsOnIllegalState() {
        every { jobResource.update(1L, Job.Update(JobState.CLAIMED, 0, null, null)) } throws IllegalStateException("conflict")

        assertFalse(manager.claim(1L))
    }

    @Test
    fun testHeartbeatSuccessWhenNotFailed() {
        val job = makeJob(1L, JobState.RUNNING)
        every { jobResource.update(1L, Job.Update(JobState.RUNNING, 30, null, null)) } returns job

        assertTrue(manager.heartbeat(1L, 30))
    }

    @Test
    fun testHeartbeatReturnsFalseWhenJobFailed() {
        val job = makeJob(1L, JobState.FAILED)
        every { jobResource.update(1L, Job.Update(JobState.RUNNING, 30, null, null)) } returns job

        assertFalse(manager.heartbeat(1L, 30))
    }

    @Test
    fun testHeartbeatReturnsTrueWhenResponseNull() {
        every { jobResource.update(1L, Job.Update(JobState.RUNNING, 30, null, null)) } returns null

        // null response means no FAILED state, so heartbeat can continue
        assertTrue(manager.heartbeat(1L, 30))
    }

    @Test
    fun testFail() {
        val report = mapOf("error" to "some error")
        every { jobResource.update(1L, Job.Update(JobState.FAILED, 60, null, report)) } returns makeJob(1L, JobState.FAILED)

        manager.fail(1L, 60, report)

        verify { jobResource.update(1L, Job.Update(JobState.FAILED, 60, null, report)) }
    }

    @Test
    fun testFailWithNullReport() {
        every { jobResource.update(1L, Job.Update(JobState.FAILED, 60, null, null)) } returns makeJob(1L, JobState.FAILED)

        manager.fail(1L, 60, null)

        verify { jobResource.update(1L, Job.Update(JobState.FAILED, 60, null, null)) }
    }

    @Test
    fun testFinish() {
        val results = mapOf("total_power_draw" to listOf(100.0))
        val report = mapOf("logs" to emptyList<Any>())
        every { jobResource.update(1L, Job.Update(JobState.FINISHED, 120, results, report)) } returns makeJob(1L, JobState.FINISHED)

        manager.finish(1L, 120, results, report)

        verify { jobResource.update(1L, Job.Update(JobState.FINISHED, 120, results, report)) }
    }

    @Test
    fun testFinishWithNullReport() {
        val results = mapOf("total_power_draw" to listOf(100.0))
        every { jobResource.update(1L, Job.Update(JobState.FINISHED, 120, results, null)) } returns makeJob(1L, JobState.FINISHED)

        manager.finish(1L, 120, results, null)

        verify { jobResource.update(1L, Job.Update(JobState.FINISHED, 120, results, null)) }
    }
}
