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
     * Ask the processor cores to run the specified [slice] and suspend execution until the trigger condition is met as
     * specified by [triggerMode].
     *
     * After the method returns, [Slice.burst] will contain the remaining burst length for each of the cores (which
     * may be zero). These changes may happen anytime during execution of this method and callers should not rely on
     * the timing of this change.
     *
     * @param slice The representation of work to run on the processors.
     * @param triggerMode The trigger condition to resume execution.
     */
    public suspend fun run(slice: Slice, triggerMode: TriggerMode = TriggerMode.FIRST) =
        select<Unit> { onRun(slice, triggerMode).invoke {} }

    /**
     * Ask the processors cores to run the specified [batch] of work slices and suspend execution until the trigger
     * condition is met as specified by [triggerMode].
     *
     * After the method returns, [Slice.burst] will contain the remaining burst length for each of the cores (which
     * may be zero). These changes may happen anytime during execution of this method and callers should not rely on
     * the timing of this change.
     *
     * In case slices in the batch do not finish processing before their deadline, [merge] is called to merge these
     * slices with the next slice to be executed.
     *
     * @param batch The batch of work to run on the processors.
     * @param triggerMode The trigger condition to resume execution.
     * @param merge The merge function for consecutive slices in case the last slice was not completed within its
     * deadline.
     */
    public suspend fun run(
        batch: List<Slice>,
        triggerMode: TriggerMode = TriggerMode.FIRST,
        merge: (Slice, Slice) -> Slice = { _, r -> r }
    ) = select<Unit> { onRun(batch, triggerMode, merge).invoke {} }

    /**
     * Ask the processor cores to run the specified [slice] and select when the trigger condition is met as specified
     * by [triggerMode].
     *
     * After the method returns, [Slice.burst] will contain the remaining burst length for each of the cores (which
     * may be zero). These changes may happen anytime during execution of this method and callers should not rely on
     * the timing of this change.
     *
     * @param slice The representation of work to request from the processors.
     * @param triggerMode The trigger condition to resume execution.
     */
    public fun onRun(slice: Slice, triggerMode: TriggerMode = TriggerMode.FIRST): SelectClause0 =
        onRun(listOf(slice), triggerMode)

    /**
     * Ask the processors cores to run the specified [batch] of work slices and select when the trigger condition is met
     * as specified by [triggerMode].
     *
     * After the method returns, [Slice.burst] will contain the remaining burst length for each of the cores (which
     * may be zero). These changes may happen anytime during execution of this method and callers should not rely on
     * the timing of this change.
     *
     * In case slices in the batch do not finish processing before their deadline, [merge] is called to merge these
     * slices with the next slice to be executed.
     *
     * @param batch The batch of work to run on the processors.
     * @param triggerMode The trigger condition to resume execution during the **last** slice.
     * @param merge The merge function for consecutive slices in case the last slice was not completed within its
     * deadline.
     */
    public fun onRun(
        batch: List<Slice>,
        triggerMode: TriggerMode = TriggerMode.FIRST,
        merge: (Slice, Slice) -> Slice = { _, r -> r }
    ): SelectClause0

    /**
     * A request to the host machine for a slice of CPU time from the processor cores.
     *
     * Both [burst] and [limit] must be of the same size and in any other case the method will throw an
     * [IllegalArgumentException].
     *
     *
     * @param burst The burst time to request from each of the processor cores.
     * @param limit The maximum usage in terms of MHz that the processing core may use while running the burst.
     * @param deadline The instant at which this slice needs to be fulfilled.
     */
    public class Slice(val burst: LongArray, val limit: DoubleArray, val deadline: Long) {
        init {
            require(burst.size == limit.size) { "Incompatible array dimensions" }
        }
    }

    /**
     * The modes for triggering a machine exit from the machine.
     */
    public enum class TriggerMode {
        /**
         * A machine exit occurs when either the first processor finishes processing a **non-zero** burst or the
         * deadline is reached.
         */
        FIRST,

        /**
         * A machine exit occurs when either the last processor finishes processing a **non-zero** burst or the deadline
         * is reached.
         */
        LAST,

        /**
         * A machine exit occurs only when the deadline is reached.
         */
        DEADLINE
    }
}
