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

import org.opendc.trace.core.internal.EventTracerImpl
import java.time.Clock

/**
 * An [EventTracer] is responsible for recording the events that occur in a system.
 */
public interface EventTracer : AutoCloseable {
    /**
     * The [Clock] used to measure the timestamp and duration of the events.
     */
    public val clock: Clock

    /**
     * Determine whether the specified [Event] class is currently enabled in any of the active recordings.
     *
     * @return `true` if the event is enabled, `false` otherwise.
     */
    public fun isEnabled(type: Class<out Event>): Boolean

    /**
     * Commit the specified [event] to the appropriate event streams.
     */
    public fun commit(event: Event)

    /**
     * Create a new [RecordingStream] which is able to actively capture events emitted to the [EventTracer].
     */
    public fun openRecording(): RecordingStream

    /**
     * Terminate the lifecycle of the [EventTracer] and close its associated event streams.
     */
    public override fun close()

    public companion object {
        /**
         * Construct a new [EventTracer] instance.
         *
         * @param clock The [Clock] used to measure the timestamps.
         */
        @JvmName("create")
        public operator fun invoke(clock: Clock): EventTracer = EventTracerImpl(clock)
    }
}

/**
 * Determine whether the [Event] of type [E] is currently enabled in any of the active recordings.
 *
 * @return `true` if the event is enabled, `false` otherwise.
 */
public inline fun <reified E : Event> EventTracer.isEnabled(): Boolean = isEnabled(E::class.java)

/**
 * Lazily construct an [Event] of type [E] if it is enabled and commit it to the appropriate event streams.
 */
public inline fun <reified E : Event> EventTracer.commit(block: () -> E) {
    if (isEnabled<E>()) {
        commit(block())
    }
}
