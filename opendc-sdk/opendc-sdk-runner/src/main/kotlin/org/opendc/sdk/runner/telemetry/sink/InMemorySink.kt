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

package org.opendc.sdk.runner.telemetry.sink

import org.opendc.sdk.model.telemetry.OutputFileSpec
import org.opendc.sdk.runner.telemetry.MetricExporter
import org.opendc.sdk.runner.telemetry.table.battery.BatterySample
import org.opendc.sdk.runner.telemetry.table.host.HostSample
import org.opendc.sdk.runner.telemetry.table.powerSource.PowerSourceSample
import org.opendc.sdk.runner.telemetry.table.service.ServiceSample
import org.opendc.sdk.runner.telemetry.table.task.TaskSample

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

            override val monitor: MetricExporter =
                object : MetricExporter {
                    override fun export(reader: BatterySample) {
                        if (OutputFileSpec.BATTERY in captureTables) battery += reader
                    }

                    override fun export(reader: HostSample) {
                        if (OutputFileSpec.HOST in captureTables) host += reader
                    }

                    override fun export(reader: PowerSourceSample) {
                        if (OutputFileSpec.POWER_SOURCE in captureTables) powerSource += reader
                    }

                    override fun export(reader: ServiceSample) {
                        if (OutputFileSpec.SERVICE in captureTables) service += reader
                    }

                    override fun export(reader: TaskSample) {
                        if (OutputFileSpec.TASK in captureTables) task += reader
                    }
                }

            override val tables: Set<OutputFileSpec> = captureTables

            override fun result(): SinkResult = CollectedMetrics(host, task, service, powerSource, battery)
        }
    }
