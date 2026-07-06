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
 * A specification for a host filter. A host filter decides how a host is selected for a given allocation request.
 * The filter is applied to all hosts in the data center and only those that pass the filter are considered for allocation.
 *
 * A host filter can be defined in a JSON object by adding the serialName as the type parameter.
 *
 * The user then has to specify any additional parameters required for the filter. The following
 * filters are available: [ComputeFilterSpec] (`"Compute"`), [SameHostHostFilterSpec] (`"SameHost"`),
 * [DifferentHostFilterSpec] (`"DifferentHost"`), [InstanceCountHostFilterSpec] (`"InstanceCount"`),
 * [RamHostFilterSpec] (`"Ram"`), [VCpuCapacityHostFilterSpec] (`"VCpuCapacity"`) and
 * [VCpuHostFilterSpec] (`"VCpu"`).
 */
@Serializable
public sealed class HostFilterSpec {
    /**
     * Validate the constraints of this host filter specification.
     *
     * The default implementation performs no checks; filters with constraints override it. The first
     * violated constraint is reported by throwing an [InvalidHostFilterException].
     *
     * @throws InvalidHostFilterException if one of the constraints is violated.
     */
    public open fun validate() {}
}

/**
 * Filter that only keeps hosts that are currently up and running.
 *
 * A host that is down (for example due to a failure) can never run a task, so this basic filter is
 * almost always included in a policy.
 *
 * @property filterName Discriminator identifying this filter. Always [HostFilterEnum.Compute].
 */
@Serializable
@SerialName("Compute")
public data class ComputeFilterSpec(
    val filterName: HostFilterEnum = HostFilterEnum.Compute,
) : HostFilterSpec()

/**
 * Affinity filter that keeps only the hosts a task wants to be co-located with.
 *
 * Note: host affinity hints are currently not applied, so this filter presently keeps every host.
 *
 * @property filterName Discriminator identifying this filter. Always [HostFilterEnum.SameHost].
 */
@Serializable
@SerialName("SameHost")
public data class SameHostHostFilterSpec(
    val filterName: HostFilterEnum = HostFilterEnum.SameHost,
) : HostFilterSpec()

/**
 * Anti-affinity filter that removes the hosts a task wants to be kept apart from.
 *
 * Note: host anti-affinity hints are currently not applied, so this filter presently keeps every host.
 *
 * @property filterName Discriminator identifying this filter. Always [HostFilterEnum.DifferentHost].
 */
@Serializable
@SerialName("DifferentHost")
public data class DifferentHostFilterSpec(
    val filterName: HostFilterEnum = HostFilterEnum.DifferentHost,
) : HostFilterSpec()

/**
 * Filter that keeps only hosts running fewer than [limit] tasks, capping how many tasks share a host.
 *
 * @property filterName Discriminator identifying this filter. Always [HostFilterEnum.InstanceCount].
 * @property limit Maximum number of tasks allowed on a host; only hosts below this count pass. Must be positive.
 */
@Serializable
@SerialName("InstanceCount")
public data class InstanceCountHostFilterSpec(
    val filterName: HostFilterEnum = HostFilterEnum.InstanceCount,
    val limit: Int,
) : HostFilterSpec() {
    override fun validate() {
        if (limit < 1) {
            throw InvalidHostFilterException("The instance count limit must be positive (currently limit=$limit)")
        }
    }
}

/**
 * Filter that keeps only hosts with enough memory available to run the task.
 *
 * With an [allocationRatio] of 1.0 a host must have at least as much free memory as the task requests.
 * A higher ratio permits memory overcommit: up to `memoryCapacity * allocationRatio` may be handed out
 * across tasks, though a task can never overcommit against itself.
 *
 * @property filterName Discriminator identifying this filter. Always [HostFilterEnum.Ram].
 * @property allocationRatio Memory overcommit ratio. Must be positive. Default is 1.0 (no overcommit).
 */
@Serializable
@SerialName("Ram")
public data class RamHostFilterSpec(
    val filterName: HostFilterEnum = HostFilterEnum.Ram,
    val allocationRatio: Double = 1.0,
) : HostFilterSpec() {
    override fun validate() {
        if (allocationRatio <= 0.0) {
            throw InvalidHostFilterException("The allocation ratio must be positive (currently allocationRatio=$allocationRatio)")
        }
    }
}

/**
 * Filter that keeps only hosts whose per-core CPU capacity meets the task's per-core CPU demand.
 *
 * @property filterName Discriminator identifying this filter. Always [HostFilterEnum.VCpuCapacity].
 */
@Serializable
@SerialName("VCpuCapacity")
public data class VCpuCapacityHostFilterSpec(
    val filterName: HostFilterEnum = HostFilterEnum.VCpuCapacity,
) : HostFilterSpec()

/**
 * Filter that keeps only hosts with enough free vCPU cores to run the task.
 *
 * With an [allocationRatio] of 1.0 a host must have at least as many free cores as the task requests.
 * A higher ratio permits CPU overcommit: up to `coreCount * allocationRatio` cores may be handed out
 * across tasks, though a task can never overcommit against itself.
 *
 * @property filterName Discriminator identifying this filter. Always [HostFilterEnum.VCpu].
 * @property allocationRatio vCPU overcommit ratio. Must be positive. Default is 1.0 (no overcommit).
 */
@Serializable
@SerialName("VCpu")
public data class VCpuHostFilterSpec(
    val filterName: HostFilterEnum = HostFilterEnum.VCpu,
    val allocationRatio: Double = 1.0,
) : HostFilterSpec() {
    override fun validate() {
        if (allocationRatio <= 0.0) {
            throw InvalidHostFilterException("The allocation ratio must be positive (currently allocationRatio=$allocationRatio)")
        }
    }
}

/**
 * Exception thrown when a [HostFilterSpec] violates one of its constraints.
 *
 * Unlike a plain [IllegalArgumentException], [message] is non-null, so callers that catch this
 * specific type can use it directly without a null fallback.
 */
public class InvalidHostFilterException(
    override val message: String,
) : IllegalArgumentException(message)

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
