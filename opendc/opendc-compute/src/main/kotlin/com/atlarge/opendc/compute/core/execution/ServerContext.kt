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
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.core.services.ServiceKey
import kotlinx.coroutines.selects.SelectClause0
import kotlinx.coroutines.selects.select

/**
 * Represents the execution context in which a bootable [Image] runs on a [Server].
 */
public interface ServerContext {
    /**
     * The server on which the image runs.
     */
    public val server: Server

    /**
     * A list of processing units available to use.
     */
    public val cpus: List<ProcessingUnit>

    /**
     * Publish the specified [service] at the given [ServiceKey].
     */
    public suspend fun <T : Any> publishService(key: ServiceKey<T>, service: T)

    /**
     * Request the specified burst time from the processor cores and suspend execution until a processor core finishes
     * processing a **non-zero** burst or until the deadline is reached.
     *
     * After the method returns, [burst] will contain the remaining burst length for each of the cores (which may be
     * zero).
     *
     * Both [burst] and [limit] must be of the same size and in any other case the method will throw an
     * [IllegalArgumentException].
     *
     * @param burst The burst time to request from each of the processor cores.
     * @param limit The maximum usage in terms of MHz that the processing core may use while running the burst.
     * @param deadline The instant at which this request needs to be fulfilled.
     */
    public suspend fun run(burst: LongArray, limit: DoubleArray, deadline: Long) {
        select<Unit> { onRun(burst, limit, deadline).invoke {} }
    }

    /**
     * Request the specified burst time from the processor cores and suspend execution until a processor core finishes
     * processing a **non-zero** burst or until the deadline is reached.
     *
     * After the method returns, [burst] will contain the remaining burst length for each of the cores (which may be
     * zero).
     *
     * Both [burst] and [limit] must be of the same size and in any other case the method will throw an
     * [IllegalArgumentException].
     *
     * @param burst The burst time to request from each of the processor cores.
     * @param limit The maximum usage in terms of MHz that the processing core may use while running the burst.
     * @param deadline The instant at which this request needs to be fulfilled.
     */
    public fun onRun(burst: LongArray, limit: DoubleArray, deadline: Long): SelectClause0
}
