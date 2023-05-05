/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.experiments.capelin.portfolio

import org.opendc.experiments.capelin.model.OperationalPhenomena
import org.opendc.experiments.capelin.model.Scenario
import org.opendc.experiments.capelin.model.Topology
import org.opendc.experiments.capelin.model.Workload
import org.opendc.experiments.compute.sampleByLoad
import org.opendc.experiments.compute.trace

/**
 * A [Portfolio] that explores the effect of operational phenomena on metrics.
 */
public class OperationalPhenomenaPortfolio : Portfolio {
    private val topology = Topology("base")
    private val workloads = listOf(
        Workload("solvinity-10%", trace("solvinity").sampleByLoad(0.1)),
        Workload("solvinity-25%", trace("solvinity").sampleByLoad(0.25)),
        Workload("solvinity-50%", trace("solvinity").sampleByLoad(0.5)),
        Workload("solvinity-100%", trace("solvinity").sampleByLoad(1.0))
    )

    private val phenomenas = listOf(
        OperationalPhenomena(failureFrequency = 24.0 * 7, hasInterference = true),
        OperationalPhenomena(failureFrequency = 0.0, hasInterference = true),
        OperationalPhenomena(failureFrequency = 24.0 * 7, hasInterference = false),
        OperationalPhenomena(failureFrequency = 0.0, hasInterference = false)
    )

    private val allocationPolicies = listOf(
        "mem",
        "mem-inv",
        "core-mem",
        "core-mem-inv",
        "active-servers",
        "active-servers-inv",
        "random"
    )

    override val scenarios: Iterable<Scenario> =
        workloads.flatMap { workload ->
            phenomenas.flatMapIndexed { index, operationalPhenomena ->
                allocationPolicies.map { allocationPolicy ->
                    Scenario(
                        topology,
                        workload,
                        operationalPhenomena,
                        allocationPolicy,
                        mapOf("workload" to workload.name, "scheduler" to allocationPolicy, "phenomena" to index.toString())
                    )
                }
            }
        }
}
