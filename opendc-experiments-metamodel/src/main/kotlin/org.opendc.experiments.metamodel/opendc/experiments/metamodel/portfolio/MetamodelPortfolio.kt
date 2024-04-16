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

package org.opendc.experiments.metamodel.portfolio

import org.opendc.compute.workload.sampleByLoad
import org.opendc.compute.workload.trace
import org.opendc.experiments.base.portfolio.model.*
import java.nio.file.Files
import java.nio.file.Paths

fun readCsvIntoArray(fileName: String): List<Array<String>> {
    return Files.readAllLines(Paths.get(fileName)).map { it.split(",").toTypedArray() }
}

/**
 * A [Portfolio] that explores the difference between horizontal and vertical scaling.
 */
public class MetamodelPortfolio : Portfolio {
    val inputFile = readCsvIntoArray(fileName = "input/configuration-input-new.csv")
    private val topologies = listOf(
        Topology("multi")
    )

    private val allocationPolicies = listOf(
        "mem", "mem-inv", "core-mem", "core-mem-inv", "active-servers", "active-servers-inv", "random"
    )

    // sub-sub model here
    override val scenarios: Iterable<Scenario> = parseInput(inputFile)

    val metrics: List<String> = getMetricsToAnalyze()

    /**
     * Parses input from configuration-input.csv, which configures the scenario in a non-code manner.
     * lateTODO: add proper error handling
     */
    private fun parseInput(input: List<Array<String>>): Iterable<Scenario> {
        val parsedScenarios: MutableList<Scenario> = mutableListOf()
        val energyModels: List<String> = listOf("sqrt", "linear", "square", "cubic")

        for (i in 0 until energyModels.size) {
            parseScenario(input[1], energyModels.get(i))?.let { parsedScenarios.add(it) }
        }

        return parsedScenarios // casting to iterable<scenario> is automatic
    }

    private fun parseScenario(input: Array<String>, energyModel: String): Scenario? {
        return try {
            var index = 0
            val topology = input[0]; index += 1;
            val workload = Workload(input[index], trace("trace").sampleByLoad(1.0)); index += 1;
            val scheduler = input[index]; index += 1;
            val failureFrequency: Double = input[index].toDouble(); index += 1;
            val metricsCount = input[index].toInt(); index += 1;
            val operationalPhenomena = OperationalPhenomena(failureFrequency, false)

            Scenario(
                topology = Topology(topology),
                energyModel = energyModel,
                workload = workload,
                operationalPhenomena = operationalPhenomena,
                allocationPolicy = scheduler,
                mapOf("topology" to topologies[0].name, "workload" to workload.name)
            )
        } catch (e: ArrayIndexOutOfBoundsException) {
            println("Error: Input array does not have enough elements.")
            null
        } catch (e: NumberFormatException) {
            println("Error: Unable to convert input to the expected type.")
            null
        } catch (e: Exception) {
            println("Error: ${e.message}")
            null
        }
    }

    private fun getMetricsToAnalyze(): List<String> {
        var indexOfMetrics = inputFile[1].size - 2 // this is the second to last column, which contains the last metric
        var metrics: List<String> = listOf()
        var readValue = ""
        var foundNumber = false // when we find the index indicating how many metrics, we'll stop

        while (!foundNumber) {
            readValue = inputFile[1][indexOfMetrics]
            try {
                readValue.toInt()
                foundNumber = true
            } catch (e: NumberFormatException) {
                metrics = metrics.plus(readValue)
                indexOfMetrics -= 1
            }
        }

        return metrics.reversed()
    }

    // a model in OpenDC is composed of multiple of these models
    // model = scenario + workload + topology + operationalPhenomena + allocationPolicy + energyModel
    // each of them is a model that lead to a big hierarchical model

    // todo: find the "big" model, that merges all of these models together
    // 1. Identify which would be these OpenDC models (the big models) and identify / explain what it is comprised of (the sub-models)
    // 2. During 1 - look at topologies (keep the scenarios and workload as "given data"), keep the operationalPhenomena and allocationPolicy as
    // experimental data (which might set with different values from experiment to experiment)

    // Topology
    // Level 1 hierarchy: datacenter = 1 up to n multiple clusters //todo check if there is any networking handled
    // Level 2: cluster is 1 up to n homogenous servers
    // Level 3: a server is 1 up to n components (I can add a model here)
    // Level 4: components are 1 up to n CPUs, or GPUs, or... //todo check which are currently supported
    // clusters of homogeonous resources and we support multiple clusters in the same dataset

    // Energy model - the focus of the work
    // Also a hierarchy
    // Level 4: a simple piece-wise model resource-by-resource where for each resource we can use a workload/load
    // dependent energy model one of ... (linear, quadratic, square...) (I can add a model here)
    // conceptual improval: look at the family of energy models, and decide if you want to extend the family (e.g., make new energy models)

    // In level 3 & 2 & 1: (focus on level 3): look for Andrew Chien & Ricardo Bianchini 's energy uses in datacenters (survey their work
    // and identify possible things / options that can be integrated in openDC) - enumerate those options, and synthesise later with @Alexandru

    // Energy metamodel
    // combines multiple energy models, at each step in time, predicts the energy use, but not for a specific situation
    // rather as a summary (for a range)
    // ultimately, multiple metamodels, e.g., one computing quartiles, one means, etc. and make statistics with these

    // Topology
    // Level 1 hierarchy: will describe this (understand better and provide a background), but not change it


    /**
     * Meeting with Dante notes
     *
     * [1] Start by changing the allocation policy
     * [2] Then run the model with different allocation policies, but on the same experiment setup and configuration
     *
     * HOW TO
     * [1] In the output folder we have experiment predictions
     *  - host (in e folder, in data.parquet, we have all the prediction data)
     *  - server (for each server, what is the current status - down? up? running?)
     *  - service (this is the overview - how many servers are running, how many are idle, etc.)
     *
     *  [2] Analyze the CPU usage (in host folder, in topology=single, in data.parquet, we have the CPU usage)
     *  - at each time stamp, how much CPU is used?
     *  - e.g., we have n hosts, each host has x CPUs => the CPU usage is the aggregation of all the CPUs (in data.parquet is a
     *  summation of all the CPUs - aka how much is used from the whole capacity)
     *
     *  [3] Make a Bash script that runs the OpenDC first, then runs the Python file, then get the results
     */
}
