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

package org.opendc.trace.parquet

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.nio.file.Files
import java.nio.file.Path

/**
 * Test suite for the [LocalInputFile].
 */
class LocalInputFileTest {
    private lateinit var path: Path
    private val content = ByteArray(10_000) { it.toByte() }

    @BeforeEach
    fun setUp() {
        path = Files.createTempFile("opendc-input", ".bin")
        Files.write(path, content)
    }

    @AfterEach
    fun tearDown() {
        Files.deleteIfExists(path)
    }

    /**
     * Test whether a range is read into the given part of an array, up to the end of the file.
     */
    @Test
    fun testReadRange() {
        LocalInputFile(path).newStream().use { stream ->
            val buf = ByteArray(200)
            val read = stream.read(buf, 50, 100)

            stream.seek(9_950)
            val end = ByteArray(100)
            val readAtEnd = stream.read(end, 0, 100)
            val readAfterEnd = stream.read(end, 0, 100)

            assertAll(
                { assertEquals(100, read) },
                { assertArrayEquals(content.copyOfRange(0, 100), buf.copyOfRange(50, 150)) },
                { assertEquals(50, readAtEnd) },
                { assertArrayEquals(content.copyOfRange(9_950, 10_000), end.copyOfRange(0, 50)) },
                { assertEquals(-1, readAfterEnd) },
                { assertEquals(0, stream.read(buf, 0, 0)) },
            )
        }
    }
}
