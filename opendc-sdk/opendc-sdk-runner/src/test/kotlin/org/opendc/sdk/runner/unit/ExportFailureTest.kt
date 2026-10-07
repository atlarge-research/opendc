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
import org.junit.jupiter.api.assertThrows
import org.opendc.sdk.model.dsl.experiment
import org.opendc.sdk.model.dsl.gib
import org.opendc.sdk.model.dsl.mhz
import org.opendc.sdk.model.dsl.mib
import org.opendc.sdk.model.dsl.minutes
import org.opendc.sdk.model.dsl.ms
import org.opendc.sdk.model.dsl.topology
import org.opendc.sdk.model.telemetry.ExportSpec
import org.opendc.sdk.model.telemetry.OutputFileSpec
import org.opendc.sdk.model.workload.InlineWorkloadSpec
import org.opendc.sdk.model.workload.TaskFragmentSpec
import org.opendc.sdk.model.workload.TaskSpec
import org.opendc.sdk.runner.OpenDC
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.sdk.runner.telemetry.MetricExporter
import org.opendc.sdk.runner.telemetry.sink.OutputSink
import org.opendc.sdk.runner.telemetry.sink.RunContext
import org.opendc.sdk.runner.telemetry.sink.SinkResult
import org.opendc.sdk.runner.telemetry.sink.SinkSession
import org.opendc.sdk.runner.telemetry.table.host.HostSample
import java.nio.file.Files

/**
 * Test suite for runs whose metrics cannot all be exported, which fail.
 */
class ExportFailureTest {
    @Test
    fun `an export that fails during the run fails the run`() {
        val sink = FailingSink(failingExport = 3)

        val error = assertThrows<Exception> { simulate(sink) }

        assertAll(
            { assertFailedRun(error) },
            { assertEquals(3, sink.exports) { "nothing should be exported after the failure" } },
            { assertTrue(sink.isClosed) { "the exporter should be closed" } },
        )
    }

    @Test
    fun `an exporter that fails to close fails the run`() {
        val sink = FailingSink(failClose = true)

        val error = assertThrows<Exception> { simulate(sink) }

        assertFailedRun(error)
    }

    private fun assertFailedRun(error: Throwable) {
        val causes = generateSequence(error) { it.cause }.toList()
        assertAll(
            { assertTrue(causes.any { it.message == "Exporting the metrics of the run failed" }) { "unexpected error: $error" } },
            { assertTrue(causes.any { it.message == "Test failure" }) { "the cause should be the failure of the exporter" } },
        )
    }

    private fun simulate(sink: OutputSink) {
        val datacenter =
            topology {
                datacenter {
                    cluster(name = "C01") {
                        host(name = "H01") {
                            cpu(coreCount = 1, coreSpeed = 2000.mhz)
                            memory(size = 1.gib)
                        }
                    }
                }
            }
        val task =
            TaskSpec(
                id = 0,
                submissionTime = 0.ms,
                duration = (10 * 60 * 1000).ms,
                cpuCoreCount = 1,
                cpuCapacity = 1000.mhz,
                memory = 0.mib,
                fragments = listOf(TaskFragmentSpec(duration = (10 * 60 * 1000).ms, cpuUsage = 1000.mhz)),
            )
        val design =
            experiment {
                name = "export-failure"
                topology(datacenter)
                workload(InlineWorkloadSpec(listOf(task)))
                exportModel(ExportSpec(exportInterval = 1.minutes, printFrequency = null))
            }

        OpenDC.builder()
            .provisioner(FileSystemResourceProvisioner(Files.createTempDirectory("export-failure")))
            .sink(sink)
            .parallelism(1)
            .build()
            .simulate(design)
    }

    /**
     * A sink of host samples that fails at the [failingExport]th export, or when it is closed.
     */
    private class FailingSink(private val failingExport: Int? = null, private val failClose: Boolean = false) : OutputSink {
        var exports = 0
        var isClosed = false

        override fun open(context: RunContext): SinkSession =
            object : SinkSession {
                override val monitor: MetricExporter =
                    object : MetricExporter, AutoCloseable {
                        override fun export(reader: HostSample) {
                            exports++
                            if (exports == failingExport) {
                                throw IllegalArgumentException("Test failure")
                            }
                        }

                        override fun close() {
                            isClosed = true
                            if (failClose) {
                                throw IllegalArgumentException("Test failure")
                            }
                        }
                    }

                override val tables: Set<OutputFileSpec> = setOf(OutputFileSpec.HOST)

                override fun result(): SinkResult? = null
            }
    }
}
