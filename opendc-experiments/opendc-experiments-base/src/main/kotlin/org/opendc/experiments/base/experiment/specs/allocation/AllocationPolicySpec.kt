/*
 * Copyright (c) 2024 AtLarge Research
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
import org.opendc.compute.simulator.scheduler.ComputeScheduler
import org.opendc.compute.simulator.scheduler.ComputeSchedulerEnum
import org.opendc.compute.simulator.scheduler.FilterScheduler
import org.opendc.compute.simulator.scheduler.timeshift.TimeshiftScheduler
import org.opendc.compute.simulator.scheduler.createPrefabComputeScheduler
import org.opendc.compute.simulator.scheduler.timeshift.TaskStopper
import org.opendc.compute.simulator.service.ComputeService
import java.time.InstantSource
import java.util.random.RandomGenerator

/**
 * specification describing how tasks are allocated
 */
@Serializable
public sealed interface AllocationPolicySpec

@Serializable
@SerialName("prefab")
public data class PrefabAllocationPolicySpec(
    val policyName: ComputeSchedulerEnum = ComputeSchedulerEnum.Mem,
) : AllocationPolicySpec {
    public val name: String = policyName.toString()
}

@Serializable
@SerialName("filter")
public data class FilterAllocationPolicySpec(
    val filters: List<HostFilterSpec>,
    val weighers: List<HostWeigherSpec>,
    val subsetSize: Int = 1,
) : AllocationPolicySpec

@Serializable
@SerialName("timeshift")
public data class TimeShiftAllocationPolicySpec(
    val filters: List<HostFilterSpec>,
    val weighers: List<HostWeigherSpec>,
    val windowSize: Int = 168,
    val subsetSize: Int = 1,
    val peakShift: Boolean = true,
    val forecast: Boolean = true,
    val forecastThreshold: Double = 0.35,
    val forecastSize: Int = 24,
    val taskStopper: TaskStopperSpec? = null,
) : AllocationPolicySpec

public fun createComputeScheduler(
    spec: AllocationPolicySpec,
    seeder: RandomGenerator,
    clock: InstantSource,
): ComputeScheduler {
    return when (spec) {
        is PrefabAllocationPolicySpec -> createPrefabComputeScheduler(spec.policyName, seeder, clock)
        is FilterAllocationPolicySpec -> {
            val filters = spec.filters.map { createHostFilter(it) }
            val weighers = spec.weighers.map { createHostWeigher(it) }
            FilterScheduler(filters, weighers, spec.subsetSize, seeder)
        }
        is TimeShiftAllocationPolicySpec -> {
            val filters = spec.filters.map { createHostFilter(it) }
            val weighers = spec.weighers.map { createHostWeigher(it) }
            TimeshiftScheduler(
                filters, weighers, spec.windowSize, clock, spec.subsetSize, spec.peakShift,
                spec.forecast, spec.forecastThreshold, spec.forecastSize, seeder
            )
        }
    }
}

@Serializable
@SerialName("taskstopper")
public data class TaskStopperSpec(
    val forecast: Boolean = true,
    val forecastThreshold: Double = 0.6,
    val forecastSize: Int = 24,
    val windowSize: Int = 168,
)

public fun createTaskStopper(
    spec: TaskStopperSpec?,
    clock: InstantSource,
): TaskStopper? {
    val taskStopper = if (spec != null) {
        TaskStopper(clock, spec.forecast,
            spec.forecastThreshold, spec.forecastSize, spec.windowSize)
    } else {
        null
    }

    return taskStopper
}
