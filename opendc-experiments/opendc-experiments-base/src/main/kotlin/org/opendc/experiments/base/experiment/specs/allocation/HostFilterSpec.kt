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

/**
 * A specification for a host filter.
 *
 * A host filter can be defined in a JSON file by adding the serialName as the type parameter.
 *
 * The user then has to specify any additional parameters required for the filter.
 */
@Serializable
public sealed class HostFilterSpec

@Serializable
@SerialName("Compute")
public data class ComputeFilterSpec(
    val filterName: HostFilterEnum = HostFilterEnum.Compute,
) : HostFilterSpec()

@Serializable
@SerialName("SameHost")
public data class SameHostHostFilterSpec(
    val filterName: HostFilterEnum = HostFilterEnum.SameHost,
) : HostFilterSpec()

@Serializable
@SerialName("DifferentHost")
public data class DifferentHostFilterSpec(
    val filterName: HostFilterEnum = HostFilterEnum.DifferentHost,
) : HostFilterSpec()

@Serializable
@SerialName("InstanceCount")
public data class InstanceCountHostFilterSpec(
    val filterName: HostFilterEnum = HostFilterEnum.InstanceCount,
    val limit: Int,
) : HostFilterSpec()

@Serializable
@SerialName("Ram")
public data class RamHostFilterSpec(
    val filterName: HostFilterEnum = HostFilterEnum.Ram,
    val allocationRatio: Double = 1.0,
) : HostFilterSpec()

@Serializable
@SerialName("VCpuCapacity")
public data class VCpuCapacityHostFilterSpec(
    val filterName: HostFilterEnum = HostFilterEnum.VCpuCapacity,
) : HostFilterSpec()

@Serializable
@SerialName("VCpu")
public data class VCpuHostFilterSpec(
    val filterName: HostFilterEnum = HostFilterEnum.VCpu,
    val allocationRatio: Double = 1.0,
) : HostFilterSpec()

public fun createHostFilter(filterSpec: HostFilterSpec): HostFilter {
    return when (filterSpec) {
        is ComputeFilterSpec -> ComputeFilter()
        is SameHostHostFilterSpec -> SameHostFilter()
        is DifferentHostFilterSpec -> DifferentHostFilter()
        is InstanceCountHostFilterSpec -> InstanceCountFilter(filterSpec.limit)
        is RamHostFilterSpec -> RamFilter(filterSpec.allocationRatio)
        is VCpuCapacityHostFilterSpec -> VCpuCapacityFilter()
        is VCpuHostFilterSpec -> VCpuFilter(filterSpec.allocationRatio)
    }
}
