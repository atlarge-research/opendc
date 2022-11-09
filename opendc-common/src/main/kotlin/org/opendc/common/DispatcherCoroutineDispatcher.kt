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

package org.opendc.common

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlin.coroutines.CoroutineContext

/**
 * A [CoroutineDispatcher] that uses a [Dispatcher] to dispatch pending co-routines.
 *
 * @param dispatcher The [Dispatcher] used to manage the execution of future tasks.
 */
@OptIn(InternalCoroutinesApi::class)
internal class DispatcherCoroutineDispatcher(private val dispatcher: Dispatcher) : CoroutineDispatcher(), Delay, DispatcherProvider {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        block.run()
    }

    override fun dispatchYield(context: CoroutineContext, block: Runnable) {
        dispatcher.schedule(block)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        dispatcher.schedule(timeMillis, CancellableContinuationRunnable(continuation) { resumeUndispatched(Unit) })
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        val handle = dispatcher.scheduleCancellable(timeMillis, block)
        return DisposableHandle { handle.cancel() }
    }

    override fun getDispatcher(): Dispatcher = dispatcher

    override fun toString(): String {
        return "DispatcherCoroutineDispatcher[dispatcher=$dispatcher]"
    }

    /**
     * This class exists to allow cleanup code to avoid throwing for cancelled continuations scheduled
     * in the future.
     */
    private class CancellableContinuationRunnable<T>(
        @JvmField val continuation: CancellableContinuation<T>,
        private val block: CancellableContinuation<T>.() -> Unit
    ) : Runnable {
        override fun run() = continuation.block()
    }
}
