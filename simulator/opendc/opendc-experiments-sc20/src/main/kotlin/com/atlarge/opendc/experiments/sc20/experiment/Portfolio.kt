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

package com.atlarge.opendc.experiments.sc20.experiment

import com.atlarge.opendc.experiments.sc20.experiment.model.OperationalPhenomena
import com.atlarge.opendc.experiments.sc20.experiment.model.Topology
import com.atlarge.opendc.experiments.sc20.experiment.model.Workload
import com.atlarge.opendc.experiments.sc20.runner.ContainerExperimentDescriptor

/**
 * A portfolio represents a collection of scenarios are tested.
 */
public abstract class Portfolio(
    override val parent: Experiment,
    val id: Int,
    val name: String
) : ContainerExperimentDescriptor() {
    /**
     * The topologies to consider.
     */
    protected abstract val topologies: List<Topology>

    /**
     * The workloads to consider.
     */
    protected abstract val workloads: List<Workload>

    /**
     * The operational phenomenas to consider.
     */
    protected abstract val operationalPhenomenas: List<OperationalPhenomena>

    /**
     * The allocation policies to consider.
     */
    protected abstract val allocationPolicies: List<String>

    /**
     * The number of repetitions to perform.
     */
    open val repetitions: Int = 32

    /**
     * Resolve the children of this container.
     */
    override val children: Sequence<Scenario> = sequence {
        var id = 0
        for (topology in topologies) {
            for (workload in workloads) {
                for (operationalPhenomena in operationalPhenomenas) {
                    for (allocationPolicy in allocationPolicies) {
                        yield(
                            Scenario(
                                this@Portfolio,
                                id++,
                                repetitions,
                                topology,
                                workload,
                                allocationPolicy,
                                operationalPhenomena
                            )
                        )
                    }
                }
            }
        }
    }
}
