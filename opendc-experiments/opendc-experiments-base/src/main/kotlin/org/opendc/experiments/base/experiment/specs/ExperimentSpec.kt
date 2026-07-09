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

@file:Suppress("DEPRECATION")

package org.opendc.experiments.base.experiment.specs

import kotlinx.serialization.Serializable
import org.opendc.compute.simulator.scheduler.ComputeSchedulerEnum
import org.opendc.experiments.base.experiment.Scenario
import org.opendc.experiments.base.experiment.specs.allocation.AllocationPolicySpec
import org.opendc.experiments.base.experiment.specs.allocation.InvalidAllocationPolicyException
import org.opendc.experiments.base.experiment.specs.allocation.PrefabAllocationPolicySpec
import java.util.UUID

/**
 * Specification of an experiments. The experiment defines what should be simulated and how.
 * The experiment file is the root of the simulation, and thus the most important file for your simulation.
 *
 * Every set-valued property below is one axis of a cartesian product: one scenario is run for
 * each combination of the axes, and each scenario is repeated [runs] times with successive seeds.
 *
 * @property name Human-readable name of the experiment. When left empty a unique name is
 * generated automatically. Used as a prefix for output folders.
 * @property outputFolder Directory that raw output is written to, under `<outputFolder>/raw-output/`.
 * Default output folder is "output".
 * @property initialSeed Seed used to initialize the random generators. Successive runs of a
 * scenario increment from this value. Default value is 0.
 * @property runs Number of runs (seeds) executed per scenario. Must be greater than 0. Default value is 1.
 * @property topologies Datacenter topologies to simulate, each referenced by a path to a topology JSON file.
 * @property workloads Workload traces to replay against the topologies.
 * @property allocationPolicies Scheduling policies that decide how and when to schedule tasks.
 * @property failureModels Failure models decide when failures are injected during the simulation, their intensity,
 * and duration. Failures cause hosts to crash, interrupting all running tasks, and making the hosts unavailable for a period.
 * Default no failures are injected.
 * @property maxNumFailures Number of failures after which a task is considered permanently failed. Default is 10.
 * @property checkpointModels Checkpointing behavior used to recover work after a failure. Default no checkpointing is used.
 * @property exportModels Telemetry export settings controlling which output files and columns are written. See
 * [ExportModelSpec] for the Default exportModel.
 */

@Serializable
@Deprecated("Replaced by the opendc-sdk model (org.opendc.sdk.model.*); run experiments with the new opendc CLI (opendc-cli).")
public data class ExperimentSpec(
    var name: String = "",
    val outputFolder: String = "output",
    val initialSeed: Int = 0,
    val runs: Int = 1,
    val topologies: Set<TopologyPathSpec>,
    val workloads: Set<WorkloadSpec>,
    val allocationPolicies: Set<AllocationPolicySpec> = setOf(PrefabAllocationPolicySpec(ComputeSchedulerEnum.Mem)),
    val failureModels: Set<FailureModelSpec?> = setOf(null),
    val maxNumFailures: Set<Int> = setOf(10),
    val checkpointModels: Set<CheckpointModelSpec?> = setOf(null),
    val exportModels: Set<ExportModelSpec> = setOf(ExportModelSpec()),
) {
    init {
        // generate name if not provided
        // TODO: improve this
        if (name == "") {
            name = "unnamed-simulation-${UUID.randomUUID().toString().substring(0, 4)}"
//                "workload=${workloads[0].name}_topology=${topologies[0].name}_allocationPolicy=${allocationPolicies[0].name}"
        }
    }

    /**
     * Validate the constraints of this experiment specification.
     *
     * All violated constraints are collected and reported together, so a user fixing an
     * experiment file sees every problem at once instead of one per run. When any constraint
     * is violated an [ExperimentValidationException] is thrown; otherwise this returns nothing.
     *
     * @throws ExperimentValidationException if one or more constraints are violated.
     */
    public fun validate() {
        val errors =
            buildList {
                if (runs <= 0) add("runs must be greater than 0 (currently runs=$runs)")

                if (topologies.isEmpty()) add("topologies can not be empty")
                for (topology in topologies) {
                    try {
                        topology.validate()
                    } catch (e: InvalidTopologyException) {
                        add(e.message)
                    }
                }

                if (workloads.isEmpty()) add("workloads can not be empty")
                for (workload in workloads) {
                    try {
                        workload.validate()
                    } catch (e: InvalidWorkloadException) {
                        addAll(e.errors)
                    }
                }

                if (allocationPolicies.isEmpty()) add("allocationPolicies cannot be empty")
                for (allocationPolicy in allocationPolicies) {
                    try {
                        allocationPolicy.validate()
                    } catch (e: InvalidAllocationPolicyException) {
                        addAll(e.errors)
                    }
                }

                if (exportModels.isEmpty()) add("exportModels can not be empty")
                for (exportModel in exportModels) {
                    try {
                        exportModel.validate()
                    } catch (e: InvalidExportModelException) {
                        addAll(e.errors)
                    }
                }

                for (failureModel in failureModels) {
                    try {
                        failureModel?.validate()
                    } catch (e: InvalidFailureModelException) {
                        addAll(e.errors)
                    }
                }

                for (checkpointModel in checkpointModels) {
                    try {
                        checkpointModel?.validate()
                    } catch (e: InvalidCheckpointModelException) {
                        addAll(e.errors)
                    }
                }

                if (maxNumFailures.any { it < 1 }) add("all maxNumFailures must be above 0 (currently runs=$maxNumFailures)")
            }

        if (errors.isNotEmpty()) {
            throw ExperimentValidationException(errors)
        }
    }

    public fun getCartesian(): List<Scenario> {
        return buildList {
            val outputFolder = "$outputFolder/$name"

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
                add(
                    Scenario(
                        i,
                        i.toString(),
                        outputFolder,
                        runs,
                        initialSeed,
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
}

/**
 * Exception thrown when an [ExperimentSpec] violates one or more of its constraints.
 *
 * @property errors The human-readable descriptions of every violated constraint.
 */
public class ExperimentValidationException(
    public val errors: List<String>,
) : IllegalArgumentException(
        "Invalid experiment specification:\n" + errors.joinToString("\n") { "  - $it" },
    )
