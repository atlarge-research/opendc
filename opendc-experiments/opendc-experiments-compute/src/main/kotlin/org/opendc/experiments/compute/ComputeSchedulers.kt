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

@file:JvmName("ComputeSchedulers")

package org.opendc.experiments.compute

import org.opendc.compute.service.scheduler.ComputeScheduler
import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.ReplayScheduler
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.filters.RamFilter
import org.opendc.compute.service.scheduler.filters.VCpuFilter
import org.opendc.compute.service.scheduler.weights.CoreRamWeigher
import org.opendc.compute.service.scheduler.weights.InstanceCountWeigher
import org.opendc.compute.service.scheduler.weights.RamWeigher
import org.opendc.compute.service.scheduler.weights.VCpuWeigher
import java.util.SplittableRandom
import java.util.random.RandomGenerator

/**
 * Create a [ComputeScheduler] for the experiment.
 */
public fun createComputeScheduler(name: String, seeder: RandomGenerator, placements: Map<String, String> = emptyMap()): ComputeScheduler {
    val cpuAllocationRatio = 16.0
    val ramAllocationRatio = 1.5
    return when (name) {
        "mem" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(RamWeigher(multiplier = 1.0))
        )
        "mem-inv" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(RamWeigher(multiplier = -1.0))
        )
        "core-mem" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(CoreRamWeigher(multiplier = 1.0))
        )
        "core-mem-inv" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(CoreRamWeigher(multiplier = -1.0))
        )
        "active-servers" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(InstanceCountWeigher(multiplier = -1.0))
        )
        "active-servers-inv" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(InstanceCountWeigher(multiplier = 1.0))
        )
        "provisioned-cores" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(VCpuWeigher(cpuAllocationRatio, multiplier = 1.0))
        )
        "provisioned-cores-inv" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(VCpuWeigher(cpuAllocationRatio, multiplier = -1.0))
        )
        "random" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = emptyList(),
            subsetSize = Int.MAX_VALUE,
            random = SplittableRandom(seeder.nextLong())
        )
        "replay" -> ReplayScheduler(placements)
        else -> throw IllegalArgumentException("Unknown policy $name")
    }
}
