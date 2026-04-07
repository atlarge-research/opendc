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

/**
 * Operational Risk (OR) utility function.
 *
 * Computes the risk that VMs will not receive the requested amount of CPU resources.
 * Based on Equation 1 from "Portfolio Scheduling for Managing Operational and
 * Disaster-Recovery Risks" (van Beek et al., ISPDC 2019).
 *
 * OR = (1/N) * Σ((demand - usage) / demand) for all hosts where demand > 0
 *
 * When a [SimulatedPlacement] is provided, the task's CPU capacity is added to the
 * target host's demand, simulating what would happen if the task were placed there.
 *
 * @return A score in [0, 1], where 0 means no contention and 1 means full contention.
 */
public class OperationalRiskUtility : UtilityFunction {
    override fun evaluate(
        hosts: Collection<HostView>,
        simulatedPlacement: SimulatedPlacement?,
    ): Double {
        var totalRisk = 0.0
        var hostsWithDemand = 0

        for (host in hosts) {
            val cpuStats = host.host.getCpuStats()
            var demand = cpuStats.demand()
            var usage = cpuStats.usage()
            val capacity = cpuStats.capacity()

            // If this host is the target of a simulated placement,
            // add the task's CPU demand and estimate new usage
            if (simulatedPlacement != null && host === simulatedPlacement.host) {
                demand += simulatedPlacement.task.cpuCapacity
                // Usage can't exceed capacity
                usage = demand.coerceAtMost(capacity)
            }

            if (demand <= 0.0) continue

            val contention = (demand - usage).coerceAtLeast(0.0) / demand
            totalRisk += contention
            hostsWithDemand++
        }

        if (hostsWithDemand == 0) return 0.0
        return totalRisk / hostsWithDemand
    }
}
