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

@file:Suppress("DEPRECATION")

package org.opendc.experiments.base.experiment.specs.allocation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.compute.simulator.scheduler.ComputeScheduler
import org.opendc.compute.simulator.scheduler.ComputeSchedulerEnum
import org.opendc.compute.simulator.scheduler.FilterScheduler
import org.opendc.compute.simulator.scheduler.createPrefabComputeScheduler
import org.opendc.compute.simulator.scheduler.timeshift.MemorizingTimeshift
import org.opendc.compute.simulator.scheduler.timeshift.TaskStopper
import org.opendc.compute.simulator.scheduler.timeshift.TimeshiftScheduler
import java.time.InstantSource
import java.util.random.RandomGenerator
import kotlin.coroutines.CoroutineContext

/**
 * Specification describing how and when tasks are allocated to hosts by the scheduler.
 *
 * An allocation policy is selected in a JSON file by adding its [SerialName] as the `type`. The following
 * implementations are available:
 * - [PrefabAllocationPolicySpec] (`"prefab"`): one of the schedulers already present in OpenDC.
 * - [FilterAllocationPolicySpec] (`"filter"`): a filter scheduler built from host filters and weighers.
 * - [TimeShiftAllocationPolicySpec] (`"timeshift"`): a carbon-aware scheduler that shifts tasks in time.
 */
@Serializable
@Deprecated("Replaced by the opendc-sdk model (org.opendc.sdk.model.*); run experiments with the new opendc CLI (opendc-cli).")
public sealed interface AllocationPolicySpec {
    /**
     * Validate the constraints of this allocation policy specification.
     *
     * The default implementation performs no checks; implementations with constraints override it. When
     * any constraint is violated all violations, including those of nested [HostFilterSpec], [HostWeigherSpec]
     * and [TaskStopperSpec] specifications, are collected and reported together by throwing an
     * [InvalidAllocationPolicyException].
     */
    public fun validate() {}
}

/**
 * An allocation policy that uses one of the schedulers already built into OpenDC.
 *
 * This is the simplest way to configure scheduling: instead of assembling filters and weighers by hand,
 * a ready-made scheduler is selected by name.
 *
 * @property policyName The prefab scheduler to use. See [ComputeSchedulerEnum] for the available options.
 * Default is [ComputeSchedulerEnum.Mem].
 */
@Serializable
@SerialName("prefab")
public data class PrefabAllocationPolicySpec(
    val policyName: ComputeSchedulerEnum = ComputeSchedulerEnum.Mem,
) : AllocationPolicySpec {
    public val name: String = policyName.toString()
}

/**
 * An allocation policy backed by a filter scheduler.
 *
 * When a task is scheduled the hosts are first reduced to the eligible ones by applying every
 * [HostFilterSpec] in [filters], and the remaining hosts are ranked by the combined score of every
 * [HostWeigherSpec] in [weighers]. The task is then placed on one of the [subsetSize] highest-ranked
 * hosts, picked at random.
 *
 * @property filters Filters that remove hosts unable to run a task. Default is a single [ComputeFilterSpec].
 * @property weighers Weighers that rank the hosts that pass the filters. Default is no weighers.
 * @property subsetSize Number of top-ranked hosts to randomly pick the placement from. Must be positive.
 * Default is 1, which always selects the single best host.
 */
@Serializable
@SerialName("filter")
public data class FilterAllocationPolicySpec(
    val filters: List<HostFilterSpec> = listOf(ComputeFilterSpec()),
    val weighers: List<HostWeigherSpec> = emptyList(),
    val subsetSize: Int = 1,
) : AllocationPolicySpec {
    override fun validate() {
        val errors =
            buildList {
                if (subsetSize <= 0) {
                    add("The subset size must be positive (currently subsetSize=$subsetSize)")
                }
                addFilterAndWeigherErrors(filters, weighers)
            }

        if (errors.isNotEmpty()) {
            throw InvalidAllocationPolicyException(errors)
        }
    }
}

/**
 * A carbon-aware allocation policy that defers tasks to periods of lower carbon intensity.
 *
 * Like [FilterAllocationPolicySpec] it filters and weighs hosts, but it additionally decides whether the
 * current moment is a "low carbon" regime and holds deferrable tasks until such a moment. The current
 * carbon intensity is compared either against a quantile of the forecast (when [forecast] is true) or
 * against the moving average of the past [windowSize] samples (when it is false). Separate short- and
 * long-task regimes are tracked using [shortForecastThreshold] and [longForecastThreshold].
 *
 * @property filters Filters that remove hosts unable to run a task. Default is a single [ComputeFilterSpec].
 * @property weighers Weighers that rank the hosts that pass the filters. Default is no weighers.
 * @property windowSize Number of past carbon-intensity samples kept, and the length of the moving average
 * used when [forecast] is false. Must be positive. Default is 168 (one week of hourly samples).
 * @property subsetSize Number of top-ranked hosts to randomly pick the placement from. Must be positive. Default is 1.
 * @property forecast Whether a forecast of the carbon intensity is used instead of the moving average. Default is true.
 * @property shortForecastThreshold Quantile, between 0.0 and 1.0, of the forecast below which the current intensity
 * counts as low carbon for short tasks (shorter than 2 hours). Default is 0.2.
 * @property longForecastThreshold Quantile, between 0.0 and 1.0, of the forecast below which the current intensity
 * counts as low carbon for long tasks. Default is 0.35.
 * @property forecastSize Number of samples forecasted into the future when [forecast] is true. Must be positive. Default is 24.
 * @property taskStopper Optional [TaskStopperSpec] that pauses already running tasks when the carbon intensity is high.
 * Default is none.
 * @property memorize Whether to use the memorizing variant of the scheduler, which caches host rankings for speed.
 * Default is true.
 */
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
) : AllocationPolicySpec {
    override fun validate() {
        val errors =
            buildList {
                if (subsetSize <= 0) {
                    add("The subset size must be positive (currently subsetSize=$subsetSize)")
                }
                if (windowSize <= 0) {
                    add("The window size must be positive (currently windowSize=$windowSize)")
                }
                if (forecastSize <= 0) {
                    add("The forecast size must be positive (currently forecastSize=$forecastSize)")
                }
                if (shortForecastThreshold !in 0.0..1.0) {
                    add(
                        "The short forecast threshold must be between 0.0 and 1.0 " +
                            "(currently shortForecastThreshold=$shortForecastThreshold)",
                    )
                }
                if (longForecastThreshold !in 0.0..1.0) {
                    add(
                        "The long forecast threshold must be between 0.0 and 1.0 " +
                            "(currently longForecastThreshold=$longForecastThreshold)",
                    )
                }
                addFilterAndWeigherErrors(filters, weighers)
                try {
                    taskStopper?.validate()
                } catch (e: InvalidAllocationPolicyException) {
                    addAll(e.errors)
                }
            }

        if (errors.isNotEmpty()) {
            throw InvalidAllocationPolicyException(errors)
        }
    }
}

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
    }
}

/**
 * Specification of a task stopper, which prematurely stops tasks when the forecasted carbon intensity
 * stays above a threshold.
 *
 * @property windowSize Number of past samples used to build the forecast. Must be positive.
 * @property forecast Whether a forecast is used instead of the current carbon intensity.
 * @property forecastThreshold Carbon-intensity threshold, as a fraction between 0.0 and 1.0, above which tasks are stopped.
 * @property forecastSize Number of samples that are forecasted into the future. Must be positive.
 */
@Serializable
@SerialName("taskstopper")
public data class TaskStopperSpec(
    val windowSize: Int = 168,
    val forecast: Boolean = true,
    val forecastThreshold: Double = 0.6,
    val forecastSize: Int = 24,
) {
    /**
     * Validate the constraints of this task stopper specification.
     *
     * All violated constraints are collected and reported together by throwing an
     * [InvalidAllocationPolicyException]; otherwise this returns nothing.
     *
     * @throws InvalidAllocationPolicyException if one or more constraints are violated.
     */
    public fun validate() {
        val errors =
            buildList {
                if (windowSize < 1) {
                    add("The window size must be positive (currently windowSize=$windowSize)")
                }
                if (forecastSize < 1) {
                    add("The forecast size must be positive (currently forecastSize=$forecastSize)")
                }
                if (forecastThreshold !in 0.0..1.0) {
                    add("The forecast threshold must be between 0.0 and 1.0 (currently forecastThreshold=$forecastThreshold)")
                }
            }

        if (errors.isNotEmpty()) {
            throw InvalidAllocationPolicyException(errors)
        }
    }
}

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

/**
 * Collect the validation errors of the given [filters] and [weighers] into this list.
 *
 * Each nested spec still throws on its own first violation; this helper catches those single-error
 * exceptions so their messages can be aggregated with the errors of the enclosing allocation policy.
 */
private fun MutableList<String>.addFilterAndWeigherErrors(
    filters: List<HostFilterSpec>,
    weighers: List<HostWeigherSpec>,
) {
    filters.forEach { filter ->
        try {
            filter.validate()
        } catch (e: InvalidHostFilterException) {
            add(e.message)
        }
    }
    weighers.forEach { weigher ->
        try {
            weigher.validate()
        } catch (e: InvalidHostWeigherException) {
            add(e.message)
        }
    }
}

/**
 * Exception thrown when an [AllocationPolicySpec] (or a [TaskStopperSpec]) violates one or more of its constraints.
 *
 * @property errors The human-readable descriptions of every violated constraint.
 */
public class InvalidAllocationPolicyException(
    public val errors: List<String>,
) : IllegalArgumentException(
        "Invalid allocation policy specification:\n" + errors.joinToString("\n") { "  - $it" },
    )
