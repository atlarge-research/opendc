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

package org.opendc.experiments.compute.telemetry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.opendc.common.Dispatcher
import org.opendc.common.asCoroutineDispatcher
import org.opendc.compute.api.Server
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.driver.Host
import org.opendc.experiments.compute.telemetry.table.HostInfo
import org.opendc.experiments.compute.telemetry.table.HostTableReader
import org.opendc.experiments.compute.telemetry.table.ServerInfo
import org.opendc.experiments.compute.telemetry.table.ServerTableReader
import org.opendc.experiments.compute.telemetry.table.ServiceTableReader
import java.time.Duration
import java.time.Instant

/**
 * A helper class to collect metrics from a [ComputeService] instance and automatically export the metrics every
 * export interval.
 *
 * @param dispatcher A [Dispatcher] for scheduling the future events.
 * @param service The [ComputeService] to monitor.
 * @param monitor The monitor to export the metrics to.
 * @param exportInterval The export interval.
 */
public class ComputeMetricReader(
    dispatcher: Dispatcher,
    private val service: ComputeService,
    private val monitor: ComputeMonitor,
    private val exportInterval: Duration = Duration.ofMinutes(5)
) : AutoCloseable {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(dispatcher.asCoroutineDispatcher())
    private val clock = dispatcher.timeSource

    /**
     * Aggregator for service metrics.
     */
    private val serviceTableReader = ServiceTableReaderImpl(service)

    /**
     * Mapping from [Host] instances to [HostTableReaderImpl]
     */
    private val hostTableReaders = mutableMapOf<Host, HostTableReaderImpl>()

    /**
     * Mapping from [Server] instances to [ServerTableReaderImpl]
     */
    private val serverTableReaders = mutableMapOf<Server, ServerTableReaderImpl>()

    /**
     * The background job that is responsible for collecting the metrics every cycle.
     */
    private val job = scope.launch {
        val intervalMs = exportInterval.toMillis()
        val service = service
        val monitor = monitor
        val hostTableReaders = hostTableReaders
        val serverTableReaders = serverTableReaders
        val serviceTableReader = serviceTableReader

        try {
            while (isActive) {
                delay(intervalMs)

                try {
                    val now = clock.instant()

                    for (host in service.hosts) {
                        val reader = hostTableReaders.computeIfAbsent(host) { HostTableReaderImpl(it) }
                        reader.record(now)
                        monitor.record(reader)
                        reader.reset()
                    }

                    for (server in service.servers) {
                        val reader = serverTableReaders.computeIfAbsent(server) { ServerTableReaderImpl(service, it) }
                        reader.record(now)
                        monitor.record(reader)
                        reader.reset()
                    }

                    serviceTableReader.record(now)
                    monitor.record(serviceTableReader)
                } catch (cause: Throwable) {
                    logger.warn(cause) { "Exporter threw an Exception" }
                }
            }
        } finally {
            if (monitor is AutoCloseable) {
                monitor.close()
            }
        }
    }

    override fun close() {
        job.cancel()
    }

    /**
     * An aggregator for service metrics before they are reported.
     */
    private class ServiceTableReaderImpl(private val service: ComputeService) : ServiceTableReader {
        private var _timestamp: Instant = Instant.MIN
        override val timestamp: Instant
            get() = _timestamp

        override val hostsUp: Int
            get() = _hostsUp
        private var _hostsUp = 0

        override val hostsDown: Int
            get() = _hostsDown
        private var _hostsDown = 0

        override val serversTotal: Int
            get() = _serversTotal
        private var _serversTotal = 0

        override val serversPending: Int
            get() = _serversPending
        private var _serversPending = 0

        override val serversActive: Int
            get() = _serversActive
        private var _serversActive = 0

        override val attemptsSuccess: Int
            get() = _attemptsSuccess
        private var _attemptsSuccess = 0

        override val attemptsFailure: Int
            get() = _attemptsFailure
        private var _attemptsFailure = 0

        override val attemptsError: Int
            get() = _attemptsError
        private var _attemptsError = 0

        /**
         * Record the next cycle.
         */
        fun record(now: Instant) {
            _timestamp = now

            val stats = service.getSchedulerStats()
            _hostsUp = stats.hostsAvailable
            _hostsDown = stats.hostsUnavailable
            _serversTotal = stats.serversTotal
            _serversPending = stats.serversPending
            _serversActive = stats.serversActive
            _attemptsSuccess = stats.attemptsSuccess.toInt()
            _attemptsFailure = stats.attemptsFailure.toInt()
            _attemptsError = stats.attemptsError.toInt()
        }
    }

    /**
     * An aggregator for host metrics before they are reported.
     */
    private class HostTableReaderImpl(host: Host) : HostTableReader {
        private val _host = host

        override val host: HostInfo = HostInfo(host.uid.toString(), host.name, "x86", host.model.cpuCount, host.model.memoryCapacity)

        override val timestamp: Instant
            get() = _timestamp
        private var _timestamp = Instant.MIN

        override val guestsTerminated: Int
            get() = _guestsTerminated
        private var _guestsTerminated = 0

        override val guestsRunning: Int
            get() = _guestsRunning
        private var _guestsRunning = 0

        override val guestsError: Int
            get() = _guestsError
        private var _guestsError = 0

        override val guestsInvalid: Int
            get() = _guestsInvalid
        private var _guestsInvalid = 0

        override val cpuLimit: Double
            get() = _cpuLimit
        private var _cpuLimit = 0.0

        override val cpuUsage: Double
            get() = _cpuUsage
        private var _cpuUsage = 0.0

        override val cpuDemand: Double
            get() = _cpuDemand
        private var _cpuDemand = 0.0

        override val cpuUtilization: Double
            get() = _cpuUtilization
        private var _cpuUtilization = 0.0

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

        override val powerUsage: Double
            get() = _powerUsage
        private var _powerUsage = 0.0

        override val powerTotal: Double
            get() = _powerTotal - previousPowerTotal
        private var _powerTotal = 0.0
        private var previousPowerTotal = 0.0

        override val uptime: Long
            get() = _uptime - previousUptime
        private var _uptime = 0L
        private var previousUptime = 0L

        override val downtime: Long
            get() = _downtime - previousDowntime
        private var _downtime = 0L
        private var previousDowntime = 0L

        override val bootTime: Instant?
            get() = _bootTime
        private var _bootTime: Instant? = null

        /**
         * Record the next cycle.
         */
        fun record(now: Instant) {
            val hostCpuStats = _host.getCpuStats()
            val hostSysStats = _host.getSystemStats()

            _timestamp = now
            _guestsTerminated = hostSysStats.guestsTerminated
            _guestsRunning = hostSysStats.guestsRunning
            _guestsError = hostSysStats.guestsError
            _guestsInvalid = hostSysStats.guestsInvalid
            _cpuLimit = hostCpuStats.capacity
            _cpuDemand = hostCpuStats.demand
            _cpuUsage = hostCpuStats.usage
            _cpuUtilization = hostCpuStats.utilization
            _cpuActiveTime = hostCpuStats.activeTime
            _cpuIdleTime = hostCpuStats.idleTime
            _cpuStealTime = hostCpuStats.stealTime
            _cpuLostTime = hostCpuStats.lostTime
            _powerUsage = hostSysStats.powerUsage
            _powerTotal = hostSysStats.energyUsage
            _uptime = hostSysStats.uptime.toMillis()
            _downtime = hostSysStats.downtime.toMillis()
            _bootTime = hostSysStats.bootTime
        }

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
    }

    /**
     * An aggregator for server metrics before they are reported.
     */
    private class ServerTableReaderImpl(private val service: ComputeService, server: Server) : ServerTableReader {
        private val _server = server

        /**
         * The static information about this server.
         */
        override val server = ServerInfo(
            server.uid.toString(),
            server.name,
            "vm",
            "x86",
            server.image.uid.toString(),
            server.image.name,
            server.flavor.cpuCount,
            server.flavor.memorySize
        )

        /**
         * The [HostInfo] of the host on which the server is hosted.
         */
        override var host: HostInfo? = null
        private var _host: Host? = null

        private var _timestamp = Instant.MIN
        override val timestamp: Instant
            get() = _timestamp

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

        /**
         * Record the next cycle.
         */
        fun record(now: Instant) {
            val newHost = service.lookupHost(_server)
            if (newHost != null && newHost.uid != _host?.uid) {
                _host = newHost
                host = HostInfo(newHost.uid.toString(), newHost.name, "x86", newHost.model.cpuCount, newHost.model.memoryCapacity)
            }

            val cpuStats = _host?.getCpuStats(_server)
            val sysStats = _host?.getSystemStats(_server)

            _timestamp = now
            _cpuLimit = cpuStats?.capacity ?: 0.0
            _cpuActiveTime = cpuStats?.activeTime ?: 0
            _cpuIdleTime = cpuStats?.idleTime ?: 0
            _cpuStealTime = cpuStats?.stealTime ?: 0
            _cpuLostTime = cpuStats?.lostTime ?: 0
            _uptime = sysStats?.uptime?.toMillis() ?: 0
            _downtime = sysStats?.downtime?.toMillis() ?: 0
            _provisionTime = _server.launchedAt
            _bootTime = sysStats?.bootTime
        }

        /**
         * Finish the aggregation for this cycle.
         */
        fun reset() {
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
}
