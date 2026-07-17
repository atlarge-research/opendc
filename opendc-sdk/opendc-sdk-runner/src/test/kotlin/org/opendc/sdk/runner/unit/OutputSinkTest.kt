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

package org.opendc.sdk.runner.unit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.sdk.model.dsl.experiment
import org.opendc.sdk.model.dsl.gib
import org.opendc.sdk.model.dsl.mhz
import org.opendc.sdk.model.dsl.mib
import org.opendc.sdk.model.dsl.minutes
import org.opendc.sdk.model.dsl.ms
import org.opendc.sdk.model.dsl.topology
import org.opendc.sdk.model.dsl.watts
import org.opendc.sdk.model.export.ExportSpec
import org.opendc.sdk.model.export.OutputFileSpec
import org.opendc.sdk.model.topology.PowerModelType
import org.opendc.sdk.model.workload.InlineWorkloadSpec
import org.opendc.sdk.model.workload.TaskFragmentSpec
import org.opendc.sdk.model.workload.TaskSpec
import org.opendc.sdk.runner.OpenDC
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.sdk.runner.sink.CallbackSink
import org.opendc.sdk.runner.sink.InMemorySink
import java.nio.file.Files

/**
 * Validates the composable output-sink pattern: multiple sinks observe the same run (fan-out) and
 * an [InMemorySink] captures exactly the tables it was configured for (granular selection).
 */
class OutputSinkTest {
    @Test
    fun `sinks compose and honour granular table selection`() {
        val datacenter =
            topology {
                cluster(name = "C01") {
                    host(name = "H01") {
                        cpu(coreCount = 1, coreSpeed = 2000.mhz)
                        memory(size = 1.gib)
                        power {
                            type = PowerModelType.LINEAR
                            maxPower = 200.watts
                            idlePower = 100.watts
                        }
                    }
                }
            }
        val task =
            TaskSpec(
                id = 0,
                name = "t0",
                submissionTime = 0.ms,
                duration = (10 * 60 * 1000).ms,
                cpuCoreCount = 1,
                cpuCapacity = 1000.mhz,
                memory = 0.mib,
                fragments = listOf(TaskFragmentSpec(duration = (10 * 60 * 1000).ms, cpuUsage = 1000.mhz)),
            )
        val design =
            experiment {
                name = "sink-test"
                topology(datacenter)
                workload(InlineWorkloadSpec(listOf(task)))
                exportModel(ExportSpec(exportInterval = 1.minutes, printFrequency = null))
            }

        var hostCallbacks = 0
        val report =
            OpenDC.builder()
                .provisioner(FileSystemResourceProvisioner(Files.createTempDirectory("sink")))
                .sink(InMemorySink(tables = setOf(OutputFileSpec.HOST, OutputFileSpec.SERVICE)))
                .sink(CallbackSink(onHost = { hostCallbacks++ }))
                .parallelism(1)
                .build()
                .simulate(design)

        val metrics = requireNotNull(report.runs.single().metrics)
        assertAll(
            { assertTrue(metrics.host.isNotEmpty()) { "the selected host table is captured" } },
            { assertTrue(metrics.service.isNotEmpty()) { "the selected service table is captured" } },
            { assertTrue(metrics.task.isEmpty()) { "the unselected task table is not captured" } },
            { assertTrue(metrics.powerSource.isEmpty()) { "the unselected power-source table is not captured" } },
            { assertTrue(hostCallbacks > 0) { "the callback sink observed the same run (fan-out)" } },
            { assertEquals(hostCallbacks, metrics.host.size) { "both sinks saw the same host records" } },
        )
    }
}
