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

package org.opendc.sdk.runner.unit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.opendc.sdk.runner.factory.readScores
import java.nio.file.Path
import kotlin.io.path.writeText

class ReadScoresTest {
    @Test
    fun `skips the header and blank lines`(
        @TempDir dir: Path,
    ) {
        val file = dir.resolve("scores.csv")
        file.writeText("task_id,score\n1, 2.5\n\n2,0.5\n")

        assertEquals(mapOf(1 to 2.5, 2 to 0.5), readScores(file))
    }

    @Test
    fun `rejects a line without a score`(
        @TempDir dir: Path,
    ) {
        val file = dir.resolve("scores.csv")
        file.writeText("task_id,score\n1,2.5\n3\n")

        val error = assertThrows<IllegalArgumentException> { readScores(file) }
        assertTrue(error.message!!.startsWith("Line 3 of"), error.message)
    }
}
