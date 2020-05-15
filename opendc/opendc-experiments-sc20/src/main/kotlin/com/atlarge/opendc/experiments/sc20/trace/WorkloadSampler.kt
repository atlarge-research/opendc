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

import com.atlarge.opendc.compute.core.workload.VmWorkload
import com.atlarge.opendc.experiments.sc20.experiment.model.Workload
import com.atlarge.opendc.format.trace.TraceEntry
import mu.KotlinLogging
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 * Sample the workload for the specified [run].
 */
fun sampleWorkload(trace: List<TraceEntry<VmWorkload>>, workload: Workload, seed: Int): List<TraceEntry<VmWorkload>> {
    return sampleRegularWorkload(trace, workload, seed)
}

/**
 * Sample a regular (non-HPC) workload.
 */
fun sampleRegularWorkload(trace: List<TraceEntry<VmWorkload>>, workload: Workload, seed: Int): List<TraceEntry<VmWorkload>> {
    val fraction = workload.fraction
    if (fraction >= 1) {
        return trace
    }

    val shuffled = trace.shuffled(Random(seed))
    val res = mutableListOf<TraceEntry<VmWorkload>>()
    val totalLoad = shuffled.sumByDouble { it.workload.image.tags.getValue("total-load") as Double }
    var currentLoad = 0.0

    for (entry in shuffled) {
        val entryLoad = entry.workload.image.tags.getValue("total-load") as Double
        if ((currentLoad + entryLoad) / totalLoad > fraction) {
            break
        }

        currentLoad += entryLoad
        res += entry
    }

    logger.info { "Sampled ${trace.size} VMs (fraction $fraction) into subset of ${res.size} VMs" }

    return res
}
