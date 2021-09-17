/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.web.runner

import org.opendc.telemetry.compute.ComputeMonitor
import org.opendc.telemetry.compute.table.HostData
import org.opendc.telemetry.compute.table.ServiceData
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * A [ComputeMonitor] that tracks the aggregate metrics for each repeat.
 */
class WebComputeMonitor : ComputeMonitor {
    override fun record(data: HostData) {
        val slices = data.downtime / SLICE_LENGTH

        hostAggregateMetrics = AggregateHostMetrics(
            hostAggregateMetrics.totalActiveTime + data.cpuActiveTime,
            hostAggregateMetrics.totalIdleTime + data.cpuIdleTime,
            hostAggregateMetrics.totalStealTime + data.cpuStealTime,
            hostAggregateMetrics.totalLostTime + data.cpuLostTime,
            hostAggregateMetrics.totalPowerDraw + data.powerTotal,
            hostAggregateMetrics.totalFailureSlices + slices,
            hostAggregateMetrics.totalFailureVmSlices + data.guestsRunning * slices
        )

        hostMetrics.compute(data.host.id) { _, prev ->
            HostMetrics(
                data.cpuUsage + (prev?.cpuUsage ?: 0.0),
                data.cpuDemand + (prev?.cpuDemand ?: 0.0),
                data.guestsRunning + (prev?.instanceCount ?: 0),
                1 + (prev?.count ?: 0)
            )
        }
    }

    private var hostAggregateMetrics: AggregateHostMetrics = AggregateHostMetrics()
    private val hostMetrics: MutableMap<String, HostMetrics> = mutableMapOf()
    private val SLICE_LENGTH: Long = 5 * 60

    data class AggregateHostMetrics(
        val totalActiveTime: Long = 0L,
        val totalIdleTime: Long = 0L,
        val totalStealTime: Long = 0L,
        val totalLostTime: Long = 0L,
        val totalPowerDraw: Double = 0.0,
        val totalFailureSlices: Double = 0.0,
        val totalFailureVmSlices: Double = 0.0,
    )

    data class HostMetrics(
        val cpuUsage: Double,
        val cpuDemand: Double,
        val instanceCount: Long,
        val count: Long
    )

    private var serviceMetrics: AggregateServiceMetrics = AggregateServiceMetrics()

    override fun record(data: ServiceData) {
        serviceMetrics = AggregateServiceMetrics(
            max(data.attemptsSuccess, serviceMetrics.vmTotalCount),
            max(data.serversPending, serviceMetrics.vmWaitingCount),
            max(data.serversActive, serviceMetrics.vmActiveCount),
            max(0, serviceMetrics.vmInactiveCount),
            max(data.attemptsFailure, serviceMetrics.vmFailedCount),
        )
    }

    data class AggregateServiceMetrics(
        val vmTotalCount: Int = 0,
        val vmWaitingCount: Int = 0,
        val vmActiveCount: Int = 0,
        val vmInactiveCount: Int = 0,
        val vmFailedCount: Int = 0
    )

    fun getResult(): Result {
        return Result(
            hostAggregateMetrics.totalActiveTime,
            hostAggregateMetrics.totalIdleTime,
            hostAggregateMetrics.totalStealTime,
            hostAggregateMetrics.totalLostTime,
            hostMetrics.map { it.value.cpuUsage / it.value.count }.average(),
            hostMetrics.map { it.value.cpuDemand / it.value.count }.average(),
            hostMetrics.map { it.value.instanceCount.toDouble() / it.value.count }.average(),
            hostMetrics.map { it.value.instanceCount.toDouble() / it.value.count }.maxOrNull() ?: 0.0,
            hostAggregateMetrics.totalPowerDraw,
            hostAggregateMetrics.totalFailureSlices.roundToLong(),
            hostAggregateMetrics.totalFailureVmSlices.roundToLong(),
            serviceMetrics.vmTotalCount,
            serviceMetrics.vmWaitingCount,
            serviceMetrics.vmInactiveCount,
            serviceMetrics.vmFailedCount,
        )
    }

    data class Result(
        val totalActiveTime: Long,
        val totalIdleTime: Long,
        val totalStealTime: Long,
        val totalLostTime: Long,
        val meanCpuUsage: Double,
        val meanCpuDemand: Double,
        val meanNumDeployedImages: Double,
        val maxNumDeployedImages: Double,
        val totalPowerDraw: Double,
        val totalFailureSlices: Long,
        val totalFailureVmSlices: Long,
        val totalVmsSubmitted: Int,
        val totalVmsQueued: Int,
        val totalVmsFinished: Int,
        val totalVmsFailed: Int
    )
}
