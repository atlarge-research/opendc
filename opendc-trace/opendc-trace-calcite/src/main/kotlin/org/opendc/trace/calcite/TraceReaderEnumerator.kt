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

package org.opendc.trace.calcite

import org.apache.calcite.linq4j.Enumerator
import org.opendc.trace.TableColumn
import org.opendc.trace.TableColumnType
import org.opendc.trace.TableReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An [Enumerator] for a [TableReader].
 */
internal class TraceReaderEnumerator<E>(
    private val reader: TableReader,
    private val columns: List<TableColumn>,
    private val cancelFlag: AtomicBoolean
) : Enumerator<E> {
    private val columnIndices = columns.map { reader.resolve(it.name) }.toIntArray()
    private var current: E? = null

    override fun moveNext(): Boolean {
        if (cancelFlag.get()) {
            return false
        }

        val reader = reader
        val res = reader.nextRow()

        if (res) {
            @Suppress("UNCHECKED_CAST")
            current = convertRow(reader) as E
        } else {
            current = null
        }

        return res
    }

    override fun current(): E = checkNotNull(current)

    override fun reset() {
        throw UnsupportedOperationException()
    }

    override fun close() {
        reader.close()
    }

    private fun convertRow(reader: TableReader): Array<Any?> {
        val res = arrayOfNulls<Any?>(columns.size)
        val columnIndices = columnIndices

        for ((index, column) in columns.withIndex()) {
            val columnIndex = columnIndices[index]
            res[index] = convertColumn(reader, column, columnIndex)
        }
        return res
    }

    private fun convertColumn(reader: TableReader, column: TableColumn, columnIndex: Int): Any? {
        return when (column.type) {
            is TableColumnType.Boolean -> reader.getBoolean(columnIndex)
            is TableColumnType.Int -> reader.getInt(columnIndex)
            is TableColumnType.Long -> reader.getLong(columnIndex)
            is TableColumnType.Float -> reader.getFloat(columnIndex)
            is TableColumnType.Double -> reader.getDouble(columnIndex)
            is TableColumnType.String -> reader.getString(columnIndex)
            is TableColumnType.UUID -> {
                val uuid = reader.getUUID(columnIndex)

                if (uuid != null) {
                    val uuidBytes = ByteArray(16)

                    ByteBuffer.wrap(uuidBytes)
                        .order(ByteOrder.BIG_ENDIAN)
                        .putLong(uuid.mostSignificantBits)
                        .putLong(uuid.leastSignificantBits)

                    uuidBytes
                } else {
                    null
                }
            }
            is TableColumnType.Instant -> reader.getInstant(columnIndex)?.toEpochMilli()
            is TableColumnType.Duration -> reader.getDuration(columnIndex)?.toMillis() ?: 0
            is TableColumnType.List -> reader.getList(columnIndex, Any::class.java)?.toTypedArray()
            is TableColumnType.Set -> reader.getSet(columnIndex, Any::class.java)?.toTypedArray()
            is TableColumnType.Map -> reader.getMap(columnIndex, Any::class.java, Any::class.java)
        }
    }
}
