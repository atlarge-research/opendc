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
 * Scores candidate hosts so the scheduler can rank them; the [multiplier] weights the score.
 */
@Serializable
public sealed interface HostWeigher : Validatable {
    /** The weight applied to this weigher's contribution to the final host score. */
    public val multiplier: Double

    override fun validate(): List<ValidationIssue> =
        if (multiplier.isFinite()) emptyList() else listOf(ValidationIssue("multiplier", "must be finite"))
}

/**
 * Weighs hosts by their available memory.
 *
 * @property multiplier The weight applied to the memory score.
 */
@Serializable
@SerialName("ram")
public data class RamWeigher(override val multiplier: Double = 1.0) : HostWeigher

/**
 * Weighs hosts by the combination of their available cores and memory.
 *
 * @property multiplier The weight applied to the core-and-memory score.
 */
@Serializable
@SerialName("coreRam")
public data class CoreRamWeigher(override val multiplier: Double = 1.0) : HostWeigher

/**
 * Weighs hosts by the number of task instances they already run.
 *
 * @property multiplier The weight applied to the instance-count score.
 */
@Serializable
@SerialName("instanceCount")
public data class InstanceCountWeigher(override val multiplier: Double = 1.0) : HostWeigher

/**
 * Weighs hosts by their available per-core capacity.
 *
 * @property multiplier The weight applied to the vCPU-capacity score.
 */
@Serializable
@SerialName("vcpuCapacity")
public data class VCpuCapacityWeigher(override val multiplier: Double = 1.0) : HostWeigher

/**
 * Weighs hosts by their available vCPUs.
 *
 * @property multiplier The weight applied to the vCPU score.
 */
@Serializable
@SerialName("vcpu")
public data class VCpuWeigher(override val multiplier: Double = 1.0) : HostWeigher

/**
 * Weighs hosts by their available vGPUs.
 *
 * @property multiplier The weight applied to the vGPU score.
 */
@Serializable
@SerialName("vgpu")
public data class VGpuWeigher(override val multiplier: Double = 1.0) : HostWeigher
