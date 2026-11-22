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

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions

/**
 * Thrown when a name or the arguments given from Python fit no Kotlin declaration. Python raises it as a `TypeError` or
 * an `AttributeError`.
 */
class ArgumentBindingException(message: String) : IllegalArgumentException(message)

/** Folds a name to the form its Python and Kotlin spellings share: `core_count` and `coreCount` both fold to `corecount`. */
private fun fold(name: String): String = name.replace("_", "").lowercase()

/**
 * Resolves the [name] a Python caller wrote to the one of the [available] names of [owner] that equals it or, failing
 * that, folds to its form. A fold that matches several names fails.
 */
internal fun resolveName(
    name: String,
    available: Collection<String>,
    owner: String,
): String {
    if (name in available) {
        return name
    }

    // Kotlin tells some names apart by case alone, such as ofGib (gibibits) and ofGiB (gibibytes)
    val matches = available.filter { fold(it) == fold(name) }.distinct().sorted()
    return when (matches.size) {
        1 -> matches.single()
        0 -> throw ArgumentBindingException("$owner has no '$name'; it has ${available.distinct().sorted().joinToString()}")
        else -> throw ArgumentBindingException("'$name' is ambiguous in $owner; use one of ${matches.joinToString()}")
    }
}

/**
 * Picks the one of the [candidates] (the overloads of a function or the constructors of a class) that the [positional]
 * and [named] arguments fit, with its bound arguments. When several fit, the one that needs no default arguments wins.
 */
internal fun <F : KFunction<*>> select(
    candidates: List<F>,
    receiver: Any?,
    positional: List<Any?>,
    named: Map<String, Any?>,
): Pair<F, Map<KParameter, Any?>> {
    if (candidates.isEmpty()) {
        throw ArgumentBindingException("There is nothing to call")
    }

    val failures = mutableListOf<ArgumentBindingException>()
    val fits =
        candidates.mapNotNull { candidate ->
            try {
                candidate to bind(candidate, receiver, positional, named)
            } catch (failure: ArgumentBindingException) {
                failures += failure
                null
            }
        }
    if (fits.isEmpty()) {
        throw failures.singleOrNull() ?: ArgumentBindingException(failures.joinToString("; ") { it.message.orEmpty() })
    }

    // Kotlin overload resolution prefers the fit that needs no default arguments too
    val complete = fits.filter { (candidate, arguments) -> arguments.size == candidate.parameters.size }
    return fits.singleOrNull() ?: complete.singleOrNull()
        ?: throw ArgumentBindingException("The arguments fit ${fits.joinToString(" and ") { signature(it.first) }}")
}

/**
 * Binds the arguments of a Python call to the parameters of [function]: [receiver] to the object it is called on, the
 * [positional] arguments in order, and the [named] arguments by name, each coerced to the type of its parameter.
 */
internal fun bind(
    function: KFunction<*>,
    receiver: Any?,
    positional: List<Any?>,
    named: Map<String, Any?>,
): Map<KParameter, Any?> {
    val values = function.parameters.filter { it.kind == KParameter.Kind.VALUE }
    if (positional.size > values.size) {
        throw ArgumentBindingException("${signature(function)} takes ${values.size} arguments, not ${positional.size}")
    }

    val receivers = function.parameters.filter { it.kind != KParameter.Kind.VALUE }
    val arguments = receivers.associateWith<KParameter, Any?> { receiver }.toMutableMap()
    positional.zip(values).forEach { (value, parameter) -> arguments[parameter] = coerceArgument(value, parameter, function) }

    val names = values.mapNotNull { it.name }
    for ((name, value) in named) {
        val parameterName = resolveName(name, names, signature(function))
        val parameter = values.first { it.name == parameterName }
        if (parameter in arguments) {
            throw ArgumentBindingException("${signature(function)} got two values for '$parameterName'")
        }
        arguments[parameter] = coerceArgument(value, parameter, function)
    }

    val missing = values.filter { it !in arguments && !it.isOptional }
    if (missing.isNotEmpty()) {
        throw ArgumentBindingException("${signature(function)} is missing ${missing.joinToString { "'${it.name}'" }}")
    }

    return arguments
}

/**
 * Calls [function] with the [arguments] from [bind]. An argument that kotlin-reflect refuses becomes an
 * [ArgumentBindingException], while what the function itself throws stays wrapped in an `InvocationTargetException`.
 */
internal fun invoke(
    function: KFunction<*>,
    arguments: Map<KParameter, Any?>,
): Any? {
    val (declaration, declarationArguments) = declarationWithDefaults(function, arguments)

    return try {
        declaration.callBy(declarationArguments)
    } catch (failure: IllegalArgumentException) {
        throw ArgumentBindingException("${signature(function)} cannot take these arguments: ${failure.message}")
    }
}

/**
 * The declaration to call [function] through with [arguments], keyed by its parameters: for a call on a value class that
 * leaves a default out, the interface member [function] overrides, which dispatches to the same override.
 */
private fun declarationWithDefaults(
    function: KFunction<*>,
    arguments: Map<KParameter, Any?>,
): Pair<KFunction<*>, Map<KParameter, Any?>> {
    val owner = function.instanceParameter?.type?.classifier as? KClass<*>
    if (owner == null || !owner.isValue || arguments.size == function.parameters.size) {
        return function to arguments
    }

    // A value class compiles its overrides of interface members without their default arguments
    val parameterNames = function.parameters.map { it.name }
    val overridden =
        owner.allSuperclasses.flatMap { it.memberFunctions }.firstOrNull { candidate ->
            candidate.name == function.name && candidate.parameters.map { it.name } == parameterNames
        } ?: return function to arguments
    return overridden to arguments.mapKeys { (parameter, _) -> overridden.parameters[parameter.index] }
}

private fun coerceArgument(
    value: Any?,
    parameter: KParameter,
    function: KFunction<*>,
): Any? =
    try {
        coerce(value, parameter.type)
    } catch (failure: ArgumentBindingException) {
        throw ArgumentBindingException("${signature(function)}: '${parameter.name}' ${failure.message}")
    }

/** Renders [function] as its Kotlin declaration reads, naming a constructor after its class. */
internal fun signature(function: KFunction<*>): String {
    val name = if (function.name == "<init>") (function.returnType.classifier as KClass<*>).simpleName else function.name
    val parameters = function.parameters.filter { it.kind == KParameter.Kind.VALUE }
    return "$name(${parameters.joinToString { "${it.name}: ${it.type}" }})"
}
