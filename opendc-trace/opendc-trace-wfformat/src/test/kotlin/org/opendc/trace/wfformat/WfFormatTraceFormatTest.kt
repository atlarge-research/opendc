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

package org.opendc.trace.wfformat

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.opendc.trace.*
import java.io.File
import java.net.URL

/**
 * Test suite for the [WfFormatTraceFormat] class.
 */
class WfFormatTraceFormatTest {
    @Test
    fun testTraceExists() {
        val input = File("src/test/resources/trace.json").toURI().toURL()
        val format = WfFormatTraceFormat()
        assertDoesNotThrow { format.open(input) }
    }

    @Test
    fun testTraceDoesNotExists() {
        val input = File("src/test/resources/trace.json").toURI().toURL()
        val format = WfFormatTraceFormat()
        assertThrows<IllegalArgumentException> { format.open(URL(input.toString() + "help")) }
    }

    @Test
    fun testTables() {
        val input = File("src/test/resources/trace.json").toURI().toURL()
        val format = WfFormatTraceFormat()
        val trace = format.open(input)

        assertEquals(listOf(TABLE_TASKS), trace.tables)
    }

    @Test
    fun testTableExists() {
        val input = File("src/test/resources/trace.json").toURI().toURL()
        val format = WfFormatTraceFormat()
        val table = format.open(input).getTable(TABLE_TASKS)

        assertNotNull(table)
        assertDoesNotThrow { table!!.newReader() }
    }

    @Test
    fun testTableDoesNotExist() {
        val input = File("src/test/resources/trace.json").toURI().toURL()
        val format = WfFormatTraceFormat()
        val trace = format.open(input)

        assertFalse(trace.containsTable("test"))
        assertNull(trace.getTable("test"))
    }

    /**
     * Smoke test for parsing WfCommons traces.
     */
    @Test
    fun testTableReader() {
        val input = File("src/test/resources/trace.json").toURI().toURL()
        val trace = WfFormatTraceFormat().open(input)
        val reader = trace.getTable(TABLE_TASKS)!!.newReader()

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("makebwaindex_mammoth_mt_krause.fasta", reader.get(TASK_ID)) },
            { assertEquals("eager-nextflow-chameleon", reader.get(TASK_WORKFLOW_ID)) },
            { assertEquals(172000, reader.get(TASK_RUNTIME).toMillis()) },
            { assertEquals(emptySet<String>(), reader.get(TASK_PARENTS)) },
        )

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("makeseqdict_mammoth_mt_krause.fasta", reader.get(TASK_ID)) },
            { assertEquals("eager-nextflow-chameleon", reader.get(TASK_WORKFLOW_ID)) },
            { assertEquals(175000, reader.get(TASK_RUNTIME).toMillis()) },
            { assertEquals(setOf("makebwaindex_mammoth_mt_krause.fasta"), reader.get(TASK_PARENTS)) },
        )

        reader.close()
    }

    /**
     * Test full iteration of the table.
     */
    @Test
    fun testTableReaderFull() {
        val input = File("src/test/resources/trace.json").toURI().toURL()
        val trace = WfFormatTraceFormat().open(input)
        val reader = trace.getTable(TABLE_TASKS)!!.newReader()

        assertDoesNotThrow {
            while (reader.nextRow()) {
                // reader.get(TASK_ID)
            }
            reader.close()
        }
    }

    @Test
    fun testTableReaderPartition() {
        val input = File("src/test/resources/trace.json").toURI().toURL()
        val format = WfFormatTraceFormat()
        val table = format.open(input).getTable(TABLE_TASKS)!!

        assertThrows<IllegalArgumentException> { table.newReader("test") }
    }
}
