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

package org.opendc.compute.simulator.telemetry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.opendc.common.Dispatcher
import org.opendc.common.asCoroutineDispatcher
import org.opendc.compute.api.TaskState
import org.opendc.compute.carbon.CarbonTrace
import org.opendc.compute.simulator.host.SimHost
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.compute.simulator.telemetry.table.HostInfo
import org.opendc.compute.simulator.telemetry.table.HostTableReader
import org.opendc.compute.simulator.telemetry.table.ServiceTableReader
import org.opendc.compute.simulator.telemetry.table.TaskInfo
import org.opendc.compute.simulator.telemetry.table.TaskTableReader
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
    private val serviceTableReader =
        ServiceTableReaderImpl(
            service,
            startTime,
        )

    private var loggCounter = 0

    /**
     * Mapping from [Host] instances to [HostTableReaderImpl]
     */
    private val hostTableReaders = mutableMapOf<SimHost, HostTableReaderImpl>()

    /**
     * Mapping from [Task] instances to [TaskTableReaderImpl]
     */
    private val taskTableReaders = mutableMapOf<ServiceTask, TaskTableReaderImpl>()

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
        loggCounter++
        try {
            val now = this.clock.instant()

            for (host in this.service.hosts) {
                val reader =
                    this.hostTableReaders.computeIfAbsent(host) {
                        HostTableReaderImpl(
                            it,
                            startTime,
                            carbonTrace,
                        )
                    }
                reader.record(now)
                this.monitor.record(reader.copy())
                reader.reset()
            }

            for (task in this.service.tasks) {
                val reader =
                    this.taskTableReaders.computeIfAbsent(task) {
                        TaskTableReaderImpl(
                            service,
                            it,
                            startTime,
                        )
                    }
                reader.record(now)
                this.monitor.record(reader.copy())
                reader.reset()
            }

            for (task in this.service.tasksToRemove) {
                task.delete()
            }
            this.service.clearTasksToRemove()

            this.serviceTableReader.record(now)
            monitor.record(this.serviceTableReader.copy())

            if (loggCounter >= 100) {
                var loggString = "\n\t\t\t\t\tMetrics after ${now.toEpochMilli() / 1000 / 60 / 60} hours:\n"
                loggString += "\t\t\t\t\t\tTasks Total: ${this.serviceTableReader.tasksTotal}\n"
                loggString += "\t\t\t\t\t\tTasks Active: ${this.serviceTableReader.tasksActive}\n"
                loggString += "\t\t\t\t\t\tTasks Pending: ${this.serviceTableReader.tasksPending}\n"
                loggString += "\t\t\t\t\t\tTasks Completed: ${this.serviceTableReader.tasksCompleted}\n"
                loggString += "\t\t\t\t\t\tTasks Terminated: ${this.serviceTableReader.tasksTerminated}\n"

                this.logger.warn { loggString }
                loggCounter = 0
            }
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
            val newServiceTable =
                ServiceTableReaderImpl(
                    service,
                )
            newServiceTable.setValues(this)

            return newServiceTable
        }

        override fun setValues(table: ServiceTableReader) {
            _timestamp = table.timestamp
            _timestampAbsolute = table.timestampAbsolute

            _hostsUp = table.hostsUp
            _hostsDown = table.hostsDown
            _tasksTotal = table.tasksTotal
            _tasksPending = table.tasksPending
            _tasksActive = table.tasksActive
            _tasksCompleted = table.tasksCompleted
            _tasksTerminated = table.tasksTerminated
            _attemptsSuccess = table.attemptsSuccess
            _attemptsFailure = table.attemptsFailure
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

        override val tasksTotal: Int
            get() = _tasksTotal
        private var _tasksTotal = 0

        override val tasksPending: Int
            get() = _tasksPending
        private var _tasksPending = 0

        override val tasksCompleted: Int
            get() = _tasksCompleted
        private var _tasksCompleted = 0

        override val tasksActive: Int
            get() = _tasksActive
        private var _tasksActive = 0

        override val tasksTerminated: Int
            get() = _tasksTerminated
        private var _tasksTerminated = 0

        override val attemptsSuccess: Int
            get() = _attemptsSuccess
        private var _attemptsSuccess = 0

        override val attemptsFailure: Int
            get() = _attemptsFailure
        private var _attemptsFailure = 0

        /**
         * Record the next cycle.
         */
        fun record(now: Instant) {
            _timestamp = now
            _timestampAbsolute = now + startTime

            val stats = service.getSchedulerStats()
            _hostsUp = stats.hostsAvailable
            _hostsDown = stats.hostsUnavailable
            _tasksTotal = stats.tasksTotal
            _tasksPending = stats.tasksPending
            _tasksCompleted = stats.tasksCompleted
            _tasksActive = stats.tasksActive
            _tasksTerminated = stats.tasksTerminated
            _attemptsSuccess = stats.attemptsSuccess.toInt()
            _attemptsFailure = stats.attemptsFailure.toInt()
        }
    }

    /**
     * An aggregator for host metrics before they are reported.
     */
    private class HostTableReaderImpl(
        host: SimHost,
        private val startTime: Duration = Duration.ofMillis(0),
        private val carbonTrace: CarbonTrace = CarbonTrace(null),
    ) : HostTableReader {
        override fun copy(): HostTableReader {
            val newHostTable =
                HostTableReaderImpl(_host)
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
            _carbonIntensity = table.carbonIntensity
            _carbonEmission = table.carbonEmission
            _uptime = table.uptime
            _downtime = table.downtime
            _bootTime = table.bootTime
            _bootTimeAbsolute = table.bootTimeAbsolute
        }

        private val _host = host

        override val host: HostInfo =
            HostInfo(
                host.getUid().toString(),
                host.getName(),
                "x86",
                host.getModel().coreCount,
                host.getModel().cpuCapacity,
                host.getModel().memoryCapacity,
            )

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

        override val cpuLimit: Float
            get() = _cpuLimit
        private var _cpuLimit = 0.0f

        override val cpuUsage: Float
            get() = _cpuUsage
        private var _cpuUsage = 0.0f

        override val cpuDemand: Float
            get() = _cpuDemand
        private var _cpuDemand = 0.0f

        override val cpuUtilization: Float
            get() = _cpuUtilization
        private var _cpuUtilization = 0.0f

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

        override val powerDraw: Float
            get() = _powerDraw
        private var _powerDraw = 0.0f

        override val energyUsage: Float
            get() = _energyUsage - previousEnergyUsage
        private var _energyUsage = 0.0f
        private var previousEnergyUsage = 0.0f

        override val carbonIntensity: Float
            get() = _carbonIntensity
        private var _carbonIntensity = 0.0f

        override val carbonEmission: Float
            get() = _carbonEmission
        private var _carbonEmission = 0.0f

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
            _carbonIntensity = carbonTrace.getCarbonIntensity(timestampAbsolute)

            _carbonEmission = carbonIntensity * (energyUsage / 3600000.0f) // convert energy usage from J to kWh
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
            previousEnergyUsage = _energyUsage
            previousUptime = _uptime
            previousDowntime = _downtime

            _guestsTerminated = 0
            _guestsRunning = 0
            _guestsError = 0
            _guestsInvalid = 0

            _cpuLimit = 0.0f
            _cpuUsage = 0.0f
            _cpuDemand = 0.0f
            _cpuUtilization = 0.0f

            _powerDraw = 0.0f
            _energyUsage = 0.0f
            _carbonIntensity = 0.0f
            _carbonEmission = 0.0f
        }
    }

    /**
     * An aggregator for task metrics before they are reported.
     */
    private class TaskTableReaderImpl(
        private val service: ComputeService,
        private val task: ServiceTask,
        private val startTime: Duration = Duration.ofMillis(0),
    ) : TaskTableReader {
        override fun copy(): TaskTableReader {
            val newTaskTable =
                TaskTableReaderImpl(
                    service,
                    task,
                )
            newTaskTable.setValues(this)

            return newTaskTable
        }

        override fun setValues(table: TaskTableReader) {
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

            _creationTime = table.creationTime
            _finishTime = table.finishTime

            _taskState = table.taskState
        }

        /**
         * The static information about this task.
         */
        override val taskInfo =
            TaskInfo(
                task.uid.toString(),
                task.name,
                "vm",
                "x86",
                task.flavor.coreCount,
                task.flavor.memorySize,
            )

        /**
         * The [HostInfo] of the host on which the task is hosted.
         */
        override var host: HostInfo? = null
        private var _host: SimHost? = null

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

        override val creationTime: Instant?
            get() = _creationTime
        private var _creationTime: Instant? = null

        override val finishTime: Instant?
            get() = _finishTime
        private var _finishTime: Instant? = null

        override val cpuLimit: Float
            get() = _cpuLimit
        private var _cpuLimit = 0.0f

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

        override val taskState: TaskState?
            get() = _taskState
        private var _taskState: TaskState? = null

        /**
         * Record the next cycle.
         */
        fun record(now: Instant) {
            val newHost = service.lookupHost(task)
            if (newHost != null && newHost.getUid() != _host?.getUid()) {
                _host = newHost
                host =
                    HostInfo(
                        newHost.getUid().toString(),
                        newHost.getName(),
                        "x86",
                        newHost.getModel().coreCount,
                        newHost.getModel().cpuCapacity,
                        newHost.getModel().memoryCapacity,
                    )
            }

            val cpuStats = _host?.getCpuStats(task)
            val sysStats = _host?.getSystemStats(task)

            _timestamp = now
            _timestampAbsolute = now + startTime

            _cpuLimit = cpuStats?.capacity ?: 0.0f
            _cpuActiveTime = cpuStats?.activeTime ?: 0
            _cpuIdleTime = cpuStats?.idleTime ?: 0
            _cpuStealTime = cpuStats?.stealTime ?: 0
            _cpuLostTime = cpuStats?.lostTime ?: 0
            _uptime = sysStats?.uptime?.toMillis() ?: 0
            _downtime = sysStats?.downtime?.toMillis() ?: 0
            _provisionTime = task.launchedAt
            _bootTime = sysStats?.bootTime
            _creationTime = task.createdAt
            _finishTime = task.finishedAt

            _taskState = task.state

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
            _cpuLimit = 0.0f
        }
    }
}
