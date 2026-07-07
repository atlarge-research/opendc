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

import kotlinx.serialization.Serializable
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue

/**
 * Configures when deferrable tasks are paused based on a (forecasted) carbon signal.
 *
 * @property windowSize The number of past samples considered when deciding to stop tasks.
 * @property forecast Whether to base the decision on forecasted rather than historical values.
 * @property forecastThreshold The normalized threshold above which tasks are stopped.
 * @property forecastSize The number of future samples to forecast.
 */
@Serializable
public data class TaskStopper(
    public val windowSize: Int = 168,
    public val forecast: Boolean = true,
    public val forecastThreshold: Double = 0.6,
    public val forecastSize: Int = 24,
) : Validatable {
    override fun validate(): List<ValidationIssue> =
        buildList {
            if (windowSize < 1) add(ValidationIssue("windowSize", "must be >= 1"))
            if (forecastSize < 1) add(ValidationIssue("forecastSize", "must be >= 1"))
            if (forecastThreshold !in 0.0..1.0) add(ValidationIssue("forecastThreshold", "must be in 0.0..1.0"))
        }
}
