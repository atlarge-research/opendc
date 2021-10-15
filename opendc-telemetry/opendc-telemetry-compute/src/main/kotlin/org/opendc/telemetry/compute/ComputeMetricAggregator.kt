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

@file:Suppress("PropertyName")

package org.opendc.telemetry.compute

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.data.PointData
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import org.opendc.telemetry.compute.table.*
import java.time.Instant

/**
 * Helper class responsible for aggregating [MetricData] into [ServiceTableReader], [HostTableReader] and [ServerTableReader].
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
                        // Record the timestamp for the service
                        service.recordTimestamp(point)

                        when (point.attributes[STATE_KEY]) {
                            "up" -> service._hostsUp = point.value.toInt()
                            "down" -> service._hostsDown = point.value.toInt()
                        }
                    }
                }
                "scheduler.servers" -> {
                    for (point in metric.longSumData.points) {
                        when (point.attributes[STATE_KEY]) {
                            "pending" -> service._serversPending = point.value.toInt()
                            "active" -> service._serversActive = point.value.toInt()
                        }
                    }
                }
                "scheduler.attempts" -> {
                    for (point in metric.longSumData.points) {
                        when (point.attributes[RESULT_KEY]) {
                            "success" -> service._attemptsSuccess = point.value.toInt()
                            "failure" -> service._attemptsFailure = point.value.toInt()
                            "error" -> service._attemptsError = point.value.toInt()
                        }
                    }
                }

                // SimHost
                "system.guests" -> {
                    val agg = getHost(hosts, resource) ?: continue

                    for (point in metric.longSumData.points) {
                        when (point.attributes[STATE_KEY]) {
                            "terminated" -> agg._guestsTerminated = point.value.toInt()
                            "running" -> agg._guestsRunning = point.value.toInt()
                            "error" -> agg._guestsRunning = point.value.toInt()
                            "invalid" -> agg._guestsInvalid = point.value.toInt()
                        }
                    }
                }
                "system.cpu.limit" -> {
                    val agg = getHost(hosts, resource) ?: continue

                    for (point in metric.doubleGaugeData.points) {
                        val server = getServer(servers, point)

                        if (server != null) {
                            server._cpuLimit = point.value
                            server._host = agg.host
                        } else {
                            agg._cpuLimit = point.value
                        }
                    }
                }
                "system.cpu.usage" -> {
                    val agg = getHost(hosts, resource) ?: continue
                    agg._cpuUsage = metric.doubleGaugeData.points.first().value
                }
                "system.cpu.demand" -> {
                    val agg = getHost(hosts, resource) ?: continue
                    agg._cpuDemand = metric.doubleGaugeData.points.first().value
                }
                "system.cpu.utilization" -> {
                    val agg = getHost(hosts, resource) ?: continue
                    agg._cpuUtilization = metric.doubleGaugeData.points.first().value
                }
                "system.cpu.time" -> {
                    val agg = getHost(hosts, resource) ?: continue

                    for (point in metric.longSumData.points) {
                        val server = getServer(servers, point)
                        val state = point.attributes[STATE_KEY]
                        if (server != null) {
                            when (state) {
                                "active" -> server._cpuActiveTime = point.value
                                "idle" -> server._cpuIdleTime = point.value
                                "steal" -> server._cpuStealTime = point.value
                                "lost" -> server._cpuLostTime = point.value
                            }
                            server._host = agg.host
                        } else {
                            when (state) {
                                "active" -> agg._cpuActiveTime = point.value
                                "idle" -> agg._cpuIdleTime = point.value
                                "steal" -> agg._cpuStealTime = point.value
                                "lost" -> agg._cpuLostTime = point.value
                            }
                        }
                    }
                }
                "system.power.usage" -> {
                    val agg = getHost(hosts, resource) ?: continue
                    agg._powerUsage = metric.doubleGaugeData.points.first().value
                }
                "system.power.total" -> {
                    val agg = getHost(hosts, resource) ?: continue
                    agg._powerTotal = metric.doubleSumData.points.first().value
                }
                "system.time" -> {
                    val agg = getHost(hosts, resource) ?: continue

                    for (point in metric.longSumData.points) {
                        val server = getServer(servers, point)

                        if (server != null) {
                            server.recordTimestamp(point)

                            when (point.attributes[STATE_KEY]) {
                                "up" -> server._uptime = point.value
                                "down" -> server._downtime = point.value
                            }
                            server._host = agg.host
                        } else {
                            agg.recordTimestamp(point)

                            when (point.attributes[STATE_KEY]) {
                                "up" -> agg._uptime = point.value
                                "down" -> agg._downtime = point.value
                            }
                        }
                    }
                }
                "system.time.boot" -> {
                    val agg = getHost(hosts, resource) ?: continue

                    for (point in metric.longGaugeData.points) {
                        val server = getServer(servers, point)

                        if (server != null) {
                            server._bootTime = Instant.ofEpochMilli(point.value)
                            server._host = agg.host
                        } else {
                            agg._bootTime = Instant.ofEpochMilli(point.value)
                        }
                    }
                }
                "system.time.provision" -> {
                    for (point in metric.longGaugeData.points) {
                        val server = getServer(servers, point) ?: continue
                        server.recordTimestamp(point)
                        server._provisionTime = Instant.ofEpochMilli(point.value)
                    }
                }
            }
        }
    }

    /**
     * Collect the data via the [monitor].
     */
    public fun collect(monitor: ComputeMonitor) {
        monitor.record(_service)

        for (host in _hosts.values) {
            monitor.record(host)
            host.reset()
        }

        for (server in _servers.values) {
            monitor.record(server)
            server.reset()
        }
    }

    /**
     * Obtain the [HostAggregator] for the specified [resource].
     */
    private fun getHost(hosts: MutableMap<String, HostAggregator>, resource: Resource): HostAggregator? {
        val id = resource.attributes[HOST_ID]
        return if (id != null) {
            hosts.getOrPut(id) { HostAggregator(resource) }
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
            servers.getOrPut(id) { ServerAggregator(point.attributes) }
        } else {
            null
        }
    }

    /**
     * An aggregator for service metrics before they are reported.
     */
    internal class ServiceAggregator : ServiceTableReader {
        private var _timestamp: Instant = Instant.MIN
        override val timestamp: Instant
            get() = _timestamp

        override val hostsUp: Int
            get() = _hostsUp
        @JvmField var _hostsUp = 0

        override val hostsDown: Int
            get() = _hostsDown
        @JvmField var _hostsDown = 0

        override val serversPending: Int
            get() = _serversPending
        @JvmField var _serversPending = 0

        override val serversActive: Int
            get() = _serversActive
        @JvmField var _serversActive = 0

        override val attemptsSuccess: Int
            get() = _attemptsSuccess
        @JvmField var _attemptsSuccess = 0

        override val attemptsFailure: Int
            get() = _attemptsFailure
        @JvmField var _attemptsFailure = 0

        override val attemptsError: Int
            get() = _attemptsError
        @JvmField var _attemptsError = 0

        /**
         * Record the timestamp of a [point] for this aggregator.
         */
        fun recordTimestamp(point: PointData) {
            _timestamp = Instant.ofEpochMilli(point.epochNanos / 1_000_000L) // ns to ms
        }
    }

    /**
     * An aggregator for host metrics before they are reported.
     */
    internal class HostAggregator(resource: Resource) : HostTableReader {
        /**
         * The static information about this host.
         */
        override val host = HostInfo(
            resource.attributes[HOST_ID]!!,
            resource.attributes[HOST_NAME] ?: "",
            resource.attributes[HOST_ARCH] ?: "",
            resource.attributes[HOST_NCPUS]?.toInt() ?: 0,
            resource.attributes[HOST_MEM_CAPACITY] ?: 0,
        )

        override val timestamp: Instant
            get() = _timestamp
        private var _timestamp = Instant.MIN

        override val guestsTerminated: Int
            get() = _guestsTerminated
        @JvmField var _guestsTerminated = 0

        override val guestsRunning: Int
            get() = _guestsRunning
        @JvmField var _guestsRunning = 0

        override val guestsError: Int
            get() = _guestsError
        @JvmField var _guestsError = 0

        override val guestsInvalid: Int
            get() = _guestsInvalid
        @JvmField var _guestsInvalid = 0

        override val cpuLimit: Double
            get() = _cpuLimit
        @JvmField var _cpuLimit = 0.0

        override val cpuUsage: Double
            get() = _cpuUsage
        @JvmField var _cpuUsage = 0.0

        override val cpuDemand: Double
            get() = _cpuDemand
        @JvmField var _cpuDemand = 0.0

        override val cpuUtilization: Double
            get() = _cpuUtilization
        @JvmField var _cpuUtilization = 0.0

        override val cpuActiveTime: Long
            get() = _cpuActiveTime - previousCpuActiveTime
        @JvmField var _cpuActiveTime = 0L
        private var previousCpuActiveTime = 0L

        override val cpuIdleTime: Long
            get() = _cpuIdleTime - previousCpuIdleTime
        @JvmField var _cpuIdleTime = 0L
        private var previousCpuIdleTime = 0L

        override val cpuStealTime: Long
            get() = _cpuStealTime - previousCpuStealTime
        @JvmField var _cpuStealTime = 0L
        private var previousCpuStealTime = 0L

        override val cpuLostTime: Long
            get() = _cpuLostTime - previousCpuLostTime
        @JvmField var _cpuLostTime = 0L
        private var previousCpuLostTime = 0L

        override val powerUsage: Double
            get() = _powerUsage
        @JvmField var _powerUsage = 0.0

        override val powerTotal: Double
            get() = _powerTotal - previousPowerTotal
        @JvmField var _powerTotal = 0.0
        private var previousPowerTotal = 0.0

        override val uptime: Long
            get() = _uptime - previousUptime
        @JvmField var _uptime = 0L
        private var previousUptime = 0L

        override val downtime: Long
            get() = _downtime - previousDowntime
        @JvmField var _downtime = 0L
        private var previousDowntime = 0L

        override val bootTime: Instant?
            get() = _bootTime
        @JvmField var _bootTime: Instant? = null

        /**
         * Finish the aggregation for this cycle.
         */
        fun reset() {
            // Reset intermediate state for next aggregation
            previousCpuActiveTime = _cpuActiveTime
            previousCpuIdleTime = _cpuIdleTime
            previousCpuStealTime = _cpuStealTime
            previousCpuLostTime = _cpuLostTime
            previousPowerTotal = _powerTotal
            previousUptime = _uptime
            previousDowntime = _downtime

            _guestsTerminated = 0
            _guestsRunning = 0
            _guestsError = 0
            _guestsInvalid = 0

            _cpuLimit = 0.0
            _cpuUsage = 0.0
            _cpuDemand = 0.0
            _cpuUtilization = 0.0

            _powerUsage = 0.0
        }

        /**
         * Record the timestamp of a [point] for this aggregator.
         */
        fun recordTimestamp(point: PointData) {
            _timestamp = Instant.ofEpochMilli(point.epochNanos / 1_000_000L) // ns to ms
        }
    }

    /**
     * An aggregator for server metrics before they are reported.
     */
    internal class ServerAggregator(attributes: Attributes) : ServerTableReader {
        /**
         * The static information about this server.
         */
        override val server = ServerInfo(
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
        override val host: HostInfo?
            get() = _host
        @JvmField var _host: HostInfo? = null

        private var _timestamp = Instant.MIN
        override val timestamp: Instant
            get() = _timestamp

        override val uptime: Long
            get() = _uptime - previousUptime
        @JvmField var _uptime: Long = 0
        private var previousUptime = 0L

        override val downtime: Long
            get() = _downtime - previousDowntime
        @JvmField var _downtime: Long = 0
        private var previousDowntime = 0L

        override val provisionTime: Instant?
            get() = _provisionTime
        @JvmField var _provisionTime: Instant? = null

        override val bootTime: Instant?
            get() = _bootTime
        @JvmField var _bootTime: Instant? = null

        override val cpuLimit: Double
            get() = _cpuLimit
        @JvmField var _cpuLimit = 0.0

        override val cpuActiveTime: Long
            get() = _cpuActiveTime - previousCpuActiveTime
        @JvmField var _cpuActiveTime = 0L
        private var previousCpuActiveTime = 0L

        override val cpuIdleTime: Long
            get() = _cpuIdleTime - previousCpuIdleTime
        @JvmField var _cpuIdleTime = 0L
        private var previousCpuIdleTime = 0L

        override val cpuStealTime: Long
            get() = _cpuStealTime - previousCpuStealTime
        @JvmField var _cpuStealTime = 0L
        private var previousCpuStealTime = 0L

        override val cpuLostTime: Long
            get() = _cpuLostTime - previousCpuLostTime
        @JvmField var _cpuLostTime = 0L
        private var previousCpuLostTime = 0L

        /**
         * Finish the aggregation for this cycle.
         */
        fun reset() {
            previousUptime = _uptime
            previousDowntime = _downtime
            previousCpuActiveTime = cpuActiveTime
            previousCpuIdleTime = cpuIdleTime
            previousCpuStealTime = cpuStealTime
            previousCpuLostTime = cpuLostTime

            _host = null
            _cpuLimit = 0.0
        }

        /**
         * Record the timestamp of a [point] for this aggregator.
         */
        fun recordTimestamp(point: PointData) {
            _timestamp = Instant.ofEpochMilli(point.epochNanos / 1_000_000L) // ns to ms
        }
    }

    private companion object {
        private val STATE_KEY = AttributeKey.stringKey("state")
        private val RESULT_KEY = AttributeKey.stringKey("result")
    }
}
