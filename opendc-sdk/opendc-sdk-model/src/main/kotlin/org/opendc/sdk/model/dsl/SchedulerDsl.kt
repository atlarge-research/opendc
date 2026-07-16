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

package org.opendc.sdk.model.dsl

import org.opendc.sdk.model.scheduler.ComputeHostFilter
import org.opendc.sdk.model.scheduler.FilterAllocationPolicy
import org.opendc.sdk.model.scheduler.HostFilter
import org.opendc.sdk.model.scheduler.HostWeigher
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicy
import org.opendc.sdk.model.scheduler.SchedulerName
import org.opendc.sdk.model.scheduler.TaskStopperSpec
import org.opendc.sdk.model.scheduler.TimeShiftAllocationPolicy

/**
 * Selects a named, prefabricated scheduler.
 *
 * @param name The prefabricated scheduler to use.
 */
public fun prefabScheduler(name: SchedulerName = SchedulerName.Mem): PrefabAllocationPolicy = PrefabAllocationPolicy(name)

/**
 * Builds a filter-then-weigh scheduler.
 *
 * @param block Configures the pipeline through a [FilterSchedulerBuilder].
 */
public fun filterScheduler(block: FilterSchedulerBuilder.() -> Unit): FilterAllocationPolicy = FilterSchedulerBuilder().apply(block).build()

/**
 * Builds a carbon-aware time-shifting scheduler.
 *
 * @param block Configures the scheduler through a [TimeShiftSchedulerBuilder].
 */
public fun timeShiftScheduler(block: TimeShiftSchedulerBuilder.() -> Unit): TimeShiftAllocationPolicy =
    TimeShiftSchedulerBuilder().apply(block).build()

/** Collects the filters, weighers, and subset size of a [FilterAllocationPolicy]. */
@SdkDsl
public class FilterSchedulerBuilder {
    private val filters = mutableListOf<HostFilter>()
    private val weighers = mutableListOf<HostWeigher>()

    /** The size of the top-ranked subset sampled from for placement. */
    public var subsetSize: Int = 1

    public fun filter(filter: HostFilter) {
        filters += filter
    }

    public fun weigher(weigher: HostWeigher) {
        weighers += weigher
    }

    internal fun build(): FilterAllocationPolicy {
        val resolvedFilters = filters.ifEmpty { listOf(ComputeHostFilter) }
        return FilterAllocationPolicy(resolvedFilters, weighers.toList(), subsetSize)
    }
}

/** Collects the configuration of a [TimeShiftAllocationPolicy]. */
@SdkDsl
public class TimeShiftSchedulerBuilder {
    private val filters = mutableListOf<HostFilter>()
    private val weighers = mutableListOf<HostWeigher>()

    /** The number of past samples considered by the carbon signal. */
    public var windowSize: Int = 168

    /** The size of the top-ranked subset sampled from for placement. */
    public var subsetSize: Int = 1

    /** Whether to base decisions on forecasted rather than historical values. */
    public var forecast: Boolean = true

    /** The normalized threshold for the short forecast horizon. */
    public var shortForecastThreshold: Double = 0.2

    /** The normalized threshold for the long forecast horizon. */
    public var longForecastThreshold: Double = 0.35

    /** The number of future samples to forecast. */
    public var forecastSize: Int = 24

    /** The optional policy controlling when deferrable tasks are paused. */
    public var taskStopper: TaskStopperSpec? = null

    /** Whether to memoize scheduling decisions across invocations. */
    public var memorize: Boolean = true

    public fun filter(filter: HostFilter) {
        filters += filter
    }

    public fun weigher(weigher: HostWeigher) {
        weighers += weigher
    }

    internal fun build(): TimeShiftAllocationPolicy {
        val resolvedFilters = filters.ifEmpty { listOf(ComputeHostFilter) }
        return TimeShiftAllocationPolicy(
            resolvedFilters, weighers.toList(), windowSize, subsetSize, forecast,
            shortForecastThreshold, longForecastThreshold, forecastSize, taskStopper, memorize,
        )
    }
}
