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

@file:JvmName("TableColumnConversions")

package org.opendc.trace.util

import org.opendc.trace.TableColumnType
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Helper method to convert a [List] into a [List] with elements of [targetElementType].
 */
public fun <T> TableColumnType.List.convertTo(value: List<*>?, targetElementType: Class<T>): List<T>? {
    require(elementType.isCompatible(targetElementType)) { "Target element type is not compatible with $elementType" }
    @Suppress("UNCHECKED_CAST")
    return value as List<T>?
}

/**
 * Helper method to convert a [Set] into a [Set] with elements of [targetElementType].
 */
public fun <T> TableColumnType.Set.convertTo(value: Set<*>?, targetElementType: Class<T>): Set<T>? {
    require(elementType.isCompatible(targetElementType)) { "Target element type is not compatible with $elementType" }
    @Suppress("UNCHECKED_CAST")
    return value as Set<T>?
}

/**
 * Helper method to convert a [Map] into a [Map] with [targetKeyType] keys and [targetValueType] values.
 */
public fun <K, V> TableColumnType.Map.convertTo(value: Map<*, *>?, targetKeyType: Class<K>, targetValueType: Class<V>): Map<K, V>? {
    require(keyType.isCompatible(targetKeyType)) { "Target key type $targetKeyType is not compatible with $keyType" }
    require(valueType.isCompatible(targetValueType)) { "Target value type $targetValueType is not compatible with $valueType" }
    @Suppress("UNCHECKED_CAST")
    return value as Map<K, V>?
}

/**
 * Helper method to determine [javaType] is compatible with this [TableColumnType].
 */
private fun TableColumnType.isCompatible(javaType: Class<*>): Boolean {
    return when (this) {
        is TableColumnType.Boolean -> javaType.isAssignableFrom(Boolean::class.java)
        is TableColumnType.Int -> javaType.isAssignableFrom(Int::class.java)
        is TableColumnType.Long -> javaType.isAssignableFrom(Long::class.java)
        is TableColumnType.Float -> javaType.isAssignableFrom(Float::class.java)
        is TableColumnType.Double -> javaType.isAssignableFrom(Double::class.java)
        is TableColumnType.String -> javaType.isAssignableFrom(String::class.java)
        is TableColumnType.UUID -> javaType.isAssignableFrom(UUID::class.java)
        is TableColumnType.Instant -> javaType.isAssignableFrom(Instant::class.java)
        is TableColumnType.Duration -> javaType.isAssignableFrom(Duration::class.java)
        is TableColumnType.List -> javaType.isAssignableFrom(List::class.java)
        is TableColumnType.Set -> javaType.isAssignableFrom(Set::class.java)
        is TableColumnType.Map -> javaType.isAssignableFrom(Map::class.java)
    }
}
