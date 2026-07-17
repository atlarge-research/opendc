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

package org.opendc.sdk.model.dsl

import org.opendc.sdk.model.checkpoint.CheckpointSpec
import org.opendc.sdk.model.experiment.ExperimentSpec
import org.opendc.sdk.model.experiment.ScenarioSpec
import org.opendc.sdk.model.export.ExportSpec
import org.opendc.sdk.model.failure.FailureModelSpec
import org.opendc.sdk.model.failure.NoFailureSpec
import org.opendc.sdk.model.scheduler.AllocationPolicySpec
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicySpec
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.workload.WorkloadSpec

/**
 * Builds an [ExperimentSpec] whose axes expand into the scenarios to run.
 *
 * @param block Configures the experiment through an [ExperimentBuilder].
 */
public fun experiment(block: ExperimentBuilder.() -> Unit): ExperimentSpec = ExperimentBuilder().apply(block).build()

/**
 * Builds a single, fully-resolved [ScenarioSpec].
 *
 * @param block Configures the scenario through a [ScenarioBuilder].
 */
public fun scenario(block: ScenarioBuilder.() -> Unit): ScenarioSpec = ScenarioBuilder().apply(block).build()

/** Collects the candidate values of each experiment axis. */
@SdkDsl
public class ExperimentBuilder {
    private val topologies = mutableSetOf<TopologySpec>()
    private val workloads = mutableSetOf<WorkloadSpec>()
    private val allocationPolicies = mutableSetOf<AllocationPolicySpec>()
    private val failureModels = mutableSetOf<FailureModelSpec>()
    private val exportModels = mutableSetOf<ExportSpec>()
    private val checkpointModels = mutableSetOf<CheckpointSpec?>()
    private val maxNumFailures = mutableSetOf<Int>()

    /** The number of independent repetitions per scenario. */
    public var runs: Int = 1

    /** The seed used to derive per-run random seeds. */
    public var initialSeed: Int = 0

    /** A human-readable name for the experiment. */
    public var name: String = ""

    public fun topology(topology: TopologySpec) {
        topologies += topology
    }

    public fun topology(block: TopologyBuilder.() -> Unit) {
        topologies += TopologyBuilder().apply(block).build()
    }

    public fun workload(workload: WorkloadSpec) {
        workloads += workload
    }

    public fun workload(block: InlineWorkloadBuilder.() -> Unit) {
        workloads += InlineWorkloadBuilder().apply(block).build()
    }

    public fun allocationPolicy(policy: AllocationPolicySpec) {
        allocationPolicies += policy
    }

    public fun failureModel(model: FailureModelSpec) {
        failureModels += model
    }

    public fun exportModel(model: ExportSpec) {
        exportModels += model
    }

    public fun checkpointModel(model: CheckpointSpec?) {
        checkpointModels += model
    }

    public fun maxNumFailures(value: Int) {
        maxNumFailures += value
    }

    internal fun build(): ExperimentSpec =
        ExperimentSpec(
            topologies = topologies.toSet(),
            workloads = workloads.toSet(),
            allocationPolicies = allocationPolicies.ifEmpty { setOf(PrefabAllocationPolicySpec()) }.toSet(),
            failureModels = failureModels.ifEmpty { setOf(NoFailureSpec) }.toSet(),
            maxNumFailures = maxNumFailures.ifEmpty { setOf(10) }.toSet(),
            checkpointModels = checkpointModels.ifEmpty { setOf(null) }.toSet(),
            exportModels = exportModels.ifEmpty { setOf(ExportSpec()) }.toSet(),
            runs = runs,
            initialSeed = initialSeed,
            name = name,
        )
}

/** Collects the components of a single [ScenarioSpec]. */
@SdkDsl
public class ScenarioBuilder {
    private var topology: TopologySpec? = null
    private var workload: WorkloadSpec? = null
    private var allocationPolicy: AllocationPolicySpec = PrefabAllocationPolicySpec()

    /** Controls which results are written and how often. */
    public var exportModel: ExportSpec = ExportSpec()

    /** The failure-injection model applied during the run. */
    public var failureModel: FailureModelSpec = NoFailureSpec

    /** Optional checkpointing configuration for tasks. */
    public var checkpointModel: CheckpointSpec? = null

    /** The maximum number of failures a task may survive before it is terminated. */
    public var maxNumFailures: Int = 10

    /** The number of independent repetitions of this scenario. */
    public var runs: Int = 1

    /** The seed used to derive per-run random seeds. */
    public var initialSeed: Int = 0

    /** A stable numeric identifier for the scenario. */
    public var id: Int = -1

    /** A human-readable name for the scenario. */
    public var name: String = ""

    public fun topology(topology: TopologySpec) {
        this.topology = topology
    }

    public fun topology(block: TopologyBuilder.() -> Unit) {
        this.topology = TopologyBuilder().apply(block).build()
    }

    public fun workload(workload: WorkloadSpec) {
        this.workload = workload
    }

    public fun workload(block: InlineWorkloadBuilder.() -> Unit) {
        this.workload = InlineWorkloadBuilder().apply(block).build()
    }

    public fun allocationPolicy(policy: AllocationPolicySpec) {
        this.allocationPolicy = policy
    }

    internal fun build(): ScenarioSpec {
        val resolvedTopology = topology ?: error("scenario requires a topology")
        val resolvedWorkload = workload ?: error("scenario requires a workload")
        return ScenarioSpec(
            resolvedTopology, resolvedWorkload, allocationPolicy, exportModel, failureModel,
            checkpointModel, maxNumFailures, runs, initialSeed, id, name,
        )
    }
}
