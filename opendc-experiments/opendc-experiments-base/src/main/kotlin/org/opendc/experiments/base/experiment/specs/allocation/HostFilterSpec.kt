package org.opendc.experiments.base.experiment.specs.allocation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.compute.simulator.scheduler.filters.ComputeFilter
import org.opendc.compute.simulator.scheduler.filters.DifferentHostFilter
import org.opendc.compute.simulator.scheduler.filters.HostFilter
import org.opendc.compute.simulator.scheduler.filters.InstanceCountFilter
import org.opendc.compute.simulator.scheduler.filters.RamFilter
import org.opendc.compute.simulator.scheduler.filters.SameHostFilter
import org.opendc.compute.simulator.scheduler.filters.VCpuCapacityFilter
import org.opendc.compute.simulator.scheduler.filters.VCpuFilter

public enum class HostFilterEnum {
    Compute,
    DifferentHost,
    InstanceCount,
    Ram,
    SameHost,
    VCpuCapacity,
    VCpu,
}







@Serializable
public sealed class HostFilterSpec

@Serializable
@SerialName("Compute")
public data class ComputeFilterSpec(
    val type: HostFilterEnum = HostFilterEnum.Compute,
) : HostFilterSpec()

@Serializable
@SerialName("DifferentHost")
public data class DifferentHostFilterSpec(
    val type: HostFilterEnum = HostFilterEnum.DifferentHost,
) : HostFilterSpec()

@Serializable
@SerialName("InstanceCount")
public data class InstanceCountHostFilterSpec(
    val type: HostFilterEnum = HostFilterEnum.InstanceCount,
    val limit: Int
) : HostFilterSpec()

@Serializable
@SerialName("Ram")
public data class RamHostFilterSpec(
    val type: HostFilterEnum = HostFilterEnum.Ram,
    val allocationRatio: Double
) : HostFilterSpec()

@Serializable
@SerialName("SameHost")
public data class SameHostHostFilterSpec(
    val type: HostFilterEnum = HostFilterEnum.SameHost,
) : HostFilterSpec()

@Serializable
@SerialName("VCpuCapacity")
public data class VCpuCapacityHostFilterSpec(
    val type: HostFilterEnum = HostFilterEnum.VCpuCapacity,
) : HostFilterSpec()

@Serializable
@SerialName("VCpu")
public data class VCpuHostFilterSpec(
    val type: HostFilterEnum = HostFilterEnum.VCpu,
    val allocationRatio: Double
) : HostFilterSpec()


public fun createHostFilter(filterSpec: HostFilterSpec): HostFilter {
    return when (filterSpec) {
        is ComputeFilterSpec -> ComputeFilter()
        is DifferentHostFilterSpec -> DifferentHostFilter()
        is InstanceCountHostFilterSpec -> InstanceCountFilter(filterSpec.limit)
        is RamHostFilterSpec -> RamFilter(filterSpec.allocationRatio)
        is SameHostHostFilterSpec -> SameHostFilter()
        is VCpuCapacityHostFilterSpec -> VCpuCapacityFilter()
        is VCpuHostFilterSpec -> VCpuFilter(filterSpec.allocationRatio)
    }
}
