package org.opendc.experiments.base.experiment.specs.allocation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.compute.simulator.scheduler.weights.CoreRamWeigher
import org.opendc.compute.simulator.scheduler.weights.HostWeigher
import org.opendc.compute.simulator.scheduler.weights.InstanceCountWeigher
import org.opendc.compute.simulator.scheduler.weights.RamWeigher
import org.opendc.compute.simulator.scheduler.weights.VCpuCapacityWeigher
import org.opendc.compute.simulator.scheduler.weights.VCpuWeigher

@Serializable
public sealed class HostWeigherSpec

@Serializable
@SerialName("CoreRam")
public data class CoreRamWeigherSpec(
    val multiplier: Double
) : HostWeigherSpec()

@Serializable
@SerialName("InstanceCount")
public data class InstanceCountWeigherSpec(
    val multiplier: Double
) : HostWeigherSpec()

@Serializable
@SerialName("Ram")
public data class RamWeigherSpec(
    val multiplier: Double
) : HostWeigherSpec()

@Serializable
@SerialName("VCpuCapacity")
public data class VCpuCapacityWeigherSpec(
    val multiplier: Double
) : HostWeigherSpec()

@Serializable
@SerialName("VCpu")
public data class VCpuWeigherSpec(
    val multiplier: Double
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
