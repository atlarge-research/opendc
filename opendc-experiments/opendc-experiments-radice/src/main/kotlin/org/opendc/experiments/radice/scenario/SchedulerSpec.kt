/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.experiments.radice.scenario

import com.fasterxml.jackson.annotation.JsonProperty
import org.opendc.compute.service.scheduler.ComputeScheduler
import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.filters.*
import org.opendc.compute.service.scheduler.weights.*
import java.util.*

/**
 * Immutable scheduler configuration.
 *
 * @property filterCapacity A flag to indicate that the host should be filtered based on their total capacity.
 * @property ramWeight Multiplier for RAM weigher (0.0 to disable).
 * @property coreRamWeight Multiplier for Core RAM weigher (0.0 to disable)
 * @property instanceCountWeight Multiplier for instance count weigher (0.0 to disable)
 * @property vcpuWeight Multiplier for vCPU weigher (0.0 to disable)
 * @property vcpuCapacityWeight Multiplier for vCPU capacity weigher (0.0 to disable).
 * @property cpuAllocationRatio Virtual CPU to physical CPU allocation ratio.
 * @property ramAllocationRatio Virtual RAM to physical RAM allocation ratio.
 * @property subsetSize The size of the subset of best hosts from which a target is randomly chosen.
 */
data class SchedulerSpec(
    @JsonProperty("filter-capacity") val filterCapacity: Boolean = false,
    @JsonProperty("ram-weight") val ramWeight: Double = 0.0,
    @JsonProperty("core-ram-weight") val coreRamWeight: Double = 0.0,
    @JsonProperty("instance-count-weight") val instanceCountWeight: Double = 0.0,
    @JsonProperty("vcpu-weight") val vcpuWeight: Double = 0.0,
    @JsonProperty("vcpu-capacity-weight") val vcpuCapacityWeight: Double = 0.0,
    @JsonProperty("cpu-allocation-ratio") val cpuAllocationRatio: Double = 16.0,
    @JsonProperty("ram-allocation-ratio") val ramAllocationRatio: Double = 1.5,
    @JsonProperty("subset-size") val subsetSize: Int = 1
) {
    /**
     * Construct a [ComputeScheduler] from this scheduler specification.
     */
    operator fun invoke(random: Random): ComputeScheduler {
        val filters = mutableListOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio))
        if (filterCapacity) {
            filters.add(VCpuCapacityFilter())
        }

        val weighers = mutableListOf<HostWeigher>()

        if (ramWeight != 0.0) {
            weighers.add(RamWeigher(ramWeight))
        }

        if (coreRamWeight != 0.0) {
            weighers.add(CoreRamWeigher(coreRamWeight))
        }

        if (instanceCountWeight != 0.0) {
            weighers.add(InstanceCountWeigher(instanceCountWeight))
        }

        if (vcpuWeight != 0.0) {
            weighers.add(VCpuWeigher(cpuAllocationRatio, vcpuWeight))
        }

        if (vcpuCapacityWeight != 0.0) {
            weighers.add(VCpuCapacityWeigher(vcpuCapacityWeight))
        }

        return FilterScheduler(filters, weighers, subsetSize, random)
    }
}
