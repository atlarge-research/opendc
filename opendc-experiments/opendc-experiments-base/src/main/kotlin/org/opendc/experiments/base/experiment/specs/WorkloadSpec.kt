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
import org.opendc.compute.workload.ComputeWorkloadLoader
import org.opendc.compute.workload.WorkloadLoader
import org.opendc.simulator.compute.workload.trace.scaling.NoDelayScaling
import org.opendc.simulator.compute.workload.trace.scaling.PerfectScaling
import org.opendc.simulator.compute.workload.trace.scaling.ScalingPolicy
import java.io.File

/**
 * specification describing a workload
 *
 * @property pathToFile
 * @property type
 * @property sampleFraction
 * @property submissionTime
 */
@Serializable
public data class WorkloadSpec(
    val pathToFile: String,
    val type: WorkloadTypes,
    val sampleFraction: Double = 1.0,
    val submissionTime: String? = null,
    val deferAll: Boolean = false,
    val scalingPolicy: ScalingPolicyEnum = ScalingPolicyEnum.NoDelay,
) {
    public val name: String = File(pathToFile).nameWithoutExtension

    init {
        require(sampleFraction > 0) { "The fraction of the tasks can not be 0.0 or lower" }
        require(File(pathToFile).exists()) { "The provided path to the workload: $pathToFile does not exist " }
    }
}

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
    type: WorkloadTypes,
    pathToFile: File,
    submissionTime: String?,
    checkpointInterval: Long,
    checkpointDuration: Long,
    checkpointIntervalScaling: Double,
    scalingPolicy: ScalingPolicy,
    deferAll: Boolean
): WorkloadLoader {
    return when (type) {
        WorkloadTypes.ComputeWorkload ->
            ComputeWorkloadLoader(
                pathToFile,
                submissionTime,
                checkpointInterval,
                checkpointDuration,
                checkpointIntervalScaling,
                scalingPolicy,
                deferAll
            )
    }
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
