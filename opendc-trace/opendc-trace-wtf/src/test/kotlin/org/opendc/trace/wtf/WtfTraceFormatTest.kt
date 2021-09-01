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

package org.opendc.trace.wtf

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.trace.*
import java.io.File
import java.net.URL

/**
 * Test suite for the [WtfTraceFormat] class.
 */
class WtfTraceFormatTest {
    @Test
    fun testTraceExists() {
        val input = File("src/test/resources/wtf-trace").toURI().toURL()
        val format = WtfTraceFormat()
        org.junit.jupiter.api.assertDoesNotThrow {
            format.open(input)
        }
    }

    @Test
    fun testTraceDoesNotExists() {
        val input = File("src/test/resources/wtf-trace").toURI().toURL()
        val format = WtfTraceFormat()
        assertThrows<IllegalArgumentException> {
            format.open(URL(input.toString() + "help"))
        }
    }

    @Test
    fun testTables() {
        val input = File("src/test/resources/wtf-trace").toURI().toURL()
        val format = WtfTraceFormat()
        val trace = format.open(input)

        assertEquals(listOf(TABLE_TASKS), trace.tables)
    }

    @Test
    fun testTableExists() {
        val input = File("src/test/resources/wtf-trace").toURI().toURL()
        val format = WtfTraceFormat()
        val table = format.open(input).getTable(TABLE_TASKS)

        assertNotNull(table)
        org.junit.jupiter.api.assertDoesNotThrow { table!!.newReader() }
    }

    @Test
    fun testTableDoesNotExist() {
        val input = File("src/test/resources/wtf-trace").toURI().toURL()
        val format = WtfTraceFormat()
        val trace = format.open(input)

        assertFalse(trace.containsTable("test"))
        assertNull(trace.getTable("test"))
    }

    /**
     * Smoke test for parsing WTF traces.
     */
    @Test
    fun testTableReader() {
        val input = File("src/test/resources/wtf-trace")
        val trace = WtfTraceFormat().open(input.toURI().toURL())
        val reader = trace.getTable(TABLE_TASKS)!!.newReader()

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals(362334516345962206, reader.getLong(TASK_ID)) },
            { assertEquals(1078341553348591493, reader.getLong(TASK_WORKFLOW_ID)) },
            { assertEquals(245604, reader.getLong(TASK_SUBMIT_TIME)) },
            { assertEquals(8163, reader.getLong(TASK_RUNTIME)) },
            { assertEquals(setOf(584055316413447529, 133113685133695608, 1008582348422865408), reader.get(TASK_PARENTS)) },
        )

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals(502010169100446658, reader.getLong(TASK_ID)) },
            { assertEquals(1078341553348591493, reader.getLong(TASK_WORKFLOW_ID)) },
            { assertEquals(251325, reader.getLong(TASK_SUBMIT_TIME)) },
            { assertEquals(8216, reader.getLong(TASK_RUNTIME)) },
            { assertEquals(setOf(584055316413447529, 133113685133695608, 1008582348422865408), reader.get(TASK_PARENTS)) },
        )

        reader.close()
    }

    @Test
    fun testTableReaderPartition() {
        val input = File("src/test/resources/wtf-trace").toURI().toURL()
        val format = WtfTraceFormat()
        val table = format.open(input).getTable(TABLE_TASKS)!!

        assertThrows<IllegalArgumentException> { table.newReader("test") }
    }
}
