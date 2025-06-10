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

        _tasksTerminated = table.tasksTerminated
        _tasksActive = table.tasksActive
        _guestsError = table.guestsError
        _guestsInvalid = table.guestsInvalid
        // CPU stats
        _cpuCapacity = table.cpuCapacity
        _cpuDemand = table.cpuDemand
        _cpuUsage = table.cpuUsage
        _cpuUtilization = table.cpuUtilization
        _cpuActiveTime = table.cpuActiveTime
        _cpuIdleTime = table.cpuIdleTime
        _cpuStealTime = table.cpuStealTime
        _cpuLostTime = table.cpuLostTime
        // GPU stats
        _gpuLimits = table.gpuLimits
        _gpuDemands = table.gpuDemands
        _gpuUsages = table.gpuUsages
        _gpuUtilizations = table.gpuUtilizations
        _gpuActiveTimes = table.gpuActiveTimes
        _gpuIdleTimes = table.gpuIdleTimes
        _gpuStealTimes = table.gpuStealTimes
        _gpuLostTimes = table.gpuLostTimes

        // energy & carbon stats
        _powerDraw = table.powerDraw
        _energyUsage = table.energyUsage
        _embodiedCarbon = table.embodiedCarbon
        _uptime = table.uptime
        _downtime = table.downtime
        _bootTime = table.bootTime
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

    override val tasksTerminated: Int
        get() = _tasksTerminated
    private var _tasksTerminated = 0

    override val tasksActive: Int
        get() = _tasksActive
    private var _tasksActive = 0

    override val guestsError: Int
        get() = _guestsError
    private var _guestsError = 0

    override val guestsInvalid: Int
        get() = _guestsInvalid
    private var _guestsInvalid = 0

    override val cpuCapacity: Double
        get() = _cpuCapacity
    private var _cpuCapacity = 0.0

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

    override val gpuLimits: ArrayList<Double>
        get() = _gpuLimits
    private var _gpuLimits: ArrayList<Double> = ArrayList()

    override val gpuUsages: ArrayList<Double>
        get() = _gpuUsages
    private var _gpuUsages: ArrayList<Double> = ArrayList()

    override val gpuDemands: ArrayList<Double>
        get() = _gpuDemands
    private var _gpuDemands: ArrayList<Double> = ArrayList()

    override val gpuUtilizations: ArrayList<Double>
        get() = _gpuUtilizations
    private var _gpuUtilizations: ArrayList<Double> = ArrayList()

    // half of the CPU stats
    override val gpuActiveTimes: ArrayList<Long>
//        get() = _gpuActiveTimes.zip(previousGpuActiveTimes) { current, previous -> current - previous} as ArrayList<Long>
        get() = (0 until _gpuActiveTimes.size).map { i -> (_gpuActiveTimes.getOrNull(i) ?: 0L) - (previousGpuActiveTimes.getOrNull(i) ?: 0L) } as ArrayList<Long>
    private var _gpuActiveTimes: ArrayList<Long> = ArrayList()
    private var previousGpuActiveTimes: ArrayList<Long> = ArrayList()

    override val gpuIdleTimes: ArrayList<Long>
//        get() = _gpuIdleTimes.zip(previousGpuIdleTimes) { current, previous -> current - previous} as ArrayList<Long>
        get() = (0 until _gpuIdleTimes.size).map { i -> (_gpuIdleTimes.getOrNull(i) ?: 0L) - (previousGpuIdleTimes.getOrNull(i) ?: 0L) } as ArrayList<Long>
    private var _gpuIdleTimes : ArrayList<Long> = ArrayList()
    private var previousGpuIdleTimes : ArrayList<Long> = ArrayList()

    override val gpuStealTimes: ArrayList<Long>
        get() = (0 until _gpuStealTimes.size).map { i -> (_gpuStealTimes.getOrNull(i) ?: 0L) - (previousGpuStealTimes.getOrNull(i) ?: 0L) } as ArrayList<Long>
    private var _gpuStealTimes : ArrayList<Long> = ArrayList()
    private var previousGpuStealTimes : ArrayList<Long> = ArrayList()

    override val gpuLostTimes: ArrayList<Long>
        get() = (0 until _gpuLostTimes.size).map { i -> (_gpuLostTimes.getOrNull(i) ?: 0L) - (previousGpuLostTimes.getOrNull(i) ?: 0L) } as ArrayList<Long>
    private var _gpuLostTimes : ArrayList<Long> = ArrayList()
    private var previousGpuLostTimes : ArrayList<Long> = ArrayList()

    override val powerDraw: Double
        get() = _powerDraw
    private var _powerDraw = 0.0

    override val energyUsage: Double
        get() = _energyUsage - previousEnergyUsage
    private var _energyUsage = 0.0
    private var previousEnergyUsage = 0.0

    override val embodiedCarbon: Double
        get() = _embodiedCarbon
    private var _embodiedCarbon = 0.0

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

    /**
     * Record the next cycle.
     */
    override fun record(now: Instant) {
        val hostCpuStats = host.getCpuStats()
        val hostSysStats = host.getSystemStats()
        val hostGpuStats = host.getGpuStats()

        _timestamp = now
        _timestampAbsolute = now + startTime

        _tasksTerminated = hostSysStats.guestsTerminated
        _tasksActive = hostSysStats.guestsRunning
        _guestsError = hostSysStats.guestsError
        _guestsInvalid = hostSysStats.guestsInvalid
        _cpuCapacity = hostCpuStats.capacity
        _cpuDemand = hostCpuStats.demand
        _cpuUsage = hostCpuStats.usage
        _cpuUtilization = hostCpuStats.utilization
        _cpuActiveTime = hostCpuStats.activeTime
        _cpuIdleTime = hostCpuStats.idleTime
        _cpuStealTime = hostCpuStats.stealTime
        _cpuLostTime = hostCpuStats.lostTime
        // GPU stats
        _gpuLimits = hostGpuStats.map { it.capacity } as ArrayList<Double>
        _gpuDemands = hostGpuStats.map { it.demand } as ArrayList<Double>
        _gpuUsages = hostGpuStats.map { it.usage } as ArrayList<Double>
        _gpuUtilizations = hostGpuStats.map { it.utilization } as ArrayList<Double>
        _gpuActiveTimes = hostGpuStats.map { it.activeTime } as ArrayList<Long>
        _gpuIdleTimes = hostGpuStats.map { it.idleTime } as ArrayList<Long>
        _gpuStealTimes = hostGpuStats.map { it.stealTime } as ArrayList<Long>
        _gpuLostTimes = hostGpuStats.map { it.lostTime } as ArrayList<Long>
        // energy & carbon stats
        _powerDraw = hostSysStats.powerDraw
        _energyUsage = hostSysStats.energyUsage
        _embodiedCarbon = hostSysStats.embodiedCarbon
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
        previousGpuActiveTimes = _gpuActiveTimes
        previousGpuIdleTimes = _gpuIdleTimes
        previousGpuStealTimes = _gpuStealTimes
        previousGpuLostTimes = _gpuLostTimes
        previousEnergyUsage = _energyUsage
        previousUptime = _uptime
        previousDowntime = _downtime

        _tasksTerminated = 0
        _tasksActive = 0
        _guestsError = 0
        _guestsInvalid = 0

        _cpuCapacity = 0.0
        _cpuUsage = 0.0
        _cpuDemand = 0.0
        _cpuUtilization = 0.0

        _powerDraw = 0.0
        _energyUsage = 0.0
        _embodiedCarbon = 0.0
    }
}
