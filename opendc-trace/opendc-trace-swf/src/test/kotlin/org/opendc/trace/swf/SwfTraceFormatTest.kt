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
import org.opendc.trace.TABLE_TASKS
import org.opendc.trace.TASK_ALLOC_NCPUS
import org.opendc.trace.TASK_ID
import java.net.URL

/**
 * Test suite for the [SwfTraceFormat] class.
 */
internal class SwfTraceFormatTest {
    @Test
    fun testTraceExists() {
        val input = checkNotNull(SwfTraceFormatTest::class.java.getResource("/trace.swf"))
        val format = SwfTraceFormat()
        assertDoesNotThrow {
            format.open(input)
        }
    }

    @Test
    fun testTraceDoesNotExists() {
        val input = checkNotNull(SwfTraceFormatTest::class.java.getResource("/trace.swf"))
        val format = SwfTraceFormat()
        assertThrows<IllegalArgumentException> {
            format.open(URL(input.toString() + "help"))
        }
    }

    @Test
    fun testTables() {
        val input = checkNotNull(SwfTraceFormatTest::class.java.getResource("/trace.swf"))
        val trace = SwfTraceFormat().open(input)

        assertEquals(listOf(TABLE_TASKS), trace.tables)
    }

    @Test
    fun testTableExists() {
        val input = checkNotNull(SwfTraceFormatTest::class.java.getResource("/trace.swf"))
        val table = SwfTraceFormat().open(input).getTable(TABLE_TASKS)

        assertNotNull(table)
        assertDoesNotThrow { table!!.newReader() }
    }

    @Test
    fun testTableDoesNotExist() {
        val input = checkNotNull(SwfTraceFormatTest::class.java.getResource("/trace.swf"))
        val trace = SwfTraceFormat().open(input)

        assertFalse(trace.containsTable("test"))
        assertNull(trace.getTable("test"))
    }

    @Test
    fun testReader() {
        val input = checkNotNull(SwfTraceFormatTest::class.java.getResource("/trace.swf"))
        val trace = SwfTraceFormat().open(input)
        val reader = trace.getTable(TABLE_TASKS)!!.newReader()

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("1", reader.get(TASK_ID)) },
            { assertEquals(306, reader.getInt(TASK_ALLOC_NCPUS)) },
            { assertTrue(reader.nextRow()) },
            { assertEquals("2", reader.get(TASK_ID)) },
            { assertEquals(17, reader.getInt(TASK_ALLOC_NCPUS)) },
        )

        reader.close()
    }

    @Test
    fun testReaderPartition() {
        val input = checkNotNull(SwfTraceFormatTest::class.java.getResource("/trace.swf"))
        val trace = SwfTraceFormat().open(input)

        assertThrows<IllegalArgumentException> {
            trace.getTable(TABLE_TASKS)!!.newReader("test")
        }
    }
}
