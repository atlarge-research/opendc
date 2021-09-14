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

package org.opendc.telemetry.compute

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.data.*
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import org.opendc.telemetry.compute.table.HostData
import org.opendc.telemetry.compute.table.HostInfo
import org.opendc.telemetry.compute.table.ServerData
import org.opendc.telemetry.compute.table.ServerInfo
import java.time.Clock

/**
 * A [MetricExporter] that redirects data to a [ComputeMonitor] implementation.
 */
public class ComputeMetricExporter(private val clock: Clock, private val monitor: ComputeMonitor) : MetricExporter {
    override fun export(metrics: Collection<MetricData>): CompletableResultCode {
        return try {
            reportServiceMetrics(metrics)
            reportHostMetrics(metrics)
            reportServerMetrics(metrics)
            CompletableResultCode.ofSuccess()
        } catch (e: Throwable) {
            CompletableResultCode.ofFailure()
        }
    }

    override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()

    private fun reportServiceMetrics(metrics: Collection<MetricData>) {
        monitor.record(extractServiceMetrics(clock.millis(), metrics))
    }

    private val hosts = mutableMapOf<String, HostAggregator>()
    private val servers = mutableMapOf<String, ServerAggregator>()

    private fun reportHostMetrics(metrics: Collection<MetricData>) {
        val hosts = hosts
        val servers = servers

        for (metric in metrics) {
            val resource = metric.resource
            val hostId = resource.attributes[HOST_ID] ?: continue
            val agg = hosts.computeIfAbsent(hostId) { HostAggregator(resource) }
            agg.accept(metric)
        }

        val monitor = monitor
        val now = clock.millis()
        for ((_, server) in servers) {
            server.record(monitor, now)
        }
    }

    private fun reportServerMetrics(metrics: Collection<MetricData>) {
        val hosts = hosts

        for (metric in metrics) {
            val resource = metric.resource
            val host = resource.attributes[HOST_ID]?.let { hosts[it]?.host }

            when (metric.name) {
                "scheduler.duration" -> mapByServer(metric.doubleHistogramData.points, host) { agg, point ->
                    agg.schedulingLatency = point.sum / point.count
                }
                "guest.time.running" -> mapByServer(metric.longSumData.points, host) { agg, point ->
                    agg.uptime = point.value
                }
                "guest.time.error" -> mapByServer(metric.longSumData.points, host) { agg, point ->
                    agg.downtime = point.value
                }
            }
        }

        val monitor = monitor
        val now = clock.millis()
        for ((_, host) in hosts) {
            host.record(monitor, now)
        }
    }

    /**
     * Helper function to map a metric by the server.
     */
    private inline fun <P : PointData> mapByServer(points: Collection<P>, host: HostInfo? = null, block: (ServerAggregator, P) -> Unit) {
        for (point in points) {
            val serverId = point.attributes[ResourceAttributes.HOST_ID] ?: continue
            val agg = servers.computeIfAbsent(serverId) { ServerAggregator(point.attributes) }

            if (host != null) {
                agg.host = host
            }

            block(agg, point)
        }
    }

    /**
     * An aggregator for host metrics before they are reported.
     */
    private class HostAggregator(resource: Resource) {
        /**
         * The static information about this host.
         */
        val host = HostInfo(
            resource.attributes[HOST_ID]!!,
            resource.attributes[HOST_NAME]!!,
            resource.attributes[HOST_ARCH]!!,
            resource.attributes[HOST_NCPUS]!!.toInt(),
            resource.attributes[HOST_MEM_CAPACITY]!!,
        )

        private var totalWork: Double = 0.0
        private var previousTotalWork = 0.0
        private var grantedWork: Double = 0.0
        private var previousGrantedWork = 0.0
        private var overcommittedWork: Double = 0.0
        private var previousOvercommittedWork = 0.0
        private var interferedWork: Double = 0.0
        private var previousInterferedWork = 0.0
        private var cpuUsage: Double = 0.0
        private var cpuDemand: Double = 0.0
        private var instanceCount: Int = 0
        private var powerDraw: Double = 0.0
        private var uptime: Long = 0
        private var previousUptime = 0L
        private var downtime: Long = 0
        private var previousDowntime = 0L

        fun record(monitor: ComputeMonitor, now: Long) {
            monitor.record(
                HostData(
                    now,
                    host,
                    totalWork - previousTotalWork,
                    grantedWork - previousGrantedWork,
                    overcommittedWork - previousOvercommittedWork,
                    interferedWork - previousInterferedWork,
                    cpuUsage,
                    cpuDemand,
                    instanceCount,
                    powerDraw,
                    uptime - previousUptime,
                    downtime - previousDowntime,
                )
            )

            previousTotalWork = totalWork
            previousGrantedWork = grantedWork
            previousOvercommittedWork = overcommittedWork
            previousInterferedWork = interferedWork
            previousUptime = uptime
            previousDowntime = downtime
            reset()
        }

        /**
         * Accept the [MetricData] for this host.
         */
        fun accept(data: MetricData) {
            when (data.name) {
                "cpu.work.total" -> totalWork = data.doubleSumData.points.first().value
                "cpu.work.granted" -> grantedWork = data.doubleSumData.points.first().value
                "cpu.work.overcommit" -> overcommittedWork = data.doubleSumData.points.first().value
                "cpu.work.interference" -> interferedWork = data.doubleSumData.points.first().value
                "power.usage" -> powerDraw = acceptHistogram(data)
                "cpu.usage" -> cpuUsage = acceptHistogram(data)
                "cpu.demand" -> cpuDemand = acceptHistogram(data)
                "guests.active" -> instanceCount = data.longSumData.points.first().value.toInt()
                "host.time.up" -> uptime = data.longSumData.points.first().value
                "host.time.down" -> downtime = data.longSumData.points.first().value
            }
        }

        private fun acceptHistogram(data: MetricData): Double {
            return when (data.type) {
                MetricDataType.HISTOGRAM -> {
                    val point = data.doubleHistogramData.points.first()
                    point.sum / point.count
                }
                MetricDataType.SUMMARY -> {
                    val point = data.doubleSummaryData.points.first()
                    point.sum / point.count
                }
                else -> error("Invalid metric type")
            }
        }

        private fun reset() {
            totalWork = 0.0
            grantedWork = 0.0
            overcommittedWork = 0.0
            interferedWork = 0.0
            cpuUsage = 0.0
            cpuDemand = 0.0
            instanceCount = 0
            powerDraw = 0.0
            uptime = 0L
            downtime = 0L
        }
    }

    /**
     * An aggregator for server metrics before they are reported.
     */
    private class ServerAggregator(attributes: Attributes) {
        /**
         * The static information about this server.
         */
        val server = ServerInfo(
            attributes[ResourceAttributes.HOST_ID]!!,
            attributes[ResourceAttributes.HOST_NAME]!!,
            attributes[ResourceAttributes.HOST_TYPE]!!,
            attributes[ResourceAttributes.HOST_ARCH]!!,
            attributes[ResourceAttributes.HOST_IMAGE_ID]!!,
            attributes[ResourceAttributes.HOST_IMAGE_NAME]!!,
            attributes[AttributeKey.longKey("host.num_cpus")]!!.toInt(),
            attributes[AttributeKey.longKey("host.mem_capacity")]!!,
        )

        /**
         * The [HostInfo] of the host on which the server is hosted.
         */
        var host: HostInfo? = null

        @JvmField var uptime: Long = 0
        private var previousUptime = 0L
        @JvmField var downtime: Long = 0
        private var previousDowntime = 0L
        @JvmField var schedulingLatency = 0.0

        fun record(monitor: ComputeMonitor, now: Long) {
            monitor.record(
                ServerData(
                    now,
                    server,
                    null,
                    uptime - previousUptime,
                    downtime - previousDowntime,
                )
            )

            previousUptime = uptime
            previousDowntime = downtime
            reset()
        }

        private fun reset() {
            host = null
            uptime = 0L
            downtime = 0L
        }
    }
}
