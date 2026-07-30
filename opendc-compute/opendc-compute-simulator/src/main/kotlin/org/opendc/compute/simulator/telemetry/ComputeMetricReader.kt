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
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.simulator.telemetry.table.battery.BatterySampler
import org.opendc.compute.simulator.telemetry.table.host.HostSampler
import org.opendc.compute.simulator.telemetry.table.powerSource.PowerSourceSampler
import org.opendc.compute.simulator.telemetry.table.service.ServiceSampler
import org.opendc.compute.simulator.telemetry.table.task.TaskSampler
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
        ),
    private val printFrequency: Int? = null,
) : AutoCloseable {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(dispatcher.asCoroutineDispatcher())
    private val clock = dispatcher.timeSource

    private val batterySampler =
        BatterySampler(
            startTime,
        )

    private val hostSampler =
        HostSampler(
            startTime,
        )

    private val powerSourceSampler =
        PowerSourceSampler(
            startTime,
        )

    private val serviceSampler =
        ServiceSampler(
            service,
            startTime,
        )

    private val taskSampler =
        TaskSampler(
            service,
            startTime,
        )

    private var loggCounter = 0

    /**
     * The background job that is responsible for collecting the metrics every cycle.
     */
    private val job =
        scope.launch {
            val intervalMs = exportInterval.toMillis()
            try {
                while (isActive) {
                    delay(intervalMs.milliseconds)

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

            if (toMonitor[OutputFiles.BATTERY] == true) {
                for (battery in this.service.batteries) {
                    val batterySample = this.batterySampler.sample(now, battery)
                    this.monitor.record(batterySample)
                }
            }

            if (toMonitor[OutputFiles.HOST] == true) {
                for (host in this.service.hosts) {
                    val hostSample = this.hostSampler.sample(now, host)
                    this.monitor.record(hostSample)
                }
            }

            if (toMonitor[OutputFiles.POWER_SOURCE] == true) {
                for (powerSource in this.service.powerSources) {
                    val powerSourceSample = this.powerSourceSampler.sample(now, powerSource)
                    this.monitor.record(powerSourceSample)
                }
            }

            if (toMonitor[OutputFiles.SERVICE] == true) {
                val serviceSample = this.serviceSampler.sample(now)
                this.monitor.record(serviceSample)
            }

            if (toMonitor[OutputFiles.TASK] == true) {
                for (task in this.service.tasks.values) {
                    val taskSample = this.taskSampler.sample(now, task)
                    this.monitor.record(taskSample)
                }
            }

            for (task in this.service.tasksToRemove) {
                task.delete()
            }
            this.service.clearTasksToRemove()

            if (printFrequency != null && loggCounter % printFrequency == 0) {
                // TODO: Fix this!
                var loggString = "\n\t\t\t\t\tMetrics after ${now.toEpochMilli() / 1000 / 60 / 60} hours:\n"
//                loggString += "\t\t\t\t\t\tTasks Total: ${this.serviceTableReader.tasksTotal}\n"
//                loggString += "\t\t\t\t\t\tTasks Active: ${this.serviceTableReader.tasksActive}\n"
//                loggString += "\t\t\t\t\t\tTasks Pending: ${this.serviceTableReader.tasksPending}\n"
//                loggString += "\t\t\t\t\t\tTasks Completed: ${this.serviceTableReader.tasksCompleted}\n"
//                loggString += "\t\t\t\t\t\tTasks Terminated: ${this.serviceTableReader.tasksTerminated}\n"

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
