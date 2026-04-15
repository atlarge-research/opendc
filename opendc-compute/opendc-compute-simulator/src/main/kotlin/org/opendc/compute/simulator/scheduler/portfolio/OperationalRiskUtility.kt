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
import org.opendc.simulator.compute.workload.trace.TraceWorkload

/**
 * Operational Risk (OR) utility function.
 *
 * Discretizes the forward-look window into 5-minute intervals. For each host and interval,
 * computes CPU utilization (demand / capacity) and multiplies by the CPU overcommit ratio
 * (total vCPUs / physical cores) to get a contention score. The per-host risk is the MAX
 * contention score across all intervals. The final score is the mean across all hosts.
 *
 * @param forwardLookMs How far ahead to look in milliseconds (default 1 hour).
 */
public class OperationalRiskUtility(
    private val forwardLookMs: Long = 3600000L,
) : UtilityFunction {
    override fun evaluate(
        hosts: Collection<HostView>,
        simulatedPlacement: SimulatedPlacement?,
    ): Double {
        val intervalMs = 300_000L // 5 minutes
        val numIntervals = ((forwardLookMs + intervalMs - 1) / intervalMs).toInt()
        if (numIntervals <= 0) return 0.0

        var totalRisk = 0.0
        var hostCount = 0

        for (host in hosts) {
            val capacity = host.host.getCpuStats().capacity()
            val physicalCores = host.host.getModel().coreCount()
            val guests = host.host.getGuests()

            val allTasks =
                guests.map { it.task }.toMutableList()
            if (simulatedPlacement != null && host === simulatedPlacement.host) {
                allTasks.add(simulatedPlacement.task)
            }

            val totalVCpus = allTasks.sumOf { it.cpuCoreCount }
            val overcommitRatio = if (physicalCores > 0) totalVCpus.toDouble() / physicalCores else 0.0

            val intervalDemand = DoubleArray(numIntervals)

            for (task in allTasks) {
                val fragments = (task.workload as? TraceWorkload)?.fragments ?: continue
                var elapsed = 0L
                for (fragment in fragments) {
                    if (elapsed >= forwardLookMs) break
                    val fragStart = elapsed
                    val fragEnd = minOf(elapsed + fragment.duration(), forwardLookMs)
                    val startInterval = (fragStart / intervalMs).toInt()
                    val endInterval = minOf(((fragEnd - 1) / intervalMs).toInt(), numIntervals - 1)

                    for (i in startInterval..endInterval) {
                        val iStart = i.toLong() * intervalMs
                        val iEnd = (i + 1).toLong() * intervalMs
                        val overlapStart = maxOf(fragStart, iStart)
                        val overlapEnd = minOf(fragEnd, iEnd)
                        val overlap = overlapEnd - overlapStart
                        if (overlap > 0) {
                            intervalDemand[i] += fragment.cpuUsage() * overlap / intervalMs
                        }
                    }
                    elapsed += fragment.duration()
                }
            }

            var maxContention = 0.0
            for (i in 0 until numIntervals) {
                val utilization = if (capacity > 0) intervalDemand[i] / capacity else 0.0
                val contention = utilization * overcommitRatio
                if (contention > maxContention) maxContention = contention
            }

            totalRisk += maxContention
            hostCount++
        }

        return if (hostCount > 0) totalRisk / hostCount else 0.0
    }
}
