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
import org.opendc.compute.topology.clusterTopology
import java.io.File

private val scenarioReader = ScenarioReader()

public fun getScenario(filePath: String): Scenario {
    return getScenario(File(filePath))
}

public fun getScenario(file: File): Scenario {
    return getScenario(scenarioReader.read(file))
}

public fun getScenario(scenarioSpec: ScenarioSpec): Scenario {
    val topology = clusterTopology(File(scenarioSpec.topology.pathToFile))
    val workload = scenarioSpec.workload
    val allocationPolicy = scenarioSpec.allocationPolicy
    val failureModel = getFailureModel(scenarioSpec.failureModel.failureInterval)
    val exportModel = scenarioSpec.exportModel
    val energyModels = scenarioSpec.powerModelSpec

    return Scenario(
        topology,
        workload,
        allocationPolicy,
        energyModels,
        failureModel,
        scenarioSpec.carbonTracePath,
        exportModel,
        scenarioSpec.outputFolder,
        scenarioSpec.name,
        scenarioSpec.runs,
        scenarioSpec.initialSeed,
    )
}
