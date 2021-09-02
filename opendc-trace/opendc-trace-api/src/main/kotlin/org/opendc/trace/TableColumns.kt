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

@file:JvmName("TableColumns")
package org.opendc.trace

/**
 * Construct a [TableColumn] with [Any] type.
 */
public fun objectColumn(name: String): TableColumn<Any> = TableColumn(name, Any::class.java)

/**
 * Construct a [TableColumn] with a [String] type.
 */
public fun stringColumn(name: String): TableColumn<String> = TableColumn(name, String::class.java)

/**
 * Construct a [TableColumn] with a [Number] type.
 */
public fun numberColumn(name: String): TableColumn<Number> = TableColumn(name, Number::class.java)

/**
 * Construct a [TableColumn] with an [Int] type.
 */
public fun intColumn(name: String): TableColumn<Int> = TableColumn(name, Int::class.java)

/**
 * Construct a [TableColumn] with a [Long] type.
 */
public fun longColumn(name: String): TableColumn<Long> = TableColumn(name, Long::class.java)

/**
 * Construct a [TableColumn] with a [Double] type.
 */
public fun doubleColumn(name: String): TableColumn<Double> = TableColumn(name, Double::class.java)

/**
 * Construct a [TableColumn] with a [Boolean] type.
 */
public fun booleanColumn(name: String): TableColumn<Boolean> = TableColumn(name, Boolean::class.java)
