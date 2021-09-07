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
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.data.PointData
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import org.opendc.telemetry.compute.table.*
import java.time.Instant
import kotlin.math.roundToLong

/**
 * Helper class responsible for aggregating [MetricData] into [ServiceData], [HostData] and [ServerData].
 */
public class ComputeMetricAggregator {
    private val _service = ServiceAggregator()
    private val _hosts = mutableMapOf<String, HostAggregator>()
    private val _servers = mutableMapOf<String, ServerAggregator>()

    /**
     * Process the specified [metrics] for this cycle.
     */
    public fun process(metrics: Collection<MetricData>) {
        val service = _service
        val hosts = _hosts
        val servers = _servers

        for (metric in metrics) {
            val resource = metric.resource

            when (metric.name) {
                // ComputeService
                "scheduler.hosts" -> {
                    for (point in metric.longSumData.points) {
                        when (point.attributes[STATE_KEY]) {
                            "up" -> service.hostsUp = point.value.toInt()
                            "down" -> service.hostsDown = point.value.toInt()
                        }
                    }
                }
                "scheduler.servers" -> {
                    for (point in metric.longSumData.points) {
                        when (point.attributes[STATE_KEY]) {
                            "pending" -> service.serversPending = point.value.toInt()
                            "active" -> service.serversActive = point.value.toInt()
                        }
                    }
                }
                "scheduler.attempts" -> {
                    for (point in metric.longSumData.points) {
                        when (point.attributes[RESULT_KEY]) {
                            "success" -> service.attemptsSuccess = point.value.toInt()
                            "failure" -> service.attemptsFailure = point.value.toInt()
                            "error" -> service.attemptsError = point.value.toInt()
                        }
                    }
                }
                "scheduler.latency" -> {
                    for (point in metric.doubleHistogramData.points) {
                        val server = getServer(servers, point) ?: continue
                        server.schedulingLatency = (point.sum / point.count).roundToLong()
                    }
                }

                // SimHost
                "system.guests" -> {
                    val agg = getHost(hosts, resource) ?: continue

                    for (point in metric.longSumData.points) {
                        when (point.attributes[STATE_KEY]) {
                            "terminated" -> agg.guestsTerminated = point.value.toInt()
                            "running" -> agg.guestsRunning = point.value.toInt()
                            "error" -> agg.guestsRunning = point.value.toInt()
                            "invalid" -> agg.guestsInvalid = point.value.toInt()
                        }
                    }
                }
                "system.cpu.limit" -> {
                    val agg = getHost(hosts, resource) ?: continue

                    for (point in metric.doubleGaugeData.points) {
                        val server = getServer(servers, point)

                        if (server != null) {
                            server.cpuLimit = point.value
                            server.host = agg.host
                        } else {
                            agg.cpuLimit = point.value
                        }
                    }
                }
                "system.cpu.usage" -> {
                    val agg = getHost(hosts, resource) ?: continue
                    agg.cpuUsage = metric.doubleGaugeData.points.first().value
                }
                "system.cpu.demand" -> {
                    val agg = getHost(hosts, resource) ?: continue
                    agg.cpuDemand = metric.doubleGaugeData.points.first().value
                }
                "system.cpu.utilization" -> {
                    val agg = getHost(hosts, resource) ?: continue
                    agg.cpuUtilization = metric.doubleGaugeData.points.first().value
                }
                "system.cpu.time" -> {
                    val agg = getHost(hosts, resource) ?: continue

                    for (point in metric.longSumData.points) {
                        val server = getServer(servers, point)
                        val state = point.attributes[STATE_KEY]
                        if (server != null) {
                            when (state) {
                                "active" -> server.cpuActiveTime = point.value
                                "idle" -> server.cpuIdleTime = point.value
                                "steal" -> server.cpuStealTime = point.value
                                "lost" -> server.cpuLostTime = point.value
                            }
                            server.host = agg.host
                        } else {
                            when (state) {
                                "active" -> agg.cpuActiveTime = point.value
                                "idle" -> agg.cpuIdleTime = point.value
                                "steal" -> agg.cpuStealTime = point.value
                                "lost" -> agg.cpuLostTime = point.value
                            }
                        }
                    }
                }
                "system.power.usage" -> {
                    val agg = getHost(hosts, resource) ?: continue
                    agg.powerUsage = metric.doubleGaugeData.points.first().value
                }
                "system.power.total" -> {
                    val agg = getHost(hosts, resource) ?: continue
                    agg.powerTotal = metric.doubleSumData.points.first().value
                }
                "system.time" -> {
                    val agg = getHost(hosts, resource) ?: continue

                    for (point in metric.longSumData.points) {
                        val server = getServer(servers, point)

                        if (server != null) {
                            when (point.attributes[STATE_KEY]) {
                                "up" -> server.uptime = point.value
                                "down" -> server.downtime = point.value
                            }
                            server.host = agg.host
                        } else {
                            when (point.attributes[STATE_KEY]) {
                                "up" -> agg.uptime = point.value
                                "down" -> agg.downtime = point.value
                            }
                        }
                    }
                }
                "system.time.boot" -> {
                    val agg = getHost(hosts, resource) ?: continue

                    for (point in metric.longGaugeData.points) {
                        val server = getServer(servers, point)

                        if (server != null) {
                            server.bootTime = point.value
                            server.host = agg.host
                        } else {
                            agg.bootTime = point.value
                        }
                    }
                }
            }
        }
    }

    /**
     * Collect the data via the [monitor].
     */
    public fun collect(now: Instant, monitor: ComputeMonitor) {
        monitor.record(_service.collect(now))

        for (host in _hosts.values) {
            monitor.record(host.collect(now))
        }

        for (server in _servers.values) {
            monitor.record(server.collect(now))
        }
    }

    /**
     * Obtain the [HostAggregator] for the specified [resource].
     */
    private fun getHost(hosts: MutableMap<String, HostAggregator>, resource: Resource): HostAggregator? {
        val id = resource.attributes[HOST_ID]
        return if (id != null) {
            hosts.computeIfAbsent(id) { HostAggregator(resource) }
        } else {
            null
        }
    }

    /**
     * Obtain the [ServerAggregator] for the specified [point].
     */
    private fun getServer(servers: MutableMap<String, ServerAggregator>, point: PointData): ServerAggregator? {
        val id = point.attributes[ResourceAttributes.HOST_ID]
        return if (id != null) {
            servers.computeIfAbsent(id) { ServerAggregator(point.attributes) }
        } else {
            null
        }
    }

    /**
     * An aggregator for service metrics before they are reported.
     */
    internal class ServiceAggregator {
        @JvmField var hostsUp = 0
        @JvmField var hostsDown = 0

        @JvmField var serversPending = 0
        @JvmField var serversActive = 0

        @JvmField var attemptsSuccess = 0
        @JvmField var attemptsFailure = 0
        @JvmField var attemptsError = 0

        /**
         * Finish the aggregation for this cycle.
         */
        fun collect(now: Instant): ServiceData = toServiceData(now)

        /**
         * Convert the aggregator state to an immutable [ServiceData].
         */
        private fun toServiceData(now: Instant): ServiceData {
            return ServiceData(now, hostsUp, hostsDown, serversPending, serversActive, attemptsSuccess, attemptsFailure, attemptsError)
        }
    }

    /**
     * An aggregator for host metrics before they are reported.
     */
    internal class HostAggregator(resource: Resource) {
        /**
         * The static information about this host.
         */
        val host = HostInfo(
            resource.attributes[HOST_ID]!!,
            resource.attributes[HOST_NAME] ?: "",
            resource.attributes[HOST_ARCH] ?: "",
            resource.attributes[HOST_NCPUS]?.toInt() ?: 0,
            resource.attributes[HOST_MEM_CAPACITY] ?: 0,
        )

        @JvmField var guestsTerminated = 0
        @JvmField var guestsRunning = 0
        @JvmField var guestsError = 0
        @JvmField var guestsInvalid = 0

        @JvmField var cpuLimit = 0.0
        @JvmField var cpuUsage = 0.0
        @JvmField var cpuDemand = 0.0
        @JvmField var cpuUtilization = 0.0

        @JvmField var cpuActiveTime = 0L
        @JvmField var cpuIdleTime = 0L
        @JvmField var cpuStealTime = 0L
        @JvmField var cpuLostTime = 0L
        private var previousCpuActiveTime = 0L
        private var previousCpuIdleTime = 0L
        private var previousCpuStealTime = 0L
        private var previousCpuLostTime = 0L

        @JvmField var powerUsage = 0.0
        @JvmField var powerTotal = 0.0
        private var previousPowerTotal = 0.0

        @JvmField var uptime = 0L
        private var previousUptime = 0L
        @JvmField var downtime = 0L
        private var previousDowntime = 0L
        @JvmField var bootTime = Long.MIN_VALUE

        /**
         * Finish the aggregation for this cycle.
         */
        fun collect(now: Instant): HostData {
            val data = toHostData(now)

            // Reset intermediate state for next aggregation
            previousCpuActiveTime = cpuActiveTime
            previousCpuIdleTime = cpuIdleTime
            previousCpuStealTime = cpuStealTime
            previousCpuLostTime = cpuLostTime
            previousPowerTotal = powerTotal
            previousUptime = uptime
            previousDowntime = downtime

            guestsTerminated = 0
            guestsRunning = 0
            guestsError = 0
            guestsInvalid = 0

            cpuLimit = 0.0
            cpuUsage = 0.0
            cpuDemand = 0.0
            cpuUtilization = 0.0

            powerUsage = 0.0

            return data
        }

        /**
         * Convert the aggregator state to an immutable [HostData] instance.
         */
        private fun toHostData(now: Instant): HostData {
            return HostData(
                now,
                host,
                guestsTerminated,
                guestsRunning,
                guestsError,
                guestsInvalid,
                cpuLimit,
                cpuUsage,
                cpuDemand,
                cpuUtilization,
                cpuActiveTime - previousCpuActiveTime,
                cpuIdleTime - previousCpuIdleTime,
                cpuStealTime - previousCpuStealTime,
                cpuLostTime - previousCpuLostTime,
                powerUsage,
                powerTotal - previousPowerTotal,
                uptime - previousUptime,
                downtime - previousDowntime,
                if (bootTime != Long.MIN_VALUE) Instant.ofEpochMilli(bootTime) else null
            )
        }
    }

    /**
     * An aggregator for server metrics before they are reported.
     */
    internal class ServerAggregator(attributes: Attributes) {
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
        @JvmField var bootTime: Long = 0
        @JvmField var schedulingLatency = 0L
        @JvmField var cpuLimit = 0.0
        @JvmField var cpuActiveTime = 0L
        @JvmField var cpuIdleTime = 0L
        @JvmField var cpuStealTime = 0L
        @JvmField var cpuLostTime = 0L
        private var previousCpuActiveTime = 0L
        private var previousCpuIdleTime = 0L
        private var previousCpuStealTime = 0L
        private var previousCpuLostTime = 0L

        /**
         * Finish the aggregation for this cycle.
         */
        fun collect(now: Instant): ServerData {
            val data = toServerData(now)

            previousUptime = uptime
            previousDowntime = downtime
            previousCpuActiveTime = cpuActiveTime
            previousCpuIdleTime = cpuIdleTime
            previousCpuStealTime = cpuStealTime
            previousCpuLostTime = cpuLostTime

            host = null
            cpuLimit = 0.0

            return data
        }

        /**
         * Convert the aggregator state into an immutable [ServerData].
         */
        private fun toServerData(now: Instant): ServerData {
            return ServerData(
                now,
                server,
                host,
                uptime - previousUptime,
                downtime - previousDowntime,
                if (bootTime != Long.MIN_VALUE) Instant.ofEpochMilli(bootTime) else null,
                schedulingLatency,
                cpuLimit,
                cpuActiveTime - previousCpuActiveTime,
                cpuIdleTime - previousCpuIdleTime,
                cpuStealTime - previousCpuStealTime,
                cpuLostTime - previousCpuLostTime
            )
        }
    }

    private companion object {
        private val STATE_KEY = AttributeKey.stringKey("state")
        private val RESULT_KEY = AttributeKey.stringKey("result")
    }
}
