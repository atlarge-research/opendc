/*
 * Copyright (c) 2021 AtLarge Research
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

@file:JvmName("EnvironmentalIndicators")
package org.opendc.telemetry.risk.indicator

import org.opendc.telemetry.compute.table.HostTableReader
import java.time.Instant

/**
 * A [RiskIndicator] that measures the total power used by the datacenter.
 *
 * @param pue The PUE of the datacenter.
 */
public fun powerConsumption(pue: Double = 1.0): RiskIndicator {
    return object : RiskIndicator {
        private val J_TO_KWH = 1.0 / 3_600_000
        var power = 0.0

        override fun record(reader: HostTableReader) {
            power += reader.powerTotal
        }

        override fun collect(now: Instant): List<RiskIndicatorPoint> {
            val totalPower = pue * power * J_TO_KWH
            return listOf(RiskIndicatorPoint.Single(null, now, totalPower))
        }

        override fun reset(now: Instant) {
            power = 0.0
        }

        override fun toString(): String = "PowerConsumptionIndicator[pue=$pue]"
    }
}

/**
 * A [RiskIndicator] that measures the CO2 emissions caused by the entire datacenter.
 *
 * @param pue The PUE of the datacenter.
 * @param footprint The amount of CO2 emissions (KG) per kWh.
 */
public fun co2Emissions(pue: Double, footprint: Double): RiskIndicator {
    return object : RiskIndicator {
        private val KWH_TO_J = 1.0 / 3_600_000
        var power = 0.0

        override fun record(reader: HostTableReader) {
            power += reader.powerTotal
        }

        override fun collect(now: Instant): List<RiskIndicatorPoint> {
            val total = pue * power * KWH_TO_J * footprint
            return listOf(RiskIndicatorPoint.Single(null, now, total))
        }

        override fun reset(now: Instant) {
            power = 0.0
        }

        override fun toString(): String = "CO2EmissionsIndicator[pue=$pue,footprint=$footprint]"
    }
}
