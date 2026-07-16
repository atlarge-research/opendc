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

package org.opendc.sdk.model.scheduler

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue
import org.opendc.sdk.model.validation.prefixed
import org.opendc.sdk.model.validation.validateEach

/**
 * Describes how tasks are placed onto hosts.
 */
@Serializable
public sealed interface AllocationPolicy : Validatable {
    override fun validate(): List<ValidationIssue> = emptyList()
}

/**
 * Selects a named, prefabricated scheduler.
 *
 * @property prefabName The prefabricated scheduler to use.
 */
@Serializable
@SerialName("prefab")
public data class PrefabAllocationPolicy(public val prefabName: SchedulerName = SchedulerName.Mem) : AllocationPolicy

/**
 * Builds a scheduler from a filter-then-weigh pipeline.
 *
 * @property filters The eligibility predicates applied to candidate hosts.
 * @property weighers The scorers used to rank the remaining candidates.
 * @property subsetSize The size of the top-ranked subset sampled from for placement.
 */
@Serializable
@SerialName("filter")
public data class FilterAllocationPolicy(
    public val filters: List<HostFilter> = listOf(ComputeHostFilter),
    public val weighers: List<HostWeigher> = emptyList(),
    public val subsetSize: Int = 1,
) : AllocationPolicy {
    override fun validate(): List<ValidationIssue> =
        buildList {
            if (subsetSize <= 0) add(ValidationIssue("subsetSize", "must be > 0"))
            addAll(filters.validateEach("filters"))
            addAll(weighers.validateEach("weighers"))
        }
}

/**
 * A filter-and-weigh scheduler that defers tasks based on a (forecasted) carbon signal.
 *
 * @property filters The eligibility predicates applied to candidate hosts.
 * @property weighers The scorers used to rank the remaining candidates.
 * @property windowSize The number of past samples considered by the carbon signal.
 * @property subsetSize The size of the top-ranked subset sampled from for placement.
 * @property forecast Whether to base decisions on forecasted rather than historical values.
 * @property shortForecastThreshold The normalized threshold for the short forecast horizon.
 * @property longForecastThreshold The normalized threshold for the long forecast horizon.
 * @property forecastSize The number of future samples to forecast.
 * @property taskStopper The optional policy controlling when deferrable tasks are paused.
 * @property memorize Whether to memoize scheduling decisions across invocations.
 */
@Serializable
@SerialName("timeshift")
public data class TimeShiftAllocationPolicy(
    public val filters: List<HostFilter> = listOf(ComputeHostFilter),
    public val weighers: List<HostWeigher> = emptyList(),
    public val windowSize: Int = 168,
    public val subsetSize: Int = 1,
    public val forecast: Boolean = true,
    public val shortForecastThreshold: Double = 0.2,
    public val longForecastThreshold: Double = 0.35,
    public val forecastSize: Int = 24,
    public val taskStopper: TaskStopperSpec? = null,
    public val memorize: Boolean = true,
) : AllocationPolicy {
    override fun validate(): List<ValidationIssue> =
        buildList {
            if (subsetSize <= 0) add(ValidationIssue("subsetSize", "must be > 0"))
            if (windowSize <= 0) add(ValidationIssue("windowSize", "must be > 0"))
            if (forecastSize <= 0) add(ValidationIssue("forecastSize", "must be > 0"))
            if (shortForecastThreshold !in 0.0..1.0) add(ValidationIssue("shortForecastThreshold", "must be in 0.0..1.0"))
            if (longForecastThreshold !in 0.0..1.0) add(ValidationIssue("longForecastThreshold", "must be in 0.0..1.0"))
            addAll(filters.validateEach("filters"))
            addAll(weighers.validateEach("weighers"))
            addAll(taskStopper?.validate().orEmpty().prefixed("taskStopper"))
        }
}
