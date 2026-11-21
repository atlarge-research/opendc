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

package org.opendc.web.dispatcher.kubernetes

import io.fabric8.kubernetes.api.model.NodeBuilder
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.api.model.TaintBuilder
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.web.dispatcher.CapacitySnapshot
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.PlatformSpan
import org.opendc.web.launcher.PeakMemory
import java.time.Instant

/**
 * Every verdict a Kubernetes run gets is read off its Job and pod, so a misread state is a run
 * retried that should have stopped, or one given up on that a retry would have fixed.
 */
class ClusterReadingTest {
    private val start = "2026-10-04T10:00:00Z"
    private val end = "2026-10-04T10:05:00Z"

    @Test
    fun `reads a launcher that exited by itself through the launcher's own codes`() {
        val state = jobState(job(), listOf(terminated(code = 0, message = report(100.0, 40.0))), gone = false)

        val outcome = (state as JobState.Ended).outcome
        assertEquals(ExitReason.OK, outcome.reason)
        assertEquals(PlatformSpan.Ran(Instant.parse(start), Instant.parse(end)), outcome.span)
        assertEquals(PeakMemory.Measured(100.0, 40.0), outcome.peakMemory)
    }

    @Test
    fun `reads a stop this server asked for over whatever the container exited with`() {
        val state = jobState(job(stopReason = ExitReason.CANCELLED), listOf(terminated(code = 0)), gone = false)

        assertEquals(ExitReason.CANCELLED, (state as JobState.Ended).outcome.reason)
    }

    @Test
    fun `reads the kernel's out-of-memory kill and the heap running out alike as memory`() {
        assertEquals(ExitReason.OOM, ended(jobState(job(), listOf(terminated(code = 137, reason = "OOMKilled")), gone = false)))
        assertEquals(ExitReason.OOM, ended(jobState(job(), listOf(terminated(code = 3)), gone = false)))
    }

    @Test
    fun `reads a pod past its deadline as a timeout`() {
        val pod = PodBuilder(terminated(code = 143)).editStatus().withReason("DeadlineExceeded").endStatus().build()

        assertEquals(ExitReason.TIMEOUT, ended(jobState(job(), listOf(pod), gone = false)))
    }

    // A signal nobody here sent says nothing about the work, so it is worth another attempt.
    @Test
    fun `reads a signal this server did not send as unexplained`() {
        assertEquals(ExitReason.UNKNOWN, ended(jobState(job(), listOf(terminated(code = 143)), gone = false)))
    }

    @Test
    fun `reads an eviction as unexplained, saying so`() {
        val pod = PodBuilder(terminated(code = 137)).editStatus().withReason("Evicted").withMessage("node out of disk").endStatus().build()

        val outcome = (jobState(job(), listOf(pod), gone = false) as JobState.Ended).outcome

        assertEquals(ExitReason.UNKNOWN, outcome.reason)
        assertTrue("node out of disk" in outcome.message)
    }

    @Test
    fun `reads an image that can never be pulled as refused, at once`() {
        val pod = waiting(reason = "InvalidImageName", message = "bad reference")

        assertTrue(jobState(job(), listOf(pod), gone = false) is JobState.Refused)
    }

    @Test
    fun `reads a pod still pulling its image as pending, with the reason`() {
        val state = jobState(job(), listOf(waiting(reason = "ContainerCreating", message = "")), gone = false)

        assertTrue(state is JobState.Pending && "ContainerCreating" in state.cause)
    }

    @Test
    fun `reads a Job nobody made a pod for as pending on quota or admission`() {
        val state = jobState(job(), emptyList(), gone = false)

        assertTrue(state is JobState.Pending && "ResourceQuota" in state.cause, "$state")
    }

    @Test
    fun `reads a Job removed before it ended as unexplained`() {
        assertEquals(ExitReason.UNKNOWN, ended(jobState(job(), emptyList(), gone = true)))
    }

    @Test
    fun `reads a running container as running from when it started`() {
        val pod =
            pod()
                .editStatus()
                .withPhase("Running")
                .addNewContainerStatus()
                .withName("launcher")
                .withNewState()
                .withNewRunning()
                .withStartedAt(start)
                .endRunning()
                .endState()
                .endContainerStatus()
                .endStatus()
                .build()

        assertEquals(JobState.Running(Instant.parse(start)), jobState(job(), listOf(pod), gone = false))
    }

    @Test
    fun `reports no memory for a container that left no report`() {
        assertEquals(
            PeakMemory.Unmeasured,
            (jobState(job(), listOf(terminated(code = 0)), gone = false) as JobState.Ended).outcome.peakMemory,
        )
    }

    // This server's pods tolerate no taint, so a node that repels them is no capacity of theirs.
    @Test
    fun `counts only nodes this server's pods can be placed on, and its ceiling when none are visible`() {
        val ceiling = CapacitySnapshot(64, 131072.0, 0, 0.0)
        val ready = node("ready", ready = true, unschedulable = false, taintEffects = listOf("PreferNoSchedule"))
        val cordoned = node("cordoned", ready = true, unschedulable = true)
        val down = node("down", ready = false, unschedulable = false)
        val reserved = node("reserved", ready = true, unschedulable = false, taintEffects = listOf("NoSchedule"))

        val seen = capacityOf(listOf(ready, cordoned, down, reserved), emptyList(), ceiling)

        assertEquals(8, seen.totalCores)
        assertEquals(16384.0, seen.totalMemoryMb, 1e-6)
        assertEquals(64, capacityOf(emptyList(), emptyList(), ceiling).totalCores)
    }

    private fun ended(state: JobState): ExitReason = (state as JobState.Ended).outcome.reason

    private fun report(
        resident: Double,
        heap: Double,
    ): String = SdkJson.json.encodeToString(PeakMemory.serializer(), PeakMemory.Measured(resident, heap))

    private fun job(stopReason: ExitReason? = null): Job =
        JobBuilder()
            .withNewMetadata()
            .withName("opendc-job")
            .withCreationTimestamp(start)
            .apply { if (stopReason != null) addToAnnotations(STOP_REASON, stopReason.name).addToAnnotations(STOP_MESSAGE, "stopped") }
            .endMetadata()
            .build()

    private fun pod(): PodBuilder =
        PodBuilder()
            .withNewMetadata()
            .withName("opendc-job-pod")
            .withCreationTimestamp(start)
            .endMetadata()
            .withNewStatus()
            .endStatus()

    private fun terminated(
        code: Int,
        reason: String = "Completed",
        message: String? = null,
    ): Pod =
        pod()
            .editStatus()
            .withPhase(if (code == 0) "Succeeded" else "Failed")
            .addNewContainerStatus()
            .withName("launcher")
            .withNewState()
            .withNewTerminated()
            .withExitCode(code)
            .withReason(reason)
            .withMessage(message)
            .withStartedAt(start)
            .withFinishedAt(end)
            .endTerminated()
            .endState()
            .endContainerStatus()
            .endStatus()
            .build()

    private fun waiting(
        reason: String,
        message: String,
    ): Pod =
        pod()
            .editStatus()
            .withPhase("Pending")
            .addNewContainerStatus()
            .withName("launcher")
            .withNewState()
            .withNewWaiting()
            .withReason(reason)
            .withMessage(message)
            .endWaiting()
            .endState()
            .endContainerStatus()
            .endStatus()
            .build()

    private fun node(
        name: String,
        ready: Boolean,
        unschedulable: Boolean,
        taintEffects: List<String> = emptyList(),
    ) = NodeBuilder()
        .withNewMetadata()
        .withName(name)
        .endMetadata()
        .withNewSpec()
        .withUnschedulable(unschedulable)
        .withTaints(taintEffects.map { TaintBuilder().withKey("example.org/reserved").withEffect(it).build() })
        .endSpec()
        .withNewStatus()
        .addToAllocatable("cpu", Quantity("8"))
        .addToAllocatable("memory", Quantity("16Gi"))
        .addNewCondition()
        .withType("Ready")
        .withStatus(if (ready) "True" else "False")
        .endCondition()
        .endStatus()
        .build()
}
