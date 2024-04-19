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

package org.opendc.experiments.scenario

import org.opendc.compute.service.scheduler.ComputeSchedulerEnum
import org.opendc.experiments.base.models.portfolio.Portfolio
import org.opendc.experiments.base.models.scenario.AllocationPolicySpec
import org.opendc.experiments.base.models.scenario.FailureModelSpec
import org.opendc.experiments.base.models.scenario.Scenario
import org.opendc.experiments.base.models.scenario.ScenarioSpec
import org.opendc.experiments.base.models.scenario.TopologySpec
import org.opendc.experiments.base.models.scenario.WorkloadSpec
import org.opendc.experiments.base.models.scenario.WorkloadTypes
import org.opendc.experiments.base.models.scenario.getScenario

/**
 * A [Portfolio] that explores the difference between horizontal and vertical scaling.
 */
public fun getExamplePortfolio(): Portfolio {
    val topologies =
        listOf(
            TopologySpec("resources/env/single.json"),
            TopologySpec("resources/env/multi.json"),
        )

    val workloads =
        listOf(
            WorkloadSpec("resources/bitbrains-small", type = WorkloadTypes.ComputeWorkload),
        )

    val failureModel = FailureModelSpec(0.0)
    val allocationPolicy = AllocationPolicySpec(ComputeSchedulerEnum.ActiveServers)

    val scenarios: Iterable<Scenario> =
        topologies.flatMap { topology ->
            workloads.map { workload ->
                getScenario(
                    ScenarioSpec(
                        topology,
                        workload,
                        allocationPolicy,
                        failureModel,
                    ),
                )
            }
        }

    return Portfolio(scenarios)
}
