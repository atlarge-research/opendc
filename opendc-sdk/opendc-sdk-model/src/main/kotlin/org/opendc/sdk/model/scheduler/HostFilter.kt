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

package org.opendc.sdk.model.scheduler

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue

/**
 * A predicate that decides whether a candidate host is eligible to receive a task.
 */
@Serializable
public sealed interface HostFilter : Validatable {
    override fun validate(): List<ValidationIssue> = emptyList()
}

/** Keeps only hosts able to fulfil the task's compute requirements. */
@Serializable
@SerialName("compute")
public data object ComputeHostFilter : HostFilter

/** Keeps hosts that already run tasks affine to the current one. */
@Serializable
@SerialName("sameHost")
public data object SameHostFilter : HostFilter

/** Keeps hosts that do not run tasks anti-affine to the current one. */
@Serializable
@SerialName("differentHost")
public data object DifferentHostFilter : HostFilter

/**
 * Keeps hosts running fewer than [limit] instances of the task group.
 *
 * @property limit The maximum number of instances allowed per host.
 */
@Serializable
@SerialName("instanceCount")
public data class InstanceCountFilter(public val limit: Int) : HostFilter {
    override fun validate(): List<ValidationIssue> = if (limit >= 1) emptyList() else listOf(ValidationIssue("limit", "must be >= 1"))
}

/**
 * Keeps hosts with enough memory given an over-commit [allocationRatio].
 *
 * @property allocationRatio The multiplier applied to physical memory capacity.
 */
@Serializable
@SerialName("ram")
public data class RamFilter(public val allocationRatio: Double = 1.0) : HostFilter {
    override fun validate(): List<ValidationIssue> =
        if (allocationRatio > 0.0) emptyList() else listOf(ValidationIssue("allocationRatio", "must be > 0"))
}

/** Keeps hosts whose per-core capacity covers the task's requested capacity. */
@Serializable
@SerialName("vcpuCapacity")
public data object VCpuCapacityFilter : HostFilter

/**
 * Keeps hosts with enough vCPUs given an over-commit [allocationRatio].
 *
 * @property allocationRatio The multiplier applied to physical core count.
 */
@Serializable
@SerialName("vcpu")
public data class VCpuFilter(public val allocationRatio: Double = 1.0) : HostFilter {
    override fun validate(): List<ValidationIssue> =
        if (allocationRatio > 0.0) emptyList() else listOf(ValidationIssue("allocationRatio", "must be > 0"))
}

/**
 * Keeps hosts with enough vGPUs given an over-commit [allocationRatio].
 *
 * @property allocationRatio The multiplier applied to physical GPU core count.
 */
@Serializable
@SerialName("vgpu")
public data class VGpuFilter(public val allocationRatio: Double = 1.0) : HostFilter {
    override fun validate(): List<ValidationIssue> =
        if (allocationRatio > 0.0) emptyList() else listOf(ValidationIssue("allocationRatio", "must be > 0"))
}
