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

package org.opendc.web.server.storage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** How a file is cut up. The rest of the multipart path needs a real bucket, so it is not tested here. */
class S3ObjectStoreTest {
    private val partSize = 32L * 1024 * 1024

    @Test
    fun `a file that fits in one part is sent in one piece`() {
        assertEquals(listOf(0L..<partSize), planParts(partSize))
        assertEquals(listOf(0L..<1L), planParts(1))
    }

    @Test
    fun `a file past the part size is cut into as many parts as it takes`() {
        assertEquals(2, planParts(partSize + 1).size)
        assertEquals(15, planParts(465L * 1024 * 1024).size)
    }

    @Test
    fun `the parts cover the file exactly, in order, with nothing missed and nothing sent twice`() {
        val size = 465L * 1024 * 1024 + 12345

        val parts = planParts(size)

        assertEquals(0L, parts.first().first)
        assertEquals(size - 1, parts.last().last)
        assertEquals(size, parts.sumOf { it.length() })
        assertTrue(parts.zipWithNext().all { (before, after) -> after.first == before.last + 1 }, "parts are not contiguous")
    }

    // Full parts are how S3ObjectStore recognises an upload with a part missing.
    @Test
    fun `every part but the last carries a full slice`() {
        for (size in listOf(partSize + 1, 3 * partSize, 3 * partSize - 1, 465L * 1024 * 1024)) {
            val lengths = planParts(size).map { it.length() }

            assertEquals(listOf(partSize), lengths.dropLast(1).distinct(), "a part before the last is short at size $size")
            assertTrue(lengths.last() <= partSize, "the last part is oversized at size $size")
        }
    }

    @Test
    fun `a file too large for the maximum number of parts gets larger parts instead`() {
        val size = 10L * 1024 * 1024 * 1024 * 1024

        val parts = planParts(size)

        assertTrue(parts.size <= 10_000, "S3 refuses more than 10000 parts, planned ${parts.size}")
        assertTrue(parts.first().length() > partSize, "the parts should have grown")
        assertEquals(size, parts.sumOf { it.length() })
    }

    private fun LongRange.length(): Long = last - first + 1
}
