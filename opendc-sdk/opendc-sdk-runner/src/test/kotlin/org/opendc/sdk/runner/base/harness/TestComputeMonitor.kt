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

package org.opendc.sdk.runner.base.harness

import org.opendc.compute.simulator.telemetry.ComputeMonitor
import org.opendc.compute.simulator.telemetry.table.host.HostTableReader
import org.opendc.compute.simulator.telemetry.table.powerSource.PowerSourceTableReader
import org.opendc.compute.simulator.telemetry.table.service.ServiceTableReader
import org.opendc.compute.simulator.telemetry.table.task.TaskTableReader

/**
 * A [ComputeMonitor] that captures per-record telemetry into in-memory series, ported verbatim from
 * the legacy `opendc-experiments-base` test suite so the ported assertions read against identical
 * fields and values.
 */
class TestComputeMonitor : ComputeMonitor {
    var taskCpuDemands = mutableMapOf<Int, ArrayList<Double>>()
    var taskCpuSupplied = mutableMapOf<Int, ArrayList<Double>>()
    var taskGpuDemands = mutableMapOf<Int, ArrayList<Double?>?>()
    var taskGpuSupplied = mutableMapOf<Int, ArrayList<Double?>?>()

    override fun record(reader: TaskTableReader) {
        val taskName: Int = reader.taskInfo.id

        if (taskName in taskCpuDemands) {
            taskCpuDemands[taskName]?.add(reader.cpuDemand)
            taskCpuSupplied[taskName]?.add(reader.cpuUsage)
        } else {
            taskCpuDemands[taskName] = arrayListOf(reader.cpuDemand)
            taskCpuSupplied[taskName] = arrayListOf(reader.cpuUsage)
        }
        if (taskName in taskGpuDemands) {
            taskGpuDemands[taskName]?.add(reader.gpuDemand)
            taskGpuSupplied[taskName]?.add(reader.gpuUsage)
        } else {
            taskGpuDemands[taskName] = arrayListOf(reader.gpuDemand)
            taskGpuSupplied[taskName] = arrayListOf(reader.gpuUsage)
        }
    }

    var attemptsSuccess = 0
    var attemptsFailure = 0
    var attemptsError = 0
    var tasksPending = 0
    var tasksActive = 0
    var tasksTerminated = 0
    var tasksCompleted = 0

    var timestamps = ArrayList<Long>()
    var absoluteTimestamps = ArrayList<Long>()

    var maxTimestamp = 0L

    override fun record(reader: ServiceTableReader) {
        attemptsSuccess = reader.attemptsSuccess
        attemptsFailure = reader.attemptsFailure
        attemptsError = 0
        tasksPending = reader.tasksPending
        tasksActive = reader.tasksActive
        tasksTerminated = reader.tasksTerminated
        tasksCompleted = reader.tasksCompleted

        timestamps.add(reader.timestamp.toEpochMilli())
        absoluteTimestamps.add(reader.timestampAbsolute.toEpochMilli())
        maxTimestamp = reader.timestamp.toEpochMilli()
    }

    var hostCpuDemands = mutableMapOf<String, ArrayList<Double>>()
    var hostCpuSupplied = mutableMapOf<String, ArrayList<Double>>()
    var hostCpuIdleTimes = mutableMapOf<String, ArrayList<Long>>()
    var hostCpuActiveTimes = mutableMapOf<String, ArrayList<Long>>()
    var hostCpuStealTimes = mutableMapOf<String, ArrayList<Long>>()
    var hostCpuLostTimes = mutableMapOf<String, ArrayList<Long>>()

    var hostGpuDemands = mutableMapOf<String, ArrayList<ArrayList<Double>>>()
    var hostGpuSupplied = mutableMapOf<String, ArrayList<ArrayList<Double>>>()
    var hostGpuIdleTimes = mutableMapOf<String, ArrayList<ArrayList<Long>>>()
    var hostGpuActiveTimes = mutableMapOf<String, ArrayList<ArrayList<Long>>>()
    var hostGpuStealTimes = mutableMapOf<String, ArrayList<ArrayList<Long>>>()
    var hostGpuLostTimes = mutableMapOf<String, ArrayList<ArrayList<Long>>>()

    var hostPowerDraws = mutableMapOf<String, ArrayList<Double>>()
    var hostEnergyUsages = mutableMapOf<String, ArrayList<Double>>()

    override fun record(reader: HostTableReader) {
        val hostName: String = reader.hostInfo.name

        if (hostName !in hostCpuDemands) {
            hostCpuIdleTimes[hostName] = ArrayList()
            hostCpuActiveTimes[hostName] = ArrayList()
            hostCpuStealTimes[hostName] = ArrayList()
            hostCpuLostTimes[hostName] = ArrayList()

            hostCpuDemands[hostName] = ArrayList()
            hostCpuSupplied[hostName] = ArrayList()
            hostPowerDraws[hostName] = ArrayList()
            hostEnergyUsages[hostName] = ArrayList()
        }
        if (hostName !in hostGpuDemands) {
            hostGpuDemands[hostName] = ArrayList()
            hostGpuSupplied[hostName] = ArrayList()
            hostGpuIdleTimes[hostName] = ArrayList()
            hostGpuActiveTimes[hostName] = ArrayList()
            hostGpuStealTimes[hostName] = ArrayList()
            hostGpuLostTimes[hostName] = ArrayList()
        }

        hostCpuDemands[hostName]?.add(reader.cpuDemand)
        hostCpuSupplied[hostName]?.add(reader.cpuUsage)
        hostCpuIdleTimes[hostName]?.add(reader.cpuIdleTime)
        hostCpuActiveTimes[hostName]?.add(reader.cpuActiveTime)
        hostCpuStealTimes[hostName]?.add(reader.cpuStealTime)
        hostCpuLostTimes[hostName]?.add(reader.cpuLostTime)

        hostGpuDemands[hostName]?.add(reader.gpuDemands)
        hostGpuSupplied[hostName]?.add(reader.gpuUsages)
        hostGpuIdleTimes[hostName]?.add(reader.gpuIdleTimes)
        hostGpuActiveTimes[hostName]?.add(reader.gpuActiveTimes)
        hostGpuStealTimes[hostName]?.add(reader.gpuStealTimes)
        hostGpuLostTimes[hostName]?.add(reader.gpuLostTimes)

        hostPowerDraws[hostName]?.add(reader.powerDraw)
        hostEnergyUsages[hostName]?.add(reader.energyUsage)
    }

    var powerDraws = ArrayList<Double>()
    var energyUsages = ArrayList<Double>()

    var carbonIntensities = ArrayList<Double>()
    var carbonEmissions = ArrayList<Double>()

    override fun record(reader: PowerSourceTableReader) {
        powerDraws.add(reader.powerDraw)
        energyUsages.add(reader.energyUsage)

        carbonIntensities.add(reader.carbonIntensity)
        carbonEmissions.add(reader.carbonEmission)
    }
}
