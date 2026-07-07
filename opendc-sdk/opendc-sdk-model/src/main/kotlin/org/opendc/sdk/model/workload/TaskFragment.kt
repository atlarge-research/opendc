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

package org.opendc.sdk.model.workload

import kotlinx.serialization.Serializable
import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.common.units.TimeDelta

/**
 * A contiguous slice of a task's execution during which the resource demand is constant.
 *
 * @property duration How long this slice lasts.
 * @property cpuUsage The CPU demand held constant over the slice.
 * @property gpuUsage The GPU compute demand held constant over the slice.
 * @property gpuMemory The GPU memory demand held constant over the slice.
 */
@Serializable
public data class TaskFragment(
    public val duration: TimeDelta,
    public val cpuUsage: Frequency,
    public val gpuUsage: Frequency = Frequency.ofMHz(0),
    public val gpuMemory: DataSize = DataSize.ofBytes(0),
)
