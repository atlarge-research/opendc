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
import org.opendc.common.units.Energy
import org.opendc.common.units.Power
import org.opendc.common.units.TimeDelta
import org.opendc.common.units.Timestamp
import org.opendc.sdk.model.carbon.CarbonIntensitySpec
import org.opendc.sdk.model.carbon.CarbonModelSpec
import org.opendc.sdk.model.carbon.InlineTraceBasedCarbonSpec
import org.opendc.sdk.model.failure.FailureSpec
import org.opendc.sdk.model.failure.InlineTraceBasedFailureSpec
import org.opendc.sdk.model.topology.BatteryPolicySpec
import org.opendc.sdk.model.topology.BatterySpec
import org.opendc.sdk.model.topology.DoubleBatteryPolicySpec
import org.opendc.sdk.model.topology.PowerSourceSpec
import org.opendc.sdk.model.topology.RunningMeanPlusPolicySpec
import org.opendc.sdk.model.topology.RunningMedianPolicySpec
import org.opendc.sdk.model.topology.RunningQuartilesPolicySpec
import org.opendc.sdk.model.topology.SingleBatteryPolicySpec
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.workload.TaskSpec
import org.opendc.sdk.runner.base.harness.HALF_LOAD_POWER
import org.opendc.sdk.runner.base.harness.IDLE_POWER
import org.opendc.sdk.runner.base.harness.MINUTE
import org.opendc.sdk.runner.base.harness.SECOND
import org.opendc.sdk.runner.base.harness.TestMetricExporter
import org.opendc.sdk.runner.base.harness.assertDelays
import org.opendc.sdk.runner.base.harness.checkpointEveryMinute
import org.opendc.sdk.runner.base.harness.createTestTask
import org.opendc.sdk.runner.base.harness.energy
import org.opendc.sdk.runner.base.harness.fragment
import org.opendc.sdk.runner.base.harness.runTest
import org.opendc.sdk.runner.base.harness.singleClusterTopology
import org.opendc.sdk.runner.base.harness.singleFailure
import org.opendc.simulator.compute.power.batteries.BatteryState

/**
 * Integration tests for batteries.
 *
 * Every test runs on a single host with one 2 GHz core ([singleClusterTopology]), unless stated otherwise, with an
 * inline carbon trace ([carbonTrace]).
 * A battery charges from the grid while the carbon intensity is below its threshold, and powers the host while the
 * carbon intensity is above its threshold.
 * Power draws and energy usages are measured at the power source, so they show what is drawn from the grid, unless they
 * are of the battery. The power draw and energy usage of a battery are what it supplies to the hosts, and its charge is
 * in J.
 */
class BatteryTest {
    /** A battery that never charges, because the constant carbon intensity (100) stays above its threshold (90). */
    private val neverChargingTopology = topology(CONSTANT_CARBON, battery(LARGE_BATTERY, SingleBatteryPolicySpec(carbonThreshold = 90.0)))

    /** A battery that charges until it is full, because the constant carbon intensity (100) stays below its threshold (120). */
    private val chargingTopology = topology(CONSTANT_CARBON, battery(LARGE_BATTERY, SingleBatteryPolicySpec(carbonThreshold = 120.0)))

    /**
     * A battery that charges while the carbon intensity is low (80), and powers the host once it rises (120) at min 10,
     * because its threshold (100) lies in between.
     */
    private val chargeThenDischargeTopology =
        topology(LOW_THEN_HIGH_CARBON, battery(SMALL_BATTERY, SingleBatteryPolicySpec(carbonThreshold = 100.0)))

    /** The host fails after 3 min, for 5 min. With [chargingTopology], the battery is still charging when the host fails. */
    private val failureWhileCharging =
        InlineTraceBasedFailureSpec(listOf(FailureSpec(TimeDelta.ofMin(3), TimeDelta.ofMin(5), 1.0)), repeat = false)

    /** The host fails after 12 min, for 5 min. With [chargeThenDischargeTopology], the battery powers the host when it fails. */
    private val failureWhileDischarging =
        InlineTraceBasedFailureSpec(listOf(FailureSpec(TimeDelta.ofMin(12), TimeDelta.ofMin(5), 1.0)), repeat = false)

    /**
     * The tests in this file use inline carbon traces. This test checks that the power source follows them:
     * [CONSTANT_CARBON] is 100 throughout, and [LOW_THEN_HIGH_CARBON] is 80 for the first 10 min and 120 afterwards.
     */
    @Test
    fun `the power source follows the carbon intensities of an inline carbon trace`() {
        val monitorConstant = runTest(topology(CONSTANT_CARBON), listOf(task(20 * MINUTE)))
        val monitorLowThenHigh = runTest(topology(LOW_THEN_HIGH_CARBON), listOf(task(20 * MINUTE)))

        // Samples 0 to 8 are taken in the first 10 min
        val lowIntensities = monitorLowThenHigh.carbonIntensities.take(9)
        val highIntensities = monitorLowThenHigh.carbonIntensities.drop(9)

        assertAll(
            { assertEquals(listOf(CONSTANT_INTENSITY), monitorConstant.carbonIntensities.distinct()) { "Constant trace incorrect" } },
            { assertEquals(listOf(LOW_INTENSITY), lowIntensities.distinct()) { "First 10 min of low-then-high trace incorrect" } },
            { assertEquals(listOf(HIGH_INTENSITY), highIntensities.distinct()) { "Last 10 min of low-then-high trace incorrect" } },
        )
    }

    /**
     * A 10 min task runs with a battery that never charges.
     * The battery starts empty, so it cannot power the host, and the grid supplies the host throughout.
     */
    @Test
    fun `an empty battery stays empty when the carbon intensity is above its threshold`() {
        val monitor = runTest(neverChargingTopology, listOf(task(10 * MINUTE)))

        val energyUsage = energy(HALF_LOAD_POWER, 10 * MINUTE)

        assertAll(
            { assertEquals(HALF_LOAD_POWER, monitor.powerDraws[0]) { "Power draw at sample 0 incorrect" } },
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertEquals(listOf(BatteryState.IDLE), monitor.batteryStates.distinct()) { "Battery states incorrect" } },
            { assertEquals(listOf(0.0), monitor.batteryCharges.distinct()) { "Battery charges incorrect" } },
        )
    }

    /**
     * A 10 min task runs with a battery that charges until it is full.
     * The battery charges 360 kJ at 1000 W, which takes 6 min. During that time, the grid supplies both the host and the
     * battery. After that, the grid only supplies the host.
     */
    @Test
    fun `a battery charges when the carbon intensity is below its threshold`() {
        val monitor = runTest(chargingTopology, listOf(task(10 * MINUTE)))

        val chargingTime = 6 * MINUTE
        val energyUsage = energy(CHARGING_POWER, chargingTime) + energy(HALF_LOAD_POWER, 10 * MINUTE)

        // At these samples, the battery charges, and is full
        val samples = listOf(0, 5)
        val expectedPowerDraws = listOf(HALF_LOAD_POWER + CHARGING_POWER, HALF_LOAD_POWER)
        val expectedStates = listOf(BatteryState.CHARGING, BatteryState.IDLE)
        val expectedCharges = listOf(energy(CHARGING_POWER, 1 * MINUTE), LARGE_BATTERY.toJoule())

        assertAll(
            { assertEquals(expectedPowerDraws, samples.map { monitor.powerDraws[it] }) { "Power draws incorrect" } },
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertBattery(monitor, samples, expectedStates, expectedCharges, powerDraws = listOf(0.0, 0.0)) },
            { assertEquals(0.0, monitor.batteryEnergyUsages.last()) { "Total battery energy usage incorrect" } },
        )
    }

    /**
     * A 20 min task runs with a battery that charges while the carbon intensity is low, and powers the host once it rises.
     * The battery charges 72 kJ at 1000 W, which takes 72 s. During that time, the grid supplies both the host and the
     * battery.
     * When the carbon intensity rises at min 10, the battery powers the host for 8 min (72 kJ at 150 W), during which the
     * grid supplies nothing. The grid supplies the host again for the last 2 min.
     */
    @Test
    fun `a battery powers the host when the carbon intensity rises above its threshold`() {
        val monitor = runTest(chargeThenDischargeTopology, listOf(task(20 * MINUTE)))

        val chargingTime = 72 * SECOND
        val dischargingTime = 8 * MINUTE
        val energyUsage = energy(CHARGING_POWER, chargingTime) + energy(HALF_LOAD_POWER, 20 * MINUTE - dischargingTime)

        // At these samples, the battery charges, is full, powers the host (for 4 min), and is empty
        val samples = listOf(0, 5, 13, 18)
        val expectedPowerDraws = listOf(HALF_LOAD_POWER + CHARGING_POWER, HALF_LOAD_POWER, 0.0, HALF_LOAD_POWER)
        val expectedStates = listOf(BatteryState.CHARGING, BatteryState.IDLE, BatteryState.DISCHARGING, BatteryState.IDLE)
        val expectedCharges =
            listOf(
                energy(CHARGING_POWER, 1 * MINUTE),
                SMALL_BATTERY.toJoule(),
                SMALL_BATTERY.toJoule() - energy(HALF_LOAD_POWER, 4 * MINUTE),
                0.0,
            )
        val expectedBatteryPowerDraws = listOf(0.0, 0.0, HALF_LOAD_POWER, 0.0)

        assertAll(
            { assertEquals(expectedPowerDraws, samples.map { monitor.powerDraws[it] }) { "Power draws incorrect" } },
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertBattery(monitor, samples, expectedStates, expectedCharges, expectedBatteryPowerDraws) },
            { assertEquals(SMALL_BATTERY.toJoule(), monitor.batteryEnergyUsages.last()) { "Total battery energy usage incorrect" } },
        )
    }

    /**
     * Like `a battery powers the host when the carbon intensity rises above its threshold`, but the task runs for 30 min.
     * The battery is empty at min 18, after which the grid supplies the host again for the remaining 12 min.
     */
    @Test
    fun `the grid powers the host again once the battery is empty`() {
        val monitor = runTest(chargeThenDischargeTopology, listOf(task(30 * MINUTE)))

        val chargingTime = 72 * SECOND
        val dischargingTime = 8 * MINUTE
        val energyUsage = energy(CHARGING_POWER, chargingTime) + energy(HALF_LOAD_POWER, 30 * MINUTE - dischargingTime)

        assertAll(
            { assertEquals(HALF_LOAD_POWER + CHARGING_POWER, monitor.powerDraws[0]) { "Power draw at sample 0 incorrect" } },
            { assertEquals(HALF_LOAD_POWER, monitor.powerDraws[5]) { "Power draw at sample 5 incorrect" } },
            { assertEquals(HALF_LOAD_POWER, monitor.powerDraws[20]) { "Power draw at sample 20 incorrect" } },
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertBattery(monitor, listOf(20), listOf(BatteryState.IDLE), charges = listOf(0.0), powerDraws = listOf(0.0)) },
            { assertEquals(SMALL_BATTERY.toJoule(), monitor.batteryEnergyUsages.last()) { "Total battery energy usage incorrect" } },
        )
    }

    /**
     * A 50 min task runs with a battery that starts full, and whose threshold (90) lies below the constant carbon
     * intensity (100).
     * The battery powers the host from the start, for 40 min (360 kJ at 150 W). After that, the grid supplies the host
     * for the remaining 10 min.
     */
    @Test
    fun `a battery that starts full powers the host until it is empty`() {
        val battery = battery(LARGE_BATTERY, SingleBatteryPolicySpec(carbonThreshold = 90.0), initialCharge = LARGE_BATTERY)

        val monitor = runTest(topology(CONSTANT_CARBON, battery), listOf(task(50 * MINUTE)))

        val dischargingTime = 40 * MINUTE
        val energyUsage = energy(HALF_LOAD_POWER, 50 * MINUTE - dischargingTime)

        // At these samples, the battery powers the host at the start, still powers the host just before it is empty, and is empty
        val samples = listOf(0, 38, 40)
        val expectedPowerDraws = listOf(0.0, 0.0, HALF_LOAD_POWER)
        val expectedStates = listOf(BatteryState.DISCHARGING, BatteryState.DISCHARGING, BatteryState.IDLE)
        val expectedCharges =
            listOf(
                LARGE_BATTERY.toJoule() - energy(HALF_LOAD_POWER, 1 * MINUTE),
                LARGE_BATTERY.toJoule() - energy(HALF_LOAD_POWER, 39 * MINUTE),
                0.0,
            )
        val expectedBatteryPowerDraws = listOf(HALF_LOAD_POWER, HALF_LOAD_POWER, 0.0)

        assertAll(
            { assertEquals(expectedPowerDraws, samples.map { monitor.powerDraws[it] }) { "Power draws incorrect" } },
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertBattery(monitor, samples, expectedStates, expectedCharges, expectedBatteryPowerDraws) },
            { assertEquals(LARGE_BATTERY.toJoule(), monitor.batteryEnergyUsages.last()) { "Total battery energy usage incorrect" } },
        )
    }

    /**
     * A 10 min task runs twice with a battery whose threshold (100) equals the constant carbon intensity: once with an
     * empty battery, and once with a full battery.
     * At its threshold, a battery discharges but does not charge. The empty battery therefore stays empty, and the grid
     * supplies the host. The full battery powers the host for the whole run (360 kJ lasts 40 min at 150 W), and supplies
     * 90 kJ.
     */
    @Test
    fun `a battery at its threshold discharges but does not charge`() {
        val policy = SingleBatteryPolicySpec(carbonThreshold = CONSTANT_INTENSITY)

        val monitorEmpty = runTest(topology(CONSTANT_CARBON, battery(LARGE_BATTERY, policy)), listOf(task(10 * MINUTE)))
        val monitorFull = runTest(topology(CONSTANT_CARBON, battery(LARGE_BATTERY, policy, LARGE_BATTERY)), listOf(task(10 * MINUTE)))

        // Either the grid or the full battery supplies the host for the whole run
        val energyUsage = energy(HALF_LOAD_POWER, 10 * MINUTE)

        assertAll(
            { assertEquals(HALF_LOAD_POWER, monitorEmpty.powerDraws[0]) { "Power draw at sample 0 with empty battery incorrect" } },
            { assertEquals(0.0, monitorFull.powerDraws[0]) { "Power draw at sample 0 with full battery incorrect" } },
            { assertEquals(energyUsage, monitorEmpty.energyUsages.last()) { "Total energy usage with empty battery incorrect" } },
            { assertEquals(0.0, monitorFull.energyUsages.last()) { "Total energy usage with full battery incorrect" } },
            { assertEquals(listOf(BatteryState.IDLE), monitorEmpty.batteryStates.distinct()) { "Empty battery states incorrect" } },
            { assertEquals(listOf(0.0), monitorEmpty.batteryCharges.distinct()) { "Empty battery charges incorrect" } },
            { assertEquals(listOf(BatteryState.DISCHARGING), monitorFull.batteryStates.distinct()) { "Full battery states incorrect" } },
            {
                assertEquals(LARGE_BATTERY.toJoule() - energyUsage, monitorFull.batteryCharges.last()) {
                    "Final charge of full battery incorrect"
                }
            },
            { assertEquals(energyUsage, monitorFull.batteryEnergyUsages.last()) { "Total energy usage of full battery incorrect" } },
        )
    }

    /**
     * A 30 min task runs twice: once without a battery, and once with a battery that charges while the carbon intensity
     * is low (80), and powers the host once it rises (120).
     * The battery moves 72 kJ of energy usage from the high to the low carbon intensity. Both runs use the same amount of
     * energy, but the run with a battery emits less carbon: 7.2 g instead of 8.0 g.
     */
    @Test
    fun `a battery lowers carbon emissions by moving energy usage to a lower carbon intensity`() {
        val workload = listOf(task(30 * MINUTE))

        val monitorNoBattery = runTest(topology(LOW_THEN_HIGH_CARBON), workload)
        val monitorBattery = runTest(chargeThenDischargeTopology, workload)

        // The carbon intensity is low for the first 10 min, and high for the remaining 20 min
        val lowCarbonEnergyNoBattery = energy(HALF_LOAD_POWER, 10 * MINUTE)
        val highCarbonEnergyNoBattery = energy(HALF_LOAD_POWER, 20 * MINUTE)
        // The battery charges 72 kJ while the carbon intensity is low, and powers the host for 8 min while it is high
        val lowCarbonEnergyBattery = lowCarbonEnergyNoBattery + energy(CHARGING_POWER, 72 * SECOND)
        val highCarbonEnergyBattery = highCarbonEnergyNoBattery - energy(HALF_LOAD_POWER, 8 * MINUTE)

        val energyUsageNoBattery = lowCarbonEnergyNoBattery + highCarbonEnergyNoBattery
        val energyUsageBattery = lowCarbonEnergyBattery + highCarbonEnergyBattery
        val carbonNoBattery =
            carbonEmission(lowCarbonEnergyNoBattery, LOW_INTENSITY) + carbonEmission(highCarbonEnergyNoBattery, HIGH_INTENSITY)
        val carbonBattery =
            carbonEmission(lowCarbonEnergyBattery, LOW_INTENSITY) + carbonEmission(highCarbonEnergyBattery, HIGH_INTENSITY)

        // At these samples, the battery charges, is full, powers the host, and is empty
        val samples = listOf(0, 2, 10, 18)
        val powerDrawsNoBattery = samples.map { monitorNoBattery.powerDraws[it] }
        val powerDrawsBattery = samples.map { monitorBattery.powerDraws[it] }
        val expectedPowerDrawsNoBattery = List(samples.size) { HALF_LOAD_POWER }
        val expectedPowerDrawsBattery = listOf(HALF_LOAD_POWER + CHARGING_POWER, HALF_LOAD_POWER, 0.0, HALF_LOAD_POWER)

        assertAll(
            { assertEquals(expectedPowerDrawsNoBattery, powerDrawsNoBattery) { "Power draws without battery incorrect" } },
            { assertEquals(expectedPowerDrawsBattery, powerDrawsBattery) { "Power draws with battery incorrect" } },
            { assertEquals(energyUsageNoBattery, monitorNoBattery.energyUsages.last()) { "Total energy usage without battery incorrect" } },
            { assertEquals(energyUsageBattery, monitorBattery.energyUsages.last()) { "Total energy usage with battery incorrect" } },
            {
                assertEquals(carbonNoBattery, monitorNoBattery.carbonEmissions.last(), 1e-6) {
                    "Total carbon emission without battery incorrect"
                }
            },
            {
                assertEquals(carbonBattery, monitorBattery.carbonEmissions.last(), 1e-6) {
                    "Total carbon emission with battery incorrect"
                }
            },
        )
    }

    /**
     * 1000 tasks of 10 min are submitted at the same time, with a battery that charges while the carbon intensity is low,
     * and powers the host once it rises.
     * The host has a single core, so the tasks run one after another. The battery should not delay them, so the
     * simulation ends after 1000 times 10 min.
     */
    @Test
    fun `a battery does not delay many queued tasks`() {
        val numTasks = 1000
        val workload = List(numTasks) { task(10 * MINUTE, id = it) }

        val monitor = runTest(chargeThenDischargeTopology, workload)

        assertEquals(numTasks * 10 * MINUTE, monitor.maxTimestamp) { "Total runtime incorrect" }
    }

    /**
     * Like `a battery powers the host when the carbon intensity rises above its threshold`, but with two hosts that each
     * run a 20 min task, so the datacenter draws 300 W.
     * The battery charges 72 kJ at 1000 W, which takes 72 s. When the carbon intensity rises at min 10, the battery
     * powers both hosts, and lasts 4 min (72 kJ at 300 W) instead of 8 min.
     */
    @Test
    fun `a battery powers all hosts of its datacenter`() {
        val battery = battery(SMALL_BATTERY, SingleBatteryPolicySpec(carbonThreshold = 100.0))
        val topology = topology(LOW_THEN_HIGH_CARBON, battery, hostCount = 2)
        val workload = List(2) { task(20 * MINUTE, id = it) }

        val monitor = runTest(topology, workload)

        val hostsPower = 2 * HALF_LOAD_POWER
        val chargingTime = 72 * SECOND
        val dischargingTime = 4 * MINUTE
        val energyUsage = energy(CHARGING_POWER, chargingTime) + energy(hostsPower, 20 * MINUTE - dischargingTime)

        // At these samples, the battery charges, is full, powers the hosts, and is empty
        val samples = listOf(0, 5, 11, 15)
        val expectedPowerDraws = listOf(hostsPower + CHARGING_POWER, hostsPower, 0.0, hostsPower)
        val expectedStates = listOf(BatteryState.CHARGING, BatteryState.IDLE, BatteryState.DISCHARGING, BatteryState.IDLE)
        val expectedCharges =
            listOf(
                energy(CHARGING_POWER, 1 * MINUTE),
                SMALL_BATTERY.toJoule(),
                SMALL_BATTERY.toJoule() - energy(hostsPower, 2 * MINUTE),
                0.0,
            )
        val expectedBatteryPowerDraws = listOf(0.0, 0.0, hostsPower, 0.0)

        assertAll(
            { assertEquals(20 * MINUTE, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(expectedPowerDraws, samples.map { monitor.powerDraws[it] }) { "Power draws incorrect" } },
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertBattery(monitor, samples, expectedStates, expectedCharges, expectedBatteryPowerDraws) },
            { assertEquals(SMALL_BATTERY.toJoule(), monitor.batteryEnergyUsages.last()) { "Total battery energy usage incorrect" } },
        )
    }

    /**
     * A 10 min task runs twice with a battery with a lower threshold of 90 and an upper threshold of 110, while the
     * carbon intensity is constant at 100: once with an empty battery, and once with a full battery.
     * Between its thresholds, a battery neither charges nor discharges, so the grid supplies only the host in both runs.
     */
    @Test
    fun `a battery with two thresholds does nothing while the carbon intensity is between them`() {
        val policy = DoubleBatteryPolicySpec(lowerThreshold = 90.0, upperThreshold = 110.0)

        val monitorEmpty = runTest(topology(CONSTANT_CARBON, battery(LARGE_BATTERY, policy)), listOf(task(10 * MINUTE)))
        val monitorFull = runTest(topology(CONSTANT_CARBON, battery(LARGE_BATTERY, policy, LARGE_BATTERY)), listOf(task(10 * MINUTE)))

        val energyUsage = energy(HALF_LOAD_POWER, 10 * MINUTE)

        assertAll(
            { assertEquals(HALF_LOAD_POWER, monitorEmpty.powerDraws[0]) { "Power draw at sample 0 with empty battery incorrect" } },
            { assertEquals(HALF_LOAD_POWER, monitorFull.powerDraws[0]) { "Power draw at sample 0 with full battery incorrect" } },
            { assertEquals(energyUsage, monitorEmpty.energyUsages.last()) { "Total energy usage with empty battery incorrect" } },
            { assertEquals(energyUsage, monitorFull.energyUsages.last()) { "Total energy usage with full battery incorrect" } },
            { assertEquals(listOf(BatteryState.IDLE), monitorEmpty.batteryStates.distinct()) { "Empty battery states incorrect" } },
            { assertEquals(listOf(0.0), monitorEmpty.batteryCharges.distinct()) { "Empty battery charges incorrect" } },
            { assertEquals(listOf(BatteryState.IDLE), monitorFull.batteryStates.distinct()) { "Full battery states incorrect" } },
            { assertEquals(listOf(LARGE_BATTERY.toJoule()), monitorFull.batteryCharges.distinct()) { "Full battery charges incorrect" } },
        )
    }

    /**
     * Like `a battery powers the host when the carbon intensity rises above its threshold`, but with a lower threshold of
     * 90 and an upper threshold of 110.
     * The battery charges while the carbon intensity (80) is below the lower threshold, and powers the host once the
     * carbon intensity (120) rises above the upper threshold, so the results are the same.
     */
    @Test
    fun `a battery with two thresholds charges below the lower and discharges above the upper threshold`() {
        val battery = battery(SMALL_BATTERY, DoubleBatteryPolicySpec(lowerThreshold = 90.0, upperThreshold = 110.0))

        val monitor = runTest(topology(LOW_THEN_HIGH_CARBON, battery), listOf(task(20 * MINUTE)))

        val chargingTime = 72 * SECOND
        val dischargingTime = 8 * MINUTE
        val energyUsage = energy(CHARGING_POWER, chargingTime) + energy(HALF_LOAD_POWER, 20 * MINUTE - dischargingTime)

        // At these samples, the battery charges, is full, powers the host (for 4 min), and is empty
        val samples = listOf(0, 5, 13, 18)
        val expectedPowerDraws = listOf(HALF_LOAD_POWER + CHARGING_POWER, HALF_LOAD_POWER, 0.0, HALF_LOAD_POWER)
        val expectedStates = listOf(BatteryState.CHARGING, BatteryState.IDLE, BatteryState.DISCHARGING, BatteryState.IDLE)
        val expectedCharges =
            listOf(
                energy(CHARGING_POWER, 1 * MINUTE),
                SMALL_BATTERY.toJoule(),
                SMALL_BATTERY.toJoule() - energy(HALF_LOAD_POWER, 4 * MINUTE),
                0.0,
            )
        val expectedBatteryPowerDraws = listOf(0.0, 0.0, HALF_LOAD_POWER, 0.0)

        assertAll(
            { assertEquals(expectedPowerDraws, samples.map { monitor.powerDraws[it] }) { "Power draws incorrect" } },
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertBattery(monitor, samples, expectedStates, expectedCharges, expectedBatteryPowerDraws) },
            { assertEquals(SMALL_BATTERY.toJoule(), monitor.batteryEnergyUsages.last()) { "Total battery energy usage incorrect" } },
        )
    }

    /**
     * A 10 min task runs with a battery that uses the running mean plus policy. The battery charges while the carbon
     * intensity is below the mean of the recent intensities, but only starts charging once the intensity stops decreasing.
     * Once it charges, it should keep charging until it is full, even if the intensity dips.
     *
     * The policy starts with a mean of 120, and the carbon intensity is 100 at the start, 105 from min 2, and 102 from min 4:
     * - At the start, the intensity (100) is below the mean (110) but decreasing, so the battery waits.
     * - At min 2, the intensity (105) is below the mean (108.3) and rising, so the battery starts charging.
     * - At min 4, the intensity (102) is below the mean (106.75) but dips, and the battery should keep charging.
     * The battery charges 360 kJ at 1000 W, so it is full at min 8.
     *
     * TODO: OpenDC does not return the correct values. `RunningMeanPlusBatteryPolicy` never sets `isCharging` to true, so
     *  the battery stops charging at the dip at min 4.
     */
    @Test @Disabled
    fun `a running mean plus battery keeps charging when the carbon intensity dips`() {
        val carbon = carbonTrace(0 * MINUTE to 100.0, 2 * MINUTE to 105.0, 4 * MINUTE to 102.0)
        val battery = battery(LARGE_BATTERY, RunningMeanPlusPolicySpec(startingThreshold = 120.0, windowSize = 10))

        val monitor = runTest(topology(carbon, battery), listOf(task(10 * MINUTE)))

        val chargingTime = 6 * MINUTE
        val energyUsage = energy(CHARGING_POWER, chargingTime) + energy(HALF_LOAD_POWER, 10 * MINUTE)

        // At these samples, the battery waits, charges, keeps charging after the dip, and is full
        val samples = listOf(0, 2, 5, 8)
        val expectedPowerDraws =
            listOf(HALF_LOAD_POWER, HALF_LOAD_POWER + CHARGING_POWER, HALF_LOAD_POWER + CHARGING_POWER, HALF_LOAD_POWER)
        val expectedStates = listOf(BatteryState.IDLE, BatteryState.CHARGING, BatteryState.CHARGING, BatteryState.IDLE)
        val expectedCharges =
            listOf(0.0, energy(CHARGING_POWER, 1 * MINUTE), energy(CHARGING_POWER, 4 * MINUTE), LARGE_BATTERY.toJoule())

        assertAll(
            { assertEquals(expectedPowerDraws, samples.map { monitor.powerDraws[it] }) { "Power draws incorrect" } },
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertBattery(monitor, samples, expectedStates, expectedCharges, powerDraws = List(samples.size) { 0.0 }) },
        )
    }

    /**
     * A 10 min task runs with a battery whose manufacturing emitted 525.6 kg of carbon, and that is expected to last a
     * year (525,600 min). Its embodied carbon is spread evenly over its lifetime, so it emits 1 g every minute.
     */
    @Test
    fun `a battery emits its embodied carbon evenly over its expected lifetime`() {
        val battery =
            battery(LARGE_BATTERY, SingleBatteryPolicySpec(carbonThreshold = 120.0)).copy(embodiedCarbon = 525.6, expectedLifetime = 1.0)

        val monitor = runTest(topology(CONSTANT_CARBON, battery), listOf(task(10 * MINUTE)))

        assertAll(
            { assertEquals(1.0, monitor.batteryEmbodiedCarbonEmissions[0], 1e-6) { "Embodied carbon emission at sample 0 incorrect" } },
            { assertEquals(5.0, monitor.batteryEmbodiedCarbonEmissions[4], 1e-6) { "Embodied carbon emission at sample 4 incorrect" } },
            { assertEquals(10.0, monitor.batteryEmbodiedCarbonEmissions.last(), 1e-6) { "Total embodied carbon emission incorrect" } },
        )
    }

    /**
     * A 10 min task runs with a battery for which no embodied carbon is given, so it emits none.
     *
     * TODO: OpenDC does not return the correct values. `BatterySpec` defaults both `embodiedCarbon` and `expectedLifetime`
     *  to 0, and `BatteryNode` divides the one by the other, so the battery emits NaN.
     */
    @Test @Disabled
    fun `a battery without embodied carbon emits none`() {
        val monitor = runTest(chargingTopology, listOf(task(10 * MINUTE)))

        assertEquals(listOf(0.0), monitor.batteryEmbodiedCarbonEmissions.distinct()) { "Embodied carbon emissions incorrect" }
    }

    /**
     * A 10 min task runs with a battery that uses the running median policy.
     * This only checks that a scenario with this policy runs. Its behaviour should be tested once it is implemented.
     *
     * TODO: OpenDC does not implement this policy. `createBatteryPolicy` has no case for `RunningMedianPolicySpec`, so
     *  building the scenario fails with "Unknown battery policy".
     */
    @Test @Disabled
    fun `a battery can use the running median policy`() {
        val battery = battery(SMALL_BATTERY, RunningMedianPolicySpec(startingThreshold = 100.0, windowSize = 10))

        val monitor = runTest(topology(LOW_THEN_HIGH_CARBON, battery), listOf(task(10 * MINUTE)))

        assertEquals(10 * MINUTE, monitor.maxTimestamp) { "Total runtime incorrect" }
    }

    /**
     * A 10 min task runs with a battery that uses the running quartiles policy.
     * This only checks that a scenario with this policy runs. Its behaviour should be tested once it is implemented.
     *
     * TODO: OpenDC does not implement this policy. `createBatteryPolicy` has no case for `RunningQuartilesPolicySpec`, so
     *  building the scenario fails with "Unknown battery policy".
     */
    @Test @Disabled
    fun `a battery can use the running quartiles policy`() {
        val battery = battery(SMALL_BATTERY, RunningQuartilesPolicySpec(startingThreshold = 100.0, windowSize = 10))

        val monitor = runTest(topology(LOW_THEN_HIGH_CARBON, battery), listOf(task(10 * MINUTE)))

        assertEquals(10 * MINUTE, monitor.maxTimestamp) { "Total runtime incorrect" }
    }

    /**
     * Like `a single failure restarts the task from scratch` in [FailuresAndCheckpointingTest], with a battery that never
     * charges.
     * A 10 min task is interrupted by a failure after 5 min, and restarts from scratch when the host recovers at min 10.
     * The battery stays empty, so the grid supplies the host throughout, also while it is failed.
     */
    @Test
    fun `a failure restarts the task from scratch with a battery that never charges`() {
        val monitor = runTest(neverChargingTopology, listOf(task(10 * MINUTE)), singleFailure)

        val expectedSchedulingDelay = 5 * MINUTE
        val expectedFailureDelay = 5 * MINUTE

        // The task runs for its 10 min plus the work it redoes. While it waits, the host is failed and idle.
        val runningTime = 10 * MINUTE + expectedFailureDelay
        val runtime = runningTime + expectedSchedulingDelay
        val energyUsage = energy(HALF_LOAD_POWER, runningTime) + energy(IDLE_POWER, expectedSchedulingDelay)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(HALF_LOAD_POWER, monitor.powerDraws[0]) { "Power draw at sample 0 incorrect" } },
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertDelays(monitor, taskId = 0, scheduling = expectedSchedulingDelay, failure = expectedFailureDelay, checkpoint = 0) },
            { assertEquals(listOf(BatteryState.IDLE), monitor.batteryStates.distinct()) { "Battery states incorrect" } },
            { assertEquals(listOf(0.0), monitor.batteryCharges.distinct()) { "Battery charges incorrect" } },
        )
    }

    /**
     * Like `a checkpointed task only redoes the work since its last snapshot` in [FailuresAndCheckpointingTest], with a
     * battery that never charges.
     * A 10 min task makes a 1 s snapshot after every minute of work, and is interrupted by a failure after 5 min. It loses
     * the 56 s of work since its last snapshot, and makes 9 snapshots in total.
     * The battery stays empty, so the grid supplies the host throughout, also while it is failed.
     */
    @Test
    fun `a checkpointed task only redoes the work since its last snapshot with a battery that never charges`() {
        val monitor = runTest(neverChargingTopology, listOf(task(10 * MINUTE)), singleFailure, checkpointModel = checkpointEveryMinute)

        val expectedSchedulingDelay = 5 * MINUTE
        val expectedFailureDelay = 56 * SECOND
        val expectedCheckpointDelay = 9 * SECOND

        // The task runs for its 10 min plus the work it redoes and its snapshots. While it waits, the host is failed and idle.
        val runningTime = 10 * MINUTE + expectedFailureDelay + expectedCheckpointDelay
        val runtime = runningTime + expectedSchedulingDelay
        val energyUsage = energy(HALF_LOAD_POWER, runningTime) + energy(IDLE_POWER, expectedSchedulingDelay)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            {
                assertDelays(
                    monitor,
                    taskId = 0,
                    scheduling = expectedSchedulingDelay,
                    failure = expectedFailureDelay,
                    checkpoint = expectedCheckpointDelay,
                )
            },
            { assertEquals(listOf(BatteryState.IDLE), monitor.batteryStates.distinct()) { "Battery states incorrect" } },
            { assertEquals(listOf(0.0), monitor.batteryCharges.distinct()) { "Battery charges incorrect" } },
        )
    }

    /**
     * A 10 min task runs with a battery that charges for the first 6 min, and is interrupted by a failure after 3 min.
     * The host is failed from min 3 to 8, after which the task restarts from scratch and finishes at min 18.
     *
     * The failure should not affect the battery: it keeps charging while the host is failed, and is full at min 6.
     * While the host is failed, it is idle, so the grid supplies 100 W to the host and 1000 W to the battery.
     */
    @Test
    fun `a failure while the battery charges does not interrupt the charging`() {
        val monitor = runTest(chargingTopology, listOf(task(10 * MINUTE)), failureWhileCharging)

        val expectedSchedulingDelay = 5 * MINUTE
        val expectedFailureDelay = 3 * MINUTE

        // The task runs for its 10 min plus the work it redoes. While it waits, the host is failed and idle.
        val runningTime = 10 * MINUTE + expectedFailureDelay
        val runtime = runningTime + expectedSchedulingDelay
        // The battery charges 360 kJ at 1000 W
        val chargingTime = 6 * MINUTE
        val energyUsage =
            energy(CHARGING_POWER, chargingTime) +
                energy(HALF_LOAD_POWER, runningTime) +
                energy(IDLE_POWER, expectedSchedulingDelay)

        // At these samples, the host runs while the battery charges, the host is failed while the battery charges, the
        // host is failed while the battery is full, and the host runs again
        val samples = listOf(0, 3, 6, 9)
        val expectedPowerDraws = listOf(HALF_LOAD_POWER + CHARGING_POWER, IDLE_POWER + CHARGING_POWER, IDLE_POWER, HALF_LOAD_POWER)
        val expectedStates = listOf(BatteryState.CHARGING, BatteryState.CHARGING, BatteryState.IDLE, BatteryState.IDLE)
        val expectedCharges =
            listOf(
                energy(CHARGING_POWER, 1 * MINUTE),
                energy(CHARGING_POWER, 4 * MINUTE),
                LARGE_BATTERY.toJoule(),
                LARGE_BATTERY.toJoule(),
            )

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(expectedPowerDraws, samples.map { monitor.powerDraws[it] }) { "Power draws incorrect" } },
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertBattery(monitor, samples, expectedStates, expectedCharges, powerDraws = List(samples.size) { 0.0 }) },
            { assertDelays(monitor, taskId = 0, scheduling = expectedSchedulingDelay, failure = expectedFailureDelay, checkpoint = 0) },
        )
    }

    /**
     * A 20 min task runs with a battery that charges while the carbon intensity is low, and powers the host once it rises
     * at min 10. The task is interrupted by a failure after 12 min, while the battery powers the host.
     * The host is failed from min 12 to 17, after which the task restarts from scratch and finishes at min 37.
     *
     * While the host is failed, the battery powers the idle host (100 W) instead of the running host (150 W), so it lasts
     * longer than the 8 min it would last without the failure.
     * It supplies 18 kJ in the 2 min before the failure, and 30 kJ during the 5 min failure. The remaining 24 kJ lasts
     * 160 s after the host recovers, so the battery is empty at 19 min 40 s, after which the grid supplies the host again.
     */
    @Test
    fun `a failure while the battery powers the host makes the battery last longer`() {
        val monitor = runTest(chargeThenDischargeTopology, listOf(task(20 * MINUTE)), failureWhileDischarging)

        val expectedSchedulingDelay = 5 * MINUTE
        val expectedFailureDelay = 12 * MINUTE

        val runtime = 20 * MINUTE + expectedFailureDelay + expectedSchedulingDelay
        val batteryEmptyTime = 17 * MINUTE + 160 * SECOND
        // The grid charges the battery and supplies the host until the carbon intensity rises, and supplies the host again
        // once the battery is empty
        val energyUsage =
            energy(CHARGING_POWER, 72 * SECOND) +
                energy(HALF_LOAD_POWER, 10 * MINUTE) +
                energy(HALF_LOAD_POWER, runtime - batteryEmptyTime)

        // At these samples, the battery charges, the battery powers the failed host, the battery still powers the host
        // (without the failure it would be empty), and the battery is empty
        val samples = listOf(0, 13, 18, 20)
        val expectedPowerDraws = listOf(HALF_LOAD_POWER + CHARGING_POWER, 0.0, 0.0, HALF_LOAD_POWER)
        // The battery powers the running host for 2 min before the failure, the failed host for 5 min, and the running host
        // again after the failure
        val expectedStates = listOf(BatteryState.CHARGING, BatteryState.DISCHARGING, BatteryState.DISCHARGING, BatteryState.IDLE)
        val expectedCharges =
            listOf(
                energy(CHARGING_POWER, 1 * MINUTE),
                SMALL_BATTERY.toJoule() - energy(HALF_LOAD_POWER, 2 * MINUTE) - energy(IDLE_POWER, 2 * MINUTE),
                SMALL_BATTERY.toJoule() - energy(HALF_LOAD_POWER, 4 * MINUTE) - energy(IDLE_POWER, 5 * MINUTE),
                0.0,
            )
        val expectedBatteryPowerDraws = listOf(0.0, IDLE_POWER, HALF_LOAD_POWER, 0.0)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(expectedPowerDraws, samples.map { monitor.powerDraws[it] }) { "Power draws incorrect" } },
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertBattery(monitor, samples, expectedStates, expectedCharges, expectedBatteryPowerDraws) },
            { assertEquals(SMALL_BATTERY.toJoule(), monitor.batteryEnergyUsages.last()) { "Total battery energy usage incorrect" } },
            { assertDelays(monitor, taskId = 0, scheduling = expectedSchedulingDelay, failure = expectedFailureDelay, checkpoint = 0) },
        )
    }

    /**
     * Like `a failure while the battery charges does not interrupt the charging`, but the task makes a 1 s snapshot after
     * every minute of work.
     * Before the failure at min 3, it makes 2 snapshots while the battery charges, the last of which finishes at 122 s.
     * It loses the 58 s of work since then, and resumes from that snapshot with 8 min of work left when the host
     * recovers at min 8. In total, 9 snapshots are made: 2 before the failure and 7 after.
     */
    @Test
    fun `a checkpointed task resumes from a snapshot made while the battery charges`() {
        val monitor = runTest(chargingTopology, listOf(task(10 * MINUTE)), failureWhileCharging, checkpointModel = checkpointEveryMinute)

        val expectedSchedulingDelay = 5 * MINUTE
        val expectedFailureDelay = 58 * SECOND
        val expectedCheckpointDelay = 9 * SECOND

        // The task runs for its 10 min plus the work it redoes and its snapshots. While it waits, the host is failed and idle.
        val runningTime = 10 * MINUTE + expectedFailureDelay + expectedCheckpointDelay
        val runtime = runningTime + expectedSchedulingDelay
        // The battery charges 360 kJ at 1000 W
        val chargingTime = 6 * MINUTE
        val energyUsage =
            energy(CHARGING_POWER, chargingTime) +
                energy(HALF_LOAD_POWER, runningTime) +
                energy(IDLE_POWER, expectedSchedulingDelay)

        // At these samples, the host runs while the battery charges, the host is failed while the battery charges, the
        // host is failed while the battery is full, and the host runs again
        val samples = listOf(0, 3, 6, 9)
        val expectedPowerDraws = listOf(HALF_LOAD_POWER + CHARGING_POWER, IDLE_POWER + CHARGING_POWER, IDLE_POWER, HALF_LOAD_POWER)
        val expectedStates = listOf(BatteryState.CHARGING, BatteryState.CHARGING, BatteryState.IDLE, BatteryState.IDLE)
        val expectedCharges =
            listOf(
                energy(CHARGING_POWER, 1 * MINUTE),
                energy(CHARGING_POWER, 4 * MINUTE),
                LARGE_BATTERY.toJoule(),
                LARGE_BATTERY.toJoule(),
            )

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(expectedPowerDraws, samples.map { monitor.powerDraws[it] }) { "Power draws incorrect" } },
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertBattery(monitor, samples, expectedStates, expectedCharges, powerDraws = List(samples.size) { 0.0 }) },
            {
                assertDelays(
                    monitor,
                    taskId = 0,
                    scheduling = expectedSchedulingDelay,
                    failure = expectedFailureDelay,
                    checkpoint = expectedCheckpointDelay,
                )
            },
        )
    }

    /**
     * Like `a failure while the battery powers the host makes the battery last longer`, but the task makes a 1 s snapshot
     * after every minute of work.
     * Before the failure at min 12, it makes 11 snapshots, the last two while the battery powers the host. The last one
     * finishes at 671 s, so the task loses the 49 s of work since then, and resumes from that snapshot with 9 min of work
     * left when the host recovers at min 17. In total, 19 snapshots are made: 11 before the failure and 8 after.
     *
     * The snapshots run at the same power as the task, so the battery is again empty at 19 min 40 s.
     */
    @Test
    fun `a checkpointed task resumes from a snapshot made while the battery powers the host`() {
        val monitor =
            runTest(
                chargeThenDischargeTopology,
                listOf(task(20 * MINUTE)),
                failureWhileDischarging,
                checkpointModel = checkpointEveryMinute,
            )

        val expectedSchedulingDelay = 5 * MINUTE
        val expectedFailureDelay = 49 * SECOND
        val expectedCheckpointDelay = 19 * SECOND

        val runtime = 20 * MINUTE + expectedFailureDelay + expectedCheckpointDelay + expectedSchedulingDelay
        val batteryEmptyTime = 17 * MINUTE + 160 * SECOND
        // The grid charges the battery and supplies the host until the carbon intensity rises, and supplies the host again
        // once the battery is empty
        val energyUsage =
            energy(CHARGING_POWER, 72 * SECOND) +
                energy(HALF_LOAD_POWER, 10 * MINUTE) +
                energy(HALF_LOAD_POWER, runtime - batteryEmptyTime)

        // At these samples, the battery charges, the battery powers the failed host, the battery still powers the host
        // (without the failure it would be empty), and the battery is empty
        val samples = listOf(0, 13, 18, 20)
        val expectedPowerDraws = listOf(HALF_LOAD_POWER + CHARGING_POWER, 0.0, 0.0, HALF_LOAD_POWER)
        // The battery powers the running host for 2 min before the failure, the failed host for 5 min, and the running host
        // again after the failure
        val expectedStates = listOf(BatteryState.CHARGING, BatteryState.DISCHARGING, BatteryState.DISCHARGING, BatteryState.IDLE)
        val expectedCharges =
            listOf(
                energy(CHARGING_POWER, 1 * MINUTE),
                SMALL_BATTERY.toJoule() - energy(HALF_LOAD_POWER, 2 * MINUTE) - energy(IDLE_POWER, 2 * MINUTE),
                SMALL_BATTERY.toJoule() - energy(HALF_LOAD_POWER, 4 * MINUTE) - energy(IDLE_POWER, 5 * MINUTE),
                0.0,
            )
        val expectedBatteryPowerDraws = listOf(0.0, IDLE_POWER, HALF_LOAD_POWER, 0.0)

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(expectedPowerDraws, samples.map { monitor.powerDraws[it] }) { "Power draws incorrect" } },
            { assertEquals(energyUsage, monitor.energyUsages.last()) { "Total energy usage incorrect" } },
            { assertBattery(monitor, samples, expectedStates, expectedCharges, expectedBatteryPowerDraws) },
            { assertEquals(SMALL_BATTERY.toJoule(), monitor.batteryEnergyUsages.last()) { "Total battery energy usage incorrect" } },
            {
                assertDelays(
                    monitor,
                    taskId = 0,
                    scheduling = expectedSchedulingDelay,
                    failure = expectedFailureDelay,
                    checkpoint = expectedCheckpointDelay,
                )
            },
        )
    }

    /** A topology with [hostCount] hosts, whose power source follows [carbon], with an optional [battery]. */
    private fun topology(
        carbon: CarbonModelSpec,
        battery: BatterySpec? = null,
        hostCount: Int = 1,
    ): TopologySpec = singleClusterTopology(hostCount = hostCount, powerSource = PowerSourceSpec(carbon = carbon), battery = battery)

    /** A battery of [capacity] that charges at [CHARGING_POWER] and starts with [initialCharge]. */
    private fun battery(
        capacity: Energy,
        policy: BatteryPolicySpec,
        initialCharge: Energy = Energy.zero,
    ): BatterySpec =
        BatterySpec(capacity = capacity, chargingSpeed = Power.ofWatts(CHARGING_POWER), initialCharge = initialCharge, policy = policy)

    /**
     * A single-core task that runs at 1000 MHz, half of the host's capacity, for [duration] ms.
     * It is submitted at the Unix epoch, the default of [createTestTask], so the simulation starts at the Unix epoch.
     */
    private fun task(
        duration: Long,
        id: Int = 0,
    ): TaskSpec = createTestTask(id = id, fragments = listOf(fragment(duration, 1000.0)))

    /** Asserts the state, the charge in J, and the power draw in W of the battery at each of [samples]. */
    private fun assertBattery(
        monitor: TestMetricExporter,
        samples: List<Int>,
        states: List<BatteryState>,
        charges: List<Double>,
        powerDraws: List<Double>,
    ) {
        assertAll(
            "Battery at samples $samples",
            { assertEquals(states, samples.map { monitor.batteryStates[it] }) { "Battery states incorrect" } },
            { assertEquals(charges, samples.map { monitor.batteryCharges[it] }) { "Battery charges incorrect" } },
            { assertEquals(powerDraws, samples.map { monitor.batteryPowerDraws[it] }) { "Battery power draws incorrect" } },
        )
    }

    /** The carbon emission in g of using [energyUsage] J at a carbon intensity of [intensity] gCO2/kWh. */
    private fun carbonEmission(
        energyUsage: Double,
        intensity: Double,
    ): Double = energyUsage / 3_600_000 * intensity

    private companion object {
        /** The carbon intensities in gCO2/kWh of the carbon traces. */
        const val CONSTANT_INTENSITY = 100.0
        const val LOW_INTENSITY = 80.0
        const val HIGH_INTENSITY = 120.0

        /** A carbon trace with a carbon intensity of [CONSTANT_INTENSITY] throughout. */
        val CONSTANT_CARBON = carbonTrace(0L to CONSTANT_INTENSITY)

        /** A carbon trace with a carbon intensity of [LOW_INTENSITY] for the first 10 min, and [HIGH_INTENSITY] after. */
        val LOW_THEN_HIGH_CARBON = carbonTrace(0L to LOW_INTENSITY, 10 * MINUTE to HIGH_INTENSITY)

        /**
         * An inline carbon trace in which the carbon intensity changes at the given times. The simulation starts at the
         * Unix epoch (see [task]), so the times are both in ms after the start of the simulation and in ms since the epoch.
         */
        fun carbonTrace(vararg intensities: Pair<Long, Double>): InlineTraceBasedCarbonSpec =
            InlineTraceBasedCarbonSpec(intensities.map { (time, intensity) -> CarbonIntensitySpec(Timestamp.ofEpochMs(time), intensity) })

        /** The power in W with which a battery charges. */
        const val CHARGING_POWER = 1000.0

        /** A battery of 360 kJ, which takes 6 min to charge, and powers a host at half load for 40 min. */
        val LARGE_BATTERY = Energy.ofKWh(0.1)

        /** A battery of 72 kJ, which takes 72 s to charge, and powers a host at half load for 8 min. */
        val SMALL_BATTERY = Energy.ofKWh(0.02)
    }
}
