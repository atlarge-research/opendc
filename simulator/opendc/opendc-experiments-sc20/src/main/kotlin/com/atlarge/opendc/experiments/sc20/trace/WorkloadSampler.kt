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
import com.atlarge.opendc.compute.core.workload.VmWorkload
import com.atlarge.opendc.experiments.sc20.experiment.model.CompositeWorkload
import com.atlarge.opendc.experiments.sc20.experiment.model.SamplingStrategy
import com.atlarge.opendc.experiments.sc20.experiment.model.Workload
import com.atlarge.opendc.format.trace.TraceEntry
import java.util.*
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
    return when {
        workload is CompositeWorkload -> sampleRegularWorkload(trace, workload, subWorkload, seed)
        workload.samplingStrategy == SamplingStrategy.HPC ->
            sampleHpcWorkload(trace, workload, seed, sampleOnLoad = false)
        workload.samplingStrategy == SamplingStrategy.HPC_LOAD ->
            sampleHpcWorkload(trace, workload, seed, sampleOnLoad = true)
        else ->
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
fun sampleHpcWorkload(
    trace: List<TraceEntry<VmWorkload>>,
    workload: Workload,
    seed: Int,
    sampleOnLoad: Boolean
): List<TraceEntry<VmWorkload>> {
    val pattern = Regex("^vm__workload__(ComputeNode|cn).*")
    val random = Random(seed)

    val fraction = workload.fraction
    val (hpc, nonHpc) = trace.partition { entry ->
        val name = entry.workload.image.name
        name.matches(pattern)
    }

    val hpcSequence = generateSequence(0) { it + 1 }
        .map { index ->
            val res = mutableListOf<TraceEntry<VmWorkload>>()
            hpc.mapTo(res) { sample(it, index) }
            res.shuffle(random)
            res
        }
        .flatten()

    val nonHpcSequence = generateSequence(0) { it + 1 }
        .map { index ->
            val res = mutableListOf<TraceEntry<VmWorkload>>()
            nonHpc.mapTo(res) { sample(it, index) }
            res.shuffle(random)
            res
        }
        .flatten()

    logger.debug { "Found ${hpc.size} HPC workloads and ${nonHpc.size} non-HPC workloads" }

    val totalLoad = if (workload is CompositeWorkload) {
        workload.totalLoad
    } else {
        trace.sumByDouble { it.workload.image.tags.getValue("total-load") as Double }
    }

    logger.debug { "Total trace load: $totalLoad" }

    val res = mutableListOf<TraceEntry<VmWorkload>>()

    if (sampleOnLoad) {
        var currentLoad = 0.0
        var i = 0
        for (entry in hpcSequence) {
            val entryLoad = entry.workload.image.tags.getValue("total-load") as Double
            if ((currentLoad + entryLoad) / totalLoad > fraction || res.size > trace.size) {
                break
            }

            currentLoad += entryLoad
            res += entry
        }

        for (entry in nonHpcSequence) {
            val entryLoad = entry.workload.image.tags.getValue("total-load") as Double
            if ((currentLoad + entryLoad) / totalLoad > 1 || res.size > trace.size) {
                break
            }

            currentLoad += entryLoad
            res += entry
        }
    } else {
        var hpcLoad = 0.0
        hpcSequence
            .take((fraction * trace.size).toInt())
            .forEach { entry ->
                hpcLoad += entry.workload.image.tags.getValue("total-load") as Double
                res.add(entry)
            }

        var nonHpcLoad = 0.0
        nonHpcSequence
            .take(((1 - fraction) * trace.size).toInt())
            .forEach { entry ->
                nonHpcLoad += entry.workload.image.tags.getValue("total-load") as Double
                res.add(entry)
            }

        logger.debug { "HPC load $hpcLoad and non-HPC load $nonHpcLoad" }
    }

    logger.info { "Sampled ${trace.size} VMs (fraction $fraction) into subset of ${res.size} VMs" }

    return res
}

/**
 * Sample a random trace entry.
 */
private fun sample(entry: TraceEntry<VmWorkload>, i: Int): TraceEntry<VmWorkload> {
    val id = UUID.nameUUIDFromBytes("${entry.workload.image.uid}-$i".toByteArray())
    val image = VmImage(
        id,
        entry.workload.image.name + "-$i",
        entry.workload.image.tags,
        entry.workload.image.flopsHistory,
        entry.workload.image.maxCores,
        entry.workload.image.requiredMemory
    )
    val vmWorkload = entry.workload.copy(uid = id, image = image, name = entry.workload.name + "-$i")
    return VmTraceEntry(vmWorkload, entry.submissionTime)
}

private class VmTraceEntry(override val workload: VmWorkload, override val submissionTime: Long) : TraceEntry<VmWorkload>
