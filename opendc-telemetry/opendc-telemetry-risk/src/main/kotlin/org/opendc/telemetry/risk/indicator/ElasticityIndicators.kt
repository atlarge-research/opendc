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

@file:JvmName("ElasticityIndicators")
package org.opendc.telemetry.risk.indicator

import org.opendc.telemetry.compute.table.HostInfo
import org.opendc.telemetry.compute.table.ServerInfo
import org.opendc.telemetry.compute.table.ServerTableReader
import java.time.Instant

/**
 * A [RiskIndicator] for the limiting the scheduling latency of VMs.
 */
public fun schedulingLatency(): RiskIndicator {
    return SchedulingLatencyIndicator()
}

/**
 * A [RiskIndicator] for the limiting the scheduling latency of VMs.
 */
internal class SchedulingLatencyIndicator : RiskIndicator {
    private var lastReset: Instant = Instant.MAX
    private val state = HashMap<String, ServerState>()

    override fun record(reader: ServerTableReader) {
        val state = state.getOrPut(reader.server.id) { ServerState(reader.server, reader.host) }
        state.provisionTime = reader.provisionTime

        if (state.bootTime == null) {
            state.bootTime = reader.bootTime
        }
    }

    override fun collect(now: Instant): List<RiskIndicatorPoint> = state.mapNotNull { (_, state) ->
        val lastReset = lastReset
        val provisionTime = state.provisionTime

        if (provisionTime != null) {
            val bootTime = state.bootTime ?: now
            val startTime = maxOf(provisionTime, lastReset) // We only count the latency since last collection
            val latency = (bootTime.toEpochMilli() - startTime.toEpochMilli()).coerceAtLeast(0)

            RiskIndicatorPoint.Single(ServerKey(state.server, state.host), now, latency.toDouble())
        } else {
            null
        }
    }

    override fun reset(now: Instant) {
        lastReset = now
    }

    override fun toString(): String = "SchedulingLatencyIndicator"

    /**
     * Helper class to collect the good/bad value for the indicator.
     */
    private data class ServerState(
        @JvmField val server: ServerInfo,
        @JvmField val host: HostInfo?
    ) {
        @JvmField var provisionTime: Instant? = null
        @JvmField var bootTime: Instant? = null
    }
}
