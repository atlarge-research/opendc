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

import org.opendc.sdk.model.scheduler.ComputeHostFilterSpec
import org.opendc.sdk.model.scheduler.FilterAllocationPolicySpec
import org.opendc.sdk.model.scheduler.HostFilterSpec
import org.opendc.sdk.model.scheduler.HostWeigherSpec
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicySpec
import org.opendc.sdk.model.scheduler.SchedulerPrefabSpec
import org.opendc.sdk.model.scheduler.TaskStopperSpec
import org.opendc.sdk.model.scheduler.TimeshiftSpec

/**
 * Selects a named, prefabricated scheduler.
 *
 * @param name The prefabricated scheduler to use.
 */
public fun prefabScheduler(name: SchedulerPrefabSpec = SchedulerPrefabSpec.Mem): PrefabAllocationPolicySpec =
    PrefabAllocationPolicySpec(name)

/**
 * Builds a filter-then-weigh scheduler.
 *
 * @param block Configures the pipeline through a [FilterSchedulerBuilder].
 */
public fun filterScheduler(block: FilterSchedulerBuilder.() -> Unit): FilterAllocationPolicySpec =
    FilterSchedulerBuilder().apply(block).build()

/** Collects the configuration of a [FilterAllocationPolicySpec]. */
@SdkDsl
public class FilterSchedulerBuilder {
    private val filters = mutableListOf<HostFilterSpec>()
    private val weighers = mutableListOf<HostWeigherSpec>()

    private var timeshift: TimeshiftSpec? = null

    public fun filter(filter: HostFilterSpec) {
        filters += filter
    }

    public fun weigher(weigher: HostWeigherSpec) {
        weighers += weigher
    }

    /** Delay deferrable tasks while the carbon intensity is high. */
    public fun timeshift(block: TimeshiftBuilder.() -> Unit = {}) {
        timeshift = TimeshiftBuilder().apply(block).build()
    }

    internal fun build(): FilterAllocationPolicySpec {
        val resolvedFilters = filters.ifEmpty { listOf(ComputeHostFilterSpec) }
        return FilterAllocationPolicySpec(resolvedFilters, weighers.toList(), timeshift)
    }
}

/** Collects the configuration of a [TimeshiftSpec]. */
@SdkDsl
public class TimeshiftBuilder {
    /** The number of past intensities in the moving average. */
    public var windowSize: Int = 168

    /** Whether to compare against the forecast instead of the moving average. */
    public var forecast: Boolean = true

    /** The forecast quantile below which the intensity is low for short tasks. */
    public var shortForecastThreshold: Double = 0.2

    /** The forecast quantile below which the intensity is low for long tasks. */
    public var longForecastThreshold: Double = 0.35

    /** The number of forecasted intensities to compare against. */
    public var forecastSize: Int = 24

    /** The optional policy that pauses running tasks while the carbon intensity is high. */
    public var taskStopper: TaskStopperSpec? = null

    internal fun build(): TimeshiftSpec =
        TimeshiftSpec(windowSize, forecast, shortForecastThreshold, longForecastThreshold, forecastSize, taskStopper)
}
