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
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.compute.workload.ComputeWorkloadLoader
import org.opendc.compute.workload.WorkloadLoader
import org.opendc.simulator.compute.workload.trace.scaling.NoDelayScaling
import org.opendc.simulator.compute.workload.trace.scaling.PerfectScaling
import org.opendc.simulator.compute.workload.trace.scaling.ScalingPolicy
import java.io.File

/**
 * Specification describing a workload trace that is replayed against the topologies.
 *
 * @property pathToFile Path to the directory of Parquet files that contains the workload trace.
 * @property type The type of workload contained in the trace. See [WorkloadTypes].
 * @property sampleFraction Fraction of the workload's tasks (by load) that is sampled and simulated.
 * Must be greater than 0.
 * @property submissionTime Optional timestamp used as the submission time of the first task. When `null` the
 * submission times from the trace are used as-is.
 * @property deferAll Whether every task should be deferred, overriding the submission times of the trace.
 * @property scalingPolicy Policy that decides how a task's remaining work is scaled when it does not receive
 * its full demand. See [ScalingPolicyEnum].
 */
@Serializable
@Deprecated("Replaced by the opendc-sdk model (org.opendc.sdk.model.*); run experiments with the new opendc CLI (opendc-cli).")
public data class WorkloadSpec(
    val pathToFile: String,
    val type: WorkloadTypes,
    val sampleFraction: Double = 1.0,
    val submissionTime: String? = null,
    val deferAll: Boolean = false,
    val scalingPolicy: ScalingPolicyEnum = ScalingPolicyEnum.NoDelay,
) {
    public val name: String = File(pathToFile).nameWithoutExtension

    /**
     * Validate the constraints of this workload specification.
     *
     * All violated constraints are collected and reported together, so a user fixing a workload sees
     * every problem at once instead of one per run. When any constraint is violated an
     * [InvalidWorkloadException] is thrown; otherwise this returns nothing.
     *
     * @throws InvalidWorkloadException if one or more constraints are violated.
     */
    public fun validate() {
        val errors =
            buildList {
                if (sampleFraction <= 0) {
                    add("The fraction of the tasks can not be 0.0 or lower (currently sampleFraction=$sampleFraction)")
                }
                if (!File(pathToFile).exists()) {
                    add("The provided path to the workload '$pathToFile' does not exist")
                }
            }

        if (errors.isNotEmpty()) {
            throw InvalidWorkloadException(errors)
        }
    }
}

/**
 * Exception thrown when a [WorkloadSpec] violates one or more of its constraints.
 *
 * @property errors The human-readable descriptions of every violated constraint.
 */
public class InvalidWorkloadException(
    public val errors: List<String>,
) : IllegalArgumentException(
        "Invalid workload specification:\n" + errors.joinToString("\n") { "  - $it" },
    )

/**
 * specification describing a workload type
 *
 * @constructor Create empty Workload types
 */
public enum class WorkloadTypes {
    ComputeWorkload,
}

/**
 * Create a workload loader for the given workload
 */
public fun getWorkloadLoader(
    workloadSpec: WorkloadSpec,
    checkpointModelSpec: CheckpointModelSpec?,
): WorkloadLoader {
    val scalingPolicy = getScalingPolicy(workloadSpec.scalingPolicy)

    return when (workloadSpec.type) {
        WorkloadTypes.ComputeWorkload ->
            ComputeWorkloadLoader(
                File(workloadSpec.pathToFile),
                workloadSpec.submissionTime,
                checkpointModelSpec?.checkpointInterval ?: 0L,
                checkpointModelSpec?.checkpointDuration ?: 0L,
                checkpointModelSpec?.checkpointIntervalScaling ?: 1.0,
                scalingPolicy,
                workloadSpec.deferAll,
            )
    }
}

public fun getWorkload(
    workloadSpec: WorkloadSpec,
    checkpointModelSpec: CheckpointModelSpec?,
): List<ServiceTask> {
    val workloadLoader =
        getWorkloadLoader(
            workloadSpec,
            checkpointModelSpec,
        )
    return workloadLoader.sampleByLoad(workloadSpec.sampleFraction)
}

public enum class ScalingPolicyEnum {
    NoDelay,
    Perfect,
}

public fun getScalingPolicy(scalingPolicyEnum: ScalingPolicyEnum): ScalingPolicy {
    return when (scalingPolicyEnum) {
        ScalingPolicyEnum.NoDelay -> NoDelayScaling()
        ScalingPolicyEnum.Perfect -> PerfectScaling()
    }
}
