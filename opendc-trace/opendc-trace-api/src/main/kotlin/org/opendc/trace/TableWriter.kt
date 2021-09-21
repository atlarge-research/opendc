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
 * Base class for writing workload traces.
 */
public interface TableWriter : AutoCloseable {
    /**
     * Start a new row in the table.
     */
    public fun startRow()

    /**
     * Flush the current row to the table.
     */
    public fun endRow()

    /**
     * Resolve the index of the specified [column] for this writer.
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
     * Set [column] to [value].
     *
     * @param index The zero-based index of the column to set the value for.
     * @param value The value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun set(index: Int, value: Any)

    /**
     * Set [column] to boolean [value].
     *
     * @param index The zero-based index of the column to set the value for.
     * @param value The boolean value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setBoolean(index: Int, value: Boolean)

    /**
     * Set [column] to integer [value].
     *
     * @param index The zero-based index of the column to set the value for.
     * @param value The integer value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setInt(index: Int, value: Int)

    /**
     * Set [column] to long [value].
     *
     * @param index The zero-based index of the column to set the value for.
     * @param value The long value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setLong(index: Int, value: Long)

    /**
     * Set [column] to double [value].
     *
     * @param index The zero-based index of the column to set the value for.
     * @param value The double value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setDouble(index: Int, value: Double)

    /**
     * Set [column] to [value].
     *
     * @param column The column to set the value for.
     * @param value The value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun <T : Any> set(column: TableColumn<T>, value: T): Unit = set(resolve(column), value)

    /**
     * Set [column] to boolean [value].
     *
     * @param column The column to set the value for.
     * @param value The boolean value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setBoolean(column: TableColumn<Boolean>, value: Boolean): Unit = setBoolean(resolve(column), value)

    /**
     * Set [column] to integer [value].
     *
     * @param column The column to set the value for.
     * @param value The integer value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setInt(column: TableColumn<Int>, value: Int): Unit = setInt(resolve(column), value)

    /**
     * Set [column] to long [value].
     *
     * @param column The column to set the value for.
     * @param value The long value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setLong(column: TableColumn<Long>, value: Long): Unit = setLong(resolve(column), value)

    /**
     * Set [column] to double [value].
     *
     * @param column The column to set the value for.
     * @param value The double value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setDouble(column: TableColumn<Double>, value: Double): Unit = setDouble(resolve(column), value)

    /**
     * Flush any buffered content to the underlying target.
     */
    public fun flush()

    /**
     * Close the writer so that no more rows can be written.
     */
    public override fun close()
}
