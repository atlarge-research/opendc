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

import java.time.Duration
import java.time.Instant
import java.util.UUID

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
     * Resolve the index of the column named [name] for this writer.
     *
     * @param name The name of the column to lookup.
     * @return The zero-based index of the column or a negative value if the column is not present in this table.
     */
    public fun resolve(name: String): Int

    /**
     * Set the column with index [index] to boolean [value].
     *
     * @param index The zero-based index of the column to set the value for.
     * @param value The boolean value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setBoolean(index: Int, value: Boolean)

    /**
     * Set the column with index [index] to integer [value].
     *
     * @param index The zero-based index of the column to set the value for.
     * @param value The integer value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setInt(index: Int, value: Int)

    /**
     * Set the column with index [index] to long [value].
     *
     * @param index The zero-based index of the column to set the value for.
     * @param value The long value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setLong(index: Int, value: Long)

    /**
     * Set the column with index [index] to float [value].
     *
     * @param index The zero-based index of the column to set the value for.
     * @param value The float value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setFloat(index: Int, value: Float)

    /**
     * Set the column with index [index] to double [value].
     *
     * @param index The zero-based index of the column to set the value for.
     * @param value The double value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setDouble(index: Int, value: Double)

    /**
     * Set the column with index [index] to [String] [value].
     *
     * @param index The zero-based index of the column to set the value for.
     * @param value The [String] value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setString(index: Int, value: String)

    /**
     * Set the column with index [index] to [UUID] [value].
     *
     * @param index The zero-based index of the column to set the value for.
     * @param value The [UUID] value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setUUID(index: Int, value: UUID)

    /**
     * Set the column with index [index] to [Instant] [value].
     *
     * @param index The zero-based index of the column to set the value for.
     * @param value The [Instant] value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setInstant(index: Int, value: Instant)

    /**
     * Set the column with index [index] to [Duration] [value].
     *
     * @param index The zero-based index of the column to set the value for.
     * @param value The [Duration] value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setDuration(index: Int, value: Duration)

    /**
     * Set the column with index [index] to [List] [value].
     *
     * @param index The zero-based index of the column to set the value for.
     * @param value The [Map] value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun <T> setList(index: Int, value: List<T>)

    /**
     * Set the column with index [index] to [Set] [value].
     *
     * @param index The zero-based index of the column to set the value for.
     * @param value The [Set] value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun <T> setSet(index: Int, value: Set<T>)

    /**
     * Set the column with index [index] to [Map] [value].
     *
     * @param index The zero-based index of the column to set the value for.
     * @param value The [Map] value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun <K, V> setMap(index: Int, value: Map<K, V>)

    /**
     * Set the column named [name] to boolean [value].
     *
     * @param name The name of the column to set the value for.
     * @param value The boolean value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setBoolean(name: String, value: Boolean): Unit = setBoolean(resolve(name), value)

    /**
     * Set the column named [name] to integer [value].
     *
     * @param name The name of the column to set the value for.
     * @param value The integer value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setInt(name: String, value: Int): Unit = setInt(resolve(name), value)

    /**
     * Set the column named [name] to long [value].
     *
     * @param name The name of the column to set the value for.
     * @param value The long value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setLong(name: String, value: Long): Unit = setLong(resolve(name), value)

    /**
     * Set the column named [name] to float [value].
     *
     * @param name The name of the column to set the value for.
     * @param value The float value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setFloat(name: String, value: Float): Unit = setFloat(resolve(name), value)

    /**
     * Set the column named [name] to double [value].
     *
     * @param name The name of the column to set the value for.
     * @param value The double value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setDouble(name: String, value: Double): Unit = setDouble(resolve(name), value)

    /**
     * Set the column named [name] to [String] [value].
     *
     * @param name The name of the column to set the value for.
     * @param value The [String] value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setString(name: String, value: String): Unit = setString(resolve(name), value)

    /**
     * Set the column named [name] to [UUID] [value].
     *
     * @param name The name of the column to set the value for.
     * @param value The [UUID] value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setUUID(name: String, value: UUID): Unit = setUUID(resolve(name), value)

    /**
     * Set the column named [name] to [Instant] [value].
     *
     * @param name The name of the column to set the value for.
     * @param value The [Instant] value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setInstant(name: String, value: Instant): Unit = setInstant(resolve(name), value)

    /**
     * Set the column named [name] to [Duration] [value].
     *
     * @param name The name of the column to set the value for.
     * @param value The [Duration] value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun setDuration(name: String, value: Duration): Unit = setDuration(resolve(name), value)

    /**
     * Set the column named [name] to [List] [value].
     *
     * @param name The name of the column to set the value for.
     * @param value The [List] value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun <T> setList(name: String, value: List<T>): Unit = setList(resolve(name), value)

    /**
     * Set the column named [name] to [Set] [value].
     *
     * @param name The name of the column to set the value for.
     * @param value The [Set] value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun <T> setSet(name: String, value: Set<T>): Unit = setSet(resolve(name), value)

    /**
     * Set the column named [name] to [Map] [value].
     *
     * @param name The name of the column to set the value for.
     * @param value The [Map] value to set the column to.
     * @throws IllegalArgumentException if the column is not valid for this method.
     */
    public fun <K, V> setMap(name: String, value: Map<K, V>): Unit = setMap(resolve(name), value)

    /**
     * Flush any buffered content to the underlying target.
     */
    public fun flush()

    /**
     * Close the writer so that no more rows can be written.
     */
    public override fun close()
}
