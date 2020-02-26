/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.compute.core.execution

import com.atlarge.opendc.compute.core.ProcessingUnit

/**
 * An interface for managing a single processing core (CPU) of a (virtual) machine.
 */
public interface ProcessorContext {
    /**
     * The information about the processing unit.
     */
    public val info: ProcessingUnit

    /**
     * Request the specified burst time from the processor and suspend execution until the processor finishes
     * processing of the requested burst.
     *
     * @param burst The burst time to request from the processor.
     * @param maxUsage The maximum usage in terms of MHz that the processing core may use while running the burst.
     * @param deadline The instant at which this request needs to be fulfilled.
     * @return The remaining burst time in case the method was cancelled or zero if the processor finished running.
     */
    public suspend fun run(burst: Long, maxUsage: Double, deadline: Long): Long
}
