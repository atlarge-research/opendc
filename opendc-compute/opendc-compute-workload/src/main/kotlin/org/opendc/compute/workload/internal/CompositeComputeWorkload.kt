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

package org.opendc.compute.workload.internal

import mu.KotlinLogging
import org.opendc.compute.workload.ComputeWorkload
import org.opendc.compute.workload.ComputeWorkloadLoader
import org.opendc.compute.workload.VirtualMachine
import java.util.*

/**
 * A [ComputeWorkload] that samples multiple workloads based on the total load of all workloads.
 */
internal class CompositeComputeWorkload(val sources: Map<ComputeWorkload, Double>) : ComputeWorkload {
    /**
     * The logging instance of this class.
     */
    private val logger = KotlinLogging.logger {}

    override fun resolve(loader: ComputeWorkloadLoader, random: Random): ComputeWorkload.Resolved {
        val traces = sources.map { (source, fraction) -> fraction to source.resolve(loader, random) }

        val totalLoad = traces.sumOf { (_, w) -> w.vms.sumOf { it.totalLoad } }

        val res = mutableListOf<VirtualMachine>()

        for ((fraction, w) in traces) {
            var currentLoad = 0.0

            for (entry in w.vms) {
                val entryLoad = entry.totalLoad
                if ((currentLoad + entryLoad) / totalLoad > fraction) {
                    break
                }

                currentLoad += entryLoad
                res += entry
            }
        }

        val vmCount = traces.sumOf { (_, w) -> w.vms.size }
        logger.info { "Sampled $vmCount VMs into subset of ${res.size} VMs" }

        return ComputeWorkload.Resolved(res, null)
    }
}
