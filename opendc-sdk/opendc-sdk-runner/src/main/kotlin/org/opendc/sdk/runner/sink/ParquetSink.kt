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
import org.opendc.compute.simulator.telemetry.parquet.ParquetComputeMonitor
import java.nio.file.Path

/**
 * Writes each run's metrics to Parquet files, reproducing the canonical OpenDC layout
 * `<root>/<experiment>/raw-output/<scenarioId>/seed=<seed>/{host,task,powerSource,battery,service}.parquet`.
 *
 * Which tables and columns are written is governed by the scenario's export model.
 *
 * @property root The output root directory.
 * @property bufferSize The writer's buffer size in records.
 */
public class ParquetSink
    @JvmOverloads
    constructor(
        private val root: Path,
        private val bufferSize: Int = 4096,
    ) : OutputSink {
        override fun open(context: RunContext): SinkSession {
            val export = context.export
            val base = root.resolve(context.experimentName).resolve("raw-output").resolve(context.scenarioId.toString())
            val partition = "seed=${context.seed}"
            val parquetMonitor = ParquetComputeMonitor(base.toFile(), partition, bufferSize, export.filesToExport, export.config)
            return object : SinkSession {
                override val monitor: ComputeMonitor = parquetMonitor
                override val tables: Set<OutputFiles> = export.filesToExport.filterValues { it }.keys

                override fun result(): SinkResult = ParquetOutput(base.resolve(partition))
            }
        }
    }
