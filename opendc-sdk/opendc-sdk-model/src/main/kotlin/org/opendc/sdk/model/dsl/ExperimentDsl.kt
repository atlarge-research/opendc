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
import org.opendc.sdk.model.experiment.Experiment
import org.opendc.sdk.model.experiment.Scenario
import org.opendc.sdk.model.export.ExportSpec
import org.opendc.sdk.model.failure.FailureModel
import org.opendc.sdk.model.failure.NoFailure
import org.opendc.sdk.model.scheduler.AllocationPolicy
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicy
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.workload.Workload

/**
 * Builds an [Experiment] whose axes expand into the scenarios to run.
 *
 * @param block Configures the experiment through an [ExperimentBuilder].
 */
public fun experiment(block: ExperimentBuilder.() -> Unit): Experiment = ExperimentBuilder().apply(block).build()

/**
 * Builds a single, fully-resolved [Scenario].
 *
 * @param block Configures the scenario through a [ScenarioBuilder].
 */
public fun scenario(block: ScenarioBuilder.() -> Unit): Scenario = ScenarioBuilder().apply(block).build()

/** Collects the candidate values of each experiment axis. */
@SdkDsl
public class ExperimentBuilder {
    private val topologies = mutableSetOf<TopologySpec>()
    private val workloads = mutableSetOf<Workload>()
    private val allocationPolicies = mutableSetOf<AllocationPolicy>()
    private val failureModels = mutableSetOf<FailureModel>()
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

    public fun workload(workload: Workload) {
        workloads += workload
    }

    public fun workload(block: InlineWorkloadBuilder.() -> Unit) {
        workloads += InlineWorkloadBuilder().apply(block).build()
    }

    public fun allocationPolicy(policy: AllocationPolicy) {
        allocationPolicies += policy
    }

    public fun failureModel(model: FailureModel) {
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

    internal fun build(): Experiment =
        Experiment(
            topologies = topologies.toSet(),
            workloads = workloads.toSet(),
            allocationPolicies = allocationPolicies.ifEmpty { setOf(PrefabAllocationPolicy()) }.toSet(),
            failureModels = failureModels.ifEmpty { setOf(NoFailure) }.toSet(),
            maxNumFailures = maxNumFailures.ifEmpty { setOf(10) }.toSet(),
            checkpointModels = checkpointModels.ifEmpty { setOf(null) }.toSet(),
            exportModels = exportModels.ifEmpty { setOf(ExportSpec()) }.toSet(),
            runs = runs,
            initialSeed = initialSeed,
            name = name,
        )
}

/** Collects the components of a single [Scenario]. */
@SdkDsl
public class ScenarioBuilder {
    private var topology: TopologySpec? = null
    private var workload: Workload? = null
    private var allocationPolicy: AllocationPolicy = PrefabAllocationPolicy()

    /** Controls which results are written and how often. */
    public var exportModel: ExportSpec = ExportSpec()

    /** The failure-injection model applied during the run. */
    public var failureModel: FailureModel = NoFailure

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

    public fun workload(workload: Workload) {
        this.workload = workload
    }

    public fun workload(block: InlineWorkloadBuilder.() -> Unit) {
        this.workload = InlineWorkloadBuilder().apply(block).build()
    }

    public fun allocationPolicy(policy: AllocationPolicy) {
        this.allocationPolicy = policy
    }

    internal fun build(): Scenario {
        val resolvedTopology = topology ?: error("scenario requires a topology")
        val resolvedWorkload = workload ?: error("scenario requires a workload")
        return Scenario(
            resolvedTopology, resolvedWorkload, allocationPolicy, exportModel, failureModel,
            checkpointModel, maxNumFailures, runs, initialSeed, id, name,
        )
    }
}
