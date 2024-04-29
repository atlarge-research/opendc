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

package org.opendc.experiments.base.scenario

import AllocationPolicySpec
import ScenarioTopologySpec
import WorkloadSpec
import org.opendc.experiments.base.scenario.specs.ScenarioSpec
import java.io.File

private val scenarioReader = ScenarioReader()

/**
 * Returns a list of Scenarios from a given file path (input).
 *
 * @param filePath The path to the file containing the scenario specifications.
 * @return A list of Scenarios.
 */
public fun getScenarios(filePath: String): List<Scenario> {
    return getScenarios(File(filePath))
}

/**
 * Returns a list of Scenarios from a given file. Reads and decodes the contents of the (JSON) file.
 *
 * @param file The file containing the scenario specifications.
 * @return A list of Scenarios.
 */
public fun getScenarios(file: File): List<Scenario> {
    return getScenarios(scenarioReader.read(file))
}

/**
 * Returns a list of Scenarios from a given ScenarioSpec by generating all possible combinations of
 * workloads, allocation policies, failure models, and export models within a topology.
 *
 * @param scenarioSpec The ScenarioSpec containing the scenario specifications.
 * @return A list of Scenarios.
 */
public fun getScenarios(scenarioSpec: ScenarioSpec): List<Scenario> {
    val scenarios = mutableListOf<Scenario>()

    for (scenarioTopologySpec in scenarioSpec.topologies) {
        for (workloadSpec in scenarioSpec.workloads) {
            for (allocationPolicySpec in scenarioSpec.allocationPolicies) {
                for (failureModelSpec in scenarioSpec.failureModels) {
                    for (carbonTracePath in scenarioSpec.carbonTracePaths) {
                        for (exportModelSpec in scenarioSpec.exportModels) {
                            val scenario =
                                Scenario(
                                    topology = scenarioTopologySpec,
                                    workload = workloadSpec,
                                    allocationPolicy = allocationPolicySpec,
                                    failureModel = failureModelSpec,
                                    carbonTracePath = carbonTracePath,
                                    exportModel = exportModelSpec,
                                    outputFolder = scenarioSpec.outputFolder,
                                    name = getOutputFolderName(scenarioSpec, scenarioTopologySpec, workloadSpec, allocationPolicySpec),
                                    runs = scenarioSpec.runs,
                                    initialSeed = scenarioSpec.initialSeed,
                                )
                            scenarios.add(scenario)
                        }
                    }
                }
            }
        }
    }

    return scenarios
}

/**
 * Returns a string representing the output folder name for a given ScenarioSpec, CpuPowerModel, AllocationPolicySpec, and topology path.
 *
 * @param scenarioSpec The ScenarioSpec.
 * @param topology The specification of the topology used
 * @param workload The specification of the workload
 * @param allocationPolicy The allocation policy used
 * @return A string representing the output folder name.
 */
public fun getOutputFolderName(
    scenarioSpec: ScenarioSpec,
    topology: ScenarioTopologySpec,
    workload: WorkloadSpec,
    allocationPolicy: AllocationPolicySpec,
): String {
    return "scenario=${scenarioSpec.name}" +
        "-topology=${topology.name}" +
        "-workload=${workload.name}" +
        "-allocation=${allocationPolicy.name}"
}
