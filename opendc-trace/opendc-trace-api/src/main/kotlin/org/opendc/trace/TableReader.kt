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

package org.opendc.trace

/**
 * Base class for reading entities from a workload trace table in streaming fashion.
 */
public interface TableReader : AutoCloseable {
    /**
     * Advance the stream until the next row is reached.
     *
     * @return `true` if the row is valid, `false` if there are no more rows.
     */
    public fun nextRow(): Boolean

    /**
     * Determine whether the [TableReader] supports the specified [column].
     */
    public fun hasColumn(column: TableColumn<*>): Boolean

    /**
     * Obtain the value of the current column with type [T].
     */
    public fun <T> get(column: TableColumn<T>): T

    /**
     * Read the specified [column] as boolean.
     */
    public fun getBoolean(column: TableColumn<Boolean>): Boolean

    /**
     * Read the specified [column] as integer.
     */
    public fun getInt(column: TableColumn<Int>): Int

    /**
     * Read the specified [column] as long.
     */
    public fun getLong(column: TableColumn<Long>): Long

    /**
     * Read the specified [column] as double.
     */
    public fun getDouble(column: TableColumn<Double>): Double

    /**
     * Closes the reader so that no further iteration or data access can be made.
     */
    public override fun close()
}
