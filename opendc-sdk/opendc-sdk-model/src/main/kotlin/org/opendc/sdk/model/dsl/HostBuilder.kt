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

import org.opendc.common.units.DataRate
import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.sdk.model.topology.CpuSpec
import org.opendc.sdk.model.topology.DistributionPolicySpec
import org.opendc.sdk.model.topology.GpuSpec
import org.opendc.sdk.model.topology.HostSpec
import org.opendc.sdk.model.topology.MaxMinFairnessPolicySpec
import org.opendc.sdk.model.topology.MemorySpec
import org.opendc.sdk.model.topology.NoVirtualizationOverheadSpec
import org.opendc.sdk.model.topology.PowerModelSpec
import org.opendc.sdk.model.topology.VirtualizationOverheadSpec

/** Builds a [HostSpec]; `cpu` and `memory` must be set before the enclosing block returns. */
@SdkDsl
public class HostBuilder(private val name: String, private val count: Int) {
    private var cpu: CpuSpec? = null
    private var memory: MemorySpec? = null
    private var gpu: GpuSpec? = null

    /** Power model for the CPU. */
    public var cpuPowerModel: PowerModelSpec = PowerModelSpec.DEFAULT

    /** Power model for the GPU. */
    public var gpuPowerModel: PowerModelSpec = PowerModelSpec.DEFAULT

    /** Policy distributing CPU capacity across tasks. */
    public var cpuDistribution: DistributionPolicySpec = MaxMinFairnessPolicySpec

    /** Policy distributing GPU capacity across tasks. */
    public var gpuDistribution: DistributionPolicySpec = MaxMinFairnessPolicySpec

    public fun cpu(
        coreCount: Int,
        coreSpeed: Frequency,
        count: Int = 1,
        vendor: String = "unknown",
        modelName: String = "unknown",
        architecture: String = "unknown",
    ) {
        cpu = CpuSpec(coreCount, coreSpeed, count, vendor, modelName, architecture)
    }

    public fun memory(
        size: DataSize,
        speed: Frequency = Frequency.ofMHz(-1),
        vendor: String = "unknown",
        modelName: String = "unknown",
        architecture: String = "unknown",
    ) {
        memory = MemorySpec(size, speed, vendor, modelName, architecture)
    }

    public fun gpu(
        coreCount: Int,
        coreSpeed: Frequency,
        count: Int = 1,
        memory: DataSize = DataSize.ofMiB(-1),
        memoryBandwidth: DataRate = DataRate.ofKibps(-1),
        vendor: String = "unknown",
        modelName: String = "unknown",
        architecture: String = "unknown",
        virtualizationOverhead: VirtualizationOverheadSpec = NoVirtualizationOverheadSpec,
    ) {
        gpu = GpuSpec(coreCount, coreSpeed, count, memory, memoryBandwidth, vendor, modelName, architecture, virtualizationOverhead)
    }

    internal fun build(): HostSpec {
        val resolvedCpu = cpu ?: error("host '$name' requires a cpu")
        val resolvedMemory = memory ?: error("host '$name' requires memory")
        return HostSpec(name, count, resolvedCpu, resolvedMemory, gpu, cpuPowerModel, gpuPowerModel, cpuDistribution, gpuDistribution)
    }
}
