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

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.DOUBLE
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT32
import org.apache.parquet.schema.Type
import org.opendc.common.logger.logger
import org.slf4j.Logger
import kotlin.reflect.KClass

/**
 * A column that can be used to build a parquet schema to export [T] records.
 *
 * See [columnSerializer] for deserialization of this class.
 *
 * ```kotlin
 * class Foo: Exportable<Foo> {
 * ...
 * val MY_FIELD = ExportColumn<Foo>(
 *      field = Types.required(PrimitiveType.PrimitiveTypeName.DOUBLE).named("my_field_name")
 * ) { exportable: Foo -> addDouble(exportable.getMyValue()) }
 * ```
 *
 * @param[field]
 * The apache parquet field, it includes information such as:
 * - Required (not)
 * - Field name
 * - [PrimitiveType] (e.g. [INT32], [DOUBLE] etc.)
 * - [LogicalTypeAnnotation] (e.g. TIMESTAMP, etc.)
 *
 * @param[getValue]
 * Retrieves the value to be exported from the [Exportable] of [T] passed as param.
 * The value returned needs to match the expected [PrimitiveType] defined in the field.
 *
 * A second type parameter could have been added to the class to enforce the correct type at compile time,
 * however it would have added too much complexity to the interface. `ExportColumn<Exportable>` -> `ExportColumn<Exportable, *>`
 *
 * @param[regex] The pattern used to determine whether a string refers to this column.
 * The default one matches the column name with either underscores or blank
 * spaces between words in a case-insensitive manner.
 *
 * @param[exportableClass]
 * The [KClass] of the [Exportable]. Used for intuitive lof messages. This class
 * can be instantiated with inline constructor [Companion.invoke] without providing this parameter.
 */
public class ExportColumn<T : Exportable>
    @PublishedApi
    internal constructor(
        public val field: Type,
        @PublishedApi internal val regex: Regex,
        @PublishedApi internal val exportableClass: KClass<T>,
        internal val getValue: (T) -> Any?,
    ) {
        /**
         * The name of the column (e.g. "timestamp").
         */
        public val name: String by lazy { field.name }

        /**
         * The primitive type of the field (e.g. INT32).
         */
        public val primitiveTypeName: PrimitiveTypeName by lazy { field.asPrimitiveType().primitiveTypeName }

        init {
            // Adds the column among those that can be deserialized.
            addField(this)
        }

        override fun toString(): String = "[ExportColumn: name=$name, exportable=${exportableClass.simpleName}]"

        public companion object {
            @PublishedApi
            internal val LOG: Logger by logger()

            /**
             * Reified constructor, needed to store [T] class without providing it as parameter.
             */
            public inline operator fun <reified T : Exportable> invoke(
                field: Type,
                regex: Regex = Regex("\\s*(?:${field.name}|${field.name.replace('_', ' ')})\\s*", RegexOption.IGNORE_CASE),
                noinline getValue: (T) -> Any?,
            ): ExportColumn<T> =
                ExportColumn(
                    field = field,
                    getValue = getValue,
                    exportableClass = T::class,
                    regex = regex,
                )

            /**
             * All the columns that have been instantiated. They are added in `init` block.
             * Keep in mind that in order to deserialize to a column, that column needs to be loaded by the jvm.
             */
            @PublishedApi
            internal val allColumns: MutableSet<ExportColumn<*>> = mutableSetOf()

            @PublishedApi
            internal val allColumnsLock: Mutex = Mutex()

            /**
             * Function invoked in the `init` block of each [ExportColumn].
             * Adds the column to those that can be deserialized.
             */
            private fun <T : Exportable> addField(column: ExportColumn<T>): Unit =
                runBlocking {
                    allColumnsLock.withLock { allColumns.add(column) }
                }

            /**
             * @return the [ExportColumn] whose [ExportColumn.regex] matches [columnName] **and**
             * whose generic type ([Exportable]) is [T] if any, `null` otherwise.
             *
             * This method needs to be inlined and reified cause of runtime type erasure
             * that does not allow to type check the generic class parameter.
             */
            @Suppress("UNCHECKED_CAST") // I do not know why it is needed since the cast is nullable.
            @PublishedApi
            internal inline fun <reified T : Exportable> matchingColOrNull(columnName: String): ExportColumn<T>? =
                runBlocking {
                    val allColumns = allColumnsLock.withLock { allColumns.toSet() }

                    allColumns.forEach { column ->
                        // If it is an ExportColumn of same type.
                        if (column.exportableClass == T::class) {
                            // Just a smart cast that always succeeds at runtime cause
                            // of type erasure but is needed at compile time.
                            (column as? ExportColumn<T>)
                                ?.regex
                                // If fieldName matches the field regex.
                                ?.matchEntire(columnName)
                                ?.let { return@runBlocking column }
                        }
                    }

                    null
                }

            /**
             * Returns all [ExportColumn]s of type [T] that have been loaded up until now.
             */
            @Suppress("UNCHECKED_CAST")
            public inline fun <reified T : Exportable> getAllLoadedColumns(): List<ExportColumn<T>> =
                runBlocking {
                    allColumnsLock.withLock { allColumns.filter { it.exportableClass == T::class } as List<ExportColumn<T>> }
                }
        }
    }
