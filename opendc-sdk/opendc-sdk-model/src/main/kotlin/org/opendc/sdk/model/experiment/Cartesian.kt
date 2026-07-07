/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.model.experiment

/**
 * Expands this experiment into its [Scenario]s by taking the mixed-radix cartesian product across every axis.
 *
 * Axes vary from least significant to most significant in the order
 * maxNumFailures, checkpointModels, failureModels, exportModels, allocationPolicies, workloads, topologies.
 * Each scenario receives its flattened index as both [Scenario.id] and [Scenario.name].
 */
public fun Experiment.expand(): List<Scenario> {
    val topologyList = topologies.toList()
    val workloadList = workloads.toList()
    val allocationList = allocationPolicies.toList()
    val exportList = exportModels.toList()
    val failureList = failureModels.toList()
    val checkpointList = checkpointModels.toList()
    val maxFailureList = maxNumFailures.toList()

    val total =
        topologyList.size * workloadList.size * allocationList.size *
            exportList.size * failureList.size * checkpointList.size * maxFailureList.size

    return (0 until total).map { i ->
        var rem = i
        val maxFailures = maxFailureList[rem % maxFailureList.size].also { rem /= maxFailureList.size }
        val checkpoint = checkpointList[rem % checkpointList.size].also { rem /= checkpointList.size }
        val failure = failureList[rem % failureList.size].also { rem /= failureList.size }
        val export = exportList[rem % exportList.size].also { rem /= exportList.size }
        val allocation = allocationList[rem % allocationList.size].also { rem /= allocationList.size }
        val workload = workloadList[rem % workloadList.size].also { rem /= workloadList.size }
        val topology = topologyList[rem % topologyList.size]
        Scenario(
            topology = topology,
            workload = workload,
            allocationPolicy = allocation,
            exportModel = export,
            failureModel = failure,
            checkpointModel = checkpoint,
            maxNumFailures = maxFailures,
            runs = runs,
            initialSeed = initialSeed,
            id = i,
            name = i.toString(),
        )
    }
}
