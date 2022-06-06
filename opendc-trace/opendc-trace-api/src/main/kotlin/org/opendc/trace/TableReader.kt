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
 * Base class for reading entities from a [Table] in streaming fashion.
 */
public interface TableReader : AutoCloseable {
    /**
     * Advance the stream until the next row is reached.
     *
     * @return `true` if the row is valid, `false` if there are no more rows.
     */
    public fun nextRow(): Boolean

    /**
     * Resolve the index of the column by its [name].
     *
     * @param name The name of the column to lookup.
     * @return The zero-based index of the column or a negative value if the column is not present in this table.
     */
    public fun resolve(name: String): Int

    /**
     * Determine whether the column with the specified [index] has a `null` value for the current row.
     *
     * @param index The zero-based index of the column to check for a null value.
     * @throws IllegalArgumentException if the column index is not valid for this reader.
     * @return `true` if the column value for the current value has a `null` value, `false` otherwise.
     */
    public fun isNull(index: Int): Boolean

    /**
     * Obtain the boolean value of the column with the specified [index].
     *
     * @param index The zero-based index of the column to obtain the value for.
     * @throws IllegalArgumentException if the column index is not valid for this reader.
     * @return The boolean value of the column or `false` if the column is `null`.
     */
    public fun getBoolean(index: Int): Boolean

    /**
     * Obtain the boolean value of the column with the specified [index].
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
     * Obtain the float value of the column with the specified [index].
     *
     * @param index The zero-based index of the column to obtain the value for.
     * @throws IllegalArgumentException if the column index is not valid for this reader.
     * @return The float value of the column or [Float.NaN] if the column is `null`.
     */
    public fun getFloat(index: Int): Float

    /**
     * Obtain the double value of the column with the specified [index].
     *
     * @param index The zero-based index of the column to obtain the value for.
     * @throws IllegalArgumentException if the column index is not valid for this reader.
     * @return The double value of the column or [Double.NaN] if the column is `null`.
     */
    public fun getDouble(index: Int): Double

    /**
     * Obtain the [String] value of the column with the specified [index].
     *
     * @param index The zero-based index of the column to obtain the value for.
     * @throws IllegalArgumentException if the column index is not valid for this reader.
     * @return The [String] value of the column or `null` if the column is `null`.
     */
    public fun getString(index: Int): String?

    /**
     * Obtain the [UUID] value of the column with the specified [index].
     *
     * @param index The zero-based index of the column to obtain the value for.
     * @throws IllegalArgumentException if the column index is not valid for this reader.
     * @return The [UUID] value of the column or `null` if the column is `null`.
     */
    public fun getUUID(index: Int): UUID?

    /**
     * Obtain the [Instant] value of the column with the specified [index].
     *
     * @param index The zero-based index of the column to obtain the value for.
     * @throws IllegalArgumentException if the column index is not valid for this reader.
     * @return The [Instant] value of the column or `null` if the column is `null`.
     */
    public fun getInstant(index: Int): Instant?

    /**
     * Obtain the [Duration] value of the column with the specified [index].
     *
     * @param index The zero-based index of the column to obtain the value for.
     * @throws IllegalArgumentException if the column index is not valid for this reader.
     * @return The [Duration] value of the column or `null` if the column is `null`.
     */
    public fun getDuration(index: Int): Duration?

    /**
     * Obtain the value of the column with the specified [index] as [List].
     *
     * @param index The zero-based index of the column to obtain the value for.
     * @param elementType Class representing the Java data type to convert elements of the designated column to.
     * @throws IllegalArgumentException if the column index is not valid for this reader or this type.
     * @return The value of the column as `List` or `null` if the column is null.
     */
    public fun <T> getList(index: Int, elementType: Class<T>): List<T>?

    /**
     * Obtain the value of the column with the specified [index] as [Set].
     *
     * @param index The zero-based index of the column to obtain the value for.
     * @param elementType Class representing the Java data type to convert elements of the designated column to.
     * @throws IllegalArgumentException if the column index is not valid for this reader or this type.
     * @return The value of the column as `Set` or `null` if the column is null.
     */
    public fun <T> getSet(index: Int, elementType: Class<T>): Set<T>?

    /**
     * Obtain the value of the column with the specified [index] as [Set].
     *
     * @param index The zero-based index of the column to obtain the value for.
     * @param keyType Class representing the Java data type to convert the keys of the designated column to.
     * @param valueType Class representing the Java data type to convert the values of the designated column to.
     * @throws IllegalArgumentException if the column index is not valid for this reader or this type.
     * @return The value of the column as `Map` or `null` if the column is null.
     */
    public fun <K, V> getMap(index: Int, keyType: Class<K>, valueType: Class<V>): Map<K, V>?

    /**
     * Determine whether a column named [name] has a `null` value for the current row.
     *
     * @param name The name of the column to lookup.
     * @throws IllegalArgumentException if the column is not valid for this table.
     * @return `true` if the column value for the current value has a `null` value, `false` otherwise.
     */
    public fun isNull(name: String): Boolean = isNull(resolve(name))

    /**
     * Read the column named [name] as boolean.
     *
     * @param name The name of the column to get the value for.
     * @throws IllegalArgumentException if the column is not valid for this reader.
     * @return The boolean value of the column or `false` if the column is `null`.
     */
    public fun getBoolean(name: String): Boolean = getBoolean(resolve(name))

    /**
     * Read the column named [name] as integer.
     *
     * @param name The name of the column to get the value for.
     * @throws IllegalArgumentException if the column is not valid for this reader.
     * @return The integer value of the column or `0` if the column is `null`.
     */
    public fun getInt(name: String): Int = getInt(resolve(name))

    /**
     * Read the column named [name] as long.
     *
     * @param name The name of the column to get the value for
     * @throws IllegalArgumentException if the column is not valid for this reader.
     * @return The long value of the column or `0` if the column is `null`.
     */
    public fun getLong(name: String): Long = getLong(resolve(name))

    /**
     * Read the column named [name] as float.
     *
     * @param name The name of the column to get the value for.
     * @throws IllegalArgumentException if the column is not valid for this reader.
     * @return The float value of the column or [Float.NaN] if the column is `null`.
     */
    public fun getFloat(name: String): Float = getFloat(resolve(name))

    /**
     * Read the column named [name] as double.
     *
     * @param name The name of the column to get the value for.
     * @throws IllegalArgumentException if the column is not valid for this reader.
     * @return The double value of the column or [Double.NaN] if the column is `null`.
     */
    public fun getDouble(name: String): Double = getDouble(resolve(name))

    /**
     * Read the column named [name] as [String].
     *
     * @param name The name of the column to get the value for.
     * @throws IllegalArgumentException if the column is not valid for this reader.
     * @return The [String] value of the column or `null` if the column is `null`.
     */
    public fun getString(name: String): String? = getString(resolve(name))

    /**
     * Read the column named [name] as [UUID].
     *
     * @param name The name of the column to get the value for.
     * @throws IllegalArgumentException if the column is not valid for this reader.
     * @return The [UUID] value of the column or `null` if the column is `null`.
     */
    public fun getUUID(name: String): UUID? = getUUID(resolve(name))

    /**
     * Read the column named [name] as [Instant].
     *
     * @param name The name of the column to get the value for.
     * @throws IllegalArgumentException if the column is not valid for this reader.
     * @return The [Instant] value of the column or `null` if the column is `null`.
     */
    public fun getInstant(name: String): Instant? = getInstant(resolve(name))

    /**
     * Read the column named [name] as [Duration].
     *
     * @param name The name of the column to get the value for.
     * @throws IllegalArgumentException if the column is not valid for this reader.
     * @return The [Duration] value of the column or `null` if the column is `null`.
     */
    public fun getDuration(name: String): Duration? = getDuration(resolve(name))

    /**
     * Obtain the value of the column named [name] as [List].
     *
     * @param name The name of the column to get the value for.
     * @param elementType Class representing the Java data type to convert elements of the designated column to.
     * @throws IllegalArgumentException if the column index is not valid for this reader or this type.
     * @return The value of the column as `List` or `null` if the column is null.
     */
    public fun <T> getList(name: String, elementType: Class<T>): List<T>? = getList(resolve(name), elementType)

    /**
     * Obtain the value of the column named [name] as [Set].
     *
     * @param name The name of the column to get the value for.
     * @param elementType Class representing the Java data type to convert elements of the designated column to.
     * @throws IllegalArgumentException if the column index is not valid for this reader or this type.
     * @return The value of the column as `Set` or `null` if the column is null.
     */
    public fun <T> getSet(name: String, elementType: Class<T>): Set<T>? = getSet(resolve(name), elementType)

    /**
     * Obtain the value of the column named [name] as [Set].
     *
     * @param name The name of the column to get the value for.
     * @param keyType Class representing the Java data type to convert the keys of the designated column to.
     * @param valueType Class representing the Java data type to convert the values of the designated column to.
     * @throws IllegalArgumentException if the column index is not valid for this reader or this type.
     * @return The value of the column as `Map` or `null` if the column is null.
     */
    public fun <K, V> getMap(name: String, keyType: Class<K>, valueType: Class<V>): Map<K, V>? =
        getMap(resolve(name), keyType, valueType)

    /**
     * Closes the reader so that no further iteration or data access can be made.
     */
    public override fun close()
}
