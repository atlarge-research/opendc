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
import org.opendc.compute.simulator.scheduler.CarbonAwareWorkflowScheduler
import org.opendc.compute.simulator.scheduler.ComputeScheduler
import org.opendc.compute.simulator.scheduler.ComputeSchedulerEnum
import org.opendc.compute.simulator.scheduler.FilterScheduler
import org.opendc.compute.simulator.scheduler.WorkflowAwareScheduler
import org.opendc.compute.simulator.scheduler.WorkflowAwareTimeshiftScheduler
import org.opendc.compute.simulator.scheduler.createPrefabComputeScheduler
import org.opendc.compute.simulator.scheduler.timeshift.MemorizingTimeshift
import org.opendc.compute.simulator.scheduler.timeshift.TaskStopper
import org.opendc.compute.simulator.scheduler.timeshift.TimeshiftScheduler
import java.time.InstantSource
import java.util.random.RandomGenerator
import kotlin.coroutines.CoroutineContext

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
    val filters: List<HostFilterSpec> = listOf(ComputeFilterSpec()),
    val weighers: List<HostWeigherSpec> = emptyList(),
    val subsetSize: Int = 1,
) : AllocationPolicySpec

@Serializable
@SerialName("timeshift")
public data class TimeShiftAllocationPolicySpec(
    val filters: List<HostFilterSpec> = listOf(ComputeFilterSpec()),
    val weighers: List<HostWeigherSpec> = emptyList(),
    val windowSize: Int = 168,
    val subsetSize: Int = 1,
    val forecast: Boolean = true,
    val shortForecastThreshold: Double = 0.2,
    val longForecastThreshold: Double = 0.35,
    val forecastSize: Int = 24,
    val taskStopper: TaskStopperSpec? = null,
    val memorize: Boolean = true,
) : AllocationPolicySpec

@Serializable
@SerialName("workflowAware")
public data class WorkflowAwareAllocationPolicySpec(
    val filters: List<HostFilterSpec> = listOf(ComputeFilterSpec()),
    val weighers: List<HostWeigherSpec> = emptyList(),
    val enableDeadlineScore: Boolean = true, // whether to include deadlines in task selection score or not
    val weightUrgency: Double = 0.2,
    val weightCriticalDependencyChain: Double = 0.2,
    val enableParallelismScore: Boolean = false,
    val weightParallelism: Double = 0.0,
    val parallelismDecayRate: Double = 0.15,
    val subsetSize: Int = 1,
    val taskLookaheadThreshold: Int = 1000,
) : AllocationPolicySpec

@Serializable
@SerialName("carbonAwareWorkflow")
public data class CarbonAwareWorkflowAllocationPolicySpec(
    val filters: List<HostFilterSpec> = listOf(ComputeFilterSpec()),
    val weighers: List<HostWeigherSpec> = emptyList(),
    val slotLengthMs: Long = 3_600_000L, // 1 hour slots
    val horizonSlots: Int = 24, // 24 hour horizon
    val enableOptimization: Boolean = true,
    val batchSize: Int = 100, // Optimize up to 100 tasks at a time
    val maxSlotsToTry: Int = 10, // Try up to 10 different time slots per task
    val searchWindowSize: Int = 24, // Search 24 slots ahead for low-carbon slots
    val subsetSize: Int = 1,
) : AllocationPolicySpec

@Serializable
@SerialName("workflowAwareTimeshift")
public data class WorkflowAwareTimeshiftAllocationPolicySpec(
    val filters: List<HostFilterSpec> = listOf(ComputeFilterSpec()),
    val weighers: List<HostWeigherSpec> = emptyList(),
    val taskDeadlineScore: Boolean = true,
    val weightUrgency: Double = 0.3,
    val weightCriticalDependencyChain: Double = 0.4,
    val weightCarbonImpact: Double = 0.3,
    val windowSize: Int = 168,
    val forecast: Boolean = true,
    val shortForecastThreshold: Double = 0.2,
    val longForecastThreshold: Double = 0.35,
    val forecastSize: Int = 24,
    val subsetSize: Int = 1,
    val taskLookaheadThreshold: Int = 1000,
) : AllocationPolicySpec

public fun createComputeScheduler(
    spec: AllocationPolicySpec,
    seeder: RandomGenerator,
    clock: InstantSource,
    numHosts: Int = 1000,
): ComputeScheduler {
    return when (spec) {
        is PrefabAllocationPolicySpec -> createPrefabComputeScheduler(spec.policyName, seeder, clock, numHosts)
        is FilterAllocationPolicySpec -> {
            val filters = spec.filters.map { createHostFilter(it) }
            val weighers = spec.weighers.map { createHostWeigher(it) }
            FilterScheduler(filters, weighers, spec.subsetSize, seeder, numHosts)
        }
        is TimeShiftAllocationPolicySpec -> {
            val filters = spec.filters.map { createHostFilter(it) }
            val weighers = spec.weighers.map { createHostWeigher(it) }
            if (spec.memorize) {
                MemorizingTimeshift(
                    filters,
                    spec.windowSize,
                    clock,
                    spec.forecast,
                    spec.shortForecastThreshold,
                    spec.longForecastThreshold,
                    spec.forecastSize,
                )
            } else {
                TimeshiftScheduler(
                    filters, weighers, spec.windowSize, clock, spec.subsetSize, spec.forecast,
                    spec.shortForecastThreshold, spec.longForecastThreshold, spec.forecastSize, seeder,
                )
            }
        }
        is WorkflowAwareAllocationPolicySpec -> {
            val filters = spec.filters.map { createHostFilter(it) }
            val weighers = spec.weighers.map { createHostWeigher(it) }
            WorkflowAwareScheduler(
                filters, weighers, spec.enableDeadlineScore, spec.weightUrgency, 
                spec.weightCriticalDependencyChain, spec.enableParallelismScore, 
                spec.weightParallelism, spec.parallelismDecayRate, clock, 
                spec.subsetSize, seeder, numHosts, spec.taskLookaheadThreshold
            )
        }
        is CarbonAwareWorkflowAllocationPolicySpec -> {
            val filters = spec.filters.map { createHostFilter(it) }
            val weighers = spec.weighers.map { createHostWeigher(it) }
            CarbonAwareWorkflowScheduler(
                filters = filters,
                weighers = weighers,
                clock = clock,
                slotLengthMs = spec.slotLengthMs,
                horizonSlots = spec.horizonSlots,
                enableOptimization = spec.enableOptimization,
                batchSize = spec.batchSize,
                maxSlotsToTry = spec.maxSlotsToTry,
                searchWindowSize = spec.searchWindowSize,
                subsetSize = spec.subsetSize,
                random = seeder,
                numHosts = numHosts,
            )
        }
        is WorkflowAwareTimeshiftAllocationPolicySpec -> {
            val filters = spec.filters.map { createHostFilter(it) }
            val weighers = spec.weighers.map { createHostWeigher(it) }
            WorkflowAwareTimeshiftScheduler(
                filters = filters,
                weighers = weighers,
                taskDeadlineScore = spec.taskDeadlineScore,
                weightUrgency = spec.weightUrgency,
                weightCriticalDependencyChain = spec.weightCriticalDependencyChain,
                weightCarbonImpact = spec.weightCarbonImpact,
                clock = clock,
                windowSize = spec.windowSize,
                forecast = spec.forecast,
                shortForecastThreshold = spec.shortForecastThreshold,
                longForecastThreshold = spec.longForecastThreshold,
                forecastSize = spec.forecastSize,
                subsetSize = spec.subsetSize,
                random = seeder,
                numHosts = numHosts,
                taskLookaheadThreshold = spec.taskLookaheadThreshold,
            )
        }
    }
}

@Serializable
@SerialName("taskstopper")
public data class TaskStopperSpec(
    val windowSize: Int = 168,
    val forecast: Boolean = true,
    val forecastThreshold: Double = 0.6,
    val forecastSize: Int = 24,
)

public fun createTaskStopper(
    spec: TaskStopperSpec?,
    context: CoroutineContext,
    clock: InstantSource,
): TaskStopper? {
    val taskStopper =
        if (spec != null) {
            TaskStopper(
                clock,
                context,
                spec.forecast,
                spec.forecastThreshold,
                spec.forecastSize,
                spec.windowSize,
            )
        } else {
            null
        }

    return taskStopper
}
