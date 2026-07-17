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

@file:Suppress("DEPRECATION")

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
 * The user then has to specify any additional parameters required for the weigher. Every weigher is
 * scaled by a `multiplier`, which controls how strongly it contributes to the final host ranking.
 * The following weighers are available: [RamWeigherSpec] (`"Ram"`), [CoreRamWeigherSpec] (`"CoreRam"`),
 * [InstanceCountWeigherSpec] (`"InstanceCount"`), [VCpuCapacityWeigherSpec] (`"VCpuCapacity"`) and
 * [VCpuWeigherSpec] (`"VCpu"`).
 */
@Serializable
@Deprecated("Replaced by the opendc-sdk model (org.opendc.sdk.model.*); run experiments with the new opendc CLI (opendc-cli).")
public sealed class HostWeigherSpec {
    /**
     * The factor by which this weigher is scaled when ranking hosts.
     */
    public abstract val multiplier: Double

    /**
     * Validate the constraints of this host weigher specification.
     *
     * The first violated constraint is reported by throwing an [InvalidHostWeigherException];
     * otherwise this returns nothing.
     *
     * @throws InvalidHostWeigherException if one of the constraints is violated.
     */
    public fun validate() {
        if (!multiplier.isFinite()) {
            throw InvalidHostWeigherException("The weigher multiplier must be a finite number (currently multiplier=$multiplier)")
        }
    }
}

/**
 * Weigher that ranks hosts by the amount of memory they have available, favouring emptier hosts.
 *
 * @property multiplier Factor by which this weigher is scaled when combined with other weighers. Default is 1.0.
 */
@Serializable
@SerialName("Ram")
public data class RamWeigherSpec(
    override val multiplier: Double = 1.0,
) : HostWeigherSpec()

/**
 * Weigher that ranks hosts by their available memory per CPU core.
 *
 * @property multiplier Factor by which this weigher is scaled when combined with other weighers. Default is 1.0.
 */
@Serializable
@SerialName("CoreRam")
public data class CoreRamWeigherSpec(
    override val multiplier: Double = 1.0,
) : HostWeigherSpec()

/**
 * Weigher that ranks hosts by the number of tasks already running on them.
 *
 * @property multiplier Factor by which this weigher is scaled when combined with other weighers. Default is 1.0.
 */
@Serializable
@SerialName("InstanceCount")
public data class InstanceCountWeigherSpec(
    override val multiplier: Double = 1.0,
) : HostWeigherSpec()

/**
 * Weigher that ranks hosts by their spare CPU capacity per core.
 *
 * @property multiplier Factor by which this weigher is scaled when combined with other weighers. Default is 1.0.
 */
@Serializable
@SerialName("VCpuCapacity")
public data class VCpuCapacityWeigherSpec(
    override val multiplier: Double = 1.0,
) : HostWeigherSpec()

/**
 * Weigher that ranks hosts by their available vCPU cores.
 *
 * @property multiplier Factor by which this weigher is scaled when combined with other weighers. Default is 1.0.
 */
@Serializable
@SerialName("VCpu")
public data class VCpuWeigherSpec(
    override val multiplier: Double = 1.0,
) : HostWeigherSpec()

/**
 * Exception thrown when a [HostWeigherSpec] violates one of its constraints.
 *
 * Unlike a plain [IllegalArgumentException], [message] is non-null, so callers that catch this
 * specific type can use it directly without a null fallback.
 */
public class InvalidHostWeigherException(
    override val message: String,
) : IllegalArgumentException(message)

public fun createHostWeigher(weigherSpec: HostWeigherSpec): HostWeigher {
    return when (weigherSpec) {
        is RamWeigherSpec -> RamWeigher(weigherSpec.multiplier)
        is CoreRamWeigherSpec -> CoreRamWeigher(weigherSpec.multiplier)
        is InstanceCountWeigherSpec -> InstanceCountWeigher(weigherSpec.multiplier)
        is VCpuCapacityWeigherSpec -> VCpuCapacityWeigher(weigherSpec.multiplier)
        is VCpuWeigherSpec -> VCpuWeigher(weigherSpec.multiplier)
    }
}
