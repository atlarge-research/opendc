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

package org.opendc.trace.gwf

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.trace.TableColumn
import org.opendc.trace.TableReader
import org.opendc.trace.conv.TABLE_TASKS
import org.opendc.trace.conv.TASK_ID
import org.opendc.trace.conv.TASK_PARENTS
import org.opendc.trace.conv.TASK_RUNTIME
import org.opendc.trace.conv.TASK_SUBMIT_TIME
import org.opendc.trace.conv.TASK_WORKFLOW_ID
import org.opendc.trace.testkit.TableReaderTestKit
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant

/**
 * Test suite for the [GwfTraceFormat] class.
 */
@DisplayName("GWF TraceFormat")
internal class GwfTraceFormatTest {
    private val format = GwfTraceFormat()

    @Test
    fun testTables() {
        val path = Paths.get(checkNotNull(GwfTraceFormatTest::class.java.getResource("/trace.gwf")).toURI())

        assertEquals(listOf(TABLE_TASKS), format.getTables(path))
    }

    @Test
    fun testTableExists() {
        val path = Paths.get(checkNotNull(GwfTraceFormatTest::class.java.getResource("/trace.gwf")).toURI())
        assertDoesNotThrow { format.getDetails(path, TABLE_TASKS) }
    }

    @Test
    fun testTableDoesNotExist() {
        val path = Paths.get(checkNotNull(GwfTraceFormatTest::class.java.getResource("/trace.gwf")).toURI())

        assertThrows<IllegalArgumentException> { format.getDetails(path, "test") }
    }

    @Test
    fun testTableReader() {
        val path = Paths.get(checkNotNull(GwfTraceFormatTest::class.java.getResource("/trace.gwf")).toURI())
        val reader = format.newReader(path, TABLE_TASKS, null)

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("0", reader.getString(TASK_WORKFLOW_ID)) },
            { assertEquals("1", reader.getString(TASK_ID)) },
            { assertEquals(Instant.ofEpochSecond(16), reader.getInstant(TASK_SUBMIT_TIME)) },
            { assertEquals(Duration.ofSeconds(11), reader.getDuration(TASK_RUNTIME)) },
            { assertEquals(emptySet<String>(), reader.getSet(TASK_PARENTS, String::class.java)) }
        )
    }

    @Test
    fun testReadingRowWithDependencies() {
        val path = Paths.get(checkNotNull(GwfTraceFormatTest::class.java.getResource("/trace.gwf")).toURI())
        val reader = format.newReader(path, TABLE_TASKS, null)

        // Move to row 7
        for (x in 1..6)
            reader.nextRow()

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("0", reader.getString(TASK_WORKFLOW_ID)) },
            { assertEquals("7", reader.getString(TASK_ID)) },
            { assertEquals(Instant.ofEpochSecond(87), reader.getInstant(TASK_SUBMIT_TIME)) },
            { assertEquals(Duration.ofSeconds(11), reader.getDuration(TASK_RUNTIME)) },
            { assertEquals(setOf("4", "5", "6"), reader.getSet(TASK_PARENTS, String::class.java)) }
        )
    }

    @DisplayName("TableReader for Tasks")
    @Nested
    inner class TasksTableReaderTest : TableReaderTestKit() {
        override lateinit var reader: TableReader
        override lateinit var columns: List<TableColumn>

        @BeforeEach
        fun setUp() {
            val path = Paths.get(checkNotNull(GwfTraceFormatTest::class.java.getResource("/trace.gwf")).toURI())

            columns = format.getDetails(path, TABLE_TASKS).columns
            reader = format.newReader(path, TABLE_TASKS, null)
        }
    }
}
