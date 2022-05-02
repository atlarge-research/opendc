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
import org.opendc.trace.TableReader
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An [Enumerator] for a [TableReader].
 */
internal class TraceReaderEnumerator<E>(
    private val reader: TableReader,
    private val columns: List<TableColumn<*>>,
    private val cancelFlag: AtomicBoolean
) : Enumerator<E> {
    private val columnIndices = columns.map { reader.resolve(it) }.toIntArray()
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

    private fun convertColumn(reader: TableReader, column: TableColumn<*>, columnIndex: Int): Any? {
        val value = reader.get(columnIndex)

        return when (column.type) {
            Instant::class.java -> Timestamp.from(value as Instant)
            Duration::class.java -> (value as Duration).toMillis()
            Set::class.java -> (value as Set<*>).toTypedArray()
            else -> value
        }
    }
}
