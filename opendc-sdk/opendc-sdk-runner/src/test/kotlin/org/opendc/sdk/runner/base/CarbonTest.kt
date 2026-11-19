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

package org.opendc.sdk.runner.base

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.opendc.sdk.model.workload.TaskSpec
import org.opendc.sdk.runner.base.harness.FULL_LOAD_POWER
import org.opendc.sdk.runner.base.harness.HALF_LOAD_POWER
import org.opendc.sdk.runner.base.harness.IDLE_POWER
import org.opendc.sdk.runner.base.harness.MINUTE
import org.opendc.sdk.runner.base.harness.SECOND
import org.opendc.sdk.runner.base.harness.TestMetricExporter
import org.opendc.sdk.runner.base.harness.carbonEmission
import org.opendc.sdk.runner.base.harness.carbonTopology
import org.opendc.sdk.runner.base.harness.carbonTrace
import org.opendc.sdk.runner.base.harness.createTestTask
import org.opendc.sdk.runner.base.harness.energy
import org.opendc.sdk.runner.base.harness.fragment
import org.opendc.sdk.runner.base.harness.runTest
import org.opendc.sdk.runner.base.harness.singleClusterTopology
import org.opendc.sdk.runner.base.harness.singleFailure
import java.time.Instant

/**
 * Integration tests for the carbon emissions of a power source.
 *
 * Every test runs on a single host with one 2 GHz core ([singleClusterTopology]), unless stated otherwise, whose power
 * source follows an inline carbon trace ([carbonTrace]). The carbon emission of the power source is the energy it
 * supplies times the carbon intensity at that moment ([carbonEmission]).
 * Like the energy usage, the carbon emission is reported cumulatively, so its last sample holds the total. A sample is
 * taken every minute, so sample i is taken at min i + 1, and reports the carbon intensity that holds at that moment.
 * The power source is sampled once more when the simulation ends, so a 10 min task gives 11 samples, the last two of
 * which are both taken at min 10.
 */
class CarbonTest {
    /**
     * A 10 min task runs at half load with a constant carbon intensity.
     * The total carbon emission is the total energy usage times the carbon intensity.
     */
    @Test
    fun `the carbon emission is the energy usage times the carbon intensity`() {
        val monitor = runTest(carbonTopology(CONSTANT_CARBON), listOf(task(10 * MINUTE)))

        val oneMinuteEmission = carbonEmission(energy(HALF_LOAD_POWER, MINUTE), CONSTANT_INTENSITY)
        val energyUsage = energy(HALF_LOAD_POWER, 10 * MINUTE)
        val totalEmission = carbonEmission(energyUsage, CONSTANT_INTENSITY)

        assertAll(
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertEquals(listOf(CONSTANT_INTENSITY), monitor.carbonIntensities.distinct()) { "Carbon intensities incorrect" } },
            { assertEquals(oneMinuteEmission, monitor.carbonEmissions[0], 1e-6) { "Carbon emission at sample 0 incorrect" } },
            { assertEquals(totalEmission, monitor.carbonEmissions.last(), 1e-6) { "Total carbon emission incorrect" } },
        )
    }

    /**
     * A 40 min task alternates every 10 min between 1000 MHz (half load) and 2000 MHz (full load). It runs once with
     * [CONSTANT_CARBON], and once with [HIGH_CARBON], whose carbon intensity is three times as high.
     * The carbon trace only affects the carbon emission, so both runs use the same energy, and the second run emits three
     * times as much carbon.
     */
    @Test
    fun `the energy usage does not depend on the carbon trace`() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * MINUTE, 1000.0),
                            fragment(10 * MINUTE, 2000.0),
                            fragment(10 * MINUTE, 1000.0),
                            fragment(10 * MINUTE, 2000.0),
                        ),
                ),
            )

        val monitorConstant = runTest(carbonTopology(CONSTANT_CARBON), workload)
        val monitorHigh = runTest(carbonTopology(HIGH_CARBON), workload)

        val energyUsage = energy(HALF_LOAD_POWER, 20 * MINUTE) + energy(FULL_LOAD_POWER, 20 * MINUTE)

        assertAll(
            { assertEquals(energyUsage, monitorConstant.energyUsages.last()) { "Total energy usage with constant trace incorrect" } },
            { assertEquals(energyUsage, monitorHigh.energyUsages.last()) { "Total energy usage with high trace incorrect" } },
            {
                assertEquals(carbonEmission(energyUsage, CONSTANT_INTENSITY), monitorConstant.carbonEmissions.last(), 1e-6) {
                    "Total carbon emission with constant trace incorrect"
                }
            },
            {
                assertEquals(carbonEmission(energyUsage, HIGH_INTENSITY), monitorHigh.carbonEmissions.last(), 1e-6) {
                    "Total carbon emission with high trace incorrect"
                }
            },
        )
    }

    /**
     * A 60 min task runs at half load with a carbon trace whose carbon intensity changes every [granularity] min, and
     * rises by 20 gCO2/kWh at every change.
     * Carbon traces come in different granularities, such as the 15 min of the real carbon traces in the test resources.
     * The carbon emission of each interval is the energy used in that interval times its carbon intensity.
     */
    @ParameterizedTest(name = "{0} min")
    @ValueSource(ints = [10, 15])
    fun `the carbon emission follows carbon traces of different granularities`(granularity: Int) {
        val intervals = 60 / granularity
        val intensities = List(intervals) { 100.0 + 20.0 * it }
        val carbon = carbonTrace(*intensities.mapIndexed { i, intensity -> i * granularity * MINUTE to intensity }.toTypedArray())

        val monitor = runTest(carbonTopology(carbon), listOf(task(60 * MINUTE)))

        val energyUsage = energy(HALF_LOAD_POWER, 60 * MINUTE)

        // The intensity changes at every multiple of the granularity. The last intensity holds until the end of the task, at
        // min 60, where the last two samples are taken.
        val sampleMinutes = (1..60) + 60
        val expectedIntensities = sampleMinutes.map { minute -> intensities[minOf(minute / granularity, intervals - 1)] }

        // The total carbon emission at the end of each interval
        val intervalEnergyUsage = energy(HALF_LOAD_POWER, granularity * MINUTE)
        val samples = List(intervals) { (it + 1) * granularity - 1 }
        val expectedEmissions = intensities.map { carbonEmission(intervalEnergyUsage, it) }.runningReduce(Double::plus)

        assertAll(
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertEquals(expectedIntensities, monitor.carbonIntensities) { "Carbon intensities incorrect" } },
            { assertCarbonEmissions(monitor, samples, expectedEmissions) },
            { assertEquals(expectedEmissions.last(), monitor.carbonEmissions.last(), 1e-6) { "Total carbon emission incorrect" } },
        )
    }

    /**
     * A 20 min task runs at half load with a carbon trace whose intervals differ in length: the carbon intensity changes
     * at min 5, 7 and 16, so its intervals last 5, 2, 9 and 4 min.
     */
    @Test
    fun `the carbon emission follows a carbon trace with intervals of different lengths`() {
        val carbon = carbonTrace(0L to 100.0, 5 * MINUTE to 150.0, 7 * MINUTE to 80.0, 16 * MINUTE to 120.0)

        val monitor = runTest(carbonTopology(carbon), listOf(task(20 * MINUTE)))

        // The intensity changes at samples 4, 6 and 15 (min 5, 7 and 16). The last two samples are both taken at min 20.
        val expectedIntensities = List(4) { 100.0 } + List(2) { 150.0 } + List(9) { 80.0 } + List(6) { 120.0 }

        // The total carbon emission at the end of each interval (min 5, 7, 16 and 20)
        val samples = listOf(4, 6, 15, 19)
        val expectedEmissions =
            listOf(5 to 100.0, 2 to 150.0, 9 to 80.0, 4 to 120.0)
                .map { (minutes, intensity) -> carbonEmission(energy(HALF_LOAD_POWER, minutes * MINUTE), intensity) }
                .runningReduce(Double::plus)

        assertAll(
            { assertEquals(expectedIntensities, monitor.carbonIntensities) { "Carbon intensities incorrect" } },
            { assertCarbonEmissions(monitor, samples, expectedEmissions) },
            { assertEquals(expectedEmissions.last(), monitor.carbonEmissions.last(), 1e-6) { "Total carbon emission incorrect" } },
        )
    }

    /**
     * A 10 min task runs at half load, and the carbon intensity rises from 100 to 200 gCO2/kWh at 90 s, between the first
     * two samples.
     * The power source should switch to the new carbon intensity at the moment it changes, not at the next sample, so the
     * carbon emitted between the first two samples is split at 90 s.
     */
    @Test
    fun `a carbon intensity change between two samples applies from the moment it occurs`() {
        val carbon = carbonTrace(0L to 100.0, 90 * SECOND to 200.0)

        val monitor = runTest(carbonTopology(carbon), listOf(task(10 * MINUTE)))

        val samples = listOf(0, 1)
        val expectedIntensities = listOf(100.0, 200.0)
        val expectedEmissions =
            listOf(
                carbonEmission(energy(HALF_LOAD_POWER, 1 * MINUTE), 100.0),
                carbonEmission(energy(HALF_LOAD_POWER, 90 * SECOND), 100.0) + carbonEmission(energy(HALF_LOAD_POWER, 30 * SECOND), 200.0),
            )
        val totalEmission =
            carbonEmission(energy(HALF_LOAD_POWER, 90 * SECOND), 100.0) +
                carbonEmission(energy(HALF_LOAD_POWER, 10 * MINUTE - 90 * SECOND), 200.0)

        assertAll(
            { assertEquals(expectedIntensities, samples.map { monitor.carbonIntensities[it] }) { "Carbon intensities incorrect" } },
            { assertCarbonEmissions(monitor, samples, expectedEmissions) },
            { assertEquals(totalEmission, monitor.carbonEmissions.last(), 1e-6) { "Total carbon emission incorrect" } },
        )
    }

    /**
     * A 15 min task runs at half load with a carbon trace whose first carbon intensity starts at min 5, after the
     * simulation has started, and changes at min 10.
     * The first carbon intensity of a trace also holds before its timestamp, so it holds from the start of the
     * simulation until min 10.
     */
    @Test
    fun `the first carbon intensity of a trace also holds before it starts`() {
        val carbon = carbonTrace(5 * MINUTE to 100.0, 10 * MINUTE to 200.0)

        val monitor = runTest(carbonTopology(carbon), listOf(task(15 * MINUTE)))

        // The intensity changes at sample 9 (min 10). The last two samples are both taken at min 15.
        val expectedIntensities = List(9) { 100.0 } + List(7) { 200.0 }
        val totalEmission =
            carbonEmission(energy(HALF_LOAD_POWER, 10 * MINUTE), 100.0) + carbonEmission(energy(HALF_LOAD_POWER, 5 * MINUTE), 200.0)

        assertAll(
            { assertEquals(expectedIntensities, monitor.carbonIntensities) { "Carbon intensities incorrect" } },
            { assertEquals(totalEmission, monitor.carbonEmissions.last(), 1e-6) { "Total carbon emission incorrect" } },
        )
    }

    /**
     * A 10 min task is submitted at 2022-01-01T00:00, so the simulation starts then instead of at the Unix epoch.
     * The carbon trace holds 300 gCO2/kWh from 10 min before the start, 100 from the start, and 200 from 5 min after.
     *
     * The timestamps of a carbon trace are absolute, so the simulation should start at the second carbon intensity, not
     * at the first one of the trace.
     */
    @Test
    fun `a simulation that does not start at the Unix epoch uses the carbon intensities from its start`() {
        val start = Instant.parse("2022-01-01T00:00:00Z").toEpochMilli()
        val carbon = carbonTrace(start - 10 * MINUTE to 300.0, start to 100.0, start + 5 * MINUTE to 200.0)

        val monitor = runTest(carbonTopology(carbon), listOf(task(10 * MINUTE, submissionTime = "2022-01-01T00:00")))

        // The intensity changes at sample 4 (min 5). The last two samples are both taken at min 10.
        val expectedIntensities = List(4) { 100.0 } + List(7) { 200.0 }
        val totalEmission =
            carbonEmission(energy(HALF_LOAD_POWER, 5 * MINUTE), 100.0) + carbonEmission(energy(HALF_LOAD_POWER, 5 * MINUTE), 200.0)

        assertAll(
            { assertEquals(expectedIntensities, monitor.carbonIntensities) { "Carbon intensities incorrect" } },
            { assertEquals(totalEmission, monitor.carbonEmissions.last(), 1e-6) { "Total carbon emission incorrect" } },
        )
    }

    /**
     * A 20 min task runs 10 min at 1000 MHz (half load) followed by 10 min at 2000 MHz (full load), while the carbon
     * intensity rises from 100 to 200 gCO2/kWh at min 15.
     * The demand and the carbon intensity change at different moments, so the carbon emission consists of three parts:
     * half load at 100, full load at 100, and full load at 200.
     */
    @Test
    fun `the carbon emission follows changes in both demand and carbon intensity`() {
        val workload = listOf(createTestTask(id = 0, fragments = listOf(fragment(10 * MINUTE, 1000.0), fragment(10 * MINUTE, 2000.0))))
        val carbon = carbonTrace(0L to 100.0, 15 * MINUTE to 200.0)

        val monitor = runTest(carbonTopology(carbon), workload)

        val energyUsage = energy(HALF_LOAD_POWER, 10 * MINUTE) + energy(FULL_LOAD_POWER, 10 * MINUTE)

        // The total carbon emission when the carbon intensity changes (min 15), and at the end of the task (min 20)
        val samples = listOf(14, 19)
        val emissionBeforeChange =
            carbonEmission(energy(HALF_LOAD_POWER, 10 * MINUTE), 100.0) + carbonEmission(energy(FULL_LOAD_POWER, 5 * MINUTE), 100.0)
        val expectedEmissions =
            listOf(emissionBeforeChange, emissionBeforeChange + carbonEmission(energy(FULL_LOAD_POWER, 5 * MINUTE), 200.0))

        assertAll(
            { assertEquals(HALF_LOAD_POWER, monitor.powerDraws[0]) { "Power draw at sample 0 incorrect" } },
            { assertEquals(FULL_LOAD_POWER, monitor.powerDraws[10]) { "Power draw at sample 10 incorrect" } },
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertCarbonEmissions(monitor, samples, expectedEmissions) },
        )
    }

    /**
     * A 10 min task is interrupted by a failure after 5 min, for 5 min ([singleFailure]), and restarts from scratch at
     * min 10. The carbon intensity rises from 100 to 200 gCO2/kWh at min 7, while the host is failed.
     * A failed host keeps drawing idle power, so it keeps emitting carbon, at both carbon intensities.
     */
    @Test
    fun `a failed host keeps emitting carbon at idle power`() {
        val carbon = carbonTrace(0L to 100.0, 7 * MINUTE to 200.0)

        val monitor = runTest(carbonTopology(carbon), listOf(task(10 * MINUTE)), singleFailure)

        // The task runs 5 min before the failure and 10 min after it. In between, the host is failed and idle for 5 min.
        val runtime = 20 * MINUTE
        val energyUsage = energy(HALF_LOAD_POWER, 15 * MINUTE) + energy(IDLE_POWER, 5 * MINUTE)
        val totalEmission =
            carbonEmission(energy(HALF_LOAD_POWER, 5 * MINUTE), 100.0) +
                carbonEmission(energy(IDLE_POWER, 2 * MINUTE), 100.0) +
                carbonEmission(energy(IDLE_POWER, 3 * MINUTE), 200.0) +
                carbonEmission(energy(HALF_LOAD_POWER, 10 * MINUTE), 200.0)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(IDLE_POWER, monitor.powerDraws[5]) { "Power draw at sample 5 incorrect" } },
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertEquals(totalEmission, monitor.carbonEmissions.last(), 1e-6) { "Total carbon emission incorrect" } },
        )
    }

    /**
     * Two hosts share a power source with a constant carbon intensity. A 10 min task runs once alone, and once together
     * with a second 10 min task, which runs on the other host.
     * The power source emits the carbon of all its hosts: with one task, the other host is idle, but still draws idle
     * power, and with two tasks, both hosts run at half load.
     */
    @Test
    fun `the carbon emission includes all hosts of the power source, also when idle`() {
        val topology = carbonTopology(CONSTANT_CARBON, hostCount = 2)

        val monitorOneTask = runTest(topology, listOf(task(10 * MINUTE)))
        val monitorTwoTasks = runTest(topology, listOf(task(10 * MINUTE, id = 0), task(10 * MINUTE, id = 1)))

        val energyUsageOneTask = energy(HALF_LOAD_POWER + IDLE_POWER, 10 * MINUTE)
        val energyUsageTwoTasks = energy(2 * HALF_LOAD_POWER, 10 * MINUTE)

        assertAll(
            { assertEquals(HALF_LOAD_POWER + IDLE_POWER, monitorOneTask.powerDraws[0]) { "Power draw with one task incorrect" } },
            { assertEquals(2 * HALF_LOAD_POWER, monitorTwoTasks.powerDraws[0]) { "Power draw with two tasks incorrect" } },
            {
                assertEquals(carbonEmission(energyUsageOneTask, CONSTANT_INTENSITY), monitorOneTask.carbonEmissions.last(), 1e-6) {
                    "Total carbon emission with one task incorrect"
                }
            },
            {
                assertEquals(carbonEmission(energyUsageTwoTasks, CONSTANT_INTENSITY), monitorTwoTasks.carbonEmissions.last(), 1e-6) {
                    "Total carbon emission with two tasks incorrect"
                }
            },
        )
    }

    /**
     * A 15 min task runs at half load, while the carbon intensity drops from 100 to 0 gCO2/kWh at min 5, and returns to
     * 100 at min 10.
     * The host keeps using energy, but emits no carbon between min 5 and 10.
     */
    @Test
    fun `a carbon intensity of zero emits no carbon`() {
        val carbon = carbonTrace(0L to 100.0, 5 * MINUTE to 0.0, 10 * MINUTE to 100.0)

        val monitor = runTest(carbonTopology(carbon), listOf(task(15 * MINUTE)))

        val energyUsage = energy(HALF_LOAD_POWER, 15 * MINUTE)
        val fiveMinuteEmission = carbonEmission(energy(HALF_LOAD_POWER, 5 * MINUTE), 100.0)

        // The carbon emission does not grow from min 5 to 10, which are samples 4 to 9
        val samples = (4..9).toList()

        assertAll(
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertCarbonEmissions(monitor, samples, List(samples.size) { fiveMinuteEmission }) },
            { assertEquals(2 * fiveMinuteEmission, monitor.carbonEmissions.last(), 1e-6) { "Total carbon emission incorrect" } },
        )
    }

    /**
     * A 10 min task runs at half load, while the carbon intensity rises from 100 to 200 gCO2/kWh at min 5.
     * The data center draws all its power from its power source, so it reports the same carbon intensity and carbon
     * emission.
     */
    @Test
    fun `a data center reports the carbon of its power source`() {
        val monitor = runTest(carbonTopology(LOW_THEN_HIGH_CARBON), listOf(task(10 * MINUTE)))

        assertAll(
            { assertEquals(monitor.carbonIntensities, monitor.dataCenterCarbonIntensities) { "Carbon intensities incorrect" } },
            { assertEquals(monitor.carbonEmissions, monitor.dataCenterCarbonEmissions) { "Carbon emissions incorrect" } },
        )
    }

    /**
     * Like `a data center reports the carbon of its power source`, but for the single cluster of the data center, which
     * draws all the power of the power source.
     *
     * TODO: OpenDC does not return the correct values. ClusterDistributor does not compute the carbon intensity and
     *  carbon emission yet, so a cluster always reports 0 for both. This will be solved later
     */
    @Test @Disabled
    fun `a cluster reports the carbon of its power source`() {
        val monitor = runTest(carbonTopology(LOW_THEN_HIGH_CARBON), listOf(task(10 * MINUTE)))

        assertAll(
            { assertEquals(monitor.carbonIntensities, monitor.clusterCarbonIntensities) { "Carbon intensities incorrect" } },
            { assertEquals(monitor.carbonEmissions, monitor.clusterCarbonEmissions) { "Carbon emissions incorrect" } },
        )
    }

    /**
     * A single-core task that runs at 1000 MHz, half of the host's capacity, for [duration] ms.
     * By default, it is submitted at the Unix epoch, so the simulation starts at the Unix epoch.
     */
    private fun task(
        duration: Long,
        id: Int = 0,
        submissionTime: String = "1970-01-01T00:00",
    ): TaskSpec = createTestTask(id = id, submissionTime = submissionTime, fragments = listOf(fragment(duration, 1000.0)))

    /** Asserts the total carbon emission in g at each of [samples]. */
    private fun assertCarbonEmissions(
        monitor: TestMetricExporter,
        samples: List<Int>,
        expected: List<Double>,
    ) {
        val assertions =
            samples.zip(expected).map { (sample, emission) ->
                { assertEquals(emission, monitor.carbonEmissions[sample], 1e-6) { "Carbon emission at sample $sample incorrect" } }
            }
        assertAll("Carbon emissions at samples $samples", assertions)
    }

    private companion object {
        /** The carbon intensities in gCO2/kWh of the constant carbon traces. */
        const val CONSTANT_INTENSITY = 100.0
        const val HIGH_INTENSITY = 300.0

        /** A carbon trace with a carbon intensity of [CONSTANT_INTENSITY] throughout. */
        val CONSTANT_CARBON = carbonTrace(0L to CONSTANT_INTENSITY)

        /** A carbon trace with a carbon intensity of [HIGH_INTENSITY] throughout. */
        val HIGH_CARBON = carbonTrace(0L to HIGH_INTENSITY)

        /** A carbon trace with a carbon intensity of 100 gCO2/kWh for the first 5 min, and 200 after. */
        val LOW_THEN_HIGH_CARBON = carbonTrace(0L to 100.0, 5 * MINUTE to 200.0)
    }
}
