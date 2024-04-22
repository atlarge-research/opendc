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

package org.opendc.experiments.base.models.scenario.specs

import AllocationPolicySpec
import ExportModelSpec
import FailureModelSpec
import TopologySpec
import WorkloadSpec
import kotlinx.serialization.Serializable

/**
 * specification describing a scenario
 *
 * @property topologies
 * @property workloads
 * @property allocationPolicies
 * @property failureModels
 * @property exportModels
 * @property outputFolder
 * @property initialSeed
 * @property runs
 */
@Serializable
public data class ScenarioSpec(
    val topologies: List<TopologySpec>,
    val workloads: List<WorkloadSpec>,
    val allocationPolicies: List<AllocationPolicySpec>,
    val failureModels: List<FailureModelSpec> = listOf(FailureModelSpec()),
    val carbonTracePaths: List<String?> = listOf(null),
    val exportModels: List<ExportModelSpec> = listOf(ExportModelSpec()),
    val outputFolder: String = "output",
    val initialSeed: Int = 0,
    val runs: Int = 1,
    var name: String = "",
) {
    init {
        require(runs > 0) { "The number of runs should always be positive" }

        // generate name if not provided
        // TODO: improve this
        if (name == "") {
            name =
                "workload=${workloads[0].name}_topology=${topologies[0].name}_allocationPolicy=${allocationPolicies[0].name}"
        }
    }
}
