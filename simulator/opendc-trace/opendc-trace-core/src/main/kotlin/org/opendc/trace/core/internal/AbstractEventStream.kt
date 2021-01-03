/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.trace.core.internal

import kotlinx.coroutines.suspendCancellableCoroutine
import org.opendc.trace.core.Event
import org.opendc.trace.core.EventStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * Base implementation of the [EventStream] implementation.
 */
internal abstract class AbstractEventStream : EventStream {
    /**
     * The state of the stream.
     */
    protected var state = StreamState.Pending

    /**
     * The event actions to dispatch to.
     */
    private val eventActions = mutableListOf<EventDispatcher>()

    /**
     * The error actions to use.
     */
    private val errorActions = mutableListOf<(Throwable) -> Unit>()

    /**
     * The close actions to use.
     */
    private val closeActions = mutableListOf<Runnable>()

    /**
     * The continuation that is invoked when the stream closes.
     */
    private var cont: Continuation<Unit>? = null

    /**
     * Dispatch the specified [event] to this stream.
     */
    fun dispatch(event: Event) {
        val actions = eventActions

        // TODO Opportunity for further optimizations if needed (e.g. dispatch based on event type)
        for (action in actions) {
            if (!action.accepts(event)) {
                continue
            }

            try {
                action(event)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Handle the specified [throwable] that occurred while dispatching an event.
     */
    private fun handleError(throwable: Throwable) {
        val actions = errorActions

        // Default exception handler
        if (actions.isEmpty()) {
            throwable.printStackTrace()
            return
        }

        for (action in actions) {
            action(throwable)
        }
    }

    override fun onEvent(action: (Event) -> Unit) {
        eventActions += EventDispatcher(null, action)
    }

    override fun <E : Event> onEvent(type: Class<E>, action: (E) -> Unit) {
        @Suppress("UNCHECKED_CAST") // This cast must succeed
        eventActions += EventDispatcher(type, action as (Event) -> Unit)
    }

    override fun onError(action: (Throwable) -> Unit) {
        errorActions += action
    }

    override fun onClose(action: Runnable) {
        closeActions += action
    }

    override fun remove(action: Any): Boolean {
        return eventActions.removeIf { it.action == action } || errorActions.remove(action) || closeActions.remove(action)
    }

    override suspend fun start() {
        check(state == StreamState.Pending) { "Stream has already started/closed" }

        state = StreamState.Started

        return suspendCancellableCoroutine { cont -> this.cont = cont }
    }

    override fun close() {
        if (state != StreamState.Closed) {
            return
        }

        state = StreamState.Closed
        cont?.resume(Unit)

        val actions = closeActions
        for (action in actions) {
            action.run()
        }
    }
}
