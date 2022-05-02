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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.opendc.trace.conv.*
import java.nio.file.Paths

/**
 * Test suite for the [WfFormatTraceFormat] class.
 */
class WfFormatTraceFormatTest {
    private val format = WfFormatTraceFormat()

    @Test
    fun testTables() {
        val path = Paths.get("src/test/resources/trace.json")

        assertEquals(listOf(TABLE_TASKS), format.getTables(path))
    }

    @Test
    fun testTableExists() {
        val path = Paths.get("src/test/resources/trace.json")
        Assertions.assertDoesNotThrow { format.getDetails(path, TABLE_TASKS) }
    }

    @Test
    fun testTableDoesNotExist() {
        val path = Paths.get("src/test/resources/trace.json")

        assertThrows<IllegalArgumentException> { format.getDetails(path, "test") }
    }

    /**
     * Smoke test for parsing WfCommons traces.
     */
    @Test
    fun testTableReader() {
        val path = Paths.get("src/test/resources/trace.json")
        val reader = format.newReader(path, TABLE_TASKS, null)

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
        val path = Paths.get("src/test/resources/trace.json")
        val reader = format.newReader(path, TABLE_TASKS, null)

        assertDoesNotThrow {
            while (reader.nextRow()) {
                // reader.get(TASK_ID)
            }
            reader.close()
        }
    }
}
