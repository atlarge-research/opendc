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

package org.opendc.sdk.runner.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.sdk.model.dsl.experiment
import org.opendc.sdk.model.dsl.gib
import org.opendc.sdk.model.dsl.hours
import org.opendc.sdk.model.dsl.mhz
import org.opendc.sdk.model.dsl.topology
import org.opendc.sdk.model.dsl.watts
import org.opendc.sdk.model.export.ExportSpec
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.topology.PowerModelType
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.workload.TraceWorkload
import org.opendc.sdk.runner.OpenDC
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.sdk.runner.sink.CollectedMetrics
import org.opendc.sdk.runner.sink.InMemorySink
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * End-to-end integration test of the trace-workload path against self-contained trace fixtures
 * (the small `bitbrains-small` bundle plus a carbon trace). Exercises what the ported base suite
 * cannot: loading a Parquet workload trace, a `sqrt` power model, carbon on a power source, a
 * multi-scenario cartesian sweep, the parquet output layout, and typed in-memory capture.
 */
class TraceWorkloadTest {
    @Test
    fun `runs a trace workload across scenarios and produces typed results and parquet`() {
        val design =
            experiment {
                name = "trace-integration"
                topology(datacenter(PowerModelType.LINEAR))
                topology(datacenter(PowerModelType.SQRT))
                workload(TraceWorkload(source = NamedReference("workloadTraces/bitbrains-small"), submissionTime = "2022-02-01T00:00:00"))
                exportModel(ExportSpec(exportInterval = 1.hours, printFrequency = null))
            }

        val output = Files.createTempDirectory("opendc-trace-it")
        val report =
            OpenDC.builder()
                .provisioner(FileSystemResourceProvisioner(testResourcesRoot))
                .output(output)
                .sink(InMemorySink())
                .build()
                .simulate(design)

        val runs = report.runs
        assertAll(
            { assert(report.scenarios.size == 2) { "expected 2 scenarios, got ${report.scenarios.size}" } },
            { assert(runs.size == 2) { "expected 2 runs, got ${runs.size}" } },
            *runs.map { run -> { assertRunProducedOutput(run.outputPath, run.metrics) } }.toTypedArray(),
        )
    }

    private fun assertRunProducedOutput(
        outputPath: Path?,
        metrics: CollectedMetrics?,
    ) {
        val dir = requireNotNull(outputPath) { "run produced no parquet output" }
        listOf("host", "powerSource", "service", "task").forEach { file ->
            assert(dir.resolve("$file.parquet").exists()) { "missing $file.parquet in $dir" }
        }
        val captured = requireNotNull(metrics) { "InMemorySink captured no metrics" }
        assert(captured.host.isNotEmpty()) { "no host samples captured" }
        assert(captured.service.isNotEmpty()) { "no service samples captured" }
        assert(captured.powerSource.sumOf { it.energyUsage } > 0.0) { "expected non-zero energy usage" }
    }

    private fun datacenter(powerModel: PowerModelType): TopologySpec =
        topology {
            cluster(name = "C01") {
                host(count = 1, name = "H01") {
                    cpu(coreCount = 64, coreSpeed = 2000.mhz)
                    memory(size = 1024.gib)
                    power {
                        type = powerModel
                        power = 400.watts
                        idlePower = 100.watts
                        maxPower = 200.watts
                    }
                }
                powerSource(carbon = NamedReference("carbonTraces/2022-01-01_2022-12-31_NL.parquet"))
            }
        }

    private companion object {
        val testResourcesRoot: Path = Path.of(object {}.javaClass.getResource("/workloadTraces")!!.toURI()).parent
    }
}
