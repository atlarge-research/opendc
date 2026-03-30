/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.compute.simulator.telemetry.table.battery

import org.opendc.simulator.compute.power.batteries.BatteryState
import org.opendc.simulator.compute.power.batteries.SimBattery
import java.time.Duration
import java.time.Instant

/**
 * An aggregator for task metrics before they are reported.
 */
public class BatteryTableReaderImpl(
    private val battery: SimBattery,
    private val startTime: Duration = Duration.ofMillis(0),
) : BatteryTableReader {
    override fun copy(): BatteryTableReader {
        val newPowerSourceTable =
            BatteryTableReaderImpl(
                battery,
            )
        newPowerSourceTable.setValues(this)

        return newPowerSourceTable
    }

    override fun setValues(table: BatteryTableReader) {
        _timestamp = table.timestamp
        _timestampAbsolute = table.timestampAbsolute

        _powerDraw = table.powerDraw
        _energyUsage = table.energyUsage
        _embodiedCarbonEmission = table.embodiedCarbonEmission
        _charge = table.charge
        _capacity = table.capacity
        _batteryState = table.batteryState
    }

    public override val batteryInfo: BatteryInfo =
        BatteryInfo(
            battery.name,
            battery.clusterName,
            "XXX",
            battery.capacity,
        )

    private var _timestamp = Instant.MIN
    override val timestamp: Instant
        get() = _timestamp

    private var _timestampAbsolute = Instant.MIN
    override val timestampAbsolute: Instant
        get() = _timestampAbsolute

    override val powerDraw: Double
        get() = _powerDraw
    private var _powerDraw = 0.0

    override val energyUsage: Double
        get() = _energyUsage - previousEnergyUsage
    private var _energyUsage = 0.0
    private var previousEnergyUsage = 0.0

    override val embodiedCarbonEmission: Double
        get() = _embodiedCarbonEmission - previousEmbodiedCarbonEmission
    private var _embodiedCarbonEmission = 0.0
    private var previousEmbodiedCarbonEmission = 0.0

    override val charge: Double
        get() = _charge
    private var _charge = 0.0

    override val capacity: Double
        get() = _capacity
    private var _capacity = 0.0

    override val batteryState: BatteryState
        get() = _batteryState
    private var _batteryState = BatteryState.IDLE

    /**
     * Record the next cycle.
     */
    override fun record(now: Instant) {
        _timestamp = now
        _timestampAbsolute = now + startTime

        battery.updateCounters()
        _powerDraw = battery.outgoingSupply
        _energyUsage = battery.totalEnergyUsage
        _embodiedCarbonEmission = battery.embodiedCarbonEmission

        _charge = battery.charge
        _capacity = battery.capacity
        _batteryState = battery.batteryState
    }

    /**
     * Finish the aggregation for this cycle.
     */
    override fun reset() {
        previousEnergyUsage = _energyUsage
        previousEmbodiedCarbonEmission = _embodiedCarbonEmission

        _powerDraw = 0.0
        _energyUsage = 0.0
        _embodiedCarbonEmission = 0.0
        _charge = 0.0
        _capacity = 0.0
        _batteryState = BatteryState.IDLE
    }
}
