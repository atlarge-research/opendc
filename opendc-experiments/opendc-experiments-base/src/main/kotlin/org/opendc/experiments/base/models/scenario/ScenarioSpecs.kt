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

import kotlinx.serialization.Serializable
import org.opendc.compute.service.scheduler.ComputeSchedulerEnum
import org.opendc.compute.workload.ComputeWorkload
import org.opendc.compute.workload.sampleByLoad
import org.opendc.compute.workload.trace
import java.io.File

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
    val exportModels: List<ExportSpec> = listOf(ExportSpec()),
    val outputFolder: String = "output",
    val initialSeed: Int = 0,
    val runs: Int = 1,
    var name: String = "",
) {
    init {
        require(runs > 0) { "The number of runs should always be positive" }

        // generate name if not provided
        if (name == "") {
            name = "workload=${workloads[0].name}_topology=${topologies[0].name}_allocationPolicy=${allocationPolicies[0].name}"
        }
    }
}

/**
 * specification describing a topology
 *
 * @property pathToFile
 */
@Serializable
public data class TopologySpec(
    val pathToFile: String,
) {
    public val name: String = File(pathToFile).nameWithoutExtension

    init {
        require(File(pathToFile).exists()) { "The provided path to the topology: $pathToFile does not exist " }
    }
}

/**
 * specification describing a workload
 *
 * @property pathToFile
 * @property type
 */
@Serializable
public data class WorkloadSpec(
    val pathToFile: String,
    val type: WorkloadTypes,
) {
    public val name: String = File(pathToFile).nameWithoutExtension

    init {
        require(File(pathToFile).exists()) { "The provided path to the workload: $pathToFile does not exist " }
    }
}

/**
 * specification describing a workload type
 *
 * @constructor Create empty Workload types
 */
public enum class WorkloadTypes {
    /**
     * Compute workload
     *
     * @constructor Create empty Compute workload
     */
    ComputeWorkload,
}

/**
 *
 *TODO: move to separate file
 * @param type
 */
public fun getWorkloadType(type: WorkloadTypes): ComputeWorkload {
    return when (type) {
        WorkloadTypes.ComputeWorkload -> trace("trace").sampleByLoad(1.0)
    }
}

/**
 * specification describing how tasks are allocated
 *
 * @property policyType
 *
 * TODO: expand with more variables such as allowed over-subscription
 */
@Serializable
public data class AllocationPolicySpec(
    val policyType: ComputeSchedulerEnum,
) {
    public val name: String = policyType.toString()
}

@Serializable
public data class PowerModelSpec(
    val type: String = "constant",
    val idlePower: Double = 200.0,
    val maxPower: Double = 350.0,
)


/**
 * specification describing the failure model
 *
 * @property failureInterval The interval between failures in s. Should be 0.0 or higher
 */
@Serializable
public data class FailureModelSpec(
    val failureInterval: Double = 0.0,
) {
    init {
        require(failureInterval >= 0.0) { "failure frequency cannot be lower than 0" }
    }
}

/**
 * specification describing how the results should be exported
 *
 * @property exportInterval The interval of exporting results in s. Should be higher than 0.0
 */
@Serializable
public data class ExportSpec(
    val exportInterval: Long = 5 * 60,
) {
    init {
        require(exportInterval > 0) { "The Export interval has to be higher than 0" }
    }
}
