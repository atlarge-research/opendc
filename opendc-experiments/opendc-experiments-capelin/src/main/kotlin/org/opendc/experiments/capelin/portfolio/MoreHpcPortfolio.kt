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
import org.opendc.experiments.compute.sampleByHpc
import org.opendc.experiments.compute.sampleByHpcLoad
import org.opendc.experiments.compute.trace

/**
 * A [Portfolio] to explore the effect of HPC workloads.
 */
public class MoreHpcPortfolio : Portfolio {
    private val topologies = listOf(
        Topology("base"),
        Topology("exp-vol-hor-hom"),
        Topology("exp-vol-ver-hom"),
        Topology("exp-vel-ver-hom")
    )
    private val workloads = listOf(
        Workload("hpc-0%", trace("solvinity").sampleByHpc(0.0)),
        Workload("hpc-25%", trace("solvinity").sampleByHpc(0.25)),
        Workload("hpc-50%", trace("solvinity").sampleByHpc(0.5)),
        Workload("hpc-100%", trace("solvinity").sampleByHpc(1.0)),
        Workload("hpc-load-25%", trace("solvinity").sampleByHpcLoad(0.25)),
        Workload("hpc-load-50%", trace("solvinity").sampleByHpcLoad(0.5)),
        Workload("hpc-load-100%", trace("solvinity").sampleByHpcLoad(1.0))
    )

    private val operationalPhenomena = OperationalPhenomena(failureFrequency = 24.0 * 7, hasInterference = true)
    private val allocationPolicy: String = "active-servers"

    override val scenarios: Iterable<Scenario> = topologies.flatMap { topology ->
        workloads.map { workload ->
            Scenario(
                topology,
                workload,
                operationalPhenomena,
                allocationPolicy,
                mapOf("topology" to topology.name, "workload" to workload.name)
            )
        }
    }
}
