/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.sdk.runner.telemetry.table.host

import org.opendc.compute.simulator.host.SimHost
import java.time.Duration
import java.time.Instant

public class HostSampler(
    private val startTime: Duration = Duration.ofMillis(0),
) {
    public fun sample(
        now: Instant,
        host: SimHost,
    ): HostSample {
        val hostCpuStats = host.getCpuStats()
        val hostSysStats = host.getSystemStats()
        val hostGpuStats = host.getGpuStats()

        val timestamp = now
        val timestampAbsolute = now + startTime

        val tasksTerminated = hostSysStats.guestsTerminated
        val tasksActive = hostSysStats.guestsRunning
        val guestsError = hostSysStats.guestsError
        val guestsInvalid = hostSysStats.guestsInvalid
        val cpuCapacity = hostCpuStats.capacity
        val cpuDemand = hostCpuStats.demand
        val cpuUsage = hostCpuStats.usage
        val cpuUtilization = hostCpuStats.utilization
        val cpuActiveTime = hostCpuStats.activeTime
        val cpuIdleTime = hostCpuStats.idleTime
        val cpuStealTime = hostCpuStats.stealTime
        val cpuLostTime = hostCpuStats.lostTime

        // GPU stats
        val gpuCapacities = hostGpuStats.map { it.capacity } as ArrayList<Double>
        val gpuDemands = hostGpuStats.map { it.demand } as ArrayList<Double>
        val gpuUsages = hostGpuStats.map { it.usage } as ArrayList<Double>
        val gpuUtilizations = hostGpuStats.map { it.utilization } as ArrayList<Double>
        val gpuActiveTimes = hostGpuStats.map { it.activeTime } as ArrayList<Long>
        val gpuIdleTimes = hostGpuStats.map { it.idleTime } as ArrayList<Long>
        val gpuStealTimes = hostGpuStats.map { it.stealTime } as ArrayList<Long>
        val gpuLostTimes = hostGpuStats.map { it.lostTime } as ArrayList<Long>
        val gpuPowerDraws = hostGpuStats.map { it.powerDraw } as ArrayList<Double>

        // energy & carbon stats
        val powerDraw = hostSysStats.powerDraw
        val energyUsage = hostSysStats.energyUsage
        val embodiedCarbon = hostSysStats.embodiedCarbon
        val uptime = hostSysStats.uptime.toMillis()
        val downtime = hostSysStats.downtime.toMillis()
        val bootTime = hostSysStats.bootTime + startTime

        return HostSample(
            hostName = host.getName(),
            clusterName = host.getClusterName(),
            coreCount = host.getModel().coreCount,
            memCapacity = host.getModel().memoryCapacity,
            timestamp = timestamp,
            timestampAbsolute = timestampAbsolute,
            tasksTerminated = tasksTerminated,
            tasksActive = tasksActive,
            guestsError = guestsError,
            guestsInvalid = guestsInvalid,
            cpuCapacity = cpuCapacity,
            cpuDemand = cpuDemand,
            cpuUsage = cpuUsage,
            cpuUtilization = cpuUtilization,
            cpuActiveTime = cpuActiveTime,
            cpuIdleTime = cpuIdleTime,
            cpuStealTime = cpuStealTime,
            cpuLostTime = cpuLostTime,
            gpuCapacities = gpuCapacities,
            gpuDemands = gpuDemands,
            gpuUsages = gpuUsages,
            gpuUtilizations = gpuUtilizations,
            gpuActiveTimes = gpuActiveTimes,
            gpuIdleTimes = gpuIdleTimes,
            gpuStealTimes = gpuStealTimes,
            gpuLostTimes = gpuLostTimes,
            gpuPowerDraws = gpuPowerDraws,
            powerDraw = powerDraw,
            energyUsage = energyUsage,
            embodiedCarbon = embodiedCarbon,
            uptime = uptime,
            downtime = downtime,
            bootTime = bootTime,
        )
    }
}
