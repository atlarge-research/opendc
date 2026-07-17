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

package org.opendc.sdk.runner.executor

import org.opendc.compute.simulator.telemetry.ComputeMonitor
import org.opendc.compute.simulator.telemetry.table.battery.BatteryTableReader
import org.opendc.compute.simulator.telemetry.table.host.HostTableReader
import org.opendc.compute.simulator.telemetry.table.powerSource.PowerSourceTableReader
import org.opendc.compute.simulator.telemetry.table.service.ServiceTableReader
import org.opendc.compute.simulator.telemetry.table.task.TaskTableReader

/**
 * Fans every metric record out to all [monitors] and closes any that are [AutoCloseable] when the
 * run ends. This is how parquet, in-memory and callback sinks observe a single run together.
 */
internal class CompositeComputeMonitor(private val monitors: List<ComputeMonitor>) : ComputeMonitor, AutoCloseable {
    override fun record(reader: TaskTableReader): Unit = monitors.forEach { it.record(reader) }

    override fun record(reader: HostTableReader): Unit = monitors.forEach { it.record(reader) }

    override fun record(reader: PowerSourceTableReader): Unit = monitors.forEach { it.record(reader) }

    override fun record(reader: BatteryTableReader): Unit = monitors.forEach { it.record(reader) }

    override fun record(reader: ServiceTableReader): Unit = monitors.forEach { it.record(reader) }

    override fun close(): Unit = monitors.forEach { if (it is AutoCloseable) it.close() }
}
