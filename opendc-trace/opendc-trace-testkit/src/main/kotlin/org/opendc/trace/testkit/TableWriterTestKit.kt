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
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.opendc.trace.TableColumn
import org.opendc.trace.TableColumnType
import org.opendc.trace.TableWriter
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * A test suite for implementations of the [TableWriter] interface.
 */
public abstract class TableWriterTestKit {
    /**
     * The [TableWriter] instance to test.
     */
    public abstract val writer: TableWriter

    /**
     * The columns of the table.
     */
    public abstract val columns: List<TableColumn>

    @AfterEach
    public fun tearDown() {
        writer.close()
    }

    /**
     * Test that we can resolve the columns of a table successfully.
     */
    @Test
    public fun testResolve() {
        assertAll(columns.map { column -> { assertNotEquals(-1, writer.resolve(column.name)) } })
    }

    /**
     * Test that resolving an empty column name fails
     */
    @Test
    public fun testResolveEmpty() {
        assertEquals(-1, writer.resolve(""))
    }

    /**
     * Test that writing non-existent columns fails.
     */
    @Test
    public fun testWriteNonExistentColumns() {
        writer.startRow()
        assertAll(
            { assertThrows<IllegalArgumentException> { writer.setBoolean(-1, false) } },
            { assertThrows<IllegalArgumentException> { writer.setInt(-1, 1) } },
            { assertThrows<IllegalArgumentException> { writer.setLong(-1, 1) } },
            { assertThrows<IllegalArgumentException> { writer.setFloat(-1, 1f) } },
            { assertThrows<IllegalArgumentException> { writer.setDouble(-1, 1.0) } },
            { assertThrows<IllegalArgumentException> { writer.setString(-1, "test") } },
            { assertThrows<IllegalArgumentException> { writer.setUUID(-1, UUID.randomUUID()) } },
            { assertThrows<IllegalArgumentException> { writer.setInstant(-1, Instant.now()) } },
            { assertThrows<IllegalArgumentException> { writer.setDuration(-1, Duration.ofMinutes(5)) } },
            { assertThrows<IllegalArgumentException> { writer.setList(-1, listOf("test")) } },
            { assertThrows<IllegalArgumentException> { writer.setSet(-1, setOf("test")) } },
            { assertThrows<IllegalArgumentException> { writer.setMap(-1, mapOf("test" to "test")) } }
        )
    }

    /**
     * Test that writing columns without a row fails.
     */
    @Test
    public fun testWriteWithoutRow() {
        assertAll(
            columns.map { column ->
                {
                    assertThrows<IllegalStateException> {
                        when (column.type) {
                            is TableColumnType.Boolean -> writer.setBoolean(column.name, true)
                            is TableColumnType.Int -> writer.setInt(column.name, 21)
                            is TableColumnType.Long -> writer.setLong(column.name, 21)
                            is TableColumnType.Float -> writer.setFloat(column.name, 42f)
                            is TableColumnType.Double -> writer.setDouble(column.name, 42.0)
                            is TableColumnType.String -> writer.setString(column.name, "test")
                            is TableColumnType.UUID -> writer.setUUID(column.name, UUID.randomUUID())
                            is TableColumnType.Instant -> writer.setInstant(column.name, Instant.now())
                            is TableColumnType.Duration -> writer.setDuration(column.name, Duration.ofMinutes(5))
                            is TableColumnType.List -> writer.setList(column.name, emptyList<String>())
                            is TableColumnType.Set -> writer.setSet(column.name, emptySet<String>())
                            is TableColumnType.Map -> writer.setMap(column.name, emptyMap<String, String>())
                        }
                    }
                }
            }
        )
    }

    /**
     * Test to verify we cannot end a row without starting it.
     */
    @Test
    public fun testEndRowWithoutStart() {
        assertThrows<IllegalStateException> { writer.endRow() }
    }
}
