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

import org.opendc.experiments.capelin.model.CompositeWorkload
import org.opendc.experiments.capelin.model.Workload
import org.opendc.simulator.compute.workload.SimWorkload

/**
 * A [TraceReader] for the internal VM workload trace format.
 *
 * @param rawReaders The internal raw trace readers to use.
 * @param workload The workload to read.
 * @param seed The seed to use for sampling.
 */
public class ParquetTraceReader(
    rawReaders: List<RawParquetTraceReader>,
    workload: Workload,
    seed: Int
) : TraceReader<SimWorkload> {
    /**
     * The iterator over the actual trace.
     */
    private val iterator: Iterator<TraceEntry<SimWorkload>> =
        rawReaders
            .map { it.read() }
            .run {
                if (workload is CompositeWorkload) {
                    this.zip(workload.workloads)
                } else {
                    this.zip(listOf(workload))
                }
            }
            .flatMap { sampleWorkload(it.first, workload, it.second, seed).sortedBy(TraceEntry<SimWorkload>::start) }
            .iterator()

    override fun hasNext(): Boolean = iterator.hasNext()

    override fun next(): TraceEntry<SimWorkload> = iterator.next()

    override fun close() {}
}
