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

package org.opendc.web.dispatcher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Every platform reads a launcher's exit code through the same table, so a code read wrongly here is
 * a failure retried forever or a fixable one given up on, on every platform at once.
 */
class ExitClassificationTest {
    @Test
    fun `reads the launcher's own codes the same way on every platform`() {
        assertEquals(ExitReason.OK, launcherExitReason(0))
        assertEquals(ExitReason.OK, launcherExitReason(23), "units failing is the units' business, not the process's")
        assertEquals(ExitReason.INVALID_SPEC, launcherExitReason(20))
        assertEquals(ExitReason.SIMULATION_ERROR, launcherExitReason(21))
        assertEquals(ExitReason.UNKNOWN, launcherExitReason(22), "a transfer that failed may succeed next time")
        assertEquals(ExitReason.OOM, launcherExitReason(JVM_OUT_OF_MEMORY))
        assertEquals(ExitReason.UNKNOWN, launcherExitReason(1))
    }

    @Test
    fun `keeps the end of a long log and starts it on a whole line`() {
        val line = "x".repeat(99)
        val log = (0 until 5000).joinToString("\n") { "$it $line" }

        val tail = logTail(log)

        assertTrue(tail.encodeToByteArray().size <= LOG_TAIL_BYTES)
        assertTrue(tail.endsWith("4999 $line"), "the last line is the one most worth keeping")
        assertTrue(tail.first().isDigit() && log.contains("\n$tail"), "the tail starts where a line does")
    }

    @Test
    fun `keeps a short log whole`() {
        assertEquals("one\ntwo", logTail("one\ntwo"))
    }
}
