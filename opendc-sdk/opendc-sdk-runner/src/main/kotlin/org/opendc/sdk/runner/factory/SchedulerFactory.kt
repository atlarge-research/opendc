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

package org.opendc.sdk.runner.factory

import org.opendc.sdk.model.scheduler.AllocationPolicySpec
import org.opendc.sdk.model.scheduler.ComputeHostFilterSpec
import org.opendc.sdk.model.scheduler.CoreRamWeigherSpec
import org.opendc.sdk.model.scheduler.DifferentHostFilterSpec
import org.opendc.sdk.model.scheduler.FilterAllocationPolicySpec
import org.opendc.sdk.model.scheduler.HostFilterSpec
import org.opendc.sdk.model.scheduler.HostWeigherSpec
import org.opendc.sdk.model.scheduler.InstanceCountFilterSpec
import org.opendc.sdk.model.scheduler.InstanceCountWeigherSpec
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicySpec
import org.opendc.sdk.model.scheduler.RamFilterSpec
import org.opendc.sdk.model.scheduler.RamWeigherSpec
import org.opendc.sdk.model.scheduler.SameHostFilterSpec
import org.opendc.sdk.model.scheduler.TimeshiftSpec
import org.opendc.sdk.model.scheduler.VCpuCapacityFilterSpec
import org.opendc.sdk.model.scheduler.VCpuCapacityWeigherSpec
import org.opendc.sdk.model.scheduler.VCpuFilterSpec
import org.opendc.sdk.model.scheduler.VCpuWeigherSpec
import org.opendc.sdk.model.scheduler.VGpuFilterSpec
import org.opendc.sdk.model.scheduler.VGpuWeigherSpec
import org.opendc.simulator.compute.scheduler.ComputeScheduler
import org.opendc.simulator.compute.scheduler.FilterScheduler
import org.opendc.simulator.compute.scheduler.filters.ComputeFilter
import org.opendc.simulator.compute.scheduler.timeshift.CarbonTimeshifter
import java.time.InstantSource
import java.util.random.RandomGenerator
import kotlin.coroutines.CoroutineContext
import org.opendc.sdk.model.scheduler.TaskStopperSpec as SdkTaskStopper
import org.opendc.simulator.compute.scheduler.filters.DifferentHostFilter as EngineDifferentHostFilter
import org.opendc.simulator.compute.scheduler.filters.HostFilter as EngineHostFilter
import org.opendc.simulator.compute.scheduler.filters.InstanceCountFilter as EngineInstanceCountFilter
import org.opendc.simulator.compute.scheduler.filters.RamFilter as EngineRamFilter
import org.opendc.simulator.compute.scheduler.filters.SameHostFilter as EngineSameHostFilter
import org.opendc.simulator.compute.scheduler.filters.VCpuCapacityFilter as EngineVCpuCapacityFilter
import org.opendc.simulator.compute.scheduler.filters.VCpuFilter as EngineVCpuFilter
import org.opendc.simulator.compute.scheduler.filters.VGpuFilter as EngineVGpuFilter
import org.opendc.simulator.compute.scheduler.timeshift.TaskStopper as EngineTaskStopper
import org.opendc.simulator.compute.scheduler.weights.CoreRamWeigher as EngineCoreRamWeigher
import org.opendc.simulator.compute.scheduler.weights.HostWeigher as EngineHostWeigher
import org.opendc.simulator.compute.scheduler.weights.InstanceCountWeigher as EngineInstanceCountWeigher
import org.opendc.simulator.compute.scheduler.weights.RamWeigher as EngineRamWeigher
import org.opendc.simulator.compute.scheduler.weights.VCpuCapacityWeigher as EngineVCpuCapacityWeigher
import org.opendc.simulator.compute.scheduler.weights.VCpuWeigher as EngineVCpuWeigher
import org.opendc.simulator.compute.scheduler.weights.VGpuWeigher as EngineVGpuWeigher

/**
 * Converts an SDK [AllocationPolicySpec] into an engine [ComputeScheduler], seeded by [seeder] and
 * clocked by [clock]. [numHosts] sizes the scheduler's internal host bookkeeping. A prefab is built
 * from the [org.opendc.sdk.model.scheduler.SchedulerPrefabSpec.policy] it stands for.
 */
public fun AllocationPolicySpec.toScheduler(
    seeder: RandomGenerator,
    clock: InstantSource,
    numHosts: Int,
): ComputeScheduler =
    when (this) {
        is PrefabAllocationPolicySpec -> prefabName.policy.toScheduler(seeder, clock, numHosts)
        is FilterAllocationPolicySpec ->
            FilterScheduler(filters.map { it.toEngine() }, weighers.map { it.toEngine() }, numHosts, timeshift?.toEngine(clock))
    }

/** Builds the engine [EngineTaskStopper] carried by timeshift settings, or null when absent. */
internal fun SdkTaskStopper?.toEngine(
    context: CoroutineContext,
    clock: InstantSource,
): EngineTaskStopper? = this?.let { EngineTaskStopper(clock, context, it.forecast, it.forecastThreshold, it.forecastSize, it.windowSize) }

/** Builds the engine [CarbonTimeshifter] for these settings. */
private fun TimeshiftSpec.toEngine(clock: InstantSource): CarbonTimeshifter =
    CarbonTimeshifter(clock, windowSize, forecast, shortForecastThreshold, longForecastThreshold, forecastSize)

private fun HostFilterSpec.toEngine(): EngineHostFilter =
    when (this) {
        ComputeHostFilterSpec -> ComputeFilter()
        SameHostFilterSpec -> EngineSameHostFilter()
        DifferentHostFilterSpec -> EngineDifferentHostFilter()
        VCpuCapacityFilterSpec -> EngineVCpuCapacityFilter()
        is InstanceCountFilterSpec -> EngineInstanceCountFilter(limit)
        is RamFilterSpec -> EngineRamFilter(allocationRatio)
        is VCpuFilterSpec -> EngineVCpuFilter(allocationRatio)
        is VGpuFilterSpec -> EngineVGpuFilter(allocationRatio)
    }

private fun HostWeigherSpec.toEngine(): EngineHostWeigher =
    when (this) {
        is RamWeigherSpec -> EngineRamWeigher(multiplier)
        is CoreRamWeigherSpec -> EngineCoreRamWeigher(multiplier)
        is InstanceCountWeigherSpec -> EngineInstanceCountWeigher(multiplier)
        is VCpuCapacityWeigherSpec -> EngineVCpuCapacityWeigher(multiplier)
        is VCpuWeigherSpec -> EngineVCpuWeigher(allocationRatio = 1.0, multiplier = multiplier)
        is VGpuWeigherSpec -> EngineVGpuWeigher(allocationRatio = 1.0, multiplier = multiplier)
    }
