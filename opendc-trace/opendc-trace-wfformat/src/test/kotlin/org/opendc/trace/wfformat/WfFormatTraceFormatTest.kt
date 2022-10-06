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

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.opendc.trace.TableColumn
import org.opendc.trace.TableReader
import org.opendc.trace.conv.TABLE_TASKS
import org.opendc.trace.conv.TASK_ID
import org.opendc.trace.conv.TASK_PARENTS
import org.opendc.trace.conv.TASK_RUNTIME
import org.opendc.trace.conv.TASK_WORKFLOW_ID
import org.opendc.trace.testkit.TableReaderTestKit
import java.nio.file.Paths

/**
 * Test suite for the [WfFormatTraceFormat] class.
 */
@DisplayName("WfFormat TraceFormat")
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
        assertDoesNotThrow { format.getDetails(path, TABLE_TASKS) }
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
            { assertEquals("makebwaindex_mammoth_mt_krause.fasta", reader.getString(TASK_ID)) },
            { assertEquals("eager-nextflow-chameleon", reader.getString(TASK_WORKFLOW_ID)) },
            { assertEquals(172000, reader.getDuration(TASK_RUNTIME)?.toMillis()) },
            { assertEquals(emptySet<String>(), reader.getSet(TASK_PARENTS, String::class.java)) }
        )

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("makeseqdict_mammoth_mt_krause.fasta", reader.getString(TASK_ID)) },
            { assertEquals("eager-nextflow-chameleon", reader.getString(TASK_WORKFLOW_ID)) },
            { assertEquals(175000, reader.getDuration(TASK_RUNTIME)?.toMillis()) },
            { assertEquals(setOf("makebwaindex_mammoth_mt_krause.fasta"), reader.getSet(TASK_PARENTS, String::class.java)) }
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

    @DisplayName("TableReader for Tasks")
    @Nested
    inner class TasksTableReaderTest : TableReaderTestKit() {
        override lateinit var reader: TableReader
        override lateinit var columns: List<TableColumn>

        @BeforeEach
        fun setUp() {
            val path = Paths.get("src/test/resources/trace.json")

            columns = format.getDetails(path, TABLE_TASKS).columns
            reader = format.newReader(path, TABLE_TASKS, null)
        }
    }
}
