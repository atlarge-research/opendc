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

package org.opendc.sdk.runner.sink

/**
 * The metrics captured in memory by an [InMemorySink], as strongly-typed samples per table.
 *
 * Each list holds one immutable sample per recorded metric snapshot (one per host/task/etc. per
 * export tick). A table not selected for capture is an empty list.
 */
public data class CollectedMetrics(
    public val host: List<HostSample> = emptyList(),
    public val task: List<TaskSample> = emptyList(),
    public val service: List<ServiceSample> = emptyList(),
    public val powerSource: List<PowerSourceSample> = emptyList(),
    public val battery: List<BatterySample> = emptyList(),
) : SinkResult

/** A single host's state at one metric snapshot. Times are in milliseconds; energy in joules. */
public data class HostSample(
    public val timestamp: Long,
    public val timestampAbsolute: Long,
    public val host: String,
    public val cluster: String,
    public val tasksActive: Int,
    public val tasksTerminated: Int,
    public val cpuCapacity: Double,
    public val cpuUsage: Double,
    public val cpuDemand: Double,
    public val cpuUtilization: Double,
    public val cpuActiveTime: Long,
    public val cpuIdleTime: Long,
    public val cpuStealTime: Long,
    public val cpuLostTime: Long,
    public val gpuUsages: List<Double>,
    public val gpuDemands: List<Double>,
    public val gpuUtilizations: List<Double>,
    public val gpuPowerDraws: List<Double>,
    public val powerDraw: Double,
    public val energyUsage: Double,
    public val embodiedCarbon: Double,
    public val uptime: Long,
    public val downtime: Long,
)

/** A single task's state at one metric snapshot. */
public data class TaskSample(
    public val timestamp: Long,
    public val timestampAbsolute: Long,
    public val taskId: Int,
    public val taskName: String?,
    public val hostName: String?,
    public val state: String?,
    public val cpuLimit: Double,
    public val cpuUsage: Double,
    public val cpuDemand: Double,
    public val cpuActiveTime: Long,
    public val cpuIdleTime: Long,
    public val cpuStealTime: Long,
    public val cpuLostTime: Long,
    public val gpuLimit: Double?,
    public val gpuUsage: Double?,
    public val gpuDemand: Double?,
    public val uptime: Long,
    public val downtime: Long,
    public val numFailures: Int,
    public val numPauses: Int,
    public val submissionTime: Long?,
    public val scheduleTime: Long?,
    public val finishTime: Long?,
    public val schedulingDelay: Long,
    public val failureDelay: Long,
    public val checkpointDelay: Long,
)

/** The compute service's aggregate state at one metric snapshot. */
public data class ServiceSample(
    public val timestamp: Long,
    public val timestampAbsolute: Long,
    public val hostsUp: Int,
    public val hostsDown: Int,
    public val tasksTotal: Int,
    public val tasksPending: Int,
    public val tasksActive: Int,
    public val tasksCompleted: Int,
    public val tasksTerminated: Int,
    public val attemptsSuccess: Int,
    public val attemptsFailure: Int,
)

/** A single power source's state at one metric snapshot. Carbon intensity is gCO2/kWh. */
public data class PowerSourceSample(
    public val timestamp: Long,
    public val timestampAbsolute: Long,
    public val hostsConnected: Int,
    public val powerDraw: Double,
    public val energyUsage: Double,
    public val carbonIntensity: Double,
    public val carbonEmission: Double,
)

/** A single battery's state at one metric snapshot. Charge and capacity are in kWh. */
public data class BatterySample(
    public val timestamp: Long,
    public val timestampAbsolute: Long,
    public val powerDraw: Double,
    public val energyUsage: Double,
    public val embodiedCarbonEmission: Double,
    public val charge: Double,
    public val capacity: Double,
    public val state: String,
)
