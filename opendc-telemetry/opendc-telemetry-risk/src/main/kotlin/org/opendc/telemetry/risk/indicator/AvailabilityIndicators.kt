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

@file:JvmName("AvailabilityIndicators")
package org.opendc.telemetry.risk.indicator

import org.opendc.telemetry.compute.table.*
import java.time.Instant

/**
 * A [RiskIndicator] for the availability percentage of the active servers in the system.
 */
public fun serverAvailability(): RiskIndicator {
    return ServerAvailabilityIndicator()
}

/**
 * A [RiskIndicator] for the availability of the hosts.
 */
public fun hostAvailability(): RiskIndicator {
    return HostAvailabilityIndicator()
}

/**
 * A [RiskIndicator] for the availability percentage of servers.
 */
internal class ServerAvailabilityIndicator : RiskIndicator {
    private val state = HashMap<String, ServerState>()

    override fun record(reader: ServerTableReader) {
        val state = state.computeIfAbsent(reader.server.id) { ServerState(reader.server, reader.host) }
        state.uptime += reader.uptime
        state.downtime += reader.downtime
    }

    override fun collect(now: Instant): List<RiskIndicatorPoint> = state.map { (_, state) ->
        val uptime = state.uptime.toDouble()
        val total = (state.uptime + state.downtime).toDouble()
        val ratio = if (total > 0.0) uptime / total else 1.0

        RiskIndicatorPoint.Ratio(ServerKey(state.server, state.host), now, uptime, total, ratio)
    }

    override fun reset(now: Instant) {
        state.clear()
    }

    override fun toString(): String = "ServerAvailabilityIndicator"

    /**
     * Helper class to collect the good/bad value for the indicator.
     */
    private data class ServerState(
        @JvmField val server: ServerInfo,
        @JvmField val host: HostInfo?,
        @JvmField var uptime: Long = 0,
        @JvmField var downtime: Long = 0
    )
}

/**
 * A [RiskIndicator] for the availability of the hosts.
 */
internal class HostAvailabilityIndicator : RiskIndicator {
    private val state = HashMap<String, HostState>()

    override fun record(reader: HostTableReader) {
        val state = state.computeIfAbsent(reader.host.id) { HostState(reader.host) }
        state.uptime += reader.uptime
        state.downtime += reader.downtime
    }

    override fun collect(now: Instant): List<RiskIndicatorPoint> = state.map { (_, state) ->
        val uptime = state.uptime.toDouble()
        val total = (state.uptime + state.downtime).toDouble()
        val ratio = if (total > 0.0) uptime / total else 1.0

        RiskIndicatorPoint.Ratio(HostKey(state.host), now, uptime, total, ratio)
    }

    override fun reset(now: Instant) {
        state.clear()
    }

    override fun toString(): String = "HostAvailabilityObjective"

    /**
     * Helper class to collect the good/bad value for the indicator.
     */
    private data class HostState(
        @JvmField val host: HostInfo,
        @JvmField var uptime: Long = 0,
        @JvmField var downtime: Long = 0
    )
}
