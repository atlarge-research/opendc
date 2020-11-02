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

package org.opendc.trace.core

/**
 * A stream of [Event]s.
 */
public interface EventStream : AutoCloseable {
    /**
     * Register the specified [action] to be performed on every event in the stream.
     */
    public fun onEvent(action: (Event) -> Unit)

    /**
     * Register the specified [action] to be performed on events of type [E].
     */
    public fun <E : Event> onEvent(type: Class<E>, action: (E) -> Unit)

    /**
     * Register the specified [action] to be performed on errors.
     */
    public fun onError(action: (Throwable) -> Unit)

    /**
     * Register the specified [action] to be performed when the stream is closed.
     */
    public fun onClose(action: Runnable)

    /**
     * Unregister the specified [action].
     *
     * @return `true` if an action was unregistered, `false` otherwise.
     */
    public fun remove(action: Any): Boolean

    /**
     * Start the processing of events in the current coroutine.
     *
     * @throws IllegalStateException if the stream was already started.
     */
    public suspend fun start()

    /**
     * Release all resources associated with this stream.
     *
     * @throws IllegalStateException if the stream was already stopped.
     */
    public override fun close()
}

/**
 * Register the specified [action] to be performed on events of type [E].
 */
public inline fun <reified E : Event> EventStream.onEvent(noinline action: (E) -> Unit) {
    onEvent(E::class.java, action)
}
