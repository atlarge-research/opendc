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

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/** The result of looking a name up on a Kotlin object, a Kotlin class or the `opendc` module. */
sealed interface Attribute {
    /** A value: a property, an enum constant, a Kotlin `object` or a top-level `val`. */
    data class Value(val value: Any?) : Attribute

    /** A function of [receiver] (an object, a companion object, or the receiver of an extension) named [name]. */
    data class MemberFunction(val receiver: Any, val name: String) : Attribute

    /** A top-level function named [name]. */
    data class TopLevelFunction(val name: String) : Attribute

    /** A class, which Python can construct and whose companion members it can reach. */
    data class Type(val type: Class<*>) : Attribute
}

/**
 * The members of a Kotlin class that Python lists and documents: the parameters of its primary constructor, the
 * properties and functions of its instances with extensions, and its enum constants, nested classes and companion members.
 */
data class KotlinMembers(
    val constructorParameters: List<ParameterSignature>,
    val instanceAttributes: List<String>,
    val classAttributes: List<String>,
)

/** A parameter as Python's signature shows it. */
data class ParameterSignature(val name: String, val type: String, val isOptional: Boolean)

/** Members that Python covers itself through equality, hashing and printing, and the `componentN` of data classes. */
private val UNLISTED_NAMES = Regex("equals|hashCode|toString|component\\d+")

/**
 * The entry points through which Python constructs Kotlin objects and calls their members by name. Every parameter and
 * result is a plain JVM type, so JPype calls them directly.
 */
object PythonInterop {
    /** Resolves [name] among the classes, objects and top-level declarations of the SDK, as `opendc.<name>` does. */
    @JvmStatic
    fun sdkMember(name: String): Attribute = unwrapping { SdkCatalog.member(name) }

    /** The names [sdkMember] resolves. */
    @JvmStatic
    fun sdkNames(): List<String> = SdkCatalog.names

    /** Constructs [type] through the public constructor that the arguments fit. */
    @JvmStatic
    fun construct(
        type: Class<*>,
        positional: List<Any?>,
        named: Map<String, Any?>,
    ): Any =
        unwrapping {
            val kotlinClass = type.kotlin
            if (kotlinClass.isAbstract || type.isInterface) {
                throw ArgumentBindingException("${kotlinClass.qualifiedName} is abstract, so it cannot be constructed")
            }

            val constructors = kotlinClass.constructors.filter { it.visibility == KVisibility.PUBLIC }
            if (constructors.isEmpty()) {
                throw ArgumentBindingException("${kotlinClass.qualifiedName} has no public constructor")
            }

            val (constructor, arguments) = select(constructors, null, positional, named)
            checkNotNull(invoke(constructor, arguments))
        }

    /** Resolves [name] on [receiver]: a property is read, a member or extension function is returned to be called. */
    @JvmStatic
    fun attribute(
        receiver: Any,
        name: String,
    ): Attribute =
        unwrapping {
            val properties = propertiesOf(receiver.javaClass)
            val names = (properties + functionsOf(receiver.javaClass)).map { it.name }
            val kotlinName = resolveName(name, names, receiver.javaClass.name)

            val property = properties.firstOrNull { it.name == kotlinName }
            if (property != null) Attribute.Value(property.getter.call(receiver)) else Attribute.MemberFunction(receiver, kotlinName)
        }

    /** Resolves [name] on the class [type]: an enum constant, a nested class, or a member of its companion object. */
    @JvmStatic
    fun classAttribute(
        type: Class<*>,
        name: String,
    ): Attribute =
        unwrapping {
            val constants = enumConstantsOf(type)
            val nested = nestedClassesOf(type)
            val companion = companionOf(type)
            val companionNames = companion?.let { membersOf(it.javaClass) }.orEmpty().map { it.name }
            val kotlinName = resolveName(name, constants.keys + nested.keys + companionNames, type.name)

            val nestedClass = nested[kotlinName]
            when {
                kotlinName in constants -> Attribute.Value(constants.getValue(kotlinName))
                nestedClass != null -> nestedClass.objectInstance?.let { Attribute.Value(it) } ?: Attribute.Type(nestedClass.java)
                // Any other name is a companion member, so there is a companion
                else -> attribute(checkNotNull(companion), kotlinName)
            }
        }

    /** Calls the function named [name] on [receiver]: a member function, or an extension function of the SDK. */
    @JvmStatic
    fun call(
        receiver: Any,
        name: String,
        positional: List<Any?>,
        named: Map<String, Any?>,
    ): Any? =
        unwrapping {
            val functions = functionsOf(receiver.javaClass)
            val kotlinName = resolveName(name, functions.map { it.name }, receiver.javaClass.name)
            val (function, arguments) = select(functions.filter { it.name == kotlinName }, receiver, positional, named)
            resultOf(function, invoke(function, arguments))
        }

    /** Calls the top-level function of the SDK named [name]. */
    @JvmStatic
    fun callTopLevel(
        name: String,
        positional: List<Any?>,
        named: Map<String, Any?>,
    ): Any? =
        unwrapping {
            val (function, arguments) = select(SdkCatalog.topLevelFunctions(name), null, positional, named)
            resultOf(function, invoke(function, arguments))
        }

    /** The members of [type] that Python lists and documents. */
    @JvmStatic
    fun describe(type: Class<*>): KotlinMembers {
        val constructor = type.kotlin.primaryConstructor?.takeIf { it.visibility == KVisibility.PUBLIC }
        val constructorParameters =
            constructor?.parameters.orEmpty().map { ParameterSignature(it.name.orEmpty(), it.type.toString(), it.isOptional) }

        val companionMembers = companionOf(type)?.let { membersOf(it.javaClass) }.orEmpty()
        val classAttributes = enumConstantsOf(type).keys.sorted() + nestedClassesOf(type).keys.sorted() + listedNames(companionMembers)

        return KotlinMembers(constructorParameters, listedNames(membersOf(type)), classAttributes)
    }

    /** The properties and functions of an instance of [type], extensions included. */
    private fun membersOf(type: Class<*>): List<KCallable<*>> = propertiesOf(type) + functionsOf(type)

    /** The properties of an instance of [type], extension properties included. */
    private fun propertiesOf(type: Class<*>): List<KProperty1<*, *>> =
        accessibleClassesOf(type).flatMap { it.memberProperties }.filter { it.visibility == KVisibility.PUBLIC }.distinctBy { it.name } +
            SdkCatalog.extensionsOf(type).filterIsInstance<KProperty1<*, *>>()

    /** The functions of an instance of [type], extension functions included. */
    private fun functionsOf(type: Class<*>): List<KFunction<*>> {
        val members = accessibleClassesOf(type).flatMap { it.memberFunctions }.filter { it.visibility == KVisibility.PUBLIC }
        return members.distinctBy { signature(it) } + SdkCatalog.extensionsOf(type).filterIsInstance<KFunction<*>>()
    }

    /**
     * The classes whose members can be called on an instance of [type]: [type] itself, or, when it cannot be accessed (a
     * private class behind a public interface, or a class internal to the JDK), its public supertypes.
     */
    private fun accessibleClassesOf(type: Class<*>): List<KClass<*>> =
        if (isAccessible(type)) listOf(type.kotlin) else type.kotlin.allSuperclasses.filter { isAccessible(it.java) }

    private fun isAccessible(type: Class<*>): Boolean = Modifier.isPublic(type.modifiers) && type.module.isExported(type.packageName)

    /** The companion object of [type], unless it has none or a private one. */
    private fun companionOf(type: Class<*>): Any? =
        type.kotlin.companionObject?.takeIf { it.visibility == KVisibility.PUBLIC }?.objectInstance

    /** The public nested classes of [type], without its companion and the `$serializer` kotlinx.serialization adds. */
    private fun nestedClassesOf(type: Class<*>): Map<String, KClass<*>> =
        type.kotlin.nestedClasses
            .filter { it.visibility == KVisibility.PUBLIC && !it.isCompanion && it.simpleName?.startsWith("$") == false }
            .associateBy { checkNotNull(it.simpleName) }

    private fun enumConstantsOf(type: Class<*>): Map<String, Any> = type.enumConstants.orEmpty().associateBy { (it as Enum<*>).name }

    private fun listedNames(callables: List<KCallable<*>>): List<String> =
        callables.map { it.name }.filterNot { UNLISTED_NAMES.matches(it) }.distinct().sorted()

    /** A function returning `Unit` returns nothing to Python. */
    private fun resultOf(
        function: KFunction<*>,
        result: Any?,
    ): Any? = if (function.returnType.classifier == Unit::class) null else result

    /** Rethrows what a reflected call threw, so the Kotlin exception reaches Python rather than its reflection wrapper. */
    private inline fun <T> unwrapping(block: () -> T): T =
        try {
            block()
        } catch (exception: InvocationTargetException) {
            throw exception.targetException
        }
}
