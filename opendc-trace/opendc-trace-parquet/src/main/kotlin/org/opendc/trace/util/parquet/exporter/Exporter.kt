/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.trace.util.parquet.exporter

import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.api.WriteSupport
import org.apache.parquet.io.api.RecordConsumer
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BINARY
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BOOLEAN
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.DOUBLE
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.FLOAT
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT32
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT64
import org.apache.parquet.schema.Type
import org.apache.parquet.schema.Types
import org.opendc.trace.util.parquet.ParquetDataWriter
import java.io.File

public class Exporter<T : Exportable>
    @PublishedApi
    internal constructor(
        outputFile: File,
        writeSupp: WriteSupport<T>,
        bufferSize: Int,
    ) : ParquetDataWriter<T>(
            path = outputFile,
            writeSupport = writeSupp,
            bufferSize = bufferSize,
        ) {
        public companion object {
            /**
             * Reified constructor that allows to use the runtime [Class.getSimpleName] name of [T] as the schema name.
             * @param[outputFile]   the output file where the [Exportable]s will be written.
             * @param[columns]       the columns that will be included in the output parquet file.
             * @param[schemaName]   the name of the schema of the output parquet file.
             */
            public inline operator fun <reified T : Exportable> invoke(
                outputFile: File,
                vararg columns: ExportColumn<T> = emptyArray(),
                schemaName: String? = null,
                bufferSize: Int = 4096,
            ): Exporter<T> =
                Exporter(
                    outputFile = outputFile,
                    writeSupp = writeSuppFor(columns.toSet(), schemaName = schemaName ?: T::class.simpleName ?: "unknown"),
                    bufferSize = bufferSize,
                )

            /**
             * Reified constructor that allows to use the runtime [Class.getSimpleName] name of [T] as the schema name.
             * @param[outputFile]   the output file where the [Exportable]s will be written.
             * @param[columns]       the columns that will be included in the output parquet file.
             * @param[schemaName]   the name of the schema of the output parquet file.
             */
            public inline operator fun <reified T : Exportable> invoke(
                outputFile: File,
                columns: Collection<ExportColumn<T>> = emptySet(),
                schemaName: String? = null,
                bufferSize: Int = 4096,
            ): Exporter<T> =
                Exporter(
                    outputFile = outputFile,
                    writeSupp = writeSuppFor(columns.toSet(), schemaName = schemaName ?: T::class.simpleName ?: "unknown"),
                    bufferSize = bufferSize,
                )

            /**
             * @return an anonymous [WriteSupport] for [T] with only the columns included in [columns].
             */
            @PublishedApi
            internal fun <T : Exportable> writeSuppFor(
                columns: Set<ExportColumn<T>>,
                schemaName: String,
            ): WriteSupport<T> =
                object : WriteSupport<T>() {
                    private lateinit var cons: RecordConsumer

                    private val schema: MessageType =
                        Types
                            .buildMessage()
                            .addFields(*columns.map { it.field }.toTypedArray())
                            .named(schemaName)

                    override fun init(configuration: Configuration): WriteContext = WriteContext(schema, emptyMap())

                    override fun prepareForWrite(recordConsumer: RecordConsumer) {
                        cons = recordConsumer
                    }

                    override fun write(record: T) =
                        with(cons) {
                            startMessage()

                            columns.forEachIndexed { idx, column ->
                                fun <T> Any.castedOrThrow(): T {
                                    @Suppress("UNCHECKED_CAST")
                                    return (this as? T) ?: throw TypeCastException(
                                        "attempt to add value of type ${this::class} to export " +
                                            "field $column which requires a different type",
                                    )
                                }
                                val valueToAdd: Any =
                                    column.getValue(
                                        record,
                                    ) ?: let {
                                        if (column.field.isRepetition(Type.Repetition.OPTIONAL)) {
                                            return@forEachIndexed
                                        } else {
                                            throw RuntimeException("trying to insert null value in required column $column")
                                        }
                                    }

                                startField(column.name, idx)
                                when (column.primitiveTypeName) {
                                    INT32 -> addInteger(valueToAdd.castedOrThrow())
                                    INT64 -> addLong(valueToAdd.castedOrThrow())
                                    DOUBLE -> addDouble(valueToAdd.castedOrThrow())
                                    BINARY -> addBinary(valueToAdd.castedOrThrow())
                                    FLOAT -> addFloat(valueToAdd.castedOrThrow())
                                    BOOLEAN -> addBoolean(valueToAdd.castedOrThrow())
                                    else -> throw RuntimeException(
                                        "parquet primitive type name '${column.primitiveTypeName} is not supported",
                                    )
                                }
                                endField(column.name, idx)
                            }

                            cons.endMessage()
                        }
                }
        }
    }
