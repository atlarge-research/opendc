/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.runner.workload

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.sdk.model.checkpoint.CheckpointSpec
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.workload.EfficientTraceWorkloadSpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec
import org.opendc.sdk.runner.executor.ResourceScope
import org.opendc.sdk.runner.factory.loadTrace
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.simulator.compute.workload.trace.TraceWorkload
import java.nio.file.Path

/**
 * Verifies that [EfficientTraceWorkloadSpec] (backed by [org.opendc.sdk.model.workload.loader.EfficientWorkloadLoader])
 * loads the exact same workload as [TraceWorkloadSpec] (backed by [org.opendc.sdk.model.workload.loader.ComputeWorkloadLoader])
 * for the same trace.
 */
class WorkloadLoaderTest {
    internal val testResourcesRoot: Path by lazy { Path.of(object {}.javaClass.getResource("/topologies")!!.toURI()).parent }

    private val provisioner = FileSystemResourceProvisioner(testResourcesRoot)
    private val resourceScope = ResourceScope(provisioner)

    @Test
    fun `Efficient Loader returns the same workload`() {
        val workloadSpec =
            TraceWorkloadSpec(source = NamedReference("workloadTraces/solvinity_small_efficient"), sampleFraction = 1.0)
        val efficientWorkloadSpec =
            EfficientTraceWorkloadSpec(
                source = NamedReference("workloadTraces/solvinity_small_efficient"),
                sampleFraction = 1.0,
            )

        val checkpointSpec = CheckpointSpec()

        val resourcePath = resourceScope.resolve(workloadSpec.source)
        val workload = workloadSpec.loadTrace(resourcePath, checkpointSpec).toList()

        val efficientResourcePath = resourceScope.resolve(efficientWorkloadSpec.source)
        val efficientWorkload = efficientWorkloadSpec.loadTrace(efficientResourcePath, checkpointSpec).toList()

        assertTrue(workload.isNotEmpty(), "The workload should not be empty")
        assertEquals(workload.size, efficientWorkload.size, "The two loaders should return the same number of tasks")

        assertOrderedBySubmission(workload, "workload")
        assertOrderedBySubmission(efficientWorkload, "efficientWorkload")

        val tasksById = workload.associateBy { it.id }
        val efficientTasksById = efficientWorkload.associateBy { it.id }

        assertEquals(tasksById.keys, efficientTasksById.keys, "Both loaders should return the same set of task ids")

        for (id in tasksById.keys) {
            assertTasksMatch(tasksById.getValue(id), efficientTasksById.getValue(id))
        }
    }

    /**
     * Verify that [tasks] is sorted by submission time, as the replayer relies on this ordering.
     */
    private fun assertOrderedBySubmission(
        tasks: List<ServiceTask>,
        label: String,
    ) {
        for (i in 1 until tasks.size) {
            assertTrue(
                tasks[i - 1].submittedAt <= tasks[i].submittedAt,
                "$label is not ordered by submittedAt at index $i: ${tasks[i - 1].submittedAt} > ${tasks[i].submittedAt}",
            )
        }
    }

    /**
     * Verify that every parameter of [task] and its workload fragments match [other].
     */
    private fun assertTasksMatch(
        task: ServiceTask,
        other: ServiceTask,
    ) {
        val id = task.id
        assertEquals(task.submittedAt, other.submittedAt, "submittedAt mismatch for task $id")
        assertEquals(task.duration, other.duration, "duration mismatch for task $id")
        assertEquals(task.cpuCoreCount, other.cpuCoreCount, "cpuCoreCount mismatch for task $id")
        assertEquals(task.cpuCapacity, other.cpuCapacity, "cpuCapacity mismatch for task $id")
        assertEquals(task.memorySize, other.memorySize, "memorySize mismatch for task $id")
        assertEquals(task.gpuCoreCount, other.gpuCoreCount, "gpuCoreCount mismatch for task $id")
        assertEquals(task.gpuCapacity, other.gpuCapacity, "gpuCapacity mismatch for task $id")
        assertEquals(task.gpuMemorySize, other.gpuMemorySize, "gpuMemorySize mismatch for task $id")
        assertEquals(task.deferrable, other.deferrable, "deferrable mismatch for task $id")
        assertEquals(task.deadline, other.deadline, "deadline mismatch for task $id")
        assertEquals(task.parents?.toList(), other.parents?.toList(), "parents mismatch for task $id")
        assertEquals(task.children?.toList(), other.children?.toList(), "children mismatch for task $id")

        assertFragmentsMatch(task.workload as TraceWorkload, other.workload as TraceWorkload, id)
    }

    /**
     * Verify that every fragment of [workload] matches [other], fragment for fragment.
     */
    private fun assertFragmentsMatch(
        workload: TraceWorkload,
        other: TraceWorkload,
        taskId: Int,
    ) {
        assertEquals(workload.length, other.length, "fragment count mismatch for task $taskId")

        for (i in 0 until workload.length) {
            val fragment = workload.getFragment(i)
            val otherFragment = other.getFragment(i)

            assertEquals(fragment.duration(), otherFragment.duration(), "fragment $i duration mismatch for task $taskId")
            assertEquals(fragment.cpuUsage(), otherFragment.cpuUsage(), "fragment $i cpuUsage mismatch for task $taskId")
            assertEquals(fragment.gpuUsage(), otherFragment.gpuUsage(), "fragment $i gpuUsage mismatch for task $taskId")
            assertEquals(
                fragment.gpuMemoryUsage(),
                otherFragment.gpuMemoryUsage(),
                "fragment $i gpuMemoryUsage mismatch for task $taskId",
            )
        }
    }
}
