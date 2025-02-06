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

package org.opendc.compute.simulator.telemetry.table.host

import org.opendc.compute.simulator.host.SimHost
import java.time.Duration
import java.time.Instant

/**
 * An aggregator for host metrics before they are reported.
 */
public class HostTableReaderImpl(
    private val host: SimHost,
    private val startTime: Duration = Duration.ofMillis(0),
) : HostTableReader {
    override fun copy(): HostTableReader {
        val newHostTable =
            HostTableReaderImpl(host)
        newHostTable.setValues(this)

        return newHostTable
    }

    override fun setValues(table: HostTableReader) {
        _timestamp = table.timestamp
        _timestampAbsolute = table.timestampAbsolute

        _guestsTerminated = table.guestsTerminated
        _guestsRunning = table.guestsRunning
        _guestsError = table.guestsError
        _guestsInvalid = table.guestsInvalid
        _cpuLimit = table.cpuLimit
        _cpuDemand = table.cpuDemand
        _cpuUsage = table.cpuUsage
        _cpuUtilization = table.cpuUtilization
        _cpuActiveTime = table.cpuActiveTime
        _cpuIdleTime = table.cpuIdleTime
        _cpuStealTime = table.cpuStealTime
        _cpuLostTime = table.cpuLostTime
        _powerDraw = table.powerDraw
        _energyUsage = table.energyUsage
        _uptime = table.uptime
        _downtime = table.downtime
        _bootTime = table.bootTime
        _bootTimeAbsolute = table.bootTimeAbsolute
    }

    override val hostInfo: HostInfo =
        HostInfo(
            host.getName(),
            host.getClusterName(),
            "x86",
            host.getModel().coreCount,
            host.getModel().cpuCapacity,
            host.getModel().memoryCapacity,
        )

    override val timestamp: Instant
        get() = _timestamp
    private var _timestamp = Instant.MIN

    override val timestampAbsolute: Instant
        get() = _timestampAbsolute
    private var _timestampAbsolute = Instant.MIN

    override val guestsTerminated: Int
        get() = _guestsTerminated
    private var _guestsTerminated = 0

    override val guestsRunning: Int
        get() = _guestsRunning
    private var _guestsRunning = 0

    override val guestsError: Int
        get() = _guestsError
    private var _guestsError = 0

    override val guestsInvalid: Int
        get() = _guestsInvalid
    private var _guestsInvalid = 0

    override val cpuLimit: Double
        get() = _cpuLimit
    private var _cpuLimit = 0.0

    override val cpuUsage: Double
        get() = _cpuUsage
    private var _cpuUsage = 0.0

    override val cpuDemand: Double
        get() = _cpuDemand
    private var _cpuDemand = 0.0

    override val cpuUtilization: Double
        get() = _cpuUtilization
    private var _cpuUtilization = 0.0

    override val cpuActiveTime: Long
        get() = _cpuActiveTime - previousCpuActiveTime
    private var _cpuActiveTime = 0L
    private var previousCpuActiveTime = 0L

    override val cpuIdleTime: Long
        get() = _cpuIdleTime - previousCpuIdleTime
    private var _cpuIdleTime = 0L
    private var previousCpuIdleTime = 0L

    override val cpuStealTime: Long
        get() = _cpuStealTime - previousCpuStealTime
    private var _cpuStealTime = 0L
    private var previousCpuStealTime = 0L

    override val cpuLostTime: Long
        get() = _cpuLostTime - previousCpuLostTime
    private var _cpuLostTime = 0L
    private var previousCpuLostTime = 0L

    override val powerDraw: Double
        get() = _powerDraw
    private var _powerDraw = 0.0

    override val energyUsage: Double
        get() = _energyUsage - previousEnergyUsage
    private var _energyUsage = 0.0
    private var previousEnergyUsage = 0.0

    override val uptime: Long
        get() = _uptime - previousUptime
    private var _uptime = 0L
    private var previousUptime = 0L

    override val downtime: Long
        get() = _downtime - previousDowntime
    private var _downtime = 0L
    private var previousDowntime = 0L

    override val bootTime: Instant?
        get() = _bootTime
    private var _bootTime: Instant? = null

    override val bootTimeAbsolute: Instant?
        get() = _bootTimeAbsolute
    private var _bootTimeAbsolute: Instant? = null

    /**
     * Record the next cycle.
     */
    override fun record(now: Instant) {
        val hostCpuStats = host.getCpuStats()
        val hostSysStats = host.getSystemStats()

        _timestamp = now
        _timestampAbsolute = now + startTime

        _guestsTerminated = hostSysStats.guestsTerminated
        _guestsRunning = hostSysStats.guestsRunning
        _guestsError = hostSysStats.guestsError
        _guestsInvalid = hostSysStats.guestsInvalid
        _cpuLimit = hostCpuStats.capacity
        _cpuDemand = hostCpuStats.demand
        _cpuUsage = hostCpuStats.usage
        _cpuUtilization = hostCpuStats.utilization
        _cpuActiveTime = hostCpuStats.activeTime
        _cpuIdleTime = hostCpuStats.idleTime
        _cpuStealTime = hostCpuStats.stealTime
        _cpuLostTime = hostCpuStats.lostTime
        _powerDraw = hostSysStats.powerDraw
        _energyUsage = hostSysStats.energyUsage
        _uptime = hostSysStats.uptime.toMillis()
        _downtime = hostSysStats.downtime.toMillis()
        _bootTime = hostSysStats.bootTime
        _bootTime = hostSysStats.bootTime + startTime
    }

    /**
     * Finish the aggregation for this cycle.
     */
    override fun reset() {
        // Reset intermediate state for next aggregation
        previousCpuActiveTime = _cpuActiveTime
        previousCpuIdleTime = _cpuIdleTime
        previousCpuStealTime = _cpuStealTime
        previousCpuLostTime = _cpuLostTime
        previousEnergyUsage = _energyUsage
        previousUptime = _uptime
        previousDowntime = _downtime

        _guestsTerminated = 0
        _guestsRunning = 0
        _guestsError = 0
        _guestsInvalid = 0

        _cpuLimit = 0.0
        _cpuUsage = 0.0
        _cpuDemand = 0.0
        _cpuUtilization = 0.0

        _powerDraw = 0.0
        _energyUsage = 0.0
    }
}
