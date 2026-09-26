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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.opendc.sdk.model.checkpoint.CheckpointSpec
import org.opendc.sdk.model.dsl.experiment
import org.opendc.sdk.model.dsl.gib
import org.opendc.sdk.model.dsl.hours
import org.opendc.sdk.model.dsl.mhz
import org.opendc.sdk.model.dsl.scenario
import org.opendc.sdk.model.dsl.timeShiftScheduler
import org.opendc.sdk.model.dsl.topology
import org.opendc.sdk.model.dsl.watts
import org.opendc.sdk.model.experiment.ScenarioSpec
import org.opendc.sdk.model.failure.FailureModelSpec
import org.opendc.sdk.model.failure.FailurePrefabSpec
import org.opendc.sdk.model.failure.PrefabFailureSpec
import org.opendc.sdk.model.failure.TraceBasedFailureSpec
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.scheduler.TaskStopperSpec
import org.opendc.sdk.model.telemetry.ExportSpec
import org.opendc.sdk.model.topology.PowerModelType
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec
import org.opendc.sdk.runner.OpenDC
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.sdk.runner.telemetry.sink.CollectedMetrics
import org.opendc.sdk.runner.telemetry.sink.InMemorySink
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
                workload(
                    TraceWorkloadSpec(source = NamedReference("workloadTraces/bitbrains-small"), submissionTime = "2022-02-01T00:00:00"),
                )
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

    /**
     * Checkpointing snapshots the running tasks. The CPU-only trace has no GPU arrays, which the snapshot must handle.
     */
    @Test
    fun `checkpoints a CPU-only trace workload`() {
        val metrics =
            simulate(
                scenario {
                    topology(datacenter(PowerModelType.LINEAR))
                    workload(traceWorkload("bitbrains-small"))
                    exportModel = ExportSpec(exportInterval = 1.hours, printFrequency = null)
                    checkpointModel = CheckpointSpec()
                },
            )

        val lastTaskSamples = metrics.task.groupBy { it.taskId }.values.map { it.last() }
        assertAll(
            { assertEquals(50, metrics.service.last().tasksCompleted) { "all tasks should complete" } },
            { assert(lastTaskSamples.sumOf { it.checkpointDelay } > 0) { "expected checkpoints to be made" } },
        )
    }

    /**
     * The task stopper pauses the running tasks when the carbon intensity is high, and reschedules them from a snapshot.
     */
    @ParameterizedTest
    @ValueSource(strings = ["bitbrains-small", "small_gpu"])
    fun `task stopper pauses a trace workload`(trace: String) {
        val metrics =
            simulate(
                scenario {
                    topology(datacenter(PowerModelType.LINEAR, gpu = trace == "small_gpu"))
                    workload(traceWorkload(trace, deferAll = true))
                    allocationPolicy(timeShiftScheduler { taskStopper = TaskStopperSpec(forecast = false) })
                    exportModel = ExportSpec(exportInterval = 1.hours, printFrequency = null)
                },
            )

        val lastService = metrics.service.last()
        val lastTaskSamples = metrics.task.groupBy { it.taskId }.values.map { it.last() }
        assertAll(
            { assertEquals(lastService.tasksTotal, lastService.tasksCompleted) { "all tasks should complete" } },
            { assert(lastTaskSamples.sumOf { it.numPauses } > 0) { "expected tasks to be paused" } },
        )
    }

    /**
     * Two runs with the same seed should produce the same failures, and thus the same results.
     */
    @Test
    fun `failures are reproducible for a fixed seed`() {
        val failureModels =
            listOf(
                PrefabFailureSpec(FailurePrefabSpec.G5k06Exp),
                TraceBasedFailureSpec(source = NamedReference("demo/failure_traces/Facebook_user_reported.parquet")),
            )

        assertAll(
            failureModels.map { failureModel ->
                {
                    val first = simulateWithFailures(failureModel)
                    val second = simulateWithFailures(failureModel)
                    assert(first.task.sumOf { it.numFailures } > 0) { "expected failures with $failureModel" }
                    assertEquals(first.service, second.service) { "service metrics differ between runs with $failureModel" }
                    assertEquals(first.task, second.task) { "task metrics differ between runs with $failureModel" }
                }
            },
        )
    }

    private fun simulateWithFailures(failureModel: FailureModelSpec): CollectedMetrics =
        simulate(
            scenario {
                topology(datacenter(PowerModelType.LINEAR, hostCount = 10))
                workload(traceWorkload("bitbrains-small"))
                exportModel = ExportSpec(exportInterval = 1.hours, printFrequency = null)
                this.failureModel = failureModel
            },
        )

    private fun traceWorkload(
        trace: String,
        deferAll: Boolean = false,
    ): TraceWorkloadSpec =
        TraceWorkloadSpec(source = NamedReference("workloadTraces/$trace"), submissionTime = "2022-02-01T00:00:00", deferAll = deferAll)

    private fun simulate(scenario: ScenarioSpec): CollectedMetrics {
        val report =
            OpenDC.builder()
                .provisioner(FileSystemResourceProvisioner(testResourcesRoot))
                .sink(InMemorySink())
                .build()
                .simulate(scenario)
        return requireNotNull(report.runs.single().metrics)
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

    private fun datacenter(
        powerModel: PowerModelType,
        hostCount: Int = 1,
        gpu: Boolean = false,
    ): TopologySpec =
        topology {
            datacenter {
                cluster(name = "C01") {
                    host(count = hostCount, name = "H01") {
                        cpu(coreCount = 64, coreSpeed = 2000.mhz)
                        memory(size = 1024.gib)
                        if (gpu) gpu(coreCount = 2, coreSpeed = 2000.mhz)
                        power {
                            type = powerModel
                            power = 400.watts
                            idlePower = 100.watts
                            maxPower = 200.watts
                        }
                    }
                }
                powerSource(carbon = NamedReference("carbonTraces/2022-01-01_2022-12-31_NL.parquet"))
            }
        }

    private companion object {
        val testResourcesRoot: Path = Path.of(object {}.javaClass.getResource("/workloadTraces")!!.toURI()).parent
    }
}
