/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.web.proto

import javax.validation.constraints.DecimalMax
import javax.validation.constraints.DecimalMin

/**
 * The workload to simulate for a scenario.
 */
public data class Workload(val trace: Trace, val samplingFraction: Double) {
    /**
     * Specification for a workload.
     *
     * @param trace The unique identifier of the trace.
     * @param samplingFraction The fraction of the workload to sample.
     */
    public data class Spec(
        val trace: String,
        @DecimalMin(value = "0.001", message = "Sampling fraction must be non-zero")
        @DecimalMax(value = "1", message = "Sampling fraction cannot exceed one")
        val samplingFraction: Double
    )
}
