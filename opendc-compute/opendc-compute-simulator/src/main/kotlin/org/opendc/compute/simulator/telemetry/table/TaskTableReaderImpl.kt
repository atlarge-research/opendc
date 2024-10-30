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

package org.opendc.compute.simulator.telemetry.table

import org.opendc.compute.api.TaskState
import org.opendc.compute.simulator.host.SimHost
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.simulator.service.ServiceTask
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
        host = table.host

        _timestamp = table.timestamp
        _timestampAbsolute = table.timestampAbsolute

        _cpuLimit = table.cpuLimit
        _cpuActiveTime = table.cpuActiveTime
        _cpuIdleTime = table.cpuIdleTime
        _cpuStealTime = table.cpuStealTime
        _cpuLostTime = table.cpuLostTime
        _uptime = table.uptime
        _downtime = table.downtime
        _provisionTime = table.provisionTime
        _bootTime = table.bootTime
        _bootTimeAbsolute = table.bootTimeAbsolute

        _creationTime = table.creationTime
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
            task.flavor.coreCount,
            task.flavor.memorySize,
        )

    /**
     * The [HostInfo] of the host on which the task is hosted.
     */
    override var host: HostInfo? = null
    private var _host: SimHost? = null

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

    override val provisionTime: Instant?
        get() = _provisionTime
    private var _provisionTime: Instant? = null

    override val bootTime: Instant?
        get() = _bootTime
    private var _bootTime: Instant? = null

    override val creationTime: Instant?
        get() = _creationTime
    private var _creationTime: Instant? = null

    override val finishTime: Instant?
        get() = _finishTime
    private var _finishTime: Instant? = null

    override val cpuLimit: Double
        get() = _cpuLimit
    private var _cpuLimit = 0.0

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

    override val bootTimeAbsolute: Instant?
        get() = _bootTimeAbsolute
    private var _bootTimeAbsolute: Instant? = null

    override val taskState: TaskState?
        get() = _taskState
    private var _taskState: TaskState? = null

    /**
     * Record the next cycle.
     */
    override fun record(now: Instant) {
        val newHost = service.lookupHost(task)
        if (newHost != null && newHost.getUid() != _host?.getUid()) {
            _host = newHost
            host =
                HostInfo(
                    newHost.getUid().toString(),
                    newHost.getName(),
                    "x86",
                    newHost.getModel().coreCount,
                    newHost.getModel().cpuCapacity,
                    newHost.getModel().memoryCapacity,
                )
        }

        val cpuStats = _host?.getCpuStats(task)
        val sysStats = _host?.getSystemStats(task)

        _timestamp = now
        _timestampAbsolute = now + startTime

        _cpuLimit = cpuStats?.capacity ?: 0.0
        _cpuActiveTime = cpuStats?.activeTime ?: 0
        _cpuIdleTime = cpuStats?.idleTime ?: 0
        _cpuStealTime = cpuStats?.stealTime ?: 0
        _cpuLostTime = cpuStats?.lostTime ?: 0
        _uptime = sysStats?.uptime?.toMillis() ?: 0
        _downtime = sysStats?.downtime?.toMillis() ?: 0
        _provisionTime = task.launchedAt
        _bootTime = sysStats?.bootTime
        _creationTime = task.createdAt
        _finishTime = task.finishedAt

        _taskState = task.state

        if (sysStats != null) {
            _bootTimeAbsolute = sysStats.bootTime + startTime
        } else {
            _bootTimeAbsolute = null
        }
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

        _host = null
        _cpuLimit = 0.0
    }
}
