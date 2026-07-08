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

package org.opendc.sdk.runner.internal

import org.opendc.compute.simulator.scheduler.ComputeScheduler
import org.opendc.compute.simulator.scheduler.FilterScheduler
import org.opendc.compute.simulator.scheduler.createPrefabComputeScheduler
import org.opendc.compute.simulator.scheduler.filters.ComputeFilter
import org.opendc.compute.simulator.scheduler.timeshift.MemorizingTimeshift
import org.opendc.compute.simulator.scheduler.timeshift.TimeshiftScheduler
import org.opendc.sdk.model.scheduler.AllocationPolicy
import org.opendc.sdk.model.scheduler.ComputeHostFilter
import org.opendc.sdk.model.scheduler.CoreRamWeigher
import org.opendc.sdk.model.scheduler.DifferentHostFilter
import org.opendc.sdk.model.scheduler.FilterAllocationPolicy
import org.opendc.sdk.model.scheduler.HostFilter
import org.opendc.sdk.model.scheduler.HostWeigher
import org.opendc.sdk.model.scheduler.InstanceCountFilter
import org.opendc.sdk.model.scheduler.InstanceCountWeigher
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicy
import org.opendc.sdk.model.scheduler.RamFilter
import org.opendc.sdk.model.scheduler.RamWeigher
import org.opendc.sdk.model.scheduler.SameHostFilter
import org.opendc.sdk.model.scheduler.TimeShiftAllocationPolicy
import org.opendc.sdk.model.scheduler.VCpuCapacityFilter
import org.opendc.sdk.model.scheduler.VCpuCapacityWeigher
import org.opendc.sdk.model.scheduler.VCpuFilter
import org.opendc.sdk.model.scheduler.VCpuWeigher
import org.opendc.sdk.model.scheduler.VGpuFilter
import org.opendc.sdk.model.scheduler.VGpuWeigher
import java.time.InstantSource
import java.util.random.RandomGenerator
import kotlin.coroutines.CoroutineContext
import org.opendc.compute.simulator.scheduler.filters.DifferentHostFilter as EngineDifferentHostFilter
import org.opendc.compute.simulator.scheduler.filters.HostFilter as EngineHostFilter
import org.opendc.compute.simulator.scheduler.filters.InstanceCountFilter as EngineInstanceCountFilter
import org.opendc.compute.simulator.scheduler.filters.RamFilter as EngineRamFilter
import org.opendc.compute.simulator.scheduler.filters.SameHostFilter as EngineSameHostFilter
import org.opendc.compute.simulator.scheduler.filters.VCpuCapacityFilter as EngineVCpuCapacityFilter
import org.opendc.compute.simulator.scheduler.filters.VCpuFilter as EngineVCpuFilter
import org.opendc.compute.simulator.scheduler.filters.VGpuFilter as EngineVGpuFilter
import org.opendc.compute.simulator.scheduler.timeshift.TaskStopper as EngineTaskStopper
import org.opendc.compute.simulator.scheduler.weights.CoreRamWeigher as EngineCoreRamWeigher
import org.opendc.compute.simulator.scheduler.weights.HostWeigher as EngineHostWeigher
import org.opendc.compute.simulator.scheduler.weights.InstanceCountWeigher as EngineInstanceCountWeigher
import org.opendc.compute.simulator.scheduler.weights.RamWeigher as EngineRamWeigher
import org.opendc.compute.simulator.scheduler.weights.VCpuCapacityWeigher as EngineVCpuCapacityWeigher
import org.opendc.compute.simulator.scheduler.weights.VCpuWeigher as EngineVCpuWeigher
import org.opendc.compute.simulator.scheduler.weights.VGpuWeigher as EngineVGpuWeigher
import org.opendc.sdk.model.scheduler.TaskStopper as SdkTaskStopper

/**
 * Converts an SDK [AllocationPolicy] into an engine [ComputeScheduler], seeded by [seeder] and
 * clocked by [clock]. [numHosts] sizes the scheduler's internal host bookkeeping.
 */
internal fun AllocationPolicy.toScheduler(
    seeder: RandomGenerator,
    clock: InstantSource,
    numHosts: Int,
): ComputeScheduler =
    when (this) {
        is PrefabAllocationPolicy -> createPrefabComputeScheduler(scheduler.name, seeder, clock, numHosts)
        is FilterAllocationPolicy ->
            FilterScheduler(filters.map { it.toEngine() }, weighers.map { it.toEngine() }, subsetSize, seeder, numHosts)
        is TimeShiftAllocationPolicy -> toTimeshiftScheduler(seeder, clock)
    }

/** Builds the engine [EngineTaskStopper] carried by a time-shifting policy, or null when absent. */
internal fun SdkTaskStopper?.toEngine(
    context: CoroutineContext,
    clock: InstantSource,
): EngineTaskStopper? = this?.let { EngineTaskStopper(clock, context, it.forecast, it.forecastThreshold, it.forecastSize, it.windowSize) }

private fun TimeShiftAllocationPolicy.toTimeshiftScheduler(
    seeder: RandomGenerator,
    clock: InstantSource,
): ComputeScheduler {
    val engineFilters = filters.map { it.toEngine() }
    if (memorize) {
        return MemorizingTimeshift(engineFilters, windowSize, clock, forecast, shortForecastThreshold, longForecastThreshold, forecastSize)
    }
    return TimeshiftScheduler(
        engineFilters,
        weighers.map { it.toEngine() },
        windowSize,
        clock,
        subsetSize,
        forecast,
        shortForecastThreshold,
        longForecastThreshold,
        forecastSize,
        seeder,
    )
}

private fun HostFilter.toEngine(): EngineHostFilter =
    when (this) {
        ComputeHostFilter -> ComputeFilter()
        SameHostFilter -> EngineSameHostFilter()
        DifferentHostFilter -> EngineDifferentHostFilter()
        VCpuCapacityFilter -> EngineVCpuCapacityFilter()
        is InstanceCountFilter -> EngineInstanceCountFilter(limit)
        is RamFilter -> EngineRamFilter(allocationRatio)
        is VCpuFilter -> EngineVCpuFilter(allocationRatio)
        is VGpuFilter -> EngineVGpuFilter(allocationRatio)
    }

private fun HostWeigher.toEngine(): EngineHostWeigher =
    when (this) {
        is RamWeigher -> EngineRamWeigher(multiplier)
        is CoreRamWeigher -> EngineCoreRamWeigher(multiplier)
        is InstanceCountWeigher -> EngineInstanceCountWeigher(multiplier)
        is VCpuCapacityWeigher -> EngineVCpuCapacityWeigher(multiplier)
        is VCpuWeigher -> EngineVCpuWeigher(allocationRatio = 1.0, multiplier = multiplier)
        is VGpuWeigher -> EngineVGpuWeigher(allocationRatio = 1.0, multiplier = multiplier)
    }
