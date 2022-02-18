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

package org.opendc.common.util

import kotlinx.coroutines.*
import java.lang.Runnable
import java.time.Clock
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * Helper class to pace the incoming scheduling requests.
 *
 * @param context The [CoroutineContext] in which the pacer runs.
 * @param clock The virtual simulation clock.
 * @param quantum The scheduling quantum.
 * @param process The process to invoke for the incoming requests.
 */
public class Pacer(
    private val context: CoroutineContext,
    private val clock: Clock,
    private val quantum: Long,
    private val process: (Long) -> Unit
) {
    /**
     * The [Delay] instance that provides scheduled execution of [Runnable]s.
     */
    @OptIn(InternalCoroutinesApi::class)
    private val delay =
        requireNotNull(context[ContinuationInterceptor] as? Delay) { "Invalid CoroutineDispatcher: no delay implementation" }

    /**
     * The current [DisposableHandle] representing the pending scheduling cycle.
     */
    private var handle: DisposableHandle? = null

    /**
     * Determine whether a scheduling cycle is pending.
     */
    public val isPending: Boolean get() = handle != null

    /**
     * Enqueue a new scheduling cycle.
     */
    public fun enqueue() {
        if (handle != null) {
            return
        }

        val quantum = quantum
        val now = clock.millis()

        // We assume that the scheduler runs at a fixed slot every time quantum (e.g t=0, t=60, t=120).
        // We calculate here the delay until the next scheduling slot.
        val timeUntilNextSlot = quantum - (now % quantum)

        @OptIn(InternalCoroutinesApi::class)
        handle = delay.invokeOnTimeout(timeUntilNextSlot, {
            process(now + timeUntilNextSlot)
            handle = null
        }, context)
    }

    /**
     * Cancel the currently pending scheduling cycle.
     */
    public fun cancel() {
        val handle = handle ?: return
        this.handle = null
        handle.dispose()
    }
}
