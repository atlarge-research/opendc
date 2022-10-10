/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.web.runner.internal

import org.opendc.experiments.compute.telemetry.ComputeMonitor
import org.opendc.experiments.compute.telemetry.table.HostTableReader
import org.opendc.experiments.compute.telemetry.table.ServiceData
import org.opendc.experiments.compute.telemetry.table.ServiceTableReader
import org.opendc.experiments.compute.telemetry.table.toServiceData
import kotlin.math.roundToLong

/**
 * A [ComputeMonitor] that tracks the aggregate metrics for each repeat.
 */
internal class WebComputeMonitor : ComputeMonitor {
    override fun record(reader: HostTableReader) {
        val slices = reader.downtime / SLICE_LENGTH

        hostAggregateMetrics = AggregateHostMetrics(
            hostAggregateMetrics.totalActiveTime + reader.cpuActiveTime,
            hostAggregateMetrics.totalIdleTime + reader.cpuIdleTime,
            hostAggregateMetrics.totalStealTime + reader.cpuStealTime,
            hostAggregateMetrics.totalLostTime + reader.cpuLostTime,
            hostAggregateMetrics.totalPowerDraw + reader.powerTotal,
            hostAggregateMetrics.totalFailureSlices + slices,
            hostAggregateMetrics.totalFailureVmSlices + reader.guestsRunning * slices
        )

        hostMetrics.compute(reader.host.id) { _, prev ->
            HostMetrics(
                reader.cpuUsage + (prev?.cpuUsage ?: 0.0),
                reader.cpuDemand + (prev?.cpuDemand ?: 0.0),
                reader.guestsRunning + (prev?.instanceCount ?: 0),
                1 + (prev?.count ?: 0)
            )
        }
    }

    private var hostAggregateMetrics: AggregateHostMetrics = AggregateHostMetrics()
    private val hostMetrics: MutableMap<String, HostMetrics> = mutableMapOf()
    private val SLICE_LENGTH: Long = 5 * 60L

    private data class AggregateHostMetrics(
        val totalActiveTime: Long = 0L,
        val totalIdleTime: Long = 0L,
        val totalStealTime: Long = 0L,
        val totalLostTime: Long = 0L,
        val totalPowerDraw: Double = 0.0,
        val totalFailureSlices: Double = 0.0,
        val totalFailureVmSlices: Double = 0.0
    )

    private data class HostMetrics(
        val cpuUsage: Double,
        val cpuDemand: Double,
        val instanceCount: Long,
        val count: Long
    )

    private lateinit var serviceData: ServiceData

    override fun record(reader: ServiceTableReader) {
        serviceData = reader.toServiceData()
    }

    /**
     * Collect the results of the simulation.
     */
    fun collectResults(): Results {
        val hostAggregateMetrics = hostAggregateMetrics
        val hostMetrics = hostMetrics
        val serviceData = serviceData

        return Results(
            hostAggregateMetrics.totalActiveTime,
            hostAggregateMetrics.totalIdleTime,
            hostAggregateMetrics.totalStealTime,
            hostAggregateMetrics.totalLostTime,
            hostMetrics.map { it.value.cpuUsage / it.value.count }.average().let { if (it.isNaN()) 0.0 else it },
            hostMetrics.map { it.value.cpuDemand / it.value.count }.average().let { if (it.isNaN()) 0.0 else it },
            hostMetrics.map { it.value.instanceCount.toDouble() / it.value.count }.average().let { if (it.isNaN()) 0.0 else it },
            hostMetrics.map { it.value.instanceCount.toDouble() / it.value.count }.maxOrNull() ?: 0.0,
            hostAggregateMetrics.totalPowerDraw,
            hostAggregateMetrics.totalFailureSlices.roundToLong(),
            hostAggregateMetrics.totalFailureVmSlices.roundToLong(),
            serviceData.serversTotal,
            serviceData.serversPending,
            serviceData.serversTotal - serviceData.serversPending - serviceData.serversActive,
            serviceData.attemptsError + serviceData.attemptsFailure
        )
    }

    /**
     * Structure of the results of a single simulation.
     */
    data class Results(
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
