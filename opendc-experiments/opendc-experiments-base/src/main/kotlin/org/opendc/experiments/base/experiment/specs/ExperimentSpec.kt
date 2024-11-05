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

package org.opendc.experiments.base.experiment.specs

import kotlinx.serialization.Serializable
import org.opendc.common.logger.infoNewLine
import org.opendc.common.logger.logger
import org.opendc.compute.simulator.telemetry.parquet.ComputeExportConfig
import java.util.UUID

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
 * @property computeExportConfig configures which parquet columns are to
 * be included in the output files.
 */

@Serializable
public data class ExperimentSpec(
    var id: Int = -1,
    var name: String = "",
    val outputFolder: String = "output",
    val initialSeed: Int = 0,
    val runs: Int = 1,
    val exportModels: Set<ExportModelSpec> = setOf(ExportModelSpec()),
    val computeExportConfig: ComputeExportConfig = ComputeExportConfig.ALL_COLUMNS,
    val maxNumFailures: Set<Int> = setOf(10),
    val topologies: Set<ScenarioTopologySpec>,
    val workloads: Set<WorkloadSpec>,
    val allocationPolicies: Set<AllocationPolicySpec> = setOf(AllocationPolicySpec()),
    val failureModels: Set<FailureModelSpec?> = setOf(null),
    val checkpointModels: Set<CheckpointModelSpec?> = setOf(null),
) {
    init {
        require(runs > 0) { "The number of runs should always be positive" }

        // generate name if not provided
        // TODO: improve this
        if (name == "") {
            name = "unnamed-simulation-${UUID.randomUUID().toString().substring(0, 4)}"
//                "workload=${workloads[0].name}_topology=${topologies[0].name}_allocationPolicy=${allocationPolicies[0].name}"
        }

        LOG.infoNewLine(computeExportConfig.fmt())
    }

    public fun getCartesian(): Sequence<ScenarioSpec> {
        return sequence {
            val checkpointDiv = maxNumFailures.size
            val failureDiv = checkpointDiv * checkpointModels.size
            val exportDiv = failureDiv * failureModels.size
            val allocationDiv = exportDiv * exportModels.size
            val workloadDiv = allocationDiv * allocationPolicies.size
            val topologyDiv = workloadDiv * workloads.size
            val numScenarios = topologyDiv * topologies.size

            val topologyList = topologies.toList()
            val workloadList = workloads.toList()
            val allocationPolicyList = allocationPolicies.toList()
            val exportModelList = exportModels.toList()
            val failureModelList = failureModels.toList()
            val checkpointModelList = checkpointModels.toList()
            val maxNumFailuresList = maxNumFailures.toList()

            for (i in 0 until numScenarios) {
                yield(
                    ScenarioSpec(
                        id,
                        name,
                        outputFolder,
                        computeExportConfig = computeExportConfig,
                        topologyList[(i / topologyDiv) % topologyList.size],
                        workloadList[(i / workloadDiv) % workloadList.size],
                        allocationPolicyList[(i / allocationDiv) % allocationPolicyList.size],
                        exportModelList[(i / exportDiv) % exportModelList.size],
                        failureModelList[(i / failureDiv) % failureModelList.size],
                        checkpointModelList[(i / checkpointDiv) % checkpointModelList.size],
                        maxNumFailuresList[i % maxNumFailuresList.size],
                    ),
                )
            }
        }
    }

    internal companion object {
        private val LOG by logger()
    }
}
