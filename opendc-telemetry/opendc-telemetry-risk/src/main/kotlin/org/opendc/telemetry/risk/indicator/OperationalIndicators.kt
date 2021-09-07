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

@file:JvmName("OperationalIndicators")
package org.opendc.telemetry.risk.indicator

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.opendc.telemetry.compute.table.HostTableReader
import java.time.Instant

/**
 * A [RiskIndicator] for the CPU utilization of the hosts.
 *
 * @param percentile The percentile to consider.
 */
public fun hostCpuSaturation(percentile: Double): RiskIndicator {
    return object : RiskIndicator {
        private val stats = DescriptiveStatistics()

        override fun record(reader: HostTableReader) {
            val cpuTotal = reader.cpuActiveTime + reader.cpuIdleTime
            val utilization = if (cpuTotal > 0) reader.cpuActiveTime.toDouble() / cpuTotal else 0.0
            stats.addValue(utilization)
        }

        override fun collect(now: Instant): List<RiskIndicatorPoint> {
            if (stats.n == 0L) {
                return emptyList()
            }

            val p = stats.getPercentile(percentile * 100.0)
            return listOf(RiskIndicatorPoint.Single(null, now, p))
        }

        override fun reset(now: Instant) {
            stats.clear()
        }

        override fun toString(): String = "HostCpuSaturationIndicator[percentile=$percentile]"
    }
}

/**
 * A [RiskIndicator] for the imbalance of the host utilization.
 */
public fun hostCpuImbalance(): RiskIndicator {
    return object : RiskIndicator {
        private val stats = DescriptiveStatistics()

        override fun record(reader: HostTableReader) {
            val cpuTotal = reader.cpuActiveTime + reader.cpuIdleTime
            val utilization = if (cpuTotal > 0) reader.cpuActiveTime.toDouble() / cpuTotal else 0.0
            stats.addValue(utilization)
        }

        override fun collect(now: Instant): List<RiskIndicatorPoint> {
            if (stats.n == 0L) {
                return emptyList()
            }

            val p = stats.standardDeviation
            return listOf(RiskIndicatorPoint.Single(null, now, p))
        }

        override fun reset(now: Instant) {
            stats.clear()
        }

        override fun toString(): String = "HostCpuImbalanceIndicator"
    }
}
