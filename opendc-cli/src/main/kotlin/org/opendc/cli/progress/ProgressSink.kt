/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.cli.progress

import org.opendc.compute.simulator.telemetry.ComputeMonitor
import org.opendc.compute.simulator.telemetry.OutputFiles
import org.opendc.compute.simulator.telemetry.table.service.ServiceTableReader
import org.opendc.sdk.runner.sink.OutputSink
import org.opendc.sdk.runner.sink.RunContext
import org.opendc.sdk.runner.sink.SinkResult
import org.opendc.sdk.runner.sink.SinkSession

/**
 * A metric sink that feeds each run's task-completion counts into a shared [ExperimentProgress].
 * Only the (single, cheap) service table is recorded, so it adds no per-host or per-task overhead.
 */
internal class ProgressSink(private val progress: ExperimentProgress) : OutputSink {
    override fun open(context: RunContext): SinkSession {
        val key = RunKey(context.scenarioId, context.seed)
        progress.registerRun(key, context.taskCount)
        return object : SinkSession {
            override val monitor: ComputeMonitor =
                object : ComputeMonitor {
                    override fun record(reader: ServiceTableReader) {
                        progress.update(key, (reader.tasksCompleted + reader.tasksTerminated).toLong())
                    }
                }
            override val tables: Set<OutputFiles> = setOf(OutputFiles.SERVICE)

            override fun result(): SinkResult? = null
        }
    }
}
