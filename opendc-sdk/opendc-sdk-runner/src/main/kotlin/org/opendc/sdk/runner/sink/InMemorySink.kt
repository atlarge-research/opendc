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
import org.opendc.compute.simulator.telemetry.table.battery.BatteryTableReader
import org.opendc.compute.simulator.telemetry.table.host.HostTableReader
import org.opendc.compute.simulator.telemetry.table.powerSource.PowerSourceTableReader
import org.opendc.compute.simulator.telemetry.table.service.ServiceTableReader
import org.opendc.compute.simulator.telemetry.table.task.TaskTableReader
import org.opendc.sdk.model.export.OutputFile
import org.opendc.sdk.runner.internal.toEngineOutputFiles

/**
 * Captures each run's metrics in memory as strongly-typed [CollectedMetrics], available on the
 * run's result. Choose which [tables] to capture; each records the full typed sample per snapshot.
 *
 * @property tables The metric tables to capture; defaults to all.
 */
public class InMemorySink
    @JvmOverloads
    constructor(
        private val tables: Set<OutputFile> = OutputFile.entries.toSet(),
    ) : OutputSink {
        override fun open(context: RunContext): SinkSession = Session(tables)

        private class Session(private val captureTables: Set<OutputFile>) : SinkSession {
            private val host = mutableListOf<HostSample>()
            private val task = mutableListOf<TaskSample>()
            private val service = mutableListOf<ServiceSample>()
            private val powerSource = mutableListOf<PowerSourceSample>()
            private val battery = mutableListOf<BatterySample>()

            override val monitor: ComputeMonitor =
                object : ComputeMonitor {
                    override fun record(reader: HostTableReader) {
                        if (OutputFile.HOST in captureTables) host += reader.toSample()
                    }

                    override fun record(reader: TaskTableReader) {
                        if (OutputFile.TASK in captureTables) task += reader.toSample()
                    }

                    override fun record(reader: ServiceTableReader) {
                        if (OutputFile.SERVICE in captureTables) service += reader.toSample()
                    }

                    override fun record(reader: PowerSourceTableReader) {
                        if (OutputFile.POWER_SOURCE in captureTables) powerSource += reader.toSample()
                    }

                    override fun record(reader: BatteryTableReader) {
                        if (OutputFile.BATTERY in captureTables) battery += reader.toSample()
                    }
                }

            override val tables: Set<OutputFiles> = captureTables.map { it.toEngineOutputFiles() }.toSet()

            override fun result(): SinkResult = CollectedMetrics(host, task, service, powerSource, battery)
        }
    }

private fun HostTableReader.toSample(): HostSample =
    HostSample(
        timestamp.toEpochMilli(),
        timestampAbsolute.toEpochMilli(),
        hostInfo.name,
        hostInfo.clusterName,
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

private fun TaskTableReader.toSample(): TaskSample =
    TaskSample(
        timestamp.toEpochMilli(),
        timestampAbsolute.toEpochMilli(),
        taskInfo.id,
        taskInfo.name,
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

private fun ServiceTableReader.toSample(): ServiceSample =
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

private fun PowerSourceTableReader.toSample(): PowerSourceSample =
    PowerSourceSample(
        timestamp.toEpochMilli(),
        timestampAbsolute.toEpochMilli(),
        hostsConnected,
        powerDraw,
        energyUsage,
        carbonIntensity,
        carbonEmission,
    )

private fun BatteryTableReader.toSample(): BatterySample =
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
