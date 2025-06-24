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
        _gpuLimit = table.gpuLimit
        _gpuDemand = table.gpuDemand
        _gpuUsage = table.gpuUsage
        _gpuActiveTime = table.gpuActiveTime
        _gpuIdleTime = table.gpuIdleTime
        _gpuStealTime = table.gpuStealTime
        _gpuLostTime = table.gpuLostTime

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
            task.flavor.gpuCoreCount,
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

    override val gpuLimit: Double?
        get() = _gpuLimit
    private var _gpuLimit: Double? = null

    override val gpuUsage: Double?
        get() = _gpuUsage
    private var _gpuUsage: Double? = null

    override val gpuDemand: Double?
        get() = _gpuDemand
    private var _gpuDemand: Double? = null

    override val gpuActiveTime: Long?
        get() = (_gpuActiveTime ?: 0L) - (previousGpuActiveTime ?: 0L)
    private var _gpuActiveTime: Long? = null
    private var previousGpuActiveTime: Long? = null

    override val gpuIdleTime: Long?
        get() = (_gpuIdleTime ?: 0L) - (previousGpuIdleTime ?: 0L)
    private var _gpuIdleTime: Long? = null
    private var previousGpuIdleTime: Long? = null

    override val gpuStealTime: Long?
        get() = (_gpuStealTime ?: 0L) - (previousGpuStealTime ?: 0L)
    private var _gpuStealTime: Long? = null
    private var previousGpuStealTime: Long? = null

    override val gpuLostTime: Long?
        get() = (_gpuLostTime ?: 0L) - (previousGpuLostTime ?: 0L)
    private var _gpuLostTime: Long? = null
    private var previousGpuLostTime: Long? = null

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

        if (gpuStats != null) {
            _gpuLimit = gpuStats.capacity
            _gpuDemand = gpuStats.demand
            _gpuUsage = gpuStats.usage
            _gpuActiveTime = gpuStats.activeTime
            _gpuIdleTime = gpuStats.idleTime
            _gpuStealTime = gpuStats.stealTime
            _gpuLostTime = gpuStats.lostTime
        } else {
            _gpuIdleTime = null
            _gpuStealTime = null
            _gpuLostTime = null
            _gpuIdleTime = null
            _gpuLimit = null
            _gpuUsage = null
            _gpuDemand = null
            _gpuActiveTime = null
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
        previousGpuActiveTime = _gpuActiveTime
        previousGpuIdleTime = _gpuIdleTime
        previousGpuStealTime = _gpuStealTime
        previousGpuLostTime = _gpuLostTime

        simHost = null
        _cpuLimit = 0.0
    }
}
