/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.experiments.capelin.trace

import org.opendc.compute.core.workload.VmWorkload
import org.opendc.compute.simulator.SimWorkloadImage
import org.opendc.experiments.capelin.model.CompositeWorkload
import org.opendc.experiments.capelin.model.Workload
import org.opendc.format.trace.TraceEntry
import org.opendc.format.trace.TraceReader
import org.opendc.simulator.compute.interference.IMAGE_PERF_INTERFERENCE_MODEL
import org.opendc.simulator.compute.interference.PerformanceInterferenceModel
import java.util.TreeSet

/**
 * A [TraceReader] for the internal VM workload trace format.
 *
 * @param reader The internal trace reader to use.
 * @param performanceInterferenceModel The performance model covering the workload in the VM trace.
 * @param run The run to which this reader belongs.
 */
@OptIn(ExperimentalStdlibApi::class)
public class Sc20ParquetTraceReader(
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
                            SimWorkloadImage(
                                image.uid,
                                image.name,
                                image.tags + mapOf(IMAGE_PERF_INTERFERENCE_MODEL to relevantPerformanceInterferenceModelItems),
                                (image as SimWorkloadImage).workload
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
