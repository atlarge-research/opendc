/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.trace.parquet.exporter

import org.apache.parquet.hadoop.example.GroupReadSupport
import org.apache.parquet.io.api.Binary
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BINARY
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.FLOAT
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT64
import org.apache.parquet.schema.Types
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.opendc.trace.parquet.LocalParquetReader
import java.nio.file.Files
import java.nio.file.Path

/**
 * Test suite for the [Exporter].
 */
class ExporterTest {
    private lateinit var path: Path

    private class Row(val count: Int?, val usage: Double, val name: String) : Exportable

    private val columns =
        listOf(
            ExportColumn<Row>(field = Types.optional(INT64).named("count"), register = false) { it.count },
            ExportColumn<Row>(field = Types.required(FLOAT).named("usage"), register = false) { it.usage },
            ExportColumn<Row>(
                field = Types.required(BINARY).`as`(LogicalTypeAnnotation.stringType()).named("name"),
                register = false,
            ) { Binary.fromString(it.name) },
        )

    @BeforeEach
    fun setUp() {
        path = Files.createTempFile("opendc-exporter", ".parquet")
    }

    @AfterEach
    fun tearDown() {
        Files.deleteIfExists(path)
    }

    /**
     * Numbers are converted to the type of their column, and a null is written to an optional column.
     */
    @Test
    fun testWrite() {
        Exporter(outputFile = path.toFile(), columns = columns).use { exporter ->
            exporter.write(Row(count = 3, usage = 1.5, name = "a"))
            exporter.write(Row(count = null, usage = 2.25, name = "b"))
        }

        val rows = LocalParquetReader(path, GroupReadSupport()).use { reader -> generateSequence { reader.read() }.toList() }

        assertAll(
            { assertEquals(2, rows.size) },
            { assertEquals(3L, rows[0].getLong("count", 0)) },
            { assertEquals(1.5f, rows[0].getFloat("usage", 0)) },
            { assertEquals("a", rows[0].getString("name", 0)) },
            { assertEquals(0, rows[1].getFieldRepetitionCount("count")) },
            { assertEquals(2.25f, rows[1].getFloat("usage", 0)) },
        )
    }

    /**
     * A column of a type the exporter cannot write is rejected when the exporter is created.
     */
    @Test
    fun testUnsupportedType() {
        val column = ExportColumn<Row>(field = Types.required(FIXED_LEN_BYTE_ARRAY).length(4).named("fixed"), register = false) { null }

        assertThrows<IllegalArgumentException> {
            Exporter(outputFile = path.toFile(), columns = listOf(column))
        }
    }
}
