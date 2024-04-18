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

import org.opendc.compute.simulator.failure.getFailureModel
import org.opendc.compute.topology.TopologyReader
import org.opendc.compute.topology.clusterTopology
import org.opendc.compute.topology.specs.TopologyJSONSpec
import org.opendc.simulator.compute.power.CpuPowerModel
import org.opendc.simulator.compute.power.getPowerModel
import java.io.File
import java.util.UUID

private val scenarioReader = ScenarioReader()

public fun getScenario(filePath: String): List<Scenario> {
    return getScenario(File(filePath))
}

public fun getScenario(file: File): List<Scenario> {
    return getScenario(scenarioReader.read(file))
}

public fun getScenario(scenarioSpec: ScenarioSpec): List<Scenario> {
    return getScenarioCombinations(scenarioSpec)
}

public fun getScenarioCombinations(scenarioSpec: ScenarioSpec): List<Scenario> {
    val topologies = getTopologies(scenarioSpec.topologies)
    val topologiess = scenarioSpec.topologies
    val workloads = scenarioSpec.workloads
    val allocationPolicies = scenarioSpec.allocationPolicies
    val failureModels = scenarioSpec.failureModels
    val exportModels = scenarioSpec.exportModels
    val scenarios = mutableListOf<Scenario>()

    for (topology in topologies) {
        var i = 0
        for (workload in workloads) {
            for (allocationPolicy in allocationPolicies) {
                for (failureModel in failureModels) {
                    for (exportModel in exportModels) {
                        for (powerModel in getPowerModelsFromTopology(topology)) {
                            val scenario = Scenario(
                                topology = clusterTopology(
                                    File(topologiess[i].pathToFile),
                                    powerModel,
                                ),
                                workload = workload,
                                allocationPolicy = allocationPolicy,
                                failureModel = getFailureModel(failureModel.failureInterval),
                                exportModel = exportModel,
                                outputFolder = scenarioSpec.outputFolder,
                                name = "scenario-${scenarioSpec.name}-model-powerModelType-scheduler-${allocationPolicy.policyType}-topology-${
                                    topologiess[i].pathToFile.replace(
                                        "/",
                                        "-"
                                    )
                                }-${UUID.randomUUID().toString().substring(0, 8)}",
                                runs = scenarioSpec.runs,
                                initialSeed = scenarioSpec.initialSeed,
                            )
                            scenarios.add(scenario)
                            i++
                        }
                    }
                }
            }
        }
    }
    return scenarios
}

public fun getTopologies(topologies: List<TopologySpec>): List<TopologyJSONSpec> {
    val readTopologies = mutableListOf<TopologyJSONSpec>()
    for (topology in topologies) {
        readTopologies.add(TopologyReader().read(File(topology.pathToFile)))
    }

    return readTopologies
}

public fun getPowerModelsFromTopology(topology: TopologyJSONSpec): List<CpuPowerModel> {
    val powerModels = mutableListOf<CpuPowerModel>()
    for (cluster in topology.clusters) {
        for (host in cluster.hosts) {
            for (model in host.powerModel) {
                powerModels.add(
                    getPowerModel(
                        model.modelType,
                        model.power,
                        model.maxPower,
                        model.idlePower
                    )
                )
            }
        }
    }
    return powerModels
}
