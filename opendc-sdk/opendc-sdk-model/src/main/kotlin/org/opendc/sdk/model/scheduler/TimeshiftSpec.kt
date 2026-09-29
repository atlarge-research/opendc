/*
 * Copyright (c) 2026 AtLarge Research
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

import kotlinx.serialization.Serializable
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue
import org.opendc.sdk.model.validation.prefixed

/**
 * Delays deferrable tasks while the carbon intensity is high, as long as they can still meet their deadline.
 *
 * Tasks shorter than two hours follow a short regime and longer tasks a long regime. With [forecast], the intensity is
 * low for a regime when it is below a quantile of the forecast; otherwise, when it is below the moving average of the
 * past [windowSize] intensities.
 *
 * @property windowSize The number of past intensities in the moving average.
 * @property forecast Whether to compare against the forecast instead of the moving average.
 * @property shortForecastThreshold The forecast quantile below which the intensity is low for short tasks.
 * @property longForecastThreshold The forecast quantile below which the intensity is low for long tasks.
 * @property forecastSize The number of forecasted intensities to compare against.
 * @property taskStopper The optional policy that pauses running tasks while the carbon intensity is high, so they wait
 *   for a better moment as well.
 */
@Serializable
public data class TimeshiftSpec(
    public val windowSize: Int = 168,
    public val forecast: Boolean = true,
    public val shortForecastThreshold: Double = 0.2,
    public val longForecastThreshold: Double = 0.35,
    public val forecastSize: Int = 24,
    public val taskStopper: TaskStopperSpec? = null,
) : Validatable {
    override fun validate(): List<ValidationIssue> =
        buildList {
            if (windowSize <= 0) add(ValidationIssue("windowSize", "must be > 0"))
            if (forecastSize <= 0) add(ValidationIssue("forecastSize", "must be > 0"))
            if (shortForecastThreshold !in 0.0..1.0) add(ValidationIssue("shortForecastThreshold", "must be in 0.0..1.0"))
            if (longForecastThreshold !in 0.0..1.0) add(ValidationIssue("longForecastThreshold", "must be in 0.0..1.0"))
            addAll(taskStopper?.validate().orEmpty().prefixed("taskStopper"))
        }
}
