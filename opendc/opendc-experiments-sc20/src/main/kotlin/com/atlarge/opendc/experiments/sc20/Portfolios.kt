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

package com.atlarge.opendc.experiments.sc20

abstract class AbstractSc20Portfolio(name: String) : Portfolio(name) {
    abstract val topologies: List<Topology>
    abstract val workloads: List<Workload>
    abstract val operationalPhenomena: List<Pair<Boolean, Boolean>>
    abstract val allocationPolicies: List<String>

    open val repetitions = 4

    override val scenarios: Sequence<Scenario> = sequence {
        for (topology in topologies) {
            for (workload in workloads) {
                for ((hasFailures, hasInterference) in operationalPhenomena) {
                    for (allocationPolicy in allocationPolicies) {
                        yield(
                            Scenario(
                                this@AbstractSc20Portfolio,
                                repetitions,
                                topology,
                                workload,
                                allocationPolicy,
                                hasFailures,
                                hasInterference
                            )
                        )
                    }
                }
            }
        }
    }
}

object HorVerPortfolio : AbstractSc20Portfolio("horizontal_vs_vertical") {
    override val topologies = listOf(
        Topology("base"),
        Topology("rep-vol-hor-hom")
        // Topology("rep-vol-hor-het"),
        // Topology("rep-vol-ver-hom"),
        // Topology("rep-vol-ver-het"),
        // Topology("exp-vol-hor-hom"),
        // Topology("exp-vol-hor-het"),
        // Topology("exp-vol-ver-hom"),
        // Topology("exp-vol-ver-het")
    )

    override val workloads = listOf(
        // Workload("solvinity", 0.1),
        // Workload("solvinity", 0.25),
        // Workload("small-parquet", 0.5),
        Workload("small-parquet", 1.0)
    )

    override val operationalPhenomena = listOf(
        // true to true
        false to true
    )

    override val allocationPolicies = listOf(
        "active-servers"
    )
}

object MoreVelocityPortfolio : AbstractSc20Portfolio("more_velocity") {
    override val topologies = listOf(
        Topology("base"),
        Topology("rep-vel-ver-hom"),
        Topology("rep-vel-ver-het"),
        Topology("exp-vel-ver-hom"),
        Topology("exp-vel-ver-het")
    )

    override val workloads = listOf(
        // Workload("solvinity", 0.1),
        // Workload("solvinity", 0.25),
        Workload("solvinity", 0.5),
        Workload("solvinity", 1.0)
    )

    override val operationalPhenomena = listOf(
        true to true
    )

    override val allocationPolicies = listOf(
        "active-servers"
    )
}

object MoreHpcPortfolio : AbstractSc20Portfolio("more_hpc") {
    override val topologies = listOf(
        Topology("base"),
        Topology("exp-vol-hor-hom"),
        Topology("exp-vol-ver-hom"),
        Topology("exp-vel-ver-hom")
    )

    override val workloads = listOf(
        // Workload("solvinity", 0.1),
        // Workload("solvinity", 0.25),
        Workload("solvinity", 0.5),
        Workload("solvinity", 1.0)
    )

    override val operationalPhenomena = listOf(
        true to true
    )

    override val allocationPolicies = listOf(
        "active-servers"
    )
}

object OperationalPhenomenaPortfolio : AbstractSc20Portfolio("operational_phenomena") {
    override val topologies = listOf(
        Topology("base")
    )

    override val workloads = listOf(
        // Workload("solvinity", 0.1),
        // Workload("solvinity", 0.25),
        Workload("solvinity", 1.0)
    )

    override val operationalPhenomena = listOf(
        true to true,
        false to true,
        true to false,
        true to true
    )

    override val allocationPolicies = listOf(
        "mem",
        "mem-inv",
        "core-mem",
        "core-mem-inv",
        "active-servers",
        "active-servers-inv",
        "random"
    )
}
