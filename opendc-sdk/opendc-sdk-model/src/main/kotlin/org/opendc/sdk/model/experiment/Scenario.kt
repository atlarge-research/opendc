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
import org.opendc.sdk.model.checkpoint.CheckpointModel
import org.opendc.sdk.model.export.ExportModel
import org.opendc.sdk.model.failure.FailureModel
import org.opendc.sdk.model.failure.NoFailure
import org.opendc.sdk.model.scheduler.AllocationPolicy
import org.opendc.sdk.model.topology.Topology
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue
import org.opendc.sdk.model.validation.prefixed
import org.opendc.sdk.model.workload.Workload

/**
 * A single, fully-resolved simulation configuration ready to be executed.
 *
 * @property topology The datacenter topology to simulate.
 * @property workload The workload submitted to the datacenter.
 * @property allocationPolicy The policy deciding how tasks are placed on hosts.
 * @property exportModel Controls which results are written and how often.
 * @property failureModel The failure-injection model applied during the run.
 * @property checkpointModel Optional checkpointing configuration for tasks.
 * @property maxNumFailures The maximum number of failures a task may survive before it is terminated.
 * @property runs The number of independent repetitions of this scenario.
 * @property initialSeed The seed used to derive per-run random seeds.
 * @property id A stable numeric identifier for the scenario.
 * @property name A human-readable name for the scenario.
 */
@Serializable
public data class Scenario(
    public val topology: Topology,
    public val workload: Workload,
    public val allocationPolicy: AllocationPolicy,
    public val exportModel: ExportModel = ExportModel(),
    public val failureModel: FailureModel = NoFailure,
    public val checkpointModel: CheckpointModel? = null,
    public val maxNumFailures: Int = 10,
    public val runs: Int = 1,
    public val initialSeed: Int = 0,
    public val id: Int = -1,
    public val name: String = "",
) : Validatable {
    override fun validate(): List<ValidationIssue> =
        buildList {
            if (runs <= 0) add(ValidationIssue("runs", "must be > 0"))
            if (maxNumFailures < 1) add(ValidationIssue("maxNumFailures", "must be >= 1"))
            addAll(topology.validate().prefixed("topology"))
            addAll(workload.validate().prefixed("workload"))
            addAll(allocationPolicy.validate().prefixed("allocationPolicy"))
            addAll(exportModel.validate().prefixed("exportModel"))
            addAll(failureModel.validate().prefixed("failureModel"))
            checkpointModel?.let { addAll(it.validate().prefixed("checkpointModel")) }
        }
}
