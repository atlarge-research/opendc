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
import org.opendc.common.units.Power
import org.opendc.sdk.model.topology.Cpu
import org.opendc.sdk.model.topology.DistributionPolicy
import org.opendc.sdk.model.topology.Gpu
import org.opendc.sdk.model.topology.Host
import org.opendc.sdk.model.topology.MaxMinFairness
import org.opendc.sdk.model.topology.Memory
import org.opendc.sdk.model.topology.NoVirtualizationOverhead
import org.opendc.sdk.model.topology.PowerModel
import org.opendc.sdk.model.topology.PowerModelType
import org.opendc.sdk.model.topology.VirtualizationOverhead

/** Builds a [Host]; `cpu` and `memory` must be set before the enclosing block returns. */
@SdkDsl
public class HostBuilder(private val name: String, private val count: Int) {
    private var cpu: Cpu? = null
    private var memory: Memory? = null
    private var gpu: Gpu? = null
    private var cpuPowerModel: PowerModel = PowerModel.DEFAULT
    private var gpuPowerModel: PowerModel = PowerModel.DEFAULT

    /** Policy distributing CPU capacity across tasks. */
    public var cpuDistribution: DistributionPolicy = MaxMinFairness

    /** Policy distributing GPU capacity across tasks. */
    public var gpuDistribution: DistributionPolicy = MaxMinFairness

    public fun cpu(
        coreCount: Int,
        coreSpeed: Frequency,
        count: Int = 1,
        vendor: String = "unknown",
        modelName: String = "unknown",
        architecture: String = "unknown",
    ) {
        cpu = Cpu(coreCount, coreSpeed, count, vendor, modelName, architecture)
    }

    public fun memory(
        size: DataSize,
        speed: Frequency = Frequency.ofMHz(-1),
        vendor: String = "unknown",
        modelName: String = "unknown",
        architecture: String = "unknown",
    ) {
        memory = Memory(size, speed, vendor, modelName, architecture)
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
        virtualizationOverhead: VirtualizationOverhead = NoVirtualizationOverhead,
    ) {
        gpu = Gpu(coreCount, coreSpeed, count, memory, memoryBandwidth, vendor, modelName, architecture, virtualizationOverhead)
    }

    public fun power(block: PowerModelBuilder.() -> Unit) {
        cpuPowerModel = PowerModelBuilder().apply(block).build()
    }

    public fun gpuPower(block: PowerModelBuilder.() -> Unit) {
        gpuPowerModel = PowerModelBuilder().apply(block).build()
    }

    internal fun build(): Host {
        val resolvedCpu = cpu ?: error("host '$name' requires a cpu")
        val resolvedMemory = memory ?: error("host '$name' requires memory")
        return Host(name, count, resolvedCpu, resolvedMemory, gpu, cpuPowerModel, gpuPowerModel, cpuDistribution, gpuDistribution)
    }
}

/** Builds a [PowerModel]; unset fields fall back to [PowerModel.DEFAULT]. */
@SdkDsl
public class PowerModelBuilder {
    /** Shape of the utilization-to-power curve. */
    public var type: PowerModelType = PowerModelType.LINEAR

    /** Power draw at full utilization. */
    public var maxPower: Power = Power.ofWatts(400)

    /** Power draw at zero utilization. */
    public var idlePower: Power = Power.ofWatts(200)

    /** Reference power used by some model types. */
    public var power: Power = Power.ofWatts(350)

    /** Multiplier applied to the modelled power draw. */
    public var calibrationFactor: Double = 1.0

    /** Asymptotic utilization parameter for non-linear models. */
    public var asymUtil: Double = 0.0

    /** Whether dynamic voltage and frequency scaling is modelled. */
    public var dvfs: Boolean = true

    internal fun build(): PowerModel = PowerModel(type, maxPower, idlePower, power, calibrationFactor, asymUtil, dvfs)
}
