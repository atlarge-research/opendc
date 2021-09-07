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

@file:JvmName("QoSIndicators")
package org.opendc.telemetry.risk.indicator

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.opendc.telemetry.compute.table.*
import java.time.Instant

/**
 * A [RiskIndicator] for the server CPU ready percentage.
 */
public fun cpuReady(): RiskIndicator {
    return CpuReadyIndicator()
}

/**
 * A [RiskIndicator] for the server CPU interference percentage.
 */
public fun cpuInterference(): RiskIndicator {
    return CpuInterferenceIndicator()
}

/**
 * A [RiskIndicator] for the host CPU ready percentage.
 *
 * @param percentile The percentile to consider.
 */
public fun hostCpuReady(percentile: Double): RiskIndicator {
    return object : RiskIndicator {
        private val stats = DescriptiveStatistics()

        override fun record(reader: HostTableReader) {
            val cpuTotal = reader.cpuActiveTime + reader.cpuIdleTime
            val cpuReady = if (cpuTotal > 0) reader.cpuStealTime.toDouble() / cpuTotal else 0.0
            stats.addValue(cpuReady)
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

        override fun toString(): String = "HostCpuReadyIndicator[percentile=$percentile]"
    }
}

/**
 * A [RiskIndicator] for the host CPU interference percentage.
 *
 * @param percentile The percentile to consider.
 */
public fun hostCpuInterference(percentile: Double): RiskIndicator {
    return object : RiskIndicator {
        private val stats = DescriptiveStatistics()

        override fun record(reader: HostTableReader) {
            val cpuTotal = reader.cpuActiveTime + reader.cpuIdleTime
            val cpuLost = if (cpuTotal > 0) (reader.cpuStealTime + reader.cpuLostTime).toDouble() / cpuTotal else 0.0
            stats.addValue(cpuLost)
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

        override fun toString(): String = "HostCpuInterferenceIndicator[percentile=$percentile]"
    }
}

/**
 * A [RiskIndicator] for CPU ready percentage.
 */
internal class CpuReadyIndicator : RiskIndicator {
    private val state = HashMap<String, ServerState>()

    override fun record(reader: ServerTableReader) {
        val state = state.getOrPut(reader.server.id) { ServerState(reader.server, reader.host) }
        state.totalTime += reader.cpuActiveTime + reader.cpuIdleTime
        state.stealTime += reader.cpuStealTime
    }

    override fun collect(now: Instant): List<RiskIndicatorPoint> = state.map { (_, state) ->
        val stealTime = state.stealTime.toDouble()
        val total = (state.totalTime).toDouble()
        val ratio = if (total > 0.0) stealTime / total else 0.0

        RiskIndicatorPoint.Ratio(ServerKey(state.server, state.host), now, stealTime, total, ratio)
    }

    override fun reset(now: Instant) {
        for ((_, state) in state) {
            state.totalTime = 0
            state.stealTime = 0
        }
    }

    override fun toString(): String = "CpuReadyIndicator"

    /**
     * Helper class to collect the good/bad value for the indicator.
     */
    private data class ServerState(@JvmField val server: ServerInfo, @JvmField val host: HostInfo?) {
        @JvmField var totalTime: Long = 0
        @JvmField var stealTime: Long = 0
    }
}

/**
 * A [RiskIndicator] for the server CPU interference percentage.
 */
internal class CpuInterferenceIndicator : RiskIndicator {
    private val state = HashMap<String, ServerState>()

    override fun record(reader: ServerTableReader) {
        val state = state.computeIfAbsent(reader.server.id) { ServerState(reader.server, reader.host) }
        state.totalTime += reader.cpuActiveTime + reader.cpuIdleTime
        state.lostTime += reader.cpuStealTime + reader.cpuLostTime
    }

    override fun collect(now: Instant): List<RiskIndicatorPoint> {
        return state.map { (_, state) ->
            val lostTime = state.lostTime.toDouble()
            val total = (state.totalTime).toDouble()
            val ratio = if (total > 0.0) lostTime / total else 0.0

            RiskIndicatorPoint.Ratio(ServerKey(state.server, state.host), now, lostTime, total, ratio)
        }
    }

    override fun reset(now: Instant) {
        for ((_, state) in state) {
            state.totalTime = 0
            state.lostTime = 0
        }
    }

    override fun toString(): String = "CpuInterferenceIndicator"

    /**
     * Helper class to collect the good/bad value for the indicator.
     */
    private data class ServerState(@JvmField val server: ServerInfo, @JvmField val host: HostInfo?) {
        @JvmField var totalTime: Long = 0
        @JvmField var lostTime: Long = 0
    }
}
