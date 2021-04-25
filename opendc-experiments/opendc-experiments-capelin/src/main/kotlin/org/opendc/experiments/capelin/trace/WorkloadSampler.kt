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

import mu.KotlinLogging
import org.opendc.experiments.capelin.model.CompositeWorkload
import org.opendc.experiments.capelin.model.SamplingStrategy
import org.opendc.experiments.capelin.model.Workload
import org.opendc.format.trace.TraceEntry
import org.opendc.simulator.compute.workload.SimWorkload
import java.util.*
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 * Sample the workload for the specified [run].
 */
public fun sampleWorkload(
    trace: List<TraceEntry<SimWorkload>>,
    workload: Workload,
    subWorkload: Workload,
    seed: Int
): List<TraceEntry<SimWorkload>> {
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
public fun sampleRegularWorkload(
    trace: List<TraceEntry<SimWorkload>>,
    workload: Workload,
    subWorkload: Workload,
    seed: Int
): List<TraceEntry<SimWorkload>> {
    val fraction = subWorkload.fraction

    val shuffled = trace.shuffled(Random(seed))
    val res = mutableListOf<TraceEntry<SimWorkload>>()
    val totalLoad = if (workload is CompositeWorkload) {
        workload.totalLoad
    } else {
        shuffled.sumByDouble { it.meta.getValue("total-load") as Double }
    }
    var currentLoad = 0.0

    for (entry in shuffled) {
        val entryLoad = entry.meta.getValue("total-load") as Double
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
public fun sampleHpcWorkload(
    trace: List<TraceEntry<SimWorkload>>,
    workload: Workload,
    seed: Int,
    sampleOnLoad: Boolean
): List<TraceEntry<SimWorkload>> {
    val pattern = Regex("^vm__workload__(ComputeNode|cn).*")
    val random = Random(seed)

    val fraction = workload.fraction
    val (hpc, nonHpc) = trace.partition { entry ->
        val name = entry.name
        name.matches(pattern)
    }

    val hpcSequence = generateSequence(0) { it + 1 }
        .map { index ->
            val res = mutableListOf<TraceEntry<SimWorkload>>()
            hpc.mapTo(res) { sample(it, index) }
            res.shuffle(random)
            res
        }
        .flatten()

    val nonHpcSequence = generateSequence(0) { it + 1 }
        .map { index ->
            val res = mutableListOf<TraceEntry<SimWorkload>>()
            nonHpc.mapTo(res) { sample(it, index) }
            res.shuffle(random)
            res
        }
        .flatten()

    logger.debug { "Found ${hpc.size} HPC workloads and ${nonHpc.size} non-HPC workloads" }

    val totalLoad = if (workload is CompositeWorkload) {
        workload.totalLoad
    } else {
        trace.sumByDouble { it.meta.getValue("total-load") as Double }
    }

    logger.debug { "Total trace load: $totalLoad" }
    var hpcCount = 0
    var hpcLoad = 0.0
    var nonHpcCount = 0
    var nonHpcLoad = 0.0

    val res = mutableListOf<TraceEntry<SimWorkload>>()

    if (sampleOnLoad) {
        var currentLoad = 0.0
        for (entry in hpcSequence) {
            val entryLoad = entry.meta.getValue("total-load") as Double
            if ((currentLoad + entryLoad) / totalLoad > fraction) {
                break
            }

            hpcLoad += entryLoad
            hpcCount += 1
            currentLoad += entryLoad
            res += entry
        }

        for (entry in nonHpcSequence) {
            val entryLoad = entry.meta.getValue("total-load") as Double
            if ((currentLoad + entryLoad) / totalLoad > 1) {
                break
            }

            nonHpcLoad += entryLoad
            nonHpcCount += 1
            currentLoad += entryLoad
            res += entry
        }
    } else {
        hpcSequence
            .take((fraction * trace.size).toInt())
            .forEach { entry ->
                hpcLoad += entry.meta.getValue("total-load") as Double
                hpcCount += 1
                res.add(entry)
            }

        nonHpcSequence
            .take(((1 - fraction) * trace.size).toInt())
            .forEach { entry ->
                nonHpcLoad += entry.meta.getValue("total-load") as Double
                nonHpcCount += 1
                res.add(entry)
            }
    }

    logger.debug { "HPC $hpcCount (load $hpcLoad) and non-HPC $nonHpcCount (load $nonHpcLoad)" }
    logger.debug { "Total sampled load: ${hpcLoad + nonHpcLoad}" }
    logger.info { "Sampled ${trace.size} VMs (fraction $fraction) into subset of ${res.size} VMs" }

    return res
}

/**
 * Sample a random trace entry.
 */
private fun sample(entry: TraceEntry<SimWorkload>, i: Int): TraceEntry<SimWorkload> {
    val uid = UUID.nameUUIDFromBytes("${entry.uid}-$i".toByteArray())
    return entry.copy(uid = uid)
}
