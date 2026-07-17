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

package org.opendc.sdk.runner.demo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.sdk.model.dsl.experiment
import org.opendc.sdk.model.dsl.hours
import org.opendc.sdk.model.experiment.ExperimentSpec
import org.opendc.sdk.model.export.ExportSpec
import org.opendc.sdk.model.failure.NoFailureSpec
import org.opendc.sdk.model.failure.TraceBasedFailureSpec
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec
import org.opendc.sdk.runner.OpenDC
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.sdk.runner.sink.CollectedMetrics
import org.opendc.sdk.runner.sink.InMemorySink
import java.nio.file.Path
import kotlin.math.abs

/**
 * The four `opendc-demos` experiments recreated (from their `_answers` topologies) as SDK
 * [ExperimentSpec]s and executed end-to-end. Topologies are loaded as SDK-model JSON from this module's
 * own resources and every trace is a self-contained fixture — nothing depends on the ephemeral
 * `opendc-demos` directory. Each test asserts the concrete result that demonstrates the experiment's
 * pedagogical finding, computed from the strongly-typed metrics captured in memory.
 */
class DemoExperimentsTest {
    /** Demo 1 — a single datacenter runs the full workload to completion, drawing energy. */
    @Test
    fun `demo 1 first experiment runs the whole workload`() {
        val result =
            simulate(
                experiment {
                    name = "1.first_experiment"
                    topology(demoTopology("surfsara_linear_NL_279.json"))
                    workload(surfWeek)
                    exportModel(demoExport)
                },
            ).single()

        assertAll(
            { assertEquals(WORKLOAD_TASKS, result.tasksTotal) { "surf_week contains $WORKLOAD_TASKS tasks" } },
            { assertEquals(WORKLOAD_TASKS, result.tasksCompleted) { "all tasks complete when there are no failures" } },
            { assertEquals(0, result.tasksTerminated) { "no tasks are terminated without failures" } },
            { assertTrue(!result.anyDowntime) { "no host suffers downtime without failures" } },
            { assertEquals(1.21125541E10, result.energy, 1.0E6) { "the datacenter draws the expected energy" } },
        )
    }

    /** Demo 2 — the *same* datacenter in different locations draws identical energy but emits different carbon. */
    @Test
    fun `demo 2 datacenter location changes carbon not energy`() {
        val byLocation =
            simulate(
                experiment {
                    name = "2.datacenter_location"
                    listOf("AT", "AU", "BE", "NL").forEach { topology(demoTopology("surfsara_sqrt_$it.json")) }
                    workload(surfWeek)
                    exportModel(demoExport)
                },
            )
        val (at, au, be, nl) = byLocation

        assertAll(
            // Identical datacenter + workload => identical energy, whatever the location.
            {
                assertTrue(
                    byLocation.all {
                        abs(it.energy - at.energy) < 1.0
                    },
                ) { "energy is identical across locations: ${byLocation.map { it.energy }}" }
            },
            // The finding: the carbon footprint depends on where the datacenter is.
            { assertTrue(setOf(at.carbon, au.carbon, be.carbon, nl.carbon).size == 4) { "carbon differs per location" } },
            {
                assertTrue(
                    au.carbon > nl.carbon && nl.carbon > at.carbon && at.carbon > be.carbon,
                ) { "AU > NL > AT > BE in carbon emitted" }
            },
            { assertEquals(1845750.9, au.carbon, 100.0) { "Australia emits the most carbon" } },
            { assertEquals(816302.9, be.carbon, 100.0) { "Belgium emits the least carbon" } },
        )
    }

    /** Demo 3 — scaling the datacenter out lowers per-host utilization and raises total energy. */
    @Test
    fun `demo 3 horizontal scaling trades utilization for energy`() {
        val byHostCount =
            simulate(
                experiment {
                    name = "3.horizontal_scaling"
                    listOf(100, 150, 200, 279).forEach { topology(demoTopology("surfsara_linear_NL_$it.json")) }
                    workload(surfWeek)
                    exportModel(demoExport)
                },
            )

        assertAll(
            { assertTrue(byHostCount.all { it.tasksCompleted == WORKLOAD_TASKS }) { "every sizing completes the workload" } },
            {
                assertTrue(
                    byHostCount.map { it.energy }.zipWithNext().all {
                            (a, b) ->
                        b > a
                    },
                ) { "more hosts => more energy: ${byHostCount.map { it.energy }}" }
            },
            {
                assertTrue(
                    byHostCount.map { it.meanCpuUtilization }.zipWithNext().all {
                            (a, b) ->
                        b < a
                    },
                ) { "more hosts => lower utilization" }
            },
            { assertEquals(0.6164, byHostCount.first().meanCpuUtilization, 1.0E-3) { "100 hosts run near 62% utilization" } },
            { assertEquals(0.2664, byHostCount.last().meanCpuUtilization, 1.0E-3) { "279 hosts run near 27% utilization" } },
        )
    }

    /** Demo 4 — injecting failure traces terminates tasks and takes hosts down; the baseline does neither. */
    @Test
    fun `demo 4 failures terminate tasks and cause downtime`() {
        val results =
            simulate(
                experiment {
                    name = "4.failures"
                    topology(demoTopology("surfsara_linear_NL_279.json"))
                    workload(surfWeek)
                    failureModel(NoFailureSpec)
                    listOf("Facebook", "Instagram", "Netflix").forEach {
                        failureModel(TraceBasedFailureSpec(source = NamedReference("failure_traces/${it}_user_reported.parquet")))
                    }
                    exportModel(demoExport)
                },
            )
        val baseline = results.first()
        val underFailure = results.drop(1)

        assertAll(
            { assertEquals(WORKLOAD_TASKS, baseline.tasksCompleted) { "the failure-free baseline completes every task" } },
            { assertEquals(0, baseline.tasksTerminated) { "the baseline terminates nothing" } },
            { assertTrue(!baseline.anyDowntime) { "the baseline has no host downtime" } },
            {
                assertTrue(
                    underFailure.all { it.tasksTerminated > 0 },
                ) { "failures terminate tasks: ${underFailure.map { it.tasksTerminated }}" }
            },
            { assertTrue(underFailure.all { it.tasksCompleted < WORKLOAD_TASKS }) { "failures leave some tasks uncompleted" } },
            { assertTrue(underFailure.all { it.anyDowntime }) { "failures take hosts down" } },
            {
                assertTrue(
                    results.all { it.tasksCompleted + it.tasksTerminated == WORKLOAD_TASKS },
                ) { "every task either completes or is terminated" }
            },
        )
    }

    private fun simulate(design: ExperimentSpec): List<ScenarioSummary> {
        val report =
            OpenDC.builder()
                .provisioner(FileSystemResourceProvisioner(demoResourcesRoot))
                .sink(InMemorySink())
                .parallelism(1)
                .build()
                .simulate(design)
        return report.runs.map { ScenarioSummary(requireNotNull(it.metrics)) }
    }

    /** The demo-relevant aggregates of one scenario's captured metrics. */
    private class ScenarioSummary(metrics: CollectedMetrics) {
        val energy = metrics.powerSource.sumOf { it.energyUsage }
        val carbon = metrics.powerSource.sumOf { it.carbonEmission }
        val tasksTotal = metrics.service.maxOf { it.tasksTotal }
        val tasksCompleted = metrics.service.maxOf { it.tasksCompleted }
        val tasksTerminated = metrics.service.maxOf { it.tasksTerminated }
        val meanCpuUtilization = metrics.host.map { it.cpuUtilization }.average()
        val anyDowntime = metrics.host.any { it.downtime > 0 }
    }

    private fun demoTopology(name: String): TopologySpec {
        val text = checkNotNull(object {}.javaClass.getResourceAsStream("/demo/topologies/$name")).use { it.readBytes().decodeToString() }
        return SdkJson.json.decodeFromString(text)
    }

    private val surfWeek = TraceWorkloadSpec(source = NamedReference("workload_traces/surf_week"))

    private val demoExport = ExportSpec(exportInterval = 1.hours, printFrequency = null)

    private companion object {
        const val WORKLOAD_TASKS = 6295
        val demoResourcesRoot: Path = Path.of(object {}.javaClass.getResource("/demo")!!.toURI())
    }
}
