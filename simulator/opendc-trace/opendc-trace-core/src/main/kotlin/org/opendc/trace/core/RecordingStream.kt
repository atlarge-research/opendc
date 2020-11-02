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
 * A recording stream that produces events from an [EventTracer].
 */
public interface RecordingStream : EventStream {
    /**
     * Enable recording of the specified event [type].
     */
    public fun enable(type: Class<out Event>)

    /**
     * Disable recording of the specified event [type]
     */
    public fun disable(type: Class<out Event>)
}

/**
 * Enable recording of events of type [E].
 */
public inline fun <reified E : Event> RecordingStream.enable() {
    enable(E::class.java)
}

/**
 * Disable recording of events of type [E].
 */
public inline fun <reified E : Event> RecordingStream.disable() {
    enable(E::class.java)
}
