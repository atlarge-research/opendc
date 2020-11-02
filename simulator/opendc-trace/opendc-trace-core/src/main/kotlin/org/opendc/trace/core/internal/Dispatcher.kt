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

import org.opendc.trace.core.Event

/**
 * The [Dispatcher] is responsible for dispatching events onto configured actions.
 */
internal class Dispatcher {
    /**
     * The event actions to dispatch to.
     */
    private val eventActions = mutableListOf<EventDispatcher>()

    /**
     * The error actions to use.
     */
    private val errorActions = mutableListOf<(Throwable) -> Unit>()

    /**
     * Dispatch the specified [event].
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
}
