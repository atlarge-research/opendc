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
import com.atlarge.opendc.experiments.sc20.experiment.model.CompositeWorkload
import com.atlarge.opendc.experiments.sc20.experiment.model.Workload
import com.atlarge.opendc.format.trace.TraceEntry
import kotlin.random.Random
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Sample the workload for the specified [run].
 */
fun sampleWorkload(
    trace: List<TraceEntry<VmWorkload>>,
    workload: Workload,
    subWorkload: Workload,
    seed: Int
): List<TraceEntry<VmWorkload>> {
    return if (workload is CompositeWorkload) {
        sampleRegularWorkload(trace, workload, subWorkload, seed)
    } else {
        sampleRegularWorkload(trace, workload, workload, seed)
    }
}

/**
 * Sample a regular (non-HPC) workload.
 */
fun sampleRegularWorkload(
    trace: List<TraceEntry<VmWorkload>>,
    workload: Workload,
    subWorkload: Workload,
    seed: Int
): List<TraceEntry<VmWorkload>> {
    val fraction = subWorkload.fraction

    val shuffled = trace.shuffled(Random(seed))
    val res = mutableListOf<TraceEntry<VmWorkload>>()
    val totalLoad = if (workload is CompositeWorkload) {
        workload.totalLoad
    } else {
        shuffled.sumByDouble { it.workload.image.tags.getValue("total-load") as Double }
    }
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

/**
 * Sample a HPC workload.
 */
fun sampleHPCWorkload(
    trace: List<TraceEntry<VmWorkload>>,
    workload: Workload,
    seed: Int,
    fraction: Double,
    sampleOnLoad: Boolean
): List<TraceEntry<VmWorkload>> {
    val pattern = Regex("^(ComputeNode|cn)")
    val random = Random(seed)

    val (hpc, nonHpc) = trace.partition { entry ->
        val name = entry.workload.image.name
        name.matches(pattern)
    }

    val totalLoad = if (workload is CompositeWorkload) {
        workload.totalLoad
    } else {
        trace.sumByDouble { it.workload.image.tags.getValue("total-load") as Double }
    }

    val res = mutableListOf<TraceEntry<VmWorkload>>()

    if (sampleOnLoad) {
        val hpcPool = mutableListOf<TraceEntry<VmWorkload>>()

        // We repeat the HPC VMs 1000 times to have a large enough pool to pick VMs from.
        repeat(1000) {
            hpcPool.addAll(hpc)
        }
        hpcPool.shuffle(random)

        var currentLoad = 0.0
        for (entry in hpcPool) {
            val entryLoad = entry.workload.image.tags.getValue("total-load") as Double
            if ((currentLoad + entryLoad) / totalLoad > fraction || res.size > trace.size) {
                break
            }

            currentLoad += entryLoad
            res += entry
        }

        (nonHpc as MutableList<TraceEntry<VmWorkload>>).shuffle(random)
        for (entry in nonHpc) {
            val entryLoad = entry.workload.image.tags.getValue("total-load") as Double
            if ((currentLoad + entryLoad) / totalLoad > 1 || res.size > trace.size) {
                break
            }

            currentLoad += entryLoad
            res += entry
        }
    } else {
        (hpc as MutableList<TraceEntry<VmWorkload>>).shuffle(random)
        (nonHpc as MutableList<TraceEntry<VmWorkload>>).shuffle(random)

        res.addAll(hpc.subList(0, (fraction * trace.size).toInt()))
        res.addAll(nonHpc.subList(0, ((1 - fraction) * trace.size).toInt()))
    }

    logger.info { "Sampled ${trace.size} VMs (fraction $fraction) into subset of ${res.size} VMs" }

    return res
}
