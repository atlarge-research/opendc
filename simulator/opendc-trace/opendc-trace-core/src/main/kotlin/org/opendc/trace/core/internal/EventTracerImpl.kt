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
import org.opendc.trace.core.EventTracer
import org.opendc.trace.core.RecordingStream
import java.lang.reflect.Modifier
import java.time.Clock
import java.util.*

/**
 * Default implementation of the [EventTracer] interface.
 */
internal class EventTracerImpl(override val clock: Clock) : EventTracer {
    /**
     * The set of enabled events.
     */
    private val enabledEvents = IdentityHashMap<Class<out Event>, MutableList<Stream>>()

    /**
     * The event streams created by the tracer.
     */
    private val streams = WeakHashMap<Stream, Unit>()

    /**
     * A flag to indicate that the stream is closed.
     */
    private var isClosed: Boolean = false

    override fun isEnabled(type: Class<out Event>): Boolean = enabledEvents.containsKey(type)

    override fun commit(event: Event) {
        val type = event.javaClass

        // Assign timestamp if not set
        if (event.timestamp == Long.MIN_VALUE) {
            event.timestamp = clock.millis()
        }

        if (!isEnabled(type) || isClosed) {
            return
        }

        val streams = enabledEvents[type] ?: return
        for (stream in streams) {
            stream.dispatch(event)
        }
    }

    override fun openRecording(): RecordingStream = Stream()

    override fun close() {
        isClosed = true

        val streams = streams
        for ((stream, _) in streams) {
            stream.close()
        }

        enabledEvents.clear()
    }

    /**
     * Enable the specified [type] for the given [stream].
     */
    private fun enableFor(type: Class<out Event>, stream: Stream) {
        val res = enabledEvents.computeIfAbsent(type) { mutableListOf() }
        res.add(stream)
    }

    /**
     * Disable the specified [type] for the given [stream].
     */
    private fun disableFor(type: Class<out Event>, stream: Stream) {
        enabledEvents[type]?.remove(stream)
    }

    /**
     * The [RecordingStream] associated with this [EventTracer] implementation.
     */
    private inner class Stream : AbstractEventStream(), RecordingStream {
        /**
         * The set of enabled events for this stream.
         */
        private val enabledEvents = IdentityHashMap<Class<out Event>, Unit>()

        init {
            streams[this] = Unit
        }

        override fun enable(type: Class<out Event>) {
            validateEventClass(type)

            if (enabledEvents.put(type, Unit) == null && state == StreamState.Started) {
                enableFor(type, this)
            }
        }

        override fun disable(type: Class<out Event>) {
            validateEventClass(type)

            if (enabledEvents.remove(type) != null && state == StreamState.Started) {
                disableFor(type, this)
            }
        }

        override suspend fun start() {
            val enabledEvents = enabledEvents
            for ((event, _) in enabledEvents) {
                enableFor(event, this)
            }

            super.start()
        }

        override fun close() {
            val enabledEvents = enabledEvents
            for ((event, _) in enabledEvents) {
                disableFor(event, this)
            }

            // Remove this stream from the active streams
            streams.remove(this)

            super.close()
        }

        /**
         * Validate the specified event subclass.
         */
        private fun validateEventClass(type: Class<out Event>) {
            require(!Modifier.isAbstract(type.modifiers)) { "Abstract event classes are not allowed" }
            require(Event::class.java.isAssignableFrom(type)) { "Must be subclass to ${Event::class.qualifiedName}" }
        }
    }
}
