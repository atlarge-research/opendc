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

package org.opendc.sdk.model.checkpoint

import kotlinx.serialization.Serializable
import org.opendc.common.units.TimeDelta
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue

/**
 * Configuration for periodic checkpointing of running tasks.
 *
 * @property interval Wall-clock time between consecutive checkpoints.
 * @property duration Time it takes to write a single checkpoint.
 * @property intervalScaling Multiplier applied to [interval] after each checkpoint.
 */
@Serializable
public data class CheckpointSpec(
    public val interval: TimeDelta = TimeDelta.ofHours(1),
    public val duration: TimeDelta = TimeDelta.ofMin(5),
    public val intervalScaling: Double = 1.0,
) : Validatable {
    override fun validate(): List<ValidationIssue> =
        buildList {
            if (interval.value <= 0.0) add(ValidationIssue("interval", "must be greater than zero"))
            if (duration.value <= 0.0) add(ValidationIssue("duration", "must be greater than zero"))
            if (intervalScaling <= 0.0) add(ValidationIssue("intervalScaling", "must be greater than zero"))
        }
}
