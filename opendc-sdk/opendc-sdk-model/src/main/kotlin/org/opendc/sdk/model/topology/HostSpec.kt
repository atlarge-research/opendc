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

package org.opendc.sdk.model.topology

import kotlinx.serialization.Serializable
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue
import org.opendc.sdk.model.validation.prefixed

/**
 * A physical host in a cluster.
 *
 * @property name Human-readable identifier.
 * @property count Number of identical hosts to instantiate.
 * @property cpu CPU specification.
 * @property memory Memory specification.
 * @property gpu Optional GPU specification.
 * @property cpuPowerModel Power model for the CPU.
 * @property gpuPowerModel Power model for the GPU.
 * @property cpuDistribution Policy distributing CPU capacity across tasks.
 * @property gpuDistribution Policy distributing GPU capacity across tasks.
 */
@Serializable
public data class HostSpec(
    public val name: String = "Host",
    public val count: Int = 1,
    public val cpu: CpuSpec,
    public val memory: MemorySpec,
    public val gpu: GpuSpec? = null,
    public val cpuPowerModel: PowerSpec = PowerSpec.DEFAULT,
    public val gpuPowerModel: PowerSpec = PowerSpec.DEFAULT,
    public val cpuDistribution: DistributionPolicy = MaxMinFairness,
    public val gpuDistribution: DistributionPolicy = MaxMinFairness,
) : Validatable {
    override fun validate(): List<ValidationIssue> =
        buildList {
            if (count <= 0) add(ValidationIssue("count", "must be > 0"))
            addAll(cpu.validate().prefixed("cpu"))
            addAll(cpuPowerModel.validate().prefixed("cpuPowerModel"))
            if (gpu != null) addAll(gpuPowerModel.validate().prefixed("gpuPowerModel"))
        }
}
