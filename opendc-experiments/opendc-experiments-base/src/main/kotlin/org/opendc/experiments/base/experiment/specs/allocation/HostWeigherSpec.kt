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

package org.opendc.experiments.base.experiment.specs.allocation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.compute.simulator.scheduler.weights.CoreRamWeigher
import org.opendc.compute.simulator.scheduler.weights.HostWeigher
import org.opendc.compute.simulator.scheduler.weights.InstanceCountWeigher
import org.opendc.compute.simulator.scheduler.weights.RamWeigher
import org.opendc.compute.simulator.scheduler.weights.VCpuCapacityWeigher
import org.opendc.compute.simulator.scheduler.weights.VCpuWeigher

/**
 * A specification for a host weigher.
 *
 * A host weigher can be defined in a JSON file by adding the serialName as the type parameter.
 *
 * The user then has to specify any additional parameters required for the weigher.
 */
@Serializable
public sealed class HostWeigherSpec

@Serializable
@SerialName("CoreRam")
public data class CoreRamWeigherSpec(
    val multiplier: Double,
) : HostWeigherSpec()

@Serializable
@SerialName("InstanceCount")
public data class InstanceCountWeigherSpec(
    val multiplier: Double,
) : HostWeigherSpec()

@Serializable
@SerialName("Ram")
public data class RamWeigherSpec(
    val multiplier: Double,
) : HostWeigherSpec()

@Serializable
@SerialName("VCpuCapacity")
public data class VCpuCapacityWeigherSpec(
    val multiplier: Double,
) : HostWeigherSpec()

@Serializable
@SerialName("VCpu")
public data class VCpuWeigherSpec(
    val multiplier: Double,
) : HostWeigherSpec()

public fun createHostWeigher(weigherSpec: HostWeigherSpec): HostWeigher {
    return when (weigherSpec) {
        is CoreRamWeigherSpec -> CoreRamWeigher(weigherSpec.multiplier)
        is InstanceCountWeigherSpec -> InstanceCountWeigher(weigherSpec.multiplier)
        is RamWeigherSpec -> RamWeigher(weigherSpec.multiplier)
        is VCpuCapacityWeigherSpec -> VCpuCapacityWeigher(weigherSpec.multiplier)
        is VCpuWeigherSpec -> VCpuWeigher(weigherSpec.multiplier)
    }
}
