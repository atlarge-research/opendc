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

package org.opendc.trace.util.parquet

import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import org.apache.parquet.avro.AvroParquetReader
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.ParquetFileWriter
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException

/**
 * Test suite for the Parquet helper classes.
 */
internal class ParquetTest {
    private val schema = SchemaBuilder
        .record("test")
        .namespace("org.opendc.format.util")
        .fields()
        .name("field").type().intType().noDefault()
        .endRecord()

    private lateinit var file: File

    /**
     * Setup the test
     */
    @BeforeEach
    fun setUp() {
        file = File.createTempFile("opendc", "parquet")
    }

    /**
     * Tear down the test.
     */
    @AfterEach
    fun tearDown() {
        file.delete()
    }

    /**
     * Initial test to verify whether the Parquet writer works.
     */
    @Test
    fun testSmoke() {
        val n = 4
        val writer = AvroParquetWriter.builder<GenericData.Record>(LocalOutputFile(file))
            .withSchema(schema)
            .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
            .build()

        try {
            repeat(n) { i ->
                val record = GenericData.Record(schema)
                record.put("field", i)
                writer.write(record)
            }
        } finally {
            writer.close()
        }

        val reader = AvroParquetReader.builder<GenericData.Record>(LocalInputFile(file))
            .build()

        var counter = 0
        try {
            while (true) {
                val record = reader.read() ?: break
                assertEquals(counter++, record.get("field"))
            }
        } finally {
            reader.close()
        }

        assertEquals(n, counter)
    }

    /**
     * Test if overwriting fails if not specified.
     */
    @Test
    fun testOverwrite() {
        assertThrows<FileAlreadyExistsException> {
            AvroParquetWriter.builder<GenericData.Record>(LocalOutputFile(file))
                .withSchema(schema)
                .build()
        }
    }

    /**
     * Test non-existent file.
     */
    @Test
    fun testNonExistent() {
        file.delete()
        assertThrows<NoSuchFileException> {
            AvroParquetReader.builder<GenericData.Record>(LocalInputFile(file))
                .build()
        }
    }
}
