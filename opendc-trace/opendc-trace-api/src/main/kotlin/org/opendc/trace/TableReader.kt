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
     * Resolve the index of the specified [column] for this reader.
     *
     * @param column The column to lookup.
     * @return The zero-based index of the column or a negative value if the column is not present in this table.
     */
    public fun resolve(column: TableColumn<*>): Int

    /**
     * Determine whether the [TableReader] supports the specified [column].
     */
    public fun hasColumn(column: TableColumn<*>): Boolean = resolve(column) >= 0

    /**
     * Determine whether the specified [column] has a `null` value for the current row.
     *
     * @param index The zero-based index of the column to check for a null value.
     * @throws IllegalArgumentException if the column index is not valid for this reader.
     * @return `true` if the column value for the current value has a `null` value, `false` otherwise.
     */
    public fun isNull(index: Int): Boolean

    /**
     * Obtain the object value of the column with the specified [index].
     *
     * @param index The zero-based index of the column to obtain the value for.
     * @throws IllegalArgumentException if the column index is not valid for this reader.
     * @return The object value of the column.
     */
    public fun get(index: Int): Any?

    /**
     * Obtain the boolean value of the column with the specified [index].
     *
     * @param index The zero-based index of the column to obtain the value for.
     * @throws IllegalArgumentException if the column index is not valid for this reader.
     * @return The boolean value of the column or `false` if the column is `null`.
     */
    public fun getBoolean(index: Int): Boolean

    /**
     * Obtain the integer value of the column with the specified [index].
     *
     * @param index The zero-based index of the column to obtain the value for.
     * @throws IllegalArgumentException if the column index is not valid for this reader.
     * @return The integer value of the column or `0` if the column is `null`.
     */
    public fun getInt(index: Int): Int

    /**
     * Obtain the double value of the column with the specified [index].
     *
     * @param index The zero-based index of the column to obtain the value for.
     * @throws IllegalArgumentException if the column index is not valid for this reader.
     * @return The long value of the column or `0` if the column is `null`.
     */
    public fun getLong(index: Int): Long

    /**
     * Obtain the double value of the column with the specified [index].
     *
     * @param index The zero-based index of the column to obtain the value for.
     * @throws IllegalArgumentException if the column index is not valid for this reader.
     * @return The double value of the column or [Double.NaN] if the column is `null`.
     */
    public fun getDouble(index: Int): Double

    /**
     * Determine whether the specified [column] has a `null` value for the current row.
     *
     * @param column The column to lookup.
     * @throws IllegalArgumentException if the column is not valid for this table.
     * @return `true` if the column value for the current value has a `null` value, `false` otherwise.
     */
    public fun isNull(column: TableColumn<*>): Boolean = isNull(resolve(column))

    /**
     * Obtain the value of the current column with type [T].
     *
     * @param column The column to obtain the value for.
     * @throws IllegalArgumentException if the column is not valid for this reader.
     * @return The object value of the column.
     */
    public fun <T> get(column: TableColumn<T>): T {
        // This cast should always succeed since the resolve the index of the typed column
        @Suppress("UNCHECKED_CAST")
        return get(resolve(column)) as T
    }

    /**
     * Read the specified [column] as boolean.
     *
     * @param column The column to obtain the value for.
     * @throws IllegalArgumentException if the column is not valid for this reader.
     * @return The boolean value of the column or `false` if the column is `null`.
     */
    public fun getBoolean(column: TableColumn<Boolean>): Boolean = getBoolean(resolve(column))

    /**
     * Read the specified [column] as integer.
     *
     * @param column The column to obtain the value for.
     * @throws IllegalArgumentException if the column is not valid for this reader.
     * @return The integer value of the column or `0` if the column is `null`.
     */
    public fun getInt(column: TableColumn<Int>): Int = getInt(resolve(column))

    /**
     * Read the specified [column] as long.
     *
     * @param column The column to obtain the value for.
     * @throws IllegalArgumentException if the column is not valid for this reader.
     * @return The long value of the column or `0` if the column is `null`.
     */
    public fun getLong(column: TableColumn<Long>): Long = getLong(resolve(column))

    /**
     * Read the specified [column] as double.
     *
     * @param column The column to obtain the value for.
     * @throws IllegalArgumentException if the column is not valid for this reader.
     * @return The double value of the column or [Double.NaN] if the column is `null`.
     */
    public fun getDouble(column: TableColumn<Double>): Double = getDouble(resolve(column))

    /**
     * Closes the reader so that no further iteration or data access can be made.
     */
    public override fun close()
}
