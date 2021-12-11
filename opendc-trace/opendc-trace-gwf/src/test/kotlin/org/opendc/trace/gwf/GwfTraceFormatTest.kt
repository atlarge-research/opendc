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

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.opendc.trace.*
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant

/**
 * Test suite for the [GwfTraceFormat] class.
 */
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
        val reader = format.newReader(path, TABLE_TASKS)

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("0", reader.get(TASK_WORKFLOW_ID)) },
            { assertEquals("1", reader.get(TASK_ID)) },
            { assertEquals(Instant.ofEpochSecond(16), reader.get(TASK_SUBMIT_TIME)) },
            { assertEquals(Duration.ofSeconds(11), reader.get(TASK_RUNTIME)) },
            { assertEquals(emptySet<String>(), reader.get(TASK_PARENTS)) },
        )
    }

    @Test
    fun testReadingRowWithDependencies() {
        val path = Paths.get(checkNotNull(GwfTraceFormatTest::class.java.getResource("/trace.gwf")).toURI())
        val reader = format.newReader(path, TABLE_TASKS)

        // Move to row 7
        for (x in 1..6)
            reader.nextRow()

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("0", reader.get(TASK_WORKFLOW_ID)) },
            { assertEquals("7", reader.get(TASK_ID)) },
            { assertEquals(Instant.ofEpochSecond(87), reader.get(TASK_SUBMIT_TIME)) },
            { assertEquals(Duration.ofSeconds(11), reader.get(TASK_RUNTIME)) },
            { assertEquals(setOf<String>("4", "5", "6"), reader.get(TASK_PARENTS)) },
        )
    }
}
