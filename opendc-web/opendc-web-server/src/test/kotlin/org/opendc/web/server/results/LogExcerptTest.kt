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

package org.opendc.web.server.results

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Picking one scenario's lines out of a log several scenarios wrote into. */
class LogExcerptTest {
    @Test
    fun `keeps a scenario's records and the stack traces under them, and nothing else`() {
        val excerpt = LogExcerpt.ofScenario(LOG.lineSequence(), scenarioIndex = 1, limit = 100)

        assertEquals(
            listOf(
                "2026-10-05T10:00:01,000 INFO  [scenario=1 seed=0] Main - starting",
                "2026-10-05T10:00:03,000 ERROR [scenario=1 seed=0] Main - The unit failed",
                "java.lang.IllegalStateException: boom",
                "\tat org.opendc.Main.run(Main.kt:12)",
            ),
            excerpt.lines,
        )
        assertEquals(0, excerpt.omitted)
    }

    @Test
    fun `tells scenario 1 apart from scenario 10`() {
        val excerpt = LogExcerpt.ofScenario(LOG.lineSequence(), scenarioIndex = 10, limit = 100)

        assertEquals(listOf("2026-10-05T10:00:04,000 INFO  [scenario=10 seed=2] Main - starting"), excerpt.lines)
    }

    @Test
    fun `keeps the last lines when there are more than the limit, and counts the rest`() {
        val excerpt = LogExcerpt.ofScenario(LOG.lineSequence(), scenarioIndex = 1, limit = 2)

        assertEquals(listOf("java.lang.IllegalStateException: boom", "\tat org.opendc.Main.run(Main.kt:12)"), excerpt.lines)
        assertEquals(2, excerpt.omitted)
    }

    private companion object {
        val LOG =
            """
            2026-10-05T10:00:00,000 INFO  Main - Reading the manifest
            2026-10-05T10:00:01,000 INFO  [scenario=1 seed=0] Main - starting
            2026-10-05T10:00:02,000 INFO  [scenario=2 seed=0] Main - starting
            Caused by: something scenario 2 threw
            2026-10-05T10:00:03,000 ERROR [scenario=1 seed=0] Main - The unit failed
            java.lang.IllegalStateException: boom
            	at org.opendc.Main.run(Main.kt:12)
            2026-10-05T10:00:04,000 INFO  [scenario=10 seed=2] Main - starting
            2026-10-05T10:00:05,000 INFO  Main - Done
            """.trimIndent()
    }
}
