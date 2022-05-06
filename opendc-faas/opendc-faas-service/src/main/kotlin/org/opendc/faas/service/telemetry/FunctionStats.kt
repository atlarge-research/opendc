/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.faas.service.telemetry

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics

/**
 * Statistics about function invocations.
 *
 * @property totalInvocations The number of function invocations.
 * @property timelyInvocations The number of function invocations that could be handled directly.
 * @property delayedInvocations The number of function invocations that are delayed (cold starts).
 * @property failedInvocations The number of function invocations that failed.
 * @property activeInstances The number of active function instances.
 * @property idleInstances The number of idle function instances.
 * @property waitTime Statistics about the wait time of a function invocation.
 * @property activeTime Statistics about the runtime of a function invocation.
 */
public data class FunctionStats(
    val totalInvocations: Long,
    val timelyInvocations: Long,
    val delayedInvocations: Long,
    val failedInvocations: Long,
    val activeInstances: Int,
    val idleInstances: Int,
    val waitTime: DescriptiveStatistics,
    val activeTime: DescriptiveStatistics
)
