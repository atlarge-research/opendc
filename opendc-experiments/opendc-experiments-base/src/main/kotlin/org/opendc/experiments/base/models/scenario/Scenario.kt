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

package org.opendc.experiments.base.models.scenario

import AllocationPolicySpec
import ExportModelSpec
import WorkloadSpec
import org.opendc.compute.simulator.failure.FailureModel
import org.opendc.compute.topology.specs.HostSpec

/**
 * A data class representing a scenario for a set of experiments.
 *
 * @property topology The list of HostSpec representing the topology of the scenario.
 * @property workload The WorkloadSpec representing the workload of the scenario.
 * @property allocationPolicy The AllocationPolicySpec representing the allocation policy of the scenario.
 * @property failureModel The FailureModel representing the failure model of the scenario. It can be null.
 * @property exportModel The ExportSpec representing the export model of the scenario. It defaults to an instance of ExportSpec.
 * @property outputFolder The String representing the output folder of the scenario. It defaults to "output".
 * @property name The String representing the name of the scenario. It defaults to an empty string.
 * @property runs The Int representing the number of runs of the scenario. It defaults to 1.
 * @property initialSeed The Int representing the initial seed of the scenario. It defaults to 0.
 */
public data class Scenario(
    val topology: List<HostSpec>,
    val workload: WorkloadSpec,
    val allocationPolicy: AllocationPolicySpec,
    val failureModel: FailureModel?,
    val carbonTracePath: String? = null,
    val exportModel: ExportModelSpec = ExportModelSpec(),
    val outputFolder: String = "output",
    val name: String = "",
    val runs: Int = 1,
    val initialSeed: Int = 0,
)
