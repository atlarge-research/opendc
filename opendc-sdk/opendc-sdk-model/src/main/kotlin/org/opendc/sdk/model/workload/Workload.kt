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

package org.opendc.sdk.model.workload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.sdk.model.resource.ResourceReference
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue
import org.opendc.sdk.model.validation.validateEach

/**
 * The set of tasks submitted to the datacenter over the course of a simulation.
 */
@Serializable
public sealed interface Workload : Validatable {
    override fun validate(): List<ValidationIssue> = emptyList()
}

/**
 * A workload loaded from an external trace resource.
 *
 * @property source Handle to the trace data resolved at runtime.
 * @property sampleFraction Fraction of the trace's tasks to sample.
 * @property submissionTime Optional ISO-8601 local date-time used as the workload start.
 * @property scalingPolicy How tasks react to resource contention.
 * @property deferAll Whether every task should be treated as deferrable.
 */
@Serializable
@SerialName("trace")
public data class TraceWorkload(
    public val source: ResourceReference,
    public val sampleFraction: Double = 1.0,
    public val submissionTime: String? = null,
    public val scalingPolicy: ScalingPolicy = ScalingPolicy.NoDelay,
    public val deferAll: Boolean = false,
) : Workload {
    override fun validate(): List<ValidationIssue> =
        buildList {
            if (sampleFraction <= 0.0) add(ValidationIssue("sampleFraction", "must be greater than zero"))
        }
}

/**
 * A workload defined directly as a list of tasks.
 *
 * @property tasks The tasks that make up the workload.
 * @property scalingPolicy How tasks react to resource contention.
 */
@Serializable
@SerialName("inline")
public data class InlineWorkload(
    public val tasks: List<Task>,
    public val scalingPolicy: ScalingPolicy = ScalingPolicy.NoDelay,
) : Workload {
    override fun validate(): List<ValidationIssue> =
        buildList {
            if (tasks.isEmpty()) add(ValidationIssue("tasks", "must not be empty"))
            addAll(tasks.validateEach("tasks"))
        }
}
