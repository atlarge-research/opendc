/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.trace.swf

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.opendc.trace.conv.TABLE_TASKS
import org.opendc.trace.conv.TASK_ALLOC_NCPUS
import org.opendc.trace.conv.TASK_ID
import java.nio.file.Paths

/**
 * Test suite for the [SwfTraceFormat] class.
 */
internal class SwfTraceFormatTest {
    private val format = SwfTraceFormat()

    @Test
    fun testTables() {
        val path = Paths.get(checkNotNull(SwfTraceFormatTest::class.java.getResource("/trace.swf")).toURI())

        assertEquals(listOf(TABLE_TASKS), format.getTables(path))
    }

    @Test
    fun testTableExists() {
        val path = Paths.get(checkNotNull(SwfTraceFormatTest::class.java.getResource("/trace.swf")).toURI())
        assertDoesNotThrow { format.getDetails(path, TABLE_TASKS) }
    }

    @Test
    fun testTableDoesNotExist() {
        val path = Paths.get(checkNotNull(SwfTraceFormatTest::class.java.getResource("/trace.swf")).toURI())

        assertThrows<IllegalArgumentException> { format.getDetails(path, "test") }
    }

    @Test
    fun testReader() {
        val path = Paths.get(checkNotNull(SwfTraceFormatTest::class.java.getResource("/trace.swf")).toURI())
        val reader = format.newReader(path, TABLE_TASKS, null)

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("1", reader.getString(TASK_ID)) },
            { assertEquals(306, reader.getInt(TASK_ALLOC_NCPUS)) },
            { assertTrue(reader.nextRow()) },
            { assertEquals("2", reader.getString(TASK_ID)) },
            { assertEquals(17, reader.getInt(TASK_ALLOC_NCPUS)) },
        )

        reader.close()
    }
}
