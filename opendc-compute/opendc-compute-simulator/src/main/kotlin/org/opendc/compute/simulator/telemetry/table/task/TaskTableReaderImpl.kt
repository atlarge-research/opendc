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

package org.opendc.compute.simulator.telemetry.table.task

import org.opendc.compute.api.TaskState
import org.opendc.compute.simulator.host.SimHost
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.compute.simulator.telemetry.table.host.HostInfo
import java.time.Duration
import java.time.Instant

/**
 * An aggregator for task metrics before they are reported.
 */
public class TaskTableReaderImpl(
    private val service: ComputeService,
    private val task: ServiceTask,
    private val startTime: Duration = Duration.ofMillis(0),
) : TaskTableReader {
    override fun copy(): TaskTableReader {
        val newTaskTable =
            TaskTableReaderImpl(
                service,
                task,
            )
        newTaskTable.setValues(this)

        return newTaskTable
    }

    override fun setValues(table: TaskTableReader) {
        hostInfo = table.hostInfo

        _hostName = table.hostName
        _timestamp = table.timestamp
        _timestampAbsolute = table.timestampAbsolute

        _cpuLimit = table.cpuLimit
        _cpuDemand = table.cpuDemand
        _cpuUsage = table.cpuUsage
        _cpuActiveTime = table.cpuActiveTime
        _cpuIdleTime = table.cpuIdleTime
        _cpuStealTime = table.cpuStealTime
        _cpuLostTime = table.cpuLostTime
        // GPU stats
        _gpuLimits = table.gpuLimits
        _gpuDemands = table.gpuDemands
        _gpuUsages = table.gpuUsages
        _gpuActiveTimes = table.gpuActiveTimes
        _gpuIdleTimes = table.gpuIdleTimes
        _gpuStealTimes = table.gpuStealTimes
        _gpuLostTimes = table.gpuLostTimes

        _uptime = table.uptime
        _downtime = table.downtime
        _numFailures = table.numFailures
        _numPauses = table.numPauses
        _scheduleTime = table.scheduleTime

        _submissionTime = table.submissionTime
        _finishTime = table.finishTime

        _taskState = table.taskState
    }

    /**
     * The static information about this task.
     */
    override val taskInfo: TaskInfo =
        TaskInfo(
            task.uid.toString(),
            task.name,
            "vm",
            "x86",
            task.flavor.cpuCoreCount,
            task.flavor.memorySize,
        )

    /**
     * The [HostInfo] of the host on which the task is hosted.
     */
    override var hostInfo: HostInfo? = null
    private var simHost: SimHost? = null

    private var _hostName: String? = null
    override val hostName: String?
        get() = _hostName

    private var _timestamp = Instant.MIN
    override val timestamp: Instant
        get() = _timestamp

    private var _timestampAbsolute = Instant.MIN
    override val timestampAbsolute: Instant
        get() = _timestampAbsolute

    override val uptime: Long
        get() = _uptime - previousUptime
    private var _uptime: Long = 0
    private var previousUptime = 0L

    override val downtime: Long
        get() = _downtime - previousDowntime
    private var _downtime: Long = 0
    private var previousDowntime = 0L

    override val numFailures: Int
        get() = _numFailures
    private var _numFailures = 0

    override val numPauses: Int
        get() = _numPauses
    private var _numPauses = 0

    override val submissionTime: Instant?
        get() = _submissionTime
    private var _submissionTime: Instant? = null

    override val scheduleTime: Instant?
        get() = _scheduleTime
    private var _scheduleTime: Instant? = null

    override val finishTime: Instant?
        get() = _finishTime
    private var _finishTime: Instant? = null

    override val cpuLimit: Double
        get() = _cpuLimit
    private var _cpuLimit = 0.0

    override val cpuUsage: Double
        get() = _cpuUsage
    private var _cpuUsage = 0.0

    override val cpuDemand: Double
        get() = _cpuDemand
    private var _cpuDemand = 0.0

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

    override val gpuLimits: DoubleArray?
        get() = _gpuLimits ?: DoubleArray(0)
    private var _gpuLimits: DoubleArray? = null

    override val gpuUsages: DoubleArray?
        get() = _gpuUsages ?: DoubleArray(0)
    private var _gpuUsages: DoubleArray? = null

    override val gpuDemands: DoubleArray?
        get() = _gpuDemands ?: DoubleArray(0)
    private var _gpuDemands: DoubleArray? = null

    override val gpuActiveTimes: LongArray?
        get() {
            val current = _gpuActiveTimes ?: return LongArray(0)
            val previous = previousGpuActiveTimes

            return if (previous == null || current.size != previous.size) { // not sure if I like the second clause
                current
            } else {
                LongArray(current.size) { i -> current[i] - previous[i] }
            }
        }
    private var _gpuActiveTimes: LongArray? = null
    private var previousGpuActiveTimes: LongArray? = null

    override val gpuIdleTimes: LongArray?
        get() {
            val current = _gpuIdleTimes ?: return LongArray(0)
            val previous = previousGpuIdleTimes

            return if (previous == null || current.size != previous.size) { // not sure if I like the second clause
                current
            } else {
                LongArray(current.size) { i -> current[i] - previous[i] }
            }
        }
    private var _gpuIdleTimes: LongArray? = null
    private var previousGpuIdleTimes: LongArray? = null

    override val gpuStealTimes: LongArray?
        get() {
            val current = _gpuStealTimes ?: return LongArray(0)
            val previous = previousGpuStealTimes

            return if (previous == null || current.size != previous.size) {
                current
            } else {
                LongArray(current.size) { i -> current[i] - previous[i] }
            }
        }
    private var _gpuStealTimes: LongArray? = null
    private var previousGpuStealTimes: LongArray? = null

    override val gpuLostTimes: LongArray?
        get() {
            val current = _gpuLostTimes ?: return LongArray(0)
            val previous = previousGpuLostTimes

            return if (previous == null || current.size != previous.size) {
                current
            } else {
                LongArray(current.size) { i -> current[i] - previous[i] }
            }
        }
    private var _gpuLostTimes: LongArray? = null
    private var previousGpuLostTimes: LongArray? = null

    override val taskState: TaskState?
        get() = _taskState
    private var _taskState: TaskState? = null

    /**
     * Record the next cycle.
     */
    override fun record(now: Instant) {
        val newHost = service.lookupHost(task)
        if (newHost != null && newHost.getName() != simHost?.getName()) {
            simHost = newHost
            hostInfo =
                HostInfo(
                    newHost.getName(),
                    newHost.getClusterName(),
                    "x86",
                    newHost.getModel().coreCount,
                    newHost.getModel().cpuCapacity,
                    newHost.getModel().memoryCapacity,
                )
        }

        val cpuStats = simHost?.getCpuStats(task)
        val sysStats = simHost?.getSystemStats(task)
        val gpuStats = simHost?.getGpuStats(task)

        _hostName = task.hostName

        _timestamp = now
        _timestampAbsolute = now + startTime

        _cpuLimit = cpuStats?.capacity ?: 0.0
        _cpuDemand = cpuStats?.demand ?: 0.0
        _cpuUsage = cpuStats?.usage ?: 0.0
        _cpuActiveTime = cpuStats?.activeTime ?: _cpuActiveTime
        _cpuIdleTime = cpuStats?.idleTime ?: _cpuIdleTime
        _cpuStealTime = cpuStats?.stealTime ?: _cpuStealTime
        _cpuLostTime = cpuStats?.lostTime ?: _cpuLostTime
        _uptime = sysStats?.uptime?.toMillis() ?: _uptime
        _downtime = sysStats?.downtime?.toMillis() ?: _downtime

        _numFailures = task.numFailures
        _numPauses = task.numPauses
        _submissionTime = task.submittedAt
        _scheduleTime = task.scheduledAt
        _finishTime = task.finishedAt

        if (gpuStats != null && gpuStats.isNotEmpty()) {
            val size = gpuStats.size
            _gpuLimits = DoubleArray(size) { i -> gpuStats[i].capacity }
            _gpuDemands = DoubleArray(size) { i -> gpuStats[i].demand }
            _gpuUsages = DoubleArray(size) { i -> gpuStats[i].usage }
            _gpuActiveTimes = LongArray(size) { i -> gpuStats[i].activeTime }
            _gpuIdleTimes = LongArray(size) { i -> gpuStats[i].idleTime }
            _gpuStealTimes = LongArray(size) { i -> gpuStats[i].stealTime }
            _gpuLostTimes = LongArray(size) { i -> gpuStats[i].lostTime }
        } else {
            _gpuIdleTimes = null
            _gpuStealTimes = null
            _gpuLostTimes = null
            _gpuIdleTimes = null
            _gpuLimits = null
            _gpuUsages = null
            _gpuDemands = null
            _gpuActiveTimes = null
        }

        _taskState = task.state
    }

    /**
     * Finish the aggregation for this cycle.
     */
    override fun reset() {
        previousUptime = _uptime
        previousDowntime = _downtime
        previousCpuActiveTime = _cpuActiveTime
        previousCpuIdleTime = _cpuIdleTime
        previousCpuStealTime = _cpuStealTime
        previousCpuLostTime = _cpuLostTime
        previousGpuActiveTimes = _gpuActiveTimes
        previousGpuIdleTimes = _gpuIdleTimes
        previousGpuStealTimes = _gpuStealTimes
        previousGpuLostTimes = _gpuLostTimes

        simHost = null
        _cpuLimit = 0.0
    }
}
