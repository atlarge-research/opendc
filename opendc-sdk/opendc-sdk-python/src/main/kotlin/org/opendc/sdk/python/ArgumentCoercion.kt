/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.sdk.python

import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.isSubclassOf

/**
 * Converts a [value] from Python to the parameter [type], or throws an [ArgumentBindingException] whose message completes
 * "'parameter' ...". A value class accepts only its own instances, and `null` fits only a nullable type.
 */
internal fun coerce(
    value: Any?,
    type: KType,
): Any? {
    // kotlin-reflect would turn a null unit into zero
    if (value == null) {
        if (type.isMarkedNullable) {
            return null
        }
        throw ArgumentBindingException("must be a $type, not None")
    }

    // JPype passes ints as Long, floats as Double and collections as List, Set or Map, which are narrowed or rebuilt
    return when (val classifier = type.classifier) {
        is KClass<*> -> coerceTo(value, classifier, type)
        is KTypeParameter -> {
            val bounds = classifier.upperBounds.mapNotNull { it.classifier as? KClass<*> }
            if (bounds.all { it.isInstance(value) }) value else throw mismatch(value, type)
        }
        else -> value
    }
}

private fun coerceTo(
    value: Any,
    classifier: KClass<*>,
    type: KType,
): Any =
    when {
        classifier == Byte::class -> integral(value, type, Byte.MIN_VALUE.toLong(), Byte.MAX_VALUE.toLong()).toByte()
        classifier == Short::class -> integral(value, type, Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong()).toShort()
        classifier == Int::class -> integral(value, type, Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
        classifier == Long::class -> integral(value, type, Long.MIN_VALUE, Long.MAX_VALUE)
        classifier == Float::class -> real(value, type).toFloat()
        classifier == Double::class -> real(value, type).toDouble()
        classifier == Char::class -> character(value, type)
        classifier == Path::class && value is String -> pathOf(value)
        classifier == File::class && value is String -> File(value)
        classifier.isSubclassOf(Map::class) -> rebuilt(entriesOf(value, type), classifier, value, type)
        classifier.isSubclassOf(Set::class) ->
            rebuilt(elementsOf(value, type).mapTo(LinkedHashSet()) { coerceArgumentOf(it, type, 0) }, classifier, value, type)
        // Iterable itself or a collection, not any Iterable: a Path is Iterable too
        classifier == Iterable::class || classifier.isSubclassOf(Collection::class) ->
            rebuilt(elementsOf(value, type).mapTo(ArrayList()) { coerceArgumentOf(it, type, 0) }, classifier, value, type)
        classifier.java.isArray -> arrayFrom(value, classifier, type)
        classifier.java.isEnum && value is String -> enumConstant(value, classifier)
        classifier.isInstance(value) -> value
        else -> throw mismatch(value, type)
    }

/** Returns the [collection], already rebuilt with its elements coerced, if [classifier] accepts its class. */
private fun rebuilt(
    collection: Any,
    classifier: KClass<*>,
    value: Any,
    type: KType,
): Any = if (classifier.isInstance(collection)) collection else throw mismatch(value, type)

private fun pathOf(value: String): Path =
    try {
        Path.of(value)
    } catch (failure: InvalidPathException) {
        throw ArgumentBindingException("must be a path, and ${failure.message}")
    }

private fun integral(
    value: Any,
    type: KType,
    min: Long,
    max: Long,
): Long {
    if (value !is Long && value !is Int && value !is Short && value !is Byte) {
        throw mismatch(value, type)
    }

    val number = (value as Number).toLong()
    if (number !in min..max) {
        throw ArgumentBindingException("must be a $type, and $number is out of its range")
    }
    return number
}

private fun real(
    value: Any,
    type: KType,
): Number = value as? Number ?: throw mismatch(value, type)

private fun character(
    value: Any,
    type: KType,
): Char =
    when {
        value is Char -> value
        value is String && value.length == 1 -> value.single()
        else -> throw mismatch(value, type)
    }

private fun elementsOf(
    value: Any,
    type: KType,
): List<Any?> =
    when {
        value is Collection<*> -> value.toList()
        value.javaClass.isArray -> List(java.lang.reflect.Array.getLength(value)) { java.lang.reflect.Array.get(value, it) }
        else -> throw mismatch(value, type)
    }

private fun entriesOf(
    value: Any,
    type: KType,
): Map<Any?, Any?> {
    if (value !is Map<*, *>) {
        throw mismatch(value, type)
    }

    return value.entries.associateTo(LinkedHashMap()) { (key, element) ->
        coerceArgumentOf(key, type, 0) to coerceArgumentOf(element, type, 1)
    }
}

/** A primitive array such as `IntArray`, or an object array such as `Array<String>`, built from a list. */
private fun arrayFrom(
    value: Any,
    classifier: KClass<*>,
    type: KType,
): Any {
    val componentType = classifier.java.componentType
    val elements = elementsOf(value, type)
    val array = java.lang.reflect.Array.newInstance(componentType, elements.size)

    elements.forEachIndexed { index, element ->
        val coerced =
            if (componentType.isPrimitive) {
                coerceTo(element ?: throw mismatch(value, type), componentType.kotlin, type)
            } else {
                coerceArgumentOf(element, type, 0)
            }
        java.lang.reflect.Array.set(array, index, coerced)
    }

    return array
}

/** Coerces [value] to the type argument at [index] of [type], or passes it on when that argument is a star. */
private fun coerceArgumentOf(
    value: Any?,
    type: KType,
    index: Int,
): Any? {
    val argument = type.arguments.getOrNull(index)?.type ?: return value
    return coerce(value, argument)
}

private fun enumConstant(
    name: String,
    classifier: KClass<*>,
): Any {
    val constants = classifier.java.enumConstants.associateBy { (it as Enum<*>).name }
    return constants.getValue(resolveName(name, constants.keys, classifier.java.name))
}

private fun mismatch(
    value: Any,
    type: KType,
): ArgumentBindingException = ArgumentBindingException("must be a $type, not a ${value.javaClass.name}")
