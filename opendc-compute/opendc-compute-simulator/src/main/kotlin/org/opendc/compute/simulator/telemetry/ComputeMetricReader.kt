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
import org.opendc.compute.simulator.host.SimHost
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.compute.simulator.telemetry.table.battery.BatteryTableReaderImpl
import org.opendc.compute.simulator.telemetry.table.costModel.CostModelTableReaderImpl
import org.opendc.compute.simulator.telemetry.table.host.HostTableReaderImpl
import org.opendc.compute.simulator.telemetry.table.powerSource.PowerSourceTableReaderImpl
import org.opendc.compute.simulator.telemetry.table.service.ServiceTableReaderImpl
import org.opendc.compute.simulator.telemetry.table.task.TaskTableReaderImpl
import org.opendc.simulator.compute.costmodel.CostModel
import org.opendc.simulator.compute.power.SimPowerSource
import org.opendc.simulator.compute.power.batteries.SimBattery
import java.time.Duration

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
    private val toMonitor: Map<OutputFiles, Boolean> =
        mapOf(
            OutputFiles.HOST to true,
            OutputFiles.TASK to true,
            OutputFiles.POWER_SOURCE to true,
            OutputFiles.BATTERY to true,
            OutputFiles.SERVICE to true,
            OutputFiles.COSTMODEL to true,
        ),
    private val printFrequency: Int? = null,
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
     * Mapping from [SimHost] instances to [HostTableReaderImpl]
     */
    private val hostTableReaders = mutableMapOf<SimHost, HostTableReaderImpl>()

    /**
     * Mapping from [ServiceTask] instances to [TaskTableReaderImpl]
     */
    private val taskTableReaders = mutableMapOf<ServiceTask, TaskTableReaderImpl>()

    /**
     * Mapping from [SimPowerSource] instances to [PowerSourceTableReaderImpl]
     */
    private val powerSourceTableReaders = mutableMapOf<SimPowerSource, PowerSourceTableReaderImpl>()

    /**
     * Mapping from [SimPowerSource] instances to [PowerSourceTableReaderImpl]
     */
    private val batteryTableReaders = mutableMapOf<SimBattery, BatteryTableReaderImpl>()

    /**
     * Mapping from [CostModel] instances to [CostModelTableReaderImpl]
     */
    private val costModelTableReaders = mutableMapOf<CostModel, CostModelTableReaderImpl>()

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
                if (monitor is AutoCloseable) {
                    monitor.close()
                }
            }
        }

    public fun loggState() {
        loggCounter++
        try {
            val now = this.clock.instant()

            if (toMonitor[OutputFiles.HOST] == true) {
                for (host in this.service.hosts) {
                    val reader =
                        this.hostTableReaders.computeIfAbsent(host) {
                            HostTableReaderImpl(
                                it,
                                startTime,
                            )
                        }
                    reader.record(now)
                    this.monitor.record(reader.copy())
                    reader.reset()
                }
            }

            if (toMonitor[OutputFiles.TASK] == true) {
                for (task in this.service.tasks.values) {
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
            }

            for (task in this.service.tasksToRemove) {
                this.taskTableReaders.remove(task)
                task.delete()
            }
            this.service.clearTasksToRemove()

            if (toMonitor[OutputFiles.POWER_SOURCE] == true) {
                for (simPowerSource in this.service.powerSources) {
                    val reader =
                        this.powerSourceTableReaders.computeIfAbsent(simPowerSource) { it ->
                            PowerSourceTableReaderImpl(
                                it,
                                startTime,
                            )
                        }

                    reader.record(now)
                    this.monitor.record(reader.copy())
                    reader.reset()
                }
            }

            if (toMonitor[OutputFiles.BATTERY] == true) {
                for (simBattery in this.service.batteries) {
                    val reader =
                        this.batteryTableReaders.computeIfAbsent(simBattery) {
                            BatteryTableReaderImpl(
                                it,
                                startTime,
                            )
                        }

                    reader.record(now)
                    this.monitor.record(reader.copy())
                    reader.reset()
                }
            }

            if (toMonitor[OutputFiles.SERVICE] == true) {
                this.serviceTableReader.record(now)
                this.monitor.record(this.serviceTableReader.copy())
            }

            if (toMonitor[OutputFiles.COSTMODEL] == true) {
                for (costModel in this.service.costModels) {
                    val reader =
                        this.costModelTableReaders.computeIfAbsent(costModel) { it ->
                            CostModelTableReaderImpl(
                                it,
                                startTime
                            )
                        }

                    reader.record(now)
                    this.monitor.record(reader.copy())
                    reader.reset()
                }
            }

            if (printFrequency != null && loggCounter % printFrequency == 0) {
                var loggString = "\n\t\t\t\t\tMetrics after ${now.toEpochMilli() / 1000 / 60 / 60} hours:\n"
                loggString += "\t\t\t\t\t\tTasks Total: ${this.serviceTableReader.tasksTotal}\n"
                loggString += "\t\t\t\t\t\tTasks Active: ${this.serviceTableReader.tasksActive}\n"
                loggString += "\t\t\t\t\t\tTasks Pending: ${this.serviceTableReader.tasksPending}\n"
                loggString += "\t\t\t\t\t\tTasks Completed: ${this.serviceTableReader.tasksCompleted}\n"
                loggString += "\t\t\t\t\t\tTasks Terminated: ${this.serviceTableReader.tasksTerminated}\n"

                this.logger.warn { loggString }
            }
        } catch (cause: Throwable) {
            this.logger.warn(cause) { "Exporter threw an Exception" }
        }
    }

    override fun close() {
        job.cancel()
    }
}
