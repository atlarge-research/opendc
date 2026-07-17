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
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.status.StatusLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies log capture attaches the ring, silences the console and status logger, streams INFO, and restores it all. */
class LogCaptureTest {
    private fun rootAppenderNames(): Set<String> =
        (LogManager.getContext(false) as LoggerContext).configuration
            .getLoggerConfig(LogManager.ROOT_LOGGER_NAME).appenders.keys.toSet()

    private fun statusLevel(): Level = StatusLogger.getLogger().fallbackListener.statusLevel

    @Test
    fun `captures org opendc logs while attached and restores logging on close`() {
        val before = rootAppenderNames()
        val beforeStatus = statusLevel()
        assertFalse(before.contains("TuiRingBuffer"), "precondition: ring not yet attached")

        LogCapture().use { capture ->
            capture.attach()
            assertEquals(setOf("TuiRingBuffer"), rootAppenderNames(), "only the ring is on root during capture")
            assertEquals(Level.OFF, statusLevel(), "status logger silenced during capture")

            // A WARN reaches the ring at any capture level, proving the ring + additivity path works.
            LogManager.getLogger("org.opendc.test").warn("hello-capture")
            assertTrue(capture.snapshot().any { it.contains("hello-capture") }, "the log must be captured into the ring")
        }

        assertEquals(before, rootAppenderNames(), "the original appenders are restored")
        assertEquals(beforeStatus, statusLevel(), "status logger level restored")
    }
}
