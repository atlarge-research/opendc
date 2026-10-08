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

import org.opendc.simulator.compute.service.ComputeService
import org.opendc.simulator.compute.task.SimTask
import org.opendc.simulator.compute.task.TaskState
import java.time.Instant

public class TaskSampler(
    private val service: ComputeService,
) {
    public fun sample(
        now: Instant,
        task: SimTask,
    ): TaskSample {
        val simHost = task.host
        val cpuStats = simHost?.getCpuStats(task)
        val gpuStats = simHost?.getGpuStats(task)

        val hostId = task.hostId

        val timestamp = now

        val numFailures = task.numFailures
        val numPauses = task.numPauses
        val scheduleTime = task.scheduledAt?.toEpochMs()?.toLong()
        val finishTime = task.finishedAt?.toEpochMs()?.toLong()

        val schedulingDelay = task.schedulingDelay.toMsLong()
        val workload = checkNotNull(task.workload) { "Task ${task.id} has no workload" }
        val failureDelay = workload.failureDelay()
        val checkpointDelay = workload.checkpointDelay()

        val taskState = task.state

        // A task that does not run uses and demands nothing. The machine of a run that just ended still reports its last
        // usage and demand, so these are only taken from it while the task runs.
        val isRunning = taskState == TaskState.RUNNING

        val cpuDemand = if (isRunning) cpuStats?.demand ?: 0.0 else 0.0
        val cpuUsage = if (isRunning) cpuStats?.usage ?: 0.0 else 0.0
        val cpuActiveTime = cpuStats?.activeTime ?: 0L
        val cpuIdleTime = cpuStats?.idleTime ?: 0L
        val cpuStealTime = cpuStats?.stealTime ?: 0L
        val cpuLostTime = cpuStats?.lostTime ?: 0L

        var gpuUsage = 0.0
        var gpuDemand = 0.0
        var gpuActiveTime = 0L
        var gpuIdleTime = 0L
        var gpuStealTime = 0L
        var gpuLostTime = 0L

        if (gpuStats != null) {
            if (isRunning) {
                gpuUsage = gpuStats.usage
                gpuDemand = gpuStats.demand
            }
            gpuActiveTime = gpuStats.activeTime
            gpuIdleTime = gpuStats.idleTime
            gpuStealTime = gpuStats.stealTime
            gpuLostTime = gpuStats.lostTime
        }

        return TaskSample(
            taskId = task.id,
            hostId = hostId,
            timestamp = timestamp,
            numFailures = numFailures,
            numPauses = numPauses,
            scheduleTime = scheduleTime,
            finishTime = finishTime,
            schedulingDelay = schedulingDelay,
            failureDelay = failureDelay,
            checkpointDelay = checkpointDelay,
            taskState = taskState,
            cpuUsage = cpuUsage,
            cpuDemand = cpuDemand,
            cpuActiveTime = cpuActiveTime,
            cpuIdleTime = cpuIdleTime,
            cpuStealTime = cpuStealTime,
            cpuLostTime = cpuLostTime,
            gpuUsage = gpuUsage,
            gpuDemand = gpuDemand,
            gpuActiveTime = gpuActiveTime,
            gpuIdleTime = gpuIdleTime,
            gpuStealTime = gpuStealTime,
            gpuLostTime = gpuLostTime,
        )
    }
}
