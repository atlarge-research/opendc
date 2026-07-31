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

package org.opendc.sdk.runner.telemetry.table.task

import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.simulator.service.ServiceTask
import java.time.Duration
import java.time.Instant

public class TaskSampler(
    private val service: ComputeService,
    private val startTime: Duration = Duration.ofMillis(0),
) {
    public fun sample(
        now: Instant,
        task: ServiceTask,
    ): TaskSample {
        val simHost = service.lookupHost(task)
        val cpuStats = simHost?.getCpuStats(task)
        val sysStats = simHost?.getSystemStats(task)
        val gpuStats = simHost?.getGpuStats(task)

        val hostName = task.hostName

        val timestamp = now
        val timestampAbsolute = now + startTime

        // TODO: This metric currently doesn't function well. It will round to the sample rate.
        val uptime = sysStats?.uptime?.toMillis() ?: 0L
        val downtime = sysStats?.downtime?.toMillis() ?: 0L

        val numFailures = task.numFailures
        val numPauses = task.numPauses
        val submissionTime = task.submittedAt
        val scheduleTime = task.scheduledAt
        val finishTime = task.finishedAt

        val schedulingDelay = task.schedulingDelay
        val failureDelay = task.workload.failureDelay()
        val checkpointDelay = task.workload.checkpointDelay()

        val taskState = task.state

        val cpuLimit = cpuStats?.capacity ?: 0.0
        val cpuDemand = cpuStats?.demand ?: 0.0
        val cpuUsage = cpuStats?.usage ?: 0.0
        val cpuActiveTime = cpuStats?.activeTime ?: 0L
        val cpuIdleTime = cpuStats?.idleTime ?: 0L
        val cpuStealTime = cpuStats?.stealTime ?: 0L
        val cpuLostTime = cpuStats?.lostTime ?: 0L

        var gpuLimit = 0.0
        var gpuUsage = 0.0
        var gpuDemand = 0.0
        var gpuActiveTime = 0L
        var gpuIdleTime = 0L
        var gpuStealTime = 0L
        var gpuLostTime = 0L

        if (gpuStats != null) {
            gpuLimit = gpuStats.capacity
            gpuUsage = gpuStats.usage
            gpuDemand = gpuStats.demand
            gpuActiveTime = gpuStats.activeTime
            gpuIdleTime = gpuStats.idleTime
            gpuStealTime = gpuStats.stealTime
            gpuLostTime = gpuStats.lostTime
        }

        return TaskSample(
            taskId = task.id,
            memCapacity = task.memorySize,
            cpuCount = task.cpuCoreCount,
            gpuCount = task.gpuCoreCount,
            hostName = hostName,
            timestamp = timestamp,
            timestampAbsolute = timestampAbsolute,
            uptime = uptime,
            downtime = downtime,
            numFailures = numFailures,
            numPauses = numPauses,
            submissionTime = submissionTime,
            scheduleTime = scheduleTime,
            finishTime = finishTime,
            schedulingDelay = schedulingDelay,
            failureDelay = failureDelay,
            checkpointDelay = checkpointDelay,
            taskState = taskState,
            cpuLimit = cpuLimit,
            cpuUsage = cpuUsage,
            cpuDemand = cpuDemand,
            cpuActiveTime = cpuActiveTime,
            cpuIdleTime = cpuIdleTime,
            cpuStealTime = cpuStealTime,
            cpuLostTime = cpuLostTime,
            gpuLimit = gpuLimit,
            gpuUsage = gpuUsage,
            gpuDemand = gpuDemand,
            gpuActiveTime = gpuActiveTime,
            gpuIdleTime = gpuIdleTime,
            gpuStealTime = gpuStealTime,
            gpuLostTime = gpuLostTime,
        )
    }
}
