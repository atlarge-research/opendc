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

package org.opendc.sdk.runner.telemetry

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.opendc.sdk.model.telemetry.OutputFileSpec
import org.opendc.sdk.runner.telemetry.table.battery.BatterySampler
import org.opendc.sdk.runner.telemetry.table.cluster.ClusterSampler
import org.opendc.sdk.runner.telemetry.table.datacenter.DataCenterSampler
import org.opendc.sdk.runner.telemetry.table.host.HostSampler
import org.opendc.sdk.runner.telemetry.table.powerSource.PowerSourceSampler
import org.opendc.sdk.runner.telemetry.table.service.ServiceSampler
import org.opendc.sdk.runner.telemetry.table.simulation.SimulationMeta
import org.opendc.sdk.runner.telemetry.table.task.TaskMeta
import org.opendc.sdk.runner.telemetry.table.task.TaskSampler
import org.opendc.sdk.runner.telemetry.table.topology.TopologyMeta
import org.opendc.simulator.compute.TaskWatcher
import org.opendc.simulator.compute.service.ComputeService
import org.opendc.simulator.compute.task.SimTask
import org.opendc.simulator.compute.task.TaskState
import org.opendc.simulator.compute.telemetry.TaskListener
import org.opendc.simulator.core.Dispatcher
import org.opendc.simulator.core.asCoroutineDispatcher
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A helper class to collect metrics from a [ComputeService] instance and automatically export the metrics every
 * export interval.
 *
 * A task is sampled at every change of its state, with the values at that moment, and every export interval while it
 * runs. A task that does not run, such as one waiting to be scheduled, is only sampled when its state changes.
 *
 * @param dispatcher A [Dispatcher] for scheduling the future events.
 * @param service The [ComputeService] to monitor.
 * @param monitor The monitor to export the metrics to.
 * @param exportInterval The export interval.
 * @param startTime The absolute time the simulation starts at. Every exported time is relative to it.
 */
public class ComputeMetricReader(
    private val dispatcher: Dispatcher,
    private val service: ComputeService,
    private val monitor: MetricExporter,
    private val exportInterval: Duration = Duration.ofMinutes(5),
    private val startTime: Duration = Duration.ofMillis(0),
    private val toMonitor: Map<OutputFileSpec, Boolean> =
        mapOf(
            OutputFileSpec.BATTERY to true,
            OutputFileSpec.CLUSTER to true,
            OutputFileSpec.DATA_CENTER to true,
            OutputFileSpec.HOST to true,
            OutputFileSpec.POWER_SOURCE to true,
            OutputFileSpec.SERVICE to true,
            OutputFileSpec.TASK to true,
        ),
    private val printFrequency: Int? = null,
) : AutoCloseable, TaskListener, TaskWatcher {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(dispatcher.asCoroutineDispatcher())
    private val clock = dispatcher.timeSource

    private val batterySampler =
        BatterySampler()

    private val clusterSampler =
        ClusterSampler()

    private val dataCenterSampler =
        DataCenterSampler()

    private val hostSampler =
        HostSampler()

    private val powerSourceSampler =
        PowerSourceSampler()

    private val serviceSampler =
        ServiceSampler(
            service,
        )

    private val taskSampler =
        TaskSampler(
            service,
        )

    private var loggCounter = 0

    /**
     * The failure that stopped the export, after which the run fails.
     */
    private var failure: Throwable? = null

    /**
     * A flag to indicate that the [monitor] has been closed, after which nothing is exported.
     */
    private var isMonitorClosed = false

    init {
        // Registered here rather than in the job, which only starts once the simulation runs: the tasks submitted or
        // deleted before that would otherwise miss their samples.
        service.addTaskListener(this)
        service.addTaskWatcher(this)
    }

    /**
     * The background job that is responsible for collecting the metrics every cycle.
     */
    private val job =
        scope.launch {
            try {
                // Exported when the simulation starts rather than on construction, as the reader may be provisioned
                // before the hosts it reports on.
                monitor.export(SimulationMeta(startTime.toMillis()))
                monitor.export(TopologyMeta.of(service))

                val intervalMs = exportInterval.toMillis()
                try {
                    while (true) {
                        delay(intervalMs.milliseconds)

                        loggState()
                    }
                } catch (cause: CancellationException) {
                    // The run ended, so the final samples are taken, unless the reader stopped because exporting failed
                    if (failure == null) {
                        loggState()
                        exportRemainingTaskMeta()
                        isMonitorClosed = true
                        if (monitor is AutoCloseable) {
                            monitor.close()
                        }
                    }
                    throw cause
                }
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Throwable) {
                failRun(cause)
            }
        }

    public fun loggState() {
        loggCounter++

        val now = this.clock.instant()

        if (toMonitor[OutputFileSpec.BATTERY] == true) {
            for (battery in this.service.batteries) {
                val batterySample = this.batterySampler.sample(now, battery)
                this.monitor.export(batterySample)
            }
        }

        if (toMonitor[OutputFileSpec.CLUSTER] == true) {
            for (cluster in this.service.clusters) {
                val clusterSample = this.clusterSampler.sample(now, cluster)
                this.monitor.export(clusterSample)
            }
        }

        if (toMonitor[OutputFileSpec.DATA_CENTER] == true) {
            for (dataCenter in this.service.dataCenters) {
                val dataCenterSample = this.dataCenterSampler.sample(now, dataCenter)
                this.monitor.export(dataCenterSample)
            }
        }

        if (toMonitor[OutputFileSpec.HOST] == true) {
            for (host in this.service.hosts) {
                val hostSample = this.hostSampler.sample(now, host)
                this.monitor.export(hostSample)
            }
        }

        if (toMonitor[OutputFileSpec.POWER_SOURCE] == true) {
            for (powerSource in this.service.powerSources) {
                val powerSourceSample = this.powerSourceSampler.sample(now, powerSource)
                this.monitor.export(powerSourceSample)
            }
        }

        if (toMonitor[OutputFileSpec.SERVICE] == true) {
            val serviceSample = this.serviceSampler.sample(now)
            this.monitor.export(serviceSample)
        }

        if (toMonitor[OutputFileSpec.TASK] == true) {
            // Only the tasks on the hosts are visited, as the service can hold many tasks that wait to be scheduled,
            // which are sampled when their state changes
            for (host in this.service.hosts) {
                for (task in host.getInstances()) {
                    if (task.state == TaskState.RUNNING) {
                        val taskSample = this.taskSampler.sample(now, task)
                        this.monitor.export(taskSample)
                    }
                }
            }
        }

        if (printFrequency != null && loggCounter % printFrequency == 0) {
            // TODO: Fix this! This now prints 3 times
            var loggString = "\n\t\t\t\t\tMetrics after ${now.toEpochMilli() / 1000 / 60 / 60} hours:\n"
            loggString += "\t\t\t\t\t\tTasks Total: ${this.service.tasksTotal}\n"
            loggString += "\t\t\t\t\t\tTasks Active: ${this.service.tasksActive}\n"
            loggString += "\t\t\t\t\t\tTasks Pending: ${this.service.tasksPending}\n"
            loggString += "\t\t\t\t\t\tTasks Completed: ${this.service.tasksCompleted}\n"
            loggString += "\t\t\t\t\t\tTasks Terminated: ${this.service.tasksTerminated}\n"

            this.logger.warn { loggString }
        }
    }

    /**
     * Export the static attributes of the tasks still in the service at the end of the run. Every other task had them
     * exported when it was deleted, so each task is exported exactly once.
     */
    private fun exportRemainingTaskMeta() {
        if (toMonitor[OutputFileSpec.TASK] != true) {
            return
        }

        for (task in service.tasks.values) {
            monitor.export(TaskMeta.of(task, startTime))
        }
    }

    /**
     * Fail the run, as its metrics cannot all be exported. The reader stops, and the [monitor] is closed first, so that
     * it leaves no writer threads or partial files behind.
     */
    private fun failRun(cause: Throwable) {
        if (failure != null) {
            return
        }

        failure = cause
        isMonitorClosed = true
        job.cancel()

        if (monitor is AutoCloseable) {
            try {
                monitor.close()
            } catch (e: Throwable) {
                // The monitor may report the failure that caused this one again
                if (e !== cause) {
                    cause.addSuppressed(e)
                }
            }
        }

        // An exception thrown by a coroutine does not reach the simulation, but one thrown by a task of its dispatcher
        // stops it, and with it the run
        dispatcher.schedule { throw IllegalStateException("Exporting the metrics of the run failed", cause) }
    }

    override fun close() {
        job.cancel()
    }

    override fun onTaskSubmission(task: SimTask) {
        // The submission of a task is sampled as the first change of its state
    }

    /**
     * Sample [task] at a change of its state, with the values at this moment. A task that just started to run has no
     * resource values yet, and the deletion of a task is not sampled, as it has left its host and the service by then:
     * the change before it, such as its completion, is its last sample.
     */
    override fun onStateChanged(
        task: SimTask,
        newState: TaskState,
    ) {
        if (toMonitor[OutputFileSpec.TASK] != true || isMonitorClosed || newState == TaskState.DELETED) {
            return
        }

        try {
            this.monitor.exportStateChange(this.taskSampler.sample(this.clock.instant(), task))
        } catch (cause: Throwable) {
            failRun(cause)
        }
    }

    override fun onTaskDeletion(task: SimTask) {
        if (toMonitor[OutputFileSpec.TASK] != true || isMonitorClosed) {
            return
        }

        try {
            // The task leaves the service, so this is the last moment to export its static attributes
            this.monitor.export(TaskMeta.of(task, startTime))
        } catch (cause: Throwable) {
            failRun(cause)
        }
    }
}
