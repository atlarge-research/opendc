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

import kotlinx.serialization.Serializable
import org.opendc.sdk.model.checkpoint.CheckpointSpec
import org.opendc.sdk.model.export.ExportSpec
import org.opendc.sdk.model.failure.FailureModel
import org.opendc.sdk.model.failure.NoFailure
import org.opendc.sdk.model.scheduler.AllocationPolicy
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicy
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue
import org.opendc.sdk.model.validation.validateEach
import org.opendc.sdk.model.workload.Workload

/**
 * A design of experiments: a set of choices per axis whose cartesian product yields the [Scenario]s to run.
 *
 * @property topologies The candidate topologies.
 * @property workloads The candidate workloads.
 * @property allocationPolicies The candidate allocation policies.
 * @property failureModels The candidate failure models.
 * @property maxNumFailures The candidate maximum-failure limits.
 * @property checkpointModels The candidate checkpoint configurations (null means no checkpointing).
 * @property exportModels The candidate export configurations.
 * @property runs The number of independent repetitions per scenario.
 * @property initialSeed The seed used to derive per-run random seeds.
 * @property name A human-readable name for the experiment.
 */
@Serializable
public data class Experiment(
    public val topologies: Set<TopologySpec>,
    public val workloads: Set<Workload>,
    public val allocationPolicies: Set<AllocationPolicy> = setOf(PrefabAllocationPolicy()),
    public val failureModels: Set<FailureModel> = setOf(NoFailure),
    public val maxNumFailures: Set<Int> = setOf(10),
    public val checkpointModels: Set<CheckpointSpec?> = setOf(null),
    public val exportModels: Set<ExportSpec> = setOf(ExportSpec()),
    public val runs: Int = 1,
    public val initialSeed: Int = 0,
    public val name: String = "",
) : Validatable {
    override fun validate(): List<ValidationIssue> =
        buildList {
            if (runs <= 0) add(ValidationIssue("runs", "must be > 0"))
            if (topologies.isEmpty()) add(ValidationIssue("topologies", "must not be empty"))
            if (workloads.isEmpty()) add(ValidationIssue("workloads", "must not be empty"))
            if (allocationPolicies.isEmpty()) add(ValidationIssue("allocationPolicies", "must not be empty"))
            if (exportModels.isEmpty()) add(ValidationIssue("exportModels", "must not be empty"))
            addAll(topologies.validateEach("topologies"))
            addAll(workloads.validateEach("workloads"))
            addAll(allocationPolicies.validateEach("allocationPolicies"))
            addAll(exportModels.validateEach("exportModels"))
            addAll(failureModels.validateEach("failureModels"))
            addAll(checkpointModels.filterNotNull().validateEach("checkpointModels"))
            maxNumFailures.forEach { if (it < 1) add(ValidationIssue("maxNumFailures", "must be >= 1")) }
        }
}
