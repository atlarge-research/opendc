/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.trace.testkit

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.opendc.trace.TableColumn
import org.opendc.trace.TableColumnType
import org.opendc.trace.TableReader

/**
 * A test suite for implementations of the [TableReader] interface.
 */
public abstract class TableReaderTestKit {
    /**
     * The [TableReader] instance to test.
     */
    public abstract val reader: TableReader

    /**
     * The columns of the table.
     */
    public abstract val columns: List<TableColumn>

    @AfterEach
    public fun tearDown() {
        reader.close()
    }

    /**
     * Test that we can resolve the columns of a table successfully.
     */
    @Test
    public fun testResolve() {
        assertAll(columns.map { column -> { assertNotEquals(-1, reader.resolve(column.name)) } })
    }

    /**
     * Test that resolving an empty column name fails
     */
    @Test
    public fun testResolveEmpty() {
        assertEquals(-1, reader.resolve(""))
    }

    /**
     * Test that reading non-existent columns fails.
     */
    @Test
    public fun testReadNonExistentColumns() {
        assumeTrue(reader.nextRow())
        assertAll(
            { assertThrows<IllegalArgumentException> { reader.isNull(-1) } },
            { assertThrows<IllegalArgumentException> { reader.getBoolean(-1) } },
            { assertThrows<IllegalArgumentException> { reader.getInt(-1) } },
            { assertThrows<IllegalArgumentException> { reader.getLong(-1) } },
            { assertThrows<IllegalArgumentException> { reader.getFloat(-1) } },
            { assertThrows<IllegalArgumentException> { reader.getDouble(-1) } },
            { assertThrows<IllegalArgumentException> { reader.getString(-1) } },
            { assertThrows<IllegalArgumentException> { reader.getUUID(-1) } },
            { assertThrows<IllegalArgumentException> { reader.getInstant(-1) } },
            { assertThrows<IllegalArgumentException> { reader.getDuration(-1) } },
            { assertThrows<IllegalArgumentException> { reader.getList(-1, Any::class.java) } },
            { assertThrows<IllegalArgumentException> { reader.getSet(-1, Any::class.java) } },
            { assertThrows<IllegalArgumentException> { reader.getMap(-1, Any::class.java, Any::class.java) } }
        )
    }

    /**
     * Test that ensures [TableReader.isNull] reports the correct value.
     */
    @Test
    public fun testVerifyNullColumns() {
        while (reader.nextRow()) {
            assertAll(
                columns.map { column ->
                    {
                        when (column.type) {
                            is TableColumnType.Boolean -> assertFalse(reader.isNull(column.name) && !reader.getBoolean(column.name))
                            is TableColumnType.Int -> assertFalse(reader.isNull(column.name) && reader.getInt(column.name) != 0)
                            is TableColumnType.Long -> assertFalse(reader.isNull(column.name) && reader.getLong(column.name) != 0L)
                            is TableColumnType.Float -> assertFalse(reader.isNull(column.name) && reader.getFloat(column.name) != 0f)
                            is TableColumnType.Double -> assertFalse(reader.isNull(column.name) && reader.getDouble(column.name) != 0.0)
                            is TableColumnType.String -> assertFalse(reader.isNull(column.name) && reader.getString(column.name) != null)
                            is TableColumnType.UUID -> assertFalse(reader.isNull(column.name) && reader.getUUID(column.name) != null)
                            is TableColumnType.Instant -> assertFalse(reader.isNull(column.name) && reader.getInstant(column.name) != null)
                            is TableColumnType.Duration -> assertFalse(reader.isNull(column.name) && reader.getDuration(column.name) != null)
                            is TableColumnType.List -> assertFalse(reader.isNull(column.name) && reader.getList(column.name, Any::class.java) != null)
                            is TableColumnType.Set -> assertFalse(reader.isNull(column.name) && reader.getSet(column.name, Any::class.java) != null)
                            is TableColumnType.Map -> assertFalse(reader.isNull(column.name) && reader.getMap(column.name, Any::class.java, Any::class.java) != null)
                        }
                    }
                }
            )
        }
    }

    /**
     * Test that we can read the entire table without any issue.
     */
    @Test
    public fun testReadFully() {
        assertDoesNotThrow {
            while (reader.nextRow()) {
                assertAll(columns.map { column -> { assertDoesNotThrow { reader.get(column) } } })
            }
            reader.close()
        }

        assertFalse(reader.nextRow()) { "Reader does not reset" }
    }

    /**
     * Test that the reader throws an exception when the columns are read without a call to [TableReader.nextRow]
     */
    @Test
    public fun testReadWithoutNextRow() {
        assertAll(columns.map { column -> { assertThrows<IllegalStateException> { reader.get(column) } } })
    }

    /**
     * Test that the reader throws an exception when the columns are read after the [TableReader] is finished.
     */
    @Test
    public fun testReadAfterFinish() {
        @Suppress("ControlFlowWithEmptyBody")
        while (reader.nextRow()) {}

        testReadWithoutNextRow()
    }

    /**
     * Helper method to map a [TableColumn] to a read.
     */
    private fun TableReader.get(column: TableColumn): Any? {
        return when (column.type) {
            is TableColumnType.Boolean -> getBoolean(column.name)
            is TableColumnType.Int -> getInt(column.name)
            is TableColumnType.Long -> getLong(column.name)
            is TableColumnType.Float -> getFloat(column.name)
            is TableColumnType.Double -> getDouble(column.name)
            is TableColumnType.String -> getString(column.name)
            is TableColumnType.UUID -> getUUID(column.name)
            is TableColumnType.Instant -> getInstant(column.name)
            is TableColumnType.Duration -> getDuration(column.name)
            is TableColumnType.List -> getList(column.name, Any::class.java)
            is TableColumnType.Set -> getSet(column.name, Any::class.java)
            is TableColumnType.Map -> getMap(column.name, Any::class.java, Any::class.java)
        }
    }
}
