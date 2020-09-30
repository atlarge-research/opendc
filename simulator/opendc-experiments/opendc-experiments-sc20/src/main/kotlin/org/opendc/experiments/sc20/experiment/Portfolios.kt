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

package org.opendc.experiments.sc20.experiment

import org.opendc.experiments.sc20.experiment.model.*

public class HorVerPortfolio(parent: Experiment, id: Int) : Portfolio(parent, id, "horizontal_vs_vertical") {
    override val topologies = listOf(
        Topology("base"),
        Topology("rep-vol-hor-hom"),
        Topology("rep-vol-hor-het"),
        Topology("rep-vol-ver-hom"),
        Topology("rep-vol-ver-het"),
        Topology("exp-vol-hor-hom"),
        Topology("exp-vol-hor-het"),
        Topology("exp-vol-ver-hom"),
        Topology("exp-vol-ver-het")
    )

    override val workloads = listOf(
        Workload("solvinity", 0.1),
        Workload("solvinity", 0.25),
        Workload("solvinity", 0.5),
        Workload("solvinity", 1.0)
    )

    override val operationalPhenomenas = listOf(
        OperationalPhenomena(failureFrequency = 24.0 * 7, hasInterference = true)
    )

    override val allocationPolicies = listOf(
        "active-servers"
    )
}

public class MoreVelocityPortfolio(parent: Experiment, id: Int) : Portfolio(parent, id, "more_velocity") {
    override val topologies = listOf(
        Topology("base"),
        Topology("rep-vel-ver-hom"),
        Topology("rep-vel-ver-het"),
        Topology("exp-vel-ver-hom"),
        Topology("exp-vel-ver-het")
    )

    override val workloads = listOf(
        Workload("solvinity", 0.1),
        Workload("solvinity", 0.25),
        Workload("solvinity", 0.5),
        Workload("solvinity", 1.0)
    )

    override val operationalPhenomenas = listOf(
        OperationalPhenomena(failureFrequency = 24.0 * 7, hasInterference = true)
    )

    override val allocationPolicies = listOf(
        "active-servers"
    )
}

public class CompositeWorkloadPortfolio(parent: Experiment, id: Int) : Portfolio(parent, id, "composite-workload") {
    private val totalSampleLoad = 1.3301733005049648E12

    override val topologies = listOf(
        Topology("base"),
        Topology("exp-vol-hor-hom"),
        Topology("exp-vol-ver-hom"),
        Topology("exp-vel-ver-hom")
    )

    override val workloads = listOf(
        CompositeWorkload(
            "all-azure",
            listOf(Workload("solvinity-short", 0.0), Workload("azure", 1.0)),
            totalSampleLoad
        ),
        CompositeWorkload(
            "solvinity-25-azure-75",
            listOf(Workload("solvinity-short", 0.25), Workload("azure", 0.75)),
            totalSampleLoad
        ),
        CompositeWorkload(
            "solvinity-50-azure-50",
            listOf(Workload("solvinity-short", 0.5), Workload("azure", 0.5)),
            totalSampleLoad
        ),
        CompositeWorkload(
            "solvinity-75-azure-25",
            listOf(Workload("solvinity-short", 0.75), Workload("azure", 0.25)),
            totalSampleLoad
        ),
        CompositeWorkload(
            "all-solvinity",
            listOf(Workload("solvinity-short", 1.0), Workload("azure", 0.0)),
            totalSampleLoad
        )
    )

    override val operationalPhenomenas = listOf(
        OperationalPhenomena(failureFrequency = 24.0 * 7, hasInterference = false)
    )

    override val allocationPolicies = listOf(
        "active-servers"
    )
}

public class OperationalPhenomenaPortfolio(parent: Experiment, id: Int) :
    Portfolio(parent, id, "operational_phenomena") {
    override val topologies = listOf(
        Topology("base")
    )

    override val workloads = listOf(
        Workload("solvinity", 0.1),
        Workload("solvinity", 0.25),
        Workload("solvinity", 0.5),
        Workload("solvinity", 1.0)
    )

    override val operationalPhenomenas = listOf(
        OperationalPhenomena(failureFrequency = 24.0 * 7, hasInterference = true),
        OperationalPhenomena(failureFrequency = 0.0, hasInterference = true),
        OperationalPhenomena(failureFrequency = 24.0 * 7, hasInterference = false),
        OperationalPhenomena(failureFrequency = 0.0, hasInterference = false)
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

public class ReplayPortfolio(parent: Experiment, id: Int) : Portfolio(parent, id, "replay") {
    override val topologies = listOf(
        Topology("base")
    )

    override val workloads = listOf(
        Workload("solvinity", 1.0)
    )

    override val operationalPhenomenas = listOf(
        OperationalPhenomena(failureFrequency = 0.0, hasInterference = false)
    )

    override val allocationPolicies = listOf(
        "replay",
        "active-servers"
    )
}

public class TestPortfolio(parent: Experiment, id: Int) : Portfolio(parent, id, "test") {
    override val repetitions: Int = 1

    override val topologies: List<Topology> = listOf(
        Topology("base")
    )

    override val workloads: List<Workload> = listOf(
        Workload("solvinity", 1.0)
    )

    override val operationalPhenomenas: List<OperationalPhenomena> = listOf(
        OperationalPhenomena(failureFrequency = 24.0 * 7, hasInterference = true)
    )

    override val allocationPolicies: List<String> = listOf("active-servers")
}

public class MoreHpcPortfolio(parent: Experiment, id: Int) : Portfolio(parent, id, "more_hpc") {
    override val topologies = listOf(
        Topology("base"),
        Topology("exp-vol-hor-hom"),
        Topology("exp-vol-ver-hom"),
        Topology("exp-vel-ver-hom")
    )

    override val workloads = listOf(
        Workload("solvinity", 0.0, samplingStrategy = SamplingStrategy.HPC),
        Workload("solvinity", 0.25, samplingStrategy = SamplingStrategy.HPC),
        Workload("solvinity", 0.5, samplingStrategy = SamplingStrategy.HPC),
        Workload("solvinity", 1.0, samplingStrategy = SamplingStrategy.HPC),
        Workload("solvinity", 0.25, samplingStrategy = SamplingStrategy.HPC_LOAD),
        Workload("solvinity", 0.5, samplingStrategy = SamplingStrategy.HPC_LOAD),
        Workload("solvinity", 1.0, samplingStrategy = SamplingStrategy.HPC_LOAD)
    )

    override val operationalPhenomenas = listOf(
        OperationalPhenomena(failureFrequency = 24.0 * 7, hasInterference = true)
    )

    override val allocationPolicies = listOf(
        "active-servers"
    )
}
