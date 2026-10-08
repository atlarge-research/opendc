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

package org.opendc.sdk.runner.unit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.common.units.DataSize
import org.opendc.sdk.model.dsl.gib
import org.opendc.sdk.model.dsl.hours
import org.opendc.sdk.model.dsl.mhz
import org.opendc.sdk.model.dsl.mib
import org.opendc.sdk.model.dsl.minutes
import org.opendc.sdk.model.dsl.ms
import org.opendc.sdk.model.dsl.scenario
import org.opendc.sdk.model.dsl.topology
import org.opendc.sdk.model.experiment.ScenarioSpec
import org.opendc.sdk.model.failure.FailurePrefabSpec
import org.opendc.sdk.model.failure.PrefabFailureSpec
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.telemetry.ExportSpec
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.workload.InlineWorkloadSpec
import org.opendc.sdk.model.workload.TaskFragmentSpec
import org.opendc.sdk.model.workload.TaskSpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec
import org.opendc.sdk.runner.OpenDC
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.sdk.runner.telemetry.sink.CallbackSink
import org.opendc.sdk.runner.telemetry.table.task.TaskSample
import org.opendc.simulator.compute.task.TaskState
import java.nio.file.Path
import java.time.Duration

/**
 * Test suite for the sampling of tasks: at every change of their state, and every export interval while they run.
 */
class TaskSamplingTest {
    @Test
    fun `a task is sampled at its changes of state, and at every export interval while it runs`() {
        // The host fits one task at a time, so the second task waits until the first completes
        val task0 = task(id = 0)
        val task1 = task(id = 1)
        val samples =
            simulate(
                scenario {
                    topology(host(count = 1, coreCount = 1, memory = 1.gib))
                    workload(InlineWorkloadSpec(listOf(task0, task1)))
                    exportModel = ExportSpec(exportInterval = 1.minutes, printFrequency = null)
                },
            )

        val start = samples.first().timestamp

        fun minutes(sample: TaskSample): Double = Duration.between(start, sample.timestamp).toMillis() / 60_000.0
        val first = samples.filter { it.taskId == 0 }
        val second = samples.filter { it.taskId == 1 }

        assertAll(
            { assertEquals(listOf(TaskState.PROVISIONING, TaskState.RUNNING), first.take(2).map { it.taskState }) },
            {
                val intervalSamples = first.count { it.taskState == TaskState.RUNNING && minutes(it) in 0.5..9.5 }
                assertEquals(9, intervalSamples) { "a sample every minute it runs" }
            },
            { assertEquals(TaskState.COMPLETED, first.last().taskState) },
            { assertEquals(listOf(TaskState.PROVISIONING, TaskState.RUNNING), second.take(2).map { it.taskState }) },
            { assertEquals(0.0, minutes(second[0])) { "sampled when it is submitted" } },
            { assertEquals(10.0, minutes(second[1])) { "not sampled while it waits" } },
            { assertEquals(TaskState.COMPLETED, second.last().taskState) },
            { assertTrue(samples.none { it.taskState == TaskState.DELETED }) { "the deletion is not sampled" } },
        )
    }

    @Test
    fun `a task that fails is sampled when it fails and when it is scheduled again`() {
        val samples =
            simulate(
                scenario {
                    topology(host(count = 10, coreCount = 64, memory = 1024.gib))
                    workload(
                        TraceWorkloadSpec(
                            source = NamedReference("workloadTraces/bitbrains-small"),
                            submissionTime = "2022-02-01T00:00:00",
                        ),
                    )
                    exportModel = ExportSpec(exportInterval = 1.hours, printFrequency = null)
                    failureModel = PrefabFailureSpec(FailurePrefabSpec.G5k06Exp)
                },
            )

        val failures = samples.withIndex().filter { it.value.taskState == TaskState.FAILED }
        val next = failures.map { (i, failed) -> samples.drop(i + 1).first { it.taskId == failed.taskId } to failed }

        assertAll(
            { assertTrue(failures.isNotEmpty()) { "expected failures" } },
            {
                assertTrue(next.all { (after, failed) -> after.timestamp == failed.timestamp }) {
                    "the next state of a failed task should follow at the same moment"
                }
            },
            {
                assertTrue(next.all { (after, _) -> after.taskState in setOf(TaskState.PROVISIONING, TaskState.TERMINATED) }) {
                    "a failed task should be scheduled again or terminated: ${next.map { it.first.taskState }.toSet()}"
                }
            },
        )
    }

    private fun simulate(scenario: ScenarioSpec): List<TaskSample> {
        val samples = mutableListOf<TaskSample>()
        OpenDC.builder()
            .provisioner(FileSystemResourceProvisioner(testResourcesRoot))
            .sink(CallbackSink(onTask = { samples += it }))
            .build()
            .simulate(scenario)
        return samples
    }

    private fun host(
        count: Int,
        coreCount: Int,
        memory: DataSize,
    ): TopologySpec =
        topology {
            datacenter {
                cluster(name = "C01") {
                    host(count = count, name = "H01") {
                        cpu(coreCount = coreCount, coreSpeed = 2000.mhz)
                        memory(size = memory)
                    }
                }
            }
        }

    private fun task(id: Int): TaskSpec =
        TaskSpec(
            id = id,
            submissionTime = 0.ms,
            duration = (10 * 60 * 1000).ms,
            cpuCoreCount = 1,
            cpuCapacity = 1000.mhz,
            memory = 1024.mib,
            fragments = listOf(TaskFragmentSpec(duration = (10 * 60 * 1000).ms, cpuUsage = 1000.mhz)),
        )

    private companion object {
        val testResourcesRoot: Path = Path.of(object {}.javaClass.getResource("/workloadTraces")!!.toURI()).parent
    }
}
