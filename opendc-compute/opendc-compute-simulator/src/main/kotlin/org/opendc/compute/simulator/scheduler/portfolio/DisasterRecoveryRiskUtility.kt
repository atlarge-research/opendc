/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.compute.simulator.scheduler.portfolio

import org.opendc.compute.simulator.service.HostView
import kotlin.math.pow

/**
 * Disaster Recovery Risk (DRR) utility function.
 *
 * Computes the risk of not being able to absorb a full datacenter failure.
 * Based on Equations 2-3 from "Portfolio Scheduling for Managing Operational and
 * Disaster-Recovery Risks" (van Beek et al., ISPDC 2019).
 *
 * Groups hosts by datacenter. For each datacenter i:
 *   W_i = total memory used (provisioned) in datacenter i
 *   E_complement_i = total available memory in all OTHER datacenters
 *
 *   rd_i = (W_i - E_complement_i) / E_complement_i   if W_i <= E_complement_i
 *   rd_i = (W_i - E_complement_i) / W_i               if W_i > E_complement_i
 *
 * System DRR: rd = (Π ((rd_i + 1) / 2))^(1/n)
 *
 * @return A score in [0, 1], where 0 means full recovery capability and 1 means no recovery possible.
 */
public class DisasterRecoveryRiskUtility : UtilityFunction {
    override fun evaluate(
        hosts: Collection<HostView>,
        simulatedPlacement: SimulatedPlacement?,
    ): Double {
        // Group hosts by datacenter
        val datacenterGroups = hosts.groupBy { it.datacenterName }

        // Need at least 2 datacenters for DRR to be meaningful
        if (datacenterGroups.size < 2) return 0.0

        // Compute per-datacenter: workload (memory used) and total memory capacity
        data class DcStats(val workload: Long, val availableMemory: Long)

        val dcStats =
            datacenterGroups.map { (_, dcHosts) ->
                var workload =
                    dcHosts.sumOf { host ->
                        host.host.getModel().memoryCapacity() - host.availableMemory
                    }
                var available = dcHosts.sumOf { it.availableMemory }

                // If the simulated placement targets a host in this datacenter,
                // account for the task's memory
                if (simulatedPlacement != null && dcHosts.any { it === simulatedPlacement.host }) {
                    workload += simulatedPlacement.task.memorySize
                    available -= simulatedPlacement.task.memorySize
                }

                DcStats(workload, available.coerceAtLeast(0))
            }

        val totalAvailable = dcStats.sumOf { it.availableMemory }
        val n = dcStats.size

        // Compute per-datacenter risk rd_i
        var product = 1.0
        for (dc in dcStats) {
            val wi = dc.workload.toDouble()
            val eComplement = (totalAvailable - dc.availableMemory).toDouble()

            val rdi: Double =
                if (eComplement <= 0.0) {
                    if (wi > 0.0) 1.0 else 0.0
                } else if (wi <= eComplement) {
                    (wi - eComplement) / eComplement
                } else {
                    (wi - eComplement) / wi
                }

            // Normalize to [0, 1]: (rd_i + 1) / 2
            product *= (rdi + 1.0) / 2.0
        }

        // Geometric mean: rd = product^(1/n)
        return product.pow(1.0 / n)
    }
}
