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

package org.opendc.experiments.base.experiment

import org.opendc.experiments.base.experiment.specs.ExperimentSpec
import java.io.File

private val experimentReader = ExperimentReader()
private val experimentWriter = ExperimentWriter()

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
    return getScenarios(experimentReader.read(file))
}

/**
 * Returns a list of Scenarios from a given ScenarioSpec by generating all possible combinations of
 * workloads, allocation policies, failure models, and export models within a topology.
 *
 * @param experimentSpec The ScenarioSpec containing the scenario specifications.
 * @return A list of Scenarios.
 */
public fun getScenarios(experimentSpec: ExperimentSpec): List<Scenario> {
    val outputFolder = experimentSpec.outputFolder + "/" + experimentSpec.name
    File(outputFolder).mkdirs()

    val trackrPath = "$outputFolder/trackr.json"
    File(trackrPath).createNewFile()

    val scenarios = experimentSpec.getCartesian()

    for (scenario in scenarios) {
        trackScenario(scenario)
    }

    return scenarios
}

/**
 * Writes a ScenarioSpec to a file.
 *
 * @param scenario The Scenario.
 */
public fun trackScenario(scenario: Scenario) {
    val trackrPath = "${scenario.outputFolder}/trackr.json"
    experimentWriter.write(
        scenario,
        File(trackrPath),
    )

    // remove the last comma
    File(trackrPath).writeText(File(trackrPath).readText().dropLast(3) + "]")
}
