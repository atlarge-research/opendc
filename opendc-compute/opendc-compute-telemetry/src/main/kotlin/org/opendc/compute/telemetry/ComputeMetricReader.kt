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

package org.opendc.compute.telemetry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.opendc.common.Dispatcher
import org.opendc.common.asCoroutineDispatcher
import org.opendc.compute.api.Server
import org.opendc.compute.carbon.CarbonTrace
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.driver.Host
import org.opendc.compute.telemetry.table.HostInfo
import org.opendc.compute.telemetry.table.HostTableReader
import org.opendc.compute.telemetry.table.ServerInfo
import org.opendc.compute.telemetry.table.ServerTableReader
import org.opendc.compute.telemetry.table.ServiceTableReader
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
    private val exportInterval: Duration = Duration.ofMinutes(5),
    private val startTime: Duration = Duration.ofMillis(0),
    private val carbonTrace: CarbonTrace = CarbonTrace(null),
) : AutoCloseable {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(dispatcher.asCoroutineDispatcher())
    private val clock = dispatcher.timeSource

    /**
     * Aggregator for service metrics.
     */
    private val serviceTableReader = ServiceTableReaderImpl(service, startTime)

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
    private val job =
        scope.launch {
            val intervalMs = exportInterval.toMillis()
            try {
                while (isActive) {
                    delay(intervalMs)

                    loggState()
                }
            } finally {
                loggState()

                if (monitor is AutoCloseable) {
                    monitor.close()
                }
            }
        }

    private fun loggState() {
        try {
            val now = this.clock.instant()

            for (host in this.service.hosts) {
                val reader = this.hostTableReaders.computeIfAbsent(host) { HostTableReaderImpl(it, startTime, carbonTrace) }
                reader.record(now)
                this.monitor.record(reader.copy())
                reader.reset()
            }

            for (server in this.service.servers) {
                val reader = this.serverTableReaders.computeIfAbsent(server) { ServerTableReaderImpl(service, it, startTime) }
                reader.record(now)
                this.monitor.record(reader.copy())
                reader.reset()
            }

            this.serviceTableReader.record(now)
            monitor.record(this.serviceTableReader.copy())
        } catch (cause: Throwable) {
            this.logger.warn(cause) { "Exporter threw an Exception" }
        }
    }

    override fun close() {
        job.cancel()
    }

    /**
     * An aggregator for service metrics before they are reported.
     */
    private class ServiceTableReaderImpl(
        private val service: ComputeService,
        private val startTime: Duration = Duration.ofMillis(0),
    ) : ServiceTableReader {
        override fun copy(): ServiceTableReader {
            val newServiceTable = ServiceTableReaderImpl(service)
            newServiceTable.setValues(this)

            return newServiceTable
        }

        override fun setValues(table: ServiceTableReader) {
            _timestamp = table.timestamp
            _timestampAbsolute = table.timestampAbsolute

            _hostsUp = table.hostsUp
            _hostsDown = table.hostsDown
            _serversTotal = table.serversTotal
            _serversPending = table.serversPending
            _serversActive = table.serversActive
            _attemptsSuccess = table.attemptsSuccess
            _attemptsFailure = table.attemptsFailure
            _attemptsError = table.attemptsError
        }

        private var _timestamp: Instant = Instant.MIN
        override val timestamp: Instant
            get() = _timestamp

        private var _timestampAbsolute: Instant = Instant.MIN
        override val timestampAbsolute: Instant
            get() = _timestampAbsolute

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
            _timestampAbsolute = now + startTime

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
    private class HostTableReaderImpl(
        host: Host,
        private val startTime: Duration = Duration.ofMillis(0),
        private val carbonTrace: CarbonTrace = CarbonTrace(null),
    ) : HostTableReader {
        override fun copy(): HostTableReader {
            val newHostTable = HostTableReaderImpl(_host)
            newHostTable.setValues(this)

            return newHostTable
        }

        override fun setValues(table: HostTableReader) {
            _timestamp = table.timestamp
            _timestampAbsolute = table.timestampAbsolute

            _guestsTerminated = table.guestsTerminated
            _guestsRunning = table.guestsRunning
            _guestsError = table.guestsError
            _guestsInvalid = table.guestsInvalid
            _cpuLimit = table.cpuLimit
            _cpuDemand = table.cpuDemand
            _cpuUsage = table.cpuUsage
            _cpuUtilization = table.cpuUtilization
            _cpuActiveTime = table.cpuActiveTime
            _cpuIdleTime = table.cpuIdleTime
            _cpuStealTime = table.cpuStealTime
            _cpuLostTime = table.cpuLostTime
            _powerDraw = table.powerDraw
            _energyUsage = table.energyUsage
            _thermalPower = table.thermalPower
            _temperature = table.temperature
            _carbonIntensity = table.carbonIntensity
            _carbonEmission = table.carbonEmission
            _uptime = table.uptime
            _downtime = table.downtime
            _bootTime = table.bootTime
            _bootTimeAbsolute = table.bootTimeAbsolute
        }

        private val _host = host

        override val host: HostInfo = HostInfo(host.uid.toString(), host.name, "x86", host.model.cpuCount, host.model.memoryCapacity)

        override val timestamp: Instant
            get() = _timestamp
        private var _timestamp = Instant.MIN

        override val timestampAbsolute: Instant
            get() = _timestampAbsolute
        private var _timestampAbsolute = Instant.MIN

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

        override val powerDraw: Double
            get() = _powerDraw
        private var _powerDraw = 0.0

        override val energyUsage: Double
            get() = _energyUsage - previousPowerTotal
        private var _energyUsage = 0.0
        private var previousPowerTotal = 0.0

        override val thermalPower: Double
            get() = _thermalPower
        private var _thermalPower = 0.0

        override val temperature: Double
            get() = _temperature
        private var _temperature = 0.0

        override val carbonIntensity: Double
            get() = _carbonIntensity
        private var _carbonIntensity = 0.0

        override val carbonEmission: Double
            get() = _carbonEmission
        private var _carbonEmission = 0.0

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

        override val bootTimeAbsolute: Instant?
            get() = _bootTimeAbsolute
        private var _bootTimeAbsolute: Instant? = null

        /**
         * Record the next cycle.
         */
        fun record(now: Instant) {
            val hostCpuStats = _host.getCpuStats()
            val hostSysStats = _host.getSystemStats()

            _timestamp = now
            _timestampAbsolute = now + startTime

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
            _powerDraw = hostSysStats.powerDraw
            _energyUsage = hostSysStats.energyUsage
            _thermalPower = hostSysStats.thermalPower
            _temperature = hostSysStats.temperatureC
            _carbonIntensity = carbonTrace.getCarbonIntensity(absoluteTimestamp)
            _carbonIntensity = carbonTrace.getCarbonIntensity(timestampAbsolute)

            _carbonEmission = carbonIntensity * (energyUsage / 3600000.0) // convert energy usage from J to kWh
            _uptime = hostSysStats.uptime.toMillis()
            _downtime = hostSysStats.downtime.toMillis()
            _bootTime = hostSysStats.bootTime
            _bootTime = hostSysStats.bootTime + startTime
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
            previousPowerTotal = _energyUsage
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

            _powerDraw = 0.0
            _energyUsage = 0.0
            _thermalPower = 0.0
            _temperature = 0.0
            _carbonIntensity = 0.0
            _carbonEmission = 0.0
        }
    }

    /**
     * An aggregator for server metrics before they are reported.
     */
    private class ServerTableReaderImpl(
        private val service: ComputeService,
        server: Server,
        private val startTime: Duration = Duration.ofMillis(0),
    ) : ServerTableReader {
        override fun copy(): ServerTableReader {
            val newServerTable = ServerTableReaderImpl(service, _server)
            newServerTable.setValues(this)

            return newServerTable
        }

        override fun setValues(table: ServerTableReader) {
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
        }

        private val _server = server

        /**
         * The static information about this server.
         */
        override val server =
            ServerInfo(
                server.uid.toString(),
                server.name,
                "vm",
                "x86",
                server.image.uid.toString(),
                server.image.name,
                server.flavor.coreCount,
                server.flavor.memorySize,
            )

        /**
         * The [HostInfo] of the host on which the server is hosted.
         */
        override var host: HostInfo? = null
        private var _host: Host? = null

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
            _timestampAbsolute = now + startTime

            _cpuLimit = cpuStats?.capacity ?: 0.0
            _cpuActiveTime = cpuStats?.activeTime ?: 0
            _cpuIdleTime = cpuStats?.idleTime ?: 0
            _cpuStealTime = cpuStats?.stealTime ?: 0
            _cpuLostTime = cpuStats?.lostTime ?: 0
            _uptime = sysStats?.uptime?.toMillis() ?: 0
            _downtime = sysStats?.downtime?.toMillis() ?: 0
            _provisionTime = _server.launchedAt
            _bootTime = sysStats?.bootTime

            if (sysStats != null) {
                _bootTimeAbsolute = sysStats.bootTime + startTime
            } else {
                _bootTimeAbsolute = null
            }
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
