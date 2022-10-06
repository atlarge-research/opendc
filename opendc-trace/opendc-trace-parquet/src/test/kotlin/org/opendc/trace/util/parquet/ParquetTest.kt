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

import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.ParquetFileWriter
import org.apache.parquet.hadoop.api.ReadSupport
import org.apache.parquet.hadoop.api.WriteSupport
import org.apache.parquet.io.api.Converter
import org.apache.parquet.io.api.GroupConverter
import org.apache.parquet.io.api.PrimitiveConverter
import org.apache.parquet.io.api.RecordConsumer
import org.apache.parquet.io.api.RecordMaterializer
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.Type
import org.apache.parquet.schema.Types
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path

/**
 * Test suite for the Parquet helper classes.
 */
internal class ParquetTest {
    private lateinit var path: Path

    private val schema = Types.buildMessage()
        .addField(
            Types.primitive(PrimitiveType.PrimitiveTypeName.INT32, Type.Repetition.REQUIRED)
                .named("field")
        )
        .named("test")
    private val writeSupport = object : WriteSupport<Int>() {
        lateinit var recordConsumer: RecordConsumer

        override fun init(configuration: Configuration): WriteContext {
            return WriteContext(schema, emptyMap())
        }

        override fun prepareForWrite(recordConsumer: RecordConsumer) {
            this.recordConsumer = recordConsumer
        }

        override fun write(record: Int) {
            val consumer = recordConsumer

            consumer.startMessage()
            consumer.startField("field", 0)
            consumer.addInteger(record)
            consumer.endField("field", 0)
            consumer.endMessage()
        }
    }

    private val readSupport = object : ReadSupport<Int>() {
        @Suppress("OVERRIDE_DEPRECATION")
        override fun init(
            configuration: Configuration,
            keyValueMetaData: Map<String, String>,
            fileSchema: MessageType
        ): ReadContext = ReadContext(fileSchema)

        override fun prepareForRead(
            configuration: Configuration,
            keyValueMetaData: Map<String, String>,
            fileSchema: MessageType,
            readContext: ReadContext
        ): RecordMaterializer<Int> = TestRecordMaterializer()
    }

    /**
     * Set up the test
     */
    @BeforeEach
    fun setUp() {
        path = Files.createTempFile("opendc", "parquet")
    }

    /**
     * Tear down the test.
     */
    @AfterEach
    fun tearDown() {
        Files.deleteIfExists(path)
    }

    /**
     * Initial test to verify whether the Parquet writer works.
     */
    @Test
    fun testSmoke() {
        val n = 4
        val writer = LocalParquetWriter.builder(path, writeSupport)
            .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
            .build()

        try {
            repeat(n) { i ->
                writer.write(i)
            }
        } finally {
            writer.close()
        }

        val reader = LocalParquetReader(path, readSupport)
        var counter = 0
        try {
            while (true) {
                val record = reader.read() ?: break
                assertEquals(counter++, record)
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
            LocalParquetWriter.builder(path, writeSupport).build()
        }
    }

    /**
     * Test non-existent file.
     */
    @Test
    fun testNonExistent() {
        Files.deleteIfExists(path)
        assertThrows<NoSuchFileException> {
            LocalParquetReader(path, readSupport)
        }
    }

    private class TestRecordMaterializer : RecordMaterializer<Int>() {
        private var current: Int = 0
        private val fieldConverter = object : PrimitiveConverter() {
            override fun addInt(value: Int) {
                current = value
            }
        }
        private val root = object : GroupConverter() {
            override fun getConverter(fieldIndex: Int): Converter {
                require(fieldIndex == 0)
                return fieldConverter
            }
            override fun start() {}
            override fun end() {}
        }

        override fun getCurrentRecord(): Int = current

        override fun getRootConverter(): GroupConverter = root
    }
}
