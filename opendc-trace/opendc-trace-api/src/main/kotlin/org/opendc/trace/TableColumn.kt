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

import java.util.*

/**
 * A column in a trace table.
 *
 * @param name The universal name of this column.
 */
public class TableColumn<out T>(public val name: String, type: Class<T>) {
    /**
     * The type of the column.
     */
    private val type: Class<*> = type

    /**
     * Determine whether the type of the column is a subtype of [column].
     */
    public fun isAssignableTo(column: TableColumn<*>): Boolean {
        return type.isAssignableFrom(column.type)
    }

    /**
     * Compute a hash code for this column.
     */
    public override fun hashCode(): Int = Objects.hash(name, type)

    /**
     * Determine whether this column is equal to [other].
     */
    public override fun equals(other: Any?): Boolean = other is TableColumn<*> && name == other.name && type == other.type

    /**
     * Return a string representation of this column.
     */
    public override fun toString(): String = "TableColumn[$name,$type]"
}
