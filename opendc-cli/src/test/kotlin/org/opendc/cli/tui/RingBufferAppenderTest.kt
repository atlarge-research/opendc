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

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.impl.Log4jLogEvent
import org.apache.logging.log4j.core.layout.PatternLayout
import org.apache.logging.log4j.message.SimpleMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Verifies the log ring buffer is bounded, formats events, and is safe under concurrent appends. */
class RingBufferAppenderTest {
    private fun layout(): PatternLayout =
        PatternLayout.newBuilder()
            .withPattern("%-5level %logger{1} %msg")
            .withConfiguration((LogManager.getContext(false) as LoggerContext).configuration)
            .build()

    private fun event(message: String): LogEvent =
        Log4jLogEvent.newBuilder()
            .setLoggerName("org.opendc.compute.simulator.Guest")
            .setLevel(Level.INFO)
            .setMessage(SimpleMessage(message))
            .build()

    @Test
    fun `keeps only the most recent lines`() {
        val appender = RingBufferAppender(layout(), capacity = 100)
        repeat(150) { appender.append(event("start $it")) }

        val snapshot = appender.snapshot()
        assertEquals(100, snapshot.size)
        assertTrue(snapshot.first().contains("start 50"), snapshot.first())
        assertTrue(snapshot.last().contains("start 149"), snapshot.last())
        appender.stop()
    }

    @Test
    fun `formats the level, logger and message`() {
        val appender = RingBufferAppender(layout(), capacity = 10)
        appender.append(event("start 5"))

        val line = appender.snapshot().single()
        assertTrue(line.startsWith("INFO"), line)
        assertTrue(line.contains("Guest"), line)
        assertTrue(line.contains("start 5"), line)
        appender.stop()
    }

    @Test
    fun `is safe under concurrent appends`() {
        val appender = RingBufferAppender(layout(), capacity = 500)
        val threads =
            (0 until 8).map { t ->
                Thread { repeat(1000) { appender.append(event("t$t-$it")) } }
            }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(500, appender.snapshot().size)
        appender.stop()
    }
}
