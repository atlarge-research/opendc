/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.experiments.sc20.trace

import com.atlarge.opendc.compute.core.image.VmImage
import com.atlarge.opendc.compute.core.workload.IMAGE_PERF_INTERFERENCE_MODEL
import com.atlarge.opendc.compute.core.workload.PerformanceInterferenceModel
import com.atlarge.opendc.compute.core.workload.VmWorkload
import com.atlarge.opendc.experiments.sc20.experiment.model.CompositeWorkload
import com.atlarge.opendc.experiments.sc20.experiment.model.Workload
import com.atlarge.opendc.format.trace.TraceEntry
import com.atlarge.opendc.format.trace.TraceReader
import java.util.TreeSet

/**
 * A [TraceReader] for the internal VM workload trace format.
 *
 * @param reader The internal trace reader to use.
 * @param performanceInterferenceModel The performance model covering the workload in the VM trace.
 * @param run The run to which this reader belongs.
 */
@OptIn(ExperimentalStdlibApi::class)
class Sc20ParquetTraceReader(
    rawReaders: List<Sc20RawParquetTraceReader>,
    performanceInterferenceModel: Map<String, PerformanceInterferenceModel>,
    workload: Workload,
    seed: Int
) : TraceReader<VmWorkload> {
    /**
     * The iterator over the actual trace.
     */
    private val iterator: Iterator<TraceEntry<VmWorkload>> =
        rawReaders
            .map { it.read() }
            .run {
                if (workload is CompositeWorkload) {
                    this.zip(workload.workloads)
                } else {
                    this.zip(listOf(workload))
                }
            }
            .map { sampleWorkload(it.first, workload, it.second, seed) }
            .flatten()
            .run {
                // Apply performance interference model
                if (performanceInterferenceModel.isEmpty())
                    this
                else {
                    map { entry ->
                        val image = entry.workload.image
                        val id = image.name
                        val relevantPerformanceInterferenceModelItems =
                            performanceInterferenceModel[id] ?: PerformanceInterferenceModel(TreeSet())

                        val newImage =
                            VmImage(
                                image.uid,
                                image.name,
                                mapOf(IMAGE_PERF_INTERFERENCE_MODEL to relevantPerformanceInterferenceModelItems),
                                image.flopsHistory,
                                image.maxCores,
                                image.requiredMemory
                            )
                        val newWorkload = entry.workload.copy(image = newImage)
                        Sc20RawParquetTraceReader.TraceEntryImpl(entry.submissionTime, newWorkload)
                    }
                }
            }
            .iterator()

    override fun hasNext(): Boolean = iterator.hasNext()

    override fun next(): TraceEntry<VmWorkload> = iterator.next()

    override fun close() {}
}
