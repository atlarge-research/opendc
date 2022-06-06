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

package org.opendc.trace

/**
 * The type of a [TableColumn].
 */
public sealed class TableColumnType {
    /**
     * A column of booleans.
     */
    public object Boolean : TableColumnType()

    /**
     * A column of 32-bit integers.
     */
    public object Int : TableColumnType()

    /**
     * A column of 64-bit integers.
     */
    public object Long : TableColumnType()

    /**
     * A column of 32-bit floats.
     */
    public object Float : TableColumnType()

    /**
     * A column of 64-bit floats.
     */
    public object Double : TableColumnType()

    /**
     * A column of UUIDs.
     */
    public object UUID : TableColumnType()

    /**
     * A column of variable-length strings.
     */
    public object String : TableColumnType()

    /**
     * A column of timestamps, mapping to [java.time.Instant].
     */
    public object Instant : TableColumnType()

    /**
     * A column of durations, mapping to [java.time.Duration]
     */
    public object Duration : TableColumnType()

    /**
     * A column containing embedded lists.
     *
     * @property elementType The type of the elements in the list.
     */
    public data class List(public val elementType: TableColumnType) : TableColumnType()

    /**
     * A column containing embedded sets.
     *
     * @property elementType The type of the elements in the sets.
     */
    public data class Set(public val elementType: TableColumnType) : TableColumnType()

    /**
     * A column containing embedded maps.
     *
     * @property keyType The type of the key.
     * @property valueType The type of the value.
     */
    public data class Map(public val keyType: TableColumnType, public val valueType: TableColumnType) : TableColumnType()
}
