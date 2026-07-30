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
import org.opendc.compute.simulator.telemetry.table.battery.BatterySample as BatterySampleTmp
import org.opendc.compute.simulator.telemetry.table.host.HostSample as HostSampleTmp
import org.opendc.compute.simulator.telemetry.table.powerSource.PowerSourceSample as PowerSourceSampleTmp
import org.opendc.compute.simulator.telemetry.table.service.ServiceSample as ServiceSampleTmp
import org.opendc.compute.simulator.telemetry.table.task.TaskSample as TaskSampleTmp

/**
 * Streams each metric snapshot to per-table callbacks as it is produced, without retaining it.
 * Only the tables with a non-null callback are recorded. Useful for live progress or streaming to
 * an external system on large sweeps where holding every sample in memory is undesirable.
 *
 * Readers are reused and reset after each callback, so copy any value you need to keep.
 */
public class CallbackSink(
    private val onHost: ((HostSampleTmp) -> Unit)? = null,
    private val onTask: ((TaskSampleTmp) -> Unit)? = null,
    private val onService: ((ServiceSampleTmp) -> Unit)? = null,
    private val onPowerSource: ((PowerSourceSampleTmp) -> Unit)? = null,
    private val onBattery: ((BatterySampleTmp) -> Unit)? = null,
) : OutputSink {
    override fun open(context: RunContext): SinkSession =
        object : SinkSession {
            override val monitor: ComputeMonitor =
                object : ComputeMonitor {
                    override fun record(reader: BatterySampleTmp) {
                        onBattery?.invoke(reader)
                    }

                    override fun record(reader: HostSampleTmp) {
                        onHost?.invoke(reader)
                    }

                    override fun record(reader: PowerSourceSampleTmp) {
                        onPowerSource?.invoke(reader)
                    }

                    override fun record(reader: ServiceSampleTmp) {
                        onService?.invoke(reader)
                    }

                    override fun record(reader: TaskSampleTmp) {
                        onTask?.invoke(reader)
                    }
                }

            override val tables: Set<OutputFiles> =
                buildSet {
                    if (onHost != null) add(OutputFiles.HOST)
                    if (onTask != null) add(OutputFiles.TASK)
                    if (onService != null) add(OutputFiles.SERVICE)
                    if (onPowerSource != null) add(OutputFiles.POWER_SOURCE)
                    if (onBattery != null) add(OutputFiles.BATTERY)
                }

            override fun result(): SinkResult? = null
        }
}
