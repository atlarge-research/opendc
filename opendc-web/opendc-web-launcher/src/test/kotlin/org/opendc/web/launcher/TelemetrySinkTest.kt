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

package org.opendc.web.launcher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.common.units.TimeDelta
import org.opendc.sdk.model.dsl.experiment
import org.opendc.sdk.model.dsl.gib
import org.opendc.sdk.model.dsl.mhz
import org.opendc.sdk.model.dsl.mib
import org.opendc.sdk.model.dsl.minutes
import org.opendc.sdk.model.dsl.ms
import org.opendc.sdk.model.dsl.seconds
import org.opendc.sdk.model.dsl.topology
import org.opendc.sdk.model.dsl.watts
import org.opendc.sdk.model.experiment.ExperimentSpec
import org.opendc.sdk.model.telemetry.ExportSpec
import org.opendc.sdk.model.topology.LinearPowerModelSpec
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.workload.InlineWorkloadSpec
import org.opendc.sdk.model.workload.TaskFragmentSpec
import org.opendc.sdk.model.workload.TaskSpec
import org.opendc.sdk.runner.OpenDC
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.sdk.runner.telemetry.sink.CallbackSink
import org.opendc.sdk.runner.telemetry.sink.OutputSink
import java.nio.file.Files
import kotlin.math.abs

private const val RUN_MS = 10 * 60 * 1000L

/**
 * What a launcher reports is what a progress bar and a live chart are drawn from, so the numbers have
 * to survive two foldings: many hosts into one line, and a run far longer than a chart into a bounded
 * number of points. Getting either wrong misreports the run rather than merely blurring it.
 */
class TelemetrySinkTest {
    @Test
    fun `folds the hosts reporting at one instant into a single reading`() {
        val telemetry = TelemetrySink()
        val perHost = mutableMapOf<Long, MutableMap<Int, Double>>()

        simulate(hosts = 3, exportInterval = 1.minutes, sinks = listOf(telemetry, powerPerHost(perHost)))

        val reported = seriesOf(telemetry, ResultMetric.HOST_POWER_DRAW)
        assertTrue(perHost.values.any { it.size == 3 }) { "the run has to report three hosts at an instant for this to mean anything" }
        for (point in reported) {
            val expected = perHost.getValue(point.t).values.sum()
            assertTrue(abs(point.value - expected) < TOLERANCE) { "fleet power at ${point.t} was ${point.value}, not the hosts' $expected" }
        }
    }

    // The engine reports the closing instant twice, once on the export interval and once the moment
    // the last task is removed. Counting the second report as more hosts would end every run at
    // double its fleet.
    @Test
    fun `counts a host reported twice at one instant once`() {
        val telemetry = TelemetrySink()
        val perHost = mutableMapOf<Long, MutableMap<Int, Double>>()
        val rows = mutableMapOf<Long, Int>()

        simulate(hosts = 2, exportInterval = 1.minutes, sinks = listOf(telemetry, powerPerHost(perHost), countingRows(rows)))

        val doubled = rows.filterValues { it > perHost.getValue(rows.keys.first()).size }.keys
        assertTrue(doubled.isNotEmpty()) { "no instant was reported twice, so this proves nothing: $rows" }
        val reported = seriesOf(telemetry, ResultMetric.HOST_POWER_DRAW).associate { it.t to it.value }
        for (instant in doubled) {
            val expected = perHost.getValue(instant).values.sum()
            val actual = reported.getValue(instant)
            assertTrue(abs(actual - expected) < TOLERANCE) { "fleet power at $instant was $actual, which is not the two hosts' $expected" }
        }
    }

    @Test
    fun `averages a share rather than adding it up, so utilization stays a share`() {
        val telemetry = TelemetrySink()

        simulate(hosts = 4, exportInterval = 1.minutes, sinks = listOf(telemetry))

        val utilization = seriesOf(telemetry, ResultMetric.HOST_CPU_UTILIZATION)
        assertTrue(utilization.isNotEmpty()) { "the run reported no utilization at all" }
        assertTrue(utilization.all { it.value in 0.0..1.0 }) { "a share of capacity left its range: ${utilization.map { it.value }}" }
    }

    // A month-long trace exported every five simulated minutes is thousands of samples that can
    // arrive in a burst. Merging them is what keeps a report a fixed size, and merging a running
    // total the way a gauge is merged would report a fraction of the work as the total.
    @Test
    fun `keeps a running total right after the points behind it have been merged`() {
        val telemetry = TelemetrySink()

        simulate(hosts = 1, exportInterval = 1.seconds, sinks = listOf(telemetry))

        val completed = seriesOf(telemetry, ResultMetric.SERVICE_TASKS_COMPLETED)
        assertTrue(completed.size <= SAMPLE_CAP) { "${completed.size} points survived a cap of $SAMPLE_CAP" }
        assertTrue(completed.size < RUN_MS / 1000) { "the run produced more samples than points, so something must have been merged" }
        assertEquals(TASK_COUNT.toDouble(), completed.last().value) { "the last point of a running total is the total" }
        assertTrue(completed.zipWithNext().all { (before, after) -> after.value >= before.value }) { "a running total went backwards" }
    }

    @Test
    fun `counts every task as done once the run has finished`() {
        val telemetry = TelemetrySink()

        simulate(hosts = 2, exportInterval = 1.minutes, sinks = listOf(telemetry))

        assertEquals(TASK_COUNT, telemetry.report().runs.single().completedTasks)
    }

    @Test
    fun `says nothing about a table the topology has none of`() {
        val telemetry = TelemetrySink()

        simulate(hosts = 1, exportInterval = 1.minutes, sinks = listOf(telemetry))

        val reported = telemetry.report().runs.single().series.map { it.metric }
        assertTrue(ResultMetric.BATTERY_CHARGE.id !in reported) { "a topology with no battery reported a battery charge" }
    }

    private fun seriesOf(
        sink: TelemetrySink,
        metric: ResultMetric,
    ): List<MetricPoint> = sink.report().runs.single().series.single { it.metric == metric.id }.points

    /** A sink that keeps each host's last power draw per instant, which is what the fold reads. */
    private fun powerPerHost(into: MutableMap<Long, MutableMap<Int, Double>>) =
        CallbackSink(
            onHost = { reader -> into.getOrPut(reader.timestamp.toEpochMilli()) { mutableMapOf() }[reader.hostId] = reader.powerDraw },
        )

    /** A sink that counts host rows per instant, so a test can tell which instants were reported twice. */
    private fun countingRows(into: MutableMap<Long, Int>) =
        CallbackSink(onHost = { reader -> into.merge(reader.timestamp.toEpochMilli(), 1, Int::plus) })

    private fun simulate(
        hosts: Int,
        exportInterval: TimeDelta,
        sinks: List<OutputSink>,
    ) {
        val runner = OpenDC.builder().provisioner(FileSystemResourceProvisioner(Files.createTempDirectory("telemetry")))
        sinks.forEach { runner.sink(it) }
        runner.parallelism(1).build().simulate(design(hosts, exportInterval))
    }

    private fun design(
        hosts: Int,
        exportInterval: TimeDelta,
    ): ExperimentSpec =
        experiment {
            name = "telemetry"
            topology(fleet(hosts))
            workload(InlineWorkloadSpec(List(TASK_COUNT) { task(it) }))
            exportModel(ExportSpec(exportInterval = exportInterval, printFrequency = null))
        }

    private fun fleet(hosts: Int): TopologySpec =
        topology {
            datacenter {
                cluster(name = "C01") {
                    repeat(hosts) { index ->
                        host(name = "H0$index") {
                            cpu(coreCount = 2, coreSpeed = 2000.mhz)
                            memory(size = 1.gib)
                            cpuPowerModel = LinearPowerModelSpec(maxPower = 200.watts, idlePower = 100.watts)
                        }
                    }
                }
            }
        }

    private fun task(id: Int): TaskSpec =
        TaskSpec(
            id = id,
            submissionTime = 0.ms,
            duration = RUN_MS.ms,
            cpuCoreCount = 1,
            cpuCapacity = 1000.mhz,
            memory = 0.mib,
            fragments = listOf(TaskFragmentSpec(duration = RUN_MS.toInt().ms, cpuUsage = 1000.mhz)),
        )

    private companion object {
        /** How many tasks every run in this test simulates, and so what a finished one has completed. */
        const val TASK_COUNT = 2

        /** Mirrors the cap the sink applies. A test that guessed a different one would prove nothing. */
        const val SAMPLE_CAP = 512

        /** Power is a double summed over hosts, so exact equality would be a test of floating point. */
        const val TOLERANCE = 1e-6
    }
}
