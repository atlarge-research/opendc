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

package org.opendc.compute.simulator.scheduler

import org.opendc.compute.simulator.scheduler.filters.ComputeFilter
import org.opendc.compute.simulator.scheduler.filters.RamFilter
import org.opendc.compute.simulator.scheduler.filters.VCpuFilter
import org.opendc.compute.simulator.scheduler.filters.VGpuFilter
import org.opendc.compute.simulator.scheduler.timeshift.TimeshiftScheduler
import org.opendc.compute.simulator.scheduler.weights.CoreRamWeigher
import org.opendc.compute.simulator.scheduler.weights.InstanceCountWeigher
import org.opendc.compute.simulator.scheduler.weights.RamWeigher
import org.opendc.compute.simulator.scheduler.weights.VCpuWeigher
import org.opendc.compute.simulator.scheduler.weights.VGpuWeigher
import java.time.InstantSource
import java.util.SplittableRandom
import java.util.random.RandomGenerator

public enum class ComputeSchedulerEnum {
    Mem,
    MemInv,
    CoreMem,
    CoreMemInv,
    ActiveServers,
    ActiveServersInv,
    ProvisionedCores,
    ProvisionedCoresInv,
    Random,
    TaskNumMemorizing,
    Timeshift,
    ProvisionedCpuGpuCores,
    ProvisionedCpuGpuCoresInv,
}

public fun createPrefabComputeScheduler(
    name: String,
    seeder: RandomGenerator,
    clock: InstantSource,
): ComputeScheduler {
    return createPrefabComputeScheduler(ComputeSchedulerEnum.valueOf(name.uppercase()), seeder, clock)
}

/**
 * Create a [ComputeScheduler] for the experiment.
 */
public fun createPrefabComputeScheduler(
    name: ComputeSchedulerEnum,
    seeder: RandomGenerator,
    clock: InstantSource,
): ComputeScheduler {
    val cpuAllocationRatio = 1.0
    val ramAllocationRatio = 1.5
    val gpuAllocationRatio = 1.0
    return when (name) {
        ComputeSchedulerEnum.Mem ->
            FilterScheduler(
                filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
                weighers = listOf(RamWeigher(multiplier = 1.0)),
            )
        ComputeSchedulerEnum.MemInv ->
            FilterScheduler(
                filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
                weighers = listOf(RamWeigher(multiplier = -1.0)),
            )
        ComputeSchedulerEnum.CoreMem ->
            FilterScheduler(
                filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
                weighers = listOf(CoreRamWeigher(multiplier = 1.0)),
            )
        ComputeSchedulerEnum.CoreMemInv ->
            FilterScheduler(
                filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
                weighers = listOf(CoreRamWeigher(multiplier = -1.0)),
            )
        ComputeSchedulerEnum.ActiveServers ->
            FilterScheduler(
                filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
                weighers = listOf(InstanceCountWeigher(multiplier = -1.0)),
            )
        ComputeSchedulerEnum.ActiveServersInv ->
            FilterScheduler(
                filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
                weighers = listOf(InstanceCountWeigher(multiplier = 1.0)),
            )
        ComputeSchedulerEnum.ProvisionedCores ->
            FilterScheduler(
                filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
                weighers = listOf(VCpuWeigher(cpuAllocationRatio, multiplier = 1.0)),
            )
        ComputeSchedulerEnum.ProvisionedCoresInv ->
            FilterScheduler(
                filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
                weighers = listOf(VCpuWeigher(cpuAllocationRatio, multiplier = -1.0)),
            )
        ComputeSchedulerEnum.Random ->
            FilterScheduler(
                filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
                weighers = emptyList(),
                subsetSize = Int.MAX_VALUE,
                random = SplittableRandom(seeder.nextLong()),
            )
        ComputeSchedulerEnum.TaskNumMemorizing ->
            MemorizingScheduler(
                filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            )
        ComputeSchedulerEnum.Timeshift ->
            TimeshiftScheduler(
                filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
                weighers = listOf(RamWeigher(multiplier = 1.0)),
                windowSize = 168,
                clock = clock,
                random = SplittableRandom(seeder.nextLong()),
            )
        ComputeSchedulerEnum.ProvisionedCpuGpuCores ->
            FilterScheduler(
                filters =
                    listOf(
                        ComputeFilter(),
                        VCpuFilter(cpuAllocationRatio),
                        VGpuFilter(gpuAllocationRatio),
                        RamFilter(ramAllocationRatio),
                    ),
                weighers = listOf(VCpuWeigher(cpuAllocationRatio, multiplier = 1.0), VGpuWeigher(gpuAllocationRatio, multiplier = 1.0)),
            )
        ComputeSchedulerEnum.ProvisionedCpuGpuCoresInv ->
            FilterScheduler(
                filters =
                    listOf(
                        ComputeFilter(),
                        VCpuFilter(cpuAllocationRatio),
                        VGpuFilter(gpuAllocationRatio),
                        RamFilter(ramAllocationRatio),
                    ),
                weighers =
                    listOf(
                        VCpuWeigher(cpuAllocationRatio, multiplier = -1.0),
                        VGpuWeigher(gpuAllocationRatio, multiplier = -1.0),
                    ),
            )
    }
}
