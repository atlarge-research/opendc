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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.InputStream

public class ScenarioReader {
    @OptIn(ExperimentalSerializationApi::class)
    public fun read(file: File): ScenarioSpec {
        val input = file.inputStream()
        val obj = Json.decodeFromStream<InputScenario>(input)

        val topologies = obj.topologies
        val workloads = obj.workloads
        val allocationPolicies = obj.allocationPolicies
        val powerModels = obj.powerModels
        val failureModels = obj.failureModels.orEmpty()
        val exportModels = obj.exportModels.orEmpty()
        val outputFolder = obj.outputFolder
        val initialSeed = obj.initialSeed
        val runs = obj.runs
        val name = obj.name

        return ScenarioSpec(
            topology = topologies[0],
            workload = workloads[0],
            allocationPolicy = allocationPolicies[0],
            powerModelSpec = powerModels[0],
            failureModel = failureModels[0],
            exportModel = exportModels[0],
            outputFolder = outputFolder,
            initialSeed = initialSeed,
            runs,
            name,
        )
    }

    /**
     * Read the specified [input].
     */
    @OptIn(ExperimentalSerializationApi::class)
    public fun read(input: InputStream): ScenarioSpec {
        val obj = Json.decodeFromStream<ScenarioSpec>(input)
        return obj
    }
}

@Serializable
public data class InputScenario(
    val topologies: List<TopologySpec>,
    val workloads: List<WorkloadSpec>,
    val allocationPolicies: List<AllocationPolicySpec>,
    val powerModels: List<PowerModelSpec>,
    val failureModels: List<FailureModelSpec>? = listOf(FailureModelSpec()),
    val exportModels: List<ExportSpec> = listOf(ExportSpec()),
    val outputFolder: String = "output",
    val initialSeed: Int = 0,
    val runs: Int,
    val name: String,
)
