/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.runner.sink

import org.opendc.compute.simulator.telemetry.ComputeMonitor
import org.opendc.compute.simulator.telemetry.OutputFiles
import org.opendc.sdk.model.export.OutputFileSpec
import org.opendc.sdk.runner.factory.toEngineOutputFiles
import org.opendc.compute.simulator.telemetry.table.battery.BatterySample as BatterySampleTmp
import org.opendc.compute.simulator.telemetry.table.host.HostSample as HostSampleTmp
import org.opendc.compute.simulator.telemetry.table.powerSource.PowerSourceSample as PowerSourceSampleTmp
import org.opendc.compute.simulator.telemetry.table.service.ServiceSample as ServiceSampleTmp
import org.opendc.compute.simulator.telemetry.table.task.TaskSample as TaskSampleTmp

/**
 * Captures each run's metrics in memory as strongly-typed [CollectedMetrics], available on the
 * run's result. Choose which [tables] to capture; each records the full typed sample per snapshot.
 *
 * @property tables The metric tables to capture; defaults to all.
 */
public class InMemorySink
    @JvmOverloads
    constructor(
        private val tables: Set<OutputFileSpec> = OutputFileSpec.entries.toSet(),
    ) : OutputSink {
        override fun open(context: RunContext): SinkSession = Session(tables)

        private class Session(private val captureTables: Set<OutputFileSpec>) : SinkSession {
            private val host = mutableListOf<HostSample>()
            private val task = mutableListOf<TaskSample>()
            private val service = mutableListOf<ServiceSample>()
            private val powerSource = mutableListOf<PowerSourceSample>()
            private val battery = mutableListOf<BatterySample>()

            override val monitor: ComputeMonitor =
                object : ComputeMonitor {
                    override fun record(reader: BatterySampleTmp) {
                        if (OutputFileSpec.BATTERY in captureTables) battery += reader.toSample()
                    }

                    override fun record(reader: HostSampleTmp) {
                        if (OutputFileSpec.HOST in captureTables) host += reader.toSample()
                    }

                    override fun record(reader: PowerSourceSampleTmp) {
                        if (OutputFileSpec.POWER_SOURCE in captureTables) powerSource += reader.toSample()
                    }

                    override fun record(reader: ServiceSampleTmp) {
                        if (OutputFileSpec.SERVICE in captureTables) service += reader.toSample()
                    }

                    override fun record(reader: TaskSampleTmp) {
                        if (OutputFileSpec.TASK in captureTables) task += reader.toSample()
                    }
                }

            override val tables: Set<OutputFiles> = captureTables.map { it.toEngineOutputFiles() }.toSet()

            override fun result(): SinkResult = CollectedMetrics(host, task, service, powerSource, battery)
        }
    }

private fun HostSampleTmp.toSample(): HostSample =
    HostSample(
        timestamp.toEpochMilli(),
        timestampAbsolute.toEpochMilli(),
        hostName ?: "uknown-host",
        clusterName ?: "uknown-cluster",
        tasksActive,
        tasksTerminated,
        cpuCapacity,
        cpuUsage,
        cpuDemand,
        cpuUtilization,
        cpuActiveTime,
        cpuIdleTime,
        cpuStealTime,
        cpuLostTime,
        gpuUsages.toList(),
        gpuDemands.toList(),
        gpuUtilizations.toList(),
        gpuPowerDraws.toList(),
        powerDraw,
        energyUsage,
        embodiedCarbon,
        uptime,
        downtime,
    )

private fun TaskSampleTmp.toSample(): TaskSample =
    TaskSample(
        timestamp.toEpochMilli(),
        timestampAbsolute.toEpochMilli(),
        taskId,
        hostName,
        taskState?.toString(),
        cpuLimit,
        cpuUsage,
        cpuDemand,
        cpuActiveTime,
        cpuIdleTime,
        cpuStealTime,
        cpuLostTime,
        gpuLimit,
        gpuUsage,
        gpuDemand,
        uptime,
        downtime,
        numFailures,
        numPauses,
        submissionTime,
        scheduleTime,
        finishTime,
        schedulingDelay,
        failureDelay,
        checkpointDelay,
    )

private fun ServiceSampleTmp.toSample(): ServiceSample =
    ServiceSample(
        timestamp.toEpochMilli(),
        timestampAbsolute.toEpochMilli(),
        hostsUp,
        hostsDown,
        tasksTotal,
        tasksPending,
        tasksActive,
        tasksCompleted,
        tasksTerminated,
        attemptsSuccess,
        attemptsFailure,
    )

private fun PowerSourceSampleTmp.toSample(): PowerSourceSample =
    PowerSourceSample(
        timestamp.toEpochMilli(),
        timestampAbsolute.toEpochMilli(),
        hostsConnected,
        powerDraw,
        energyUsage,
        carbonIntensity,
        carbonEmission,
    )

private fun BatterySampleTmp.toSample(): BatterySample =
    BatterySample(
        timestamp.toEpochMilli(),
        timestampAbsolute.toEpochMilli(),
        powerDraw,
        energyUsage,
        embodiedCarbonEmission,
        charge,
        capacity,
        batteryState.toString(),
    )
