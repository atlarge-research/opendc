/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.cli.tui

import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.apache.logging.log4j.core.layout.PatternLayout
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

/**
 * A log4j appender that keeps the most recent [capacity] formatted log lines in a bounded,
 * thread-safe ring buffer. Simulation threads push into it via [append] while the dashboard's render
 * thread reads the tail via [snapshot]; both are lock-free.
 */
internal class RingBufferAppender(
    private val patternLayout: PatternLayout,
    private val capacity: Int = RING_CAPACITY,
) : AbstractAppender("TuiRingBuffer", null, patternLayout, true, Property.EMPTY_ARRAY) {
    private val lines = ConcurrentLinkedDeque<String>()
    private val count = AtomicInteger(0)

    init {
        start()
    }

    override fun append(event: LogEvent) {
        lines.addLast(patternLayout.toSerializable(event).trimEnd('\n', '\r'))
        if (count.incrementAndGet() > capacity && lines.pollFirst() != null) {
            count.decrementAndGet()
        }
    }

    /** An immutable copy of the buffered lines, oldest first. Non-draining: safe to poll every frame. */
    fun snapshot(): List<String> = ArrayList(lines)

    private companion object {
        const val RING_CAPACITY = 500
    }
}
