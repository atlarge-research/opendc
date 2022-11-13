/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.experiments.compute.internal

import mu.KotlinLogging
import org.opendc.experiments.compute.ComputeWorkload
import org.opendc.experiments.compute.ComputeWorkloadLoader
import org.opendc.experiments.compute.VirtualMachine
import java.util.UUID
import java.util.random.RandomGenerator

/**
 * A [ComputeWorkload] that samples HPC VMs in the workload.
 *
 * @param fraction The fraction of load/virtual machines to sample
 * @param sampleLoad A flag to indicate that the sampling should be based on the total load or on the number of VMs.
 */
internal class HpcSampledComputeWorkload(val source: ComputeWorkload, val fraction: Double, val sampleLoad: Boolean = false) : ComputeWorkload {
    /**
     * The logging instance of this class.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The pattern to match compute nodes in the workload.
     */
    private val pattern = Regex("^(ComputeNode|cn).*")

    override fun resolve(loader: ComputeWorkloadLoader, random: RandomGenerator): List<VirtualMachine> {
        val vms = source.resolve(loader, random)

        val (hpc, nonHpc) = vms.partition { entry ->
            val name = entry.name
            name.matches(pattern)
        }

        val hpcSequence = generateSequence(0) { it + 1 }
            .map { index ->
                val res = mutableListOf<VirtualMachine>()
                hpc.mapTo(res) { sample(it, index) }
                res
            }
            .flatten()

        val nonHpcSequence = generateSequence(0) { it + 1 }
            .map { index ->
                val res = mutableListOf<VirtualMachine>()
                nonHpc.mapTo(res) { sample(it, index) }
                res
            }
            .flatten()

        logger.debug { "Found ${hpc.size} HPC workloads and ${nonHpc.size} non-HPC workloads" }

        val totalLoad = vms.sumOf { it.totalLoad }

        logger.debug { "Total trace load: $totalLoad" }
        var hpcCount = 0
        var hpcLoad = 0.0
        var nonHpcCount = 0
        var nonHpcLoad = 0.0

        val res = mutableListOf<VirtualMachine>()

        if (sampleLoad) {
            var currentLoad = 0.0
            for (entry in hpcSequence) {
                val entryLoad = entry.totalLoad
                if ((currentLoad + entryLoad) / totalLoad > fraction) {
                    break
                }

                hpcLoad += entryLoad
                hpcCount += 1
                currentLoad += entryLoad
                res += entry
            }

            for (entry in nonHpcSequence) {
                val entryLoad = entry.totalLoad
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
                .take((fraction * vms.size).toInt())
                .forEach { entry ->
                    hpcLoad += entry.totalLoad
                    hpcCount += 1
                    res.add(entry)
                }

            nonHpcSequence
                .take(((1 - fraction) * vms.size).toInt())
                .forEach { entry ->
                    nonHpcLoad += entry.totalLoad
                    nonHpcCount += 1
                    res.add(entry)
                }
        }

        logger.debug { "HPC $hpcCount (load $hpcLoad) and non-HPC $nonHpcCount (load $nonHpcLoad)" }
        logger.debug { "Total sampled load: ${hpcLoad + nonHpcLoad}" }
        logger.info { "Sampled ${vms.size} VMs (fraction $fraction) into subset of ${res.size} VMs" }

        return res
    }

    /**
     * Sample a random trace entry.
     */
    private fun sample(entry: VirtualMachine, i: Int): VirtualMachine {
        val uid = UUID.nameUUIDFromBytes("${entry.uid}-$i".toByteArray())
        return entry.copy(uid = uid)
    }
}
