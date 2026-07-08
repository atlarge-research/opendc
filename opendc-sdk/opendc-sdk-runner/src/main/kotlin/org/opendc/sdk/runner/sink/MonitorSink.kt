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

/**
 * Feeds each run's metrics to a caller-supplied [ComputeMonitor] — the full-control escape hatch
 * for consumers with an existing monitor (e.g. the web module's reporting monitor).
 *
 * A fresh monitor is produced per run by [factory]; the fixed-instance secondary constructor is
 * intended for single-run use, since one monitor cannot be safely shared across concurrent runs.
 *
 * @property factory Produces the monitor for a given run.
 * @property tables The metric tables to record for the monitor.
 */
public class MonitorSink
    @JvmOverloads
    constructor(
        private val tables: Set<OutputFiles> = OutputFiles.entries.toSet(),
        private val factory: (RunContext) -> ComputeMonitor,
    ) : OutputSink {
        public constructor(monitor: ComputeMonitor, tables: Set<OutputFiles> = OutputFiles.entries.toSet()) : this(tables, { monitor })

        override fun open(context: RunContext): SinkSession {
            val runMonitor = factory(context)
            return object : SinkSession {
                override val monitor: ComputeMonitor = runMonitor
                override val tables: Set<OutputFiles> = this@MonitorSink.tables

                override fun result(): SinkResult? = null
            }
        }
    }
