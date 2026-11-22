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

import org.opendc.common.units.Frequency
import org.opendc.sdk.model.experiment.ExperimentSpec
import org.opendc.sdk.runner.OpenDC
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.extensionReceiverParameter

// Values of Metadata.kind
private const val CLASS_KIND = 1
private const val FILE_FACADE_KIND = 2
private const val MULTI_FILE_FACADE_KIND = 4

/** A package to scan in the jar or directory that its [anchor] class was loaded from, without the [excluded] subpackages. */
private class ScannedPackage(val anchor: Class<*>, val path: String, val excluded: List<String> = emptyList())

/**
 * The public classes, objects and top-level declarations of the SDK model and runner and of the units they use, found by
 * scanning their jars so that Python reaches each by name. The DSL is left out, as Python mirrors it by hand.
 */
internal object SdkCatalog {
    private val scanned =
        listOf(
            ScannedPackage(ExperimentSpec::class.java, "org/opendc/sdk/model", excluded = listOf("org/opendc/sdk/model/dsl/")),
            ScannedPackage(OpenDC::class.java, "org/opendc/sdk/runner"),
            ScannedPackage(Frequency::class.java, "org/opendc/common/units"),
        )

    private val declarations: List<Class<*>> by lazy { scanned.flatMap(::classesIn) }

    private val classes: Map<String, KClass<*>> by lazy {
        val publicClasses =
            declarations
                .filter { kindOf(it) == CLASS_KIND }
                .map { it.kotlin }
                .filter { it.visibility == KVisibility.PUBLIC }

        publicClasses.groupBy { checkNotNull(it.simpleName) }.mapValues { (name, sharing) ->
            sharing.singleOrNull() ?: error("Several SDK classes are named $name: ${sharing.joinToString { it.java.name }}")
        }
    }

    /**
     * The public top-level declarations, less those with reified type parameters that reflection cannot call. They are
     * listed through `Reflection`, as kotlin-reflect has no public API for file facades.
     */
    private val facadeMembers: List<KCallable<*>> by lazy {
        declarations
            .filter { kindOf(it) == FILE_FACADE_KIND || kindOf(it) == MULTI_FILE_FACADE_KIND }
            .flatMap { Reflection.getOrCreateKotlinPackage(it).members }
            .filter { it.visibility == KVisibility.PUBLIC && it.typeParameters.none { parameter -> parameter.isReified } }
    }

    private val topLevel: Map<String, List<KCallable<*>>> by lazy {
        facadeMembers.filter { it.extensionReceiverParameter == null }.groupBy { it.name }
    }

    private val extensions: List<KCallable<*>> by lazy { facadeMembers.filter { it.extensionReceiverParameter != null } }

    /** The names of the classes, objects and top-level declarations. */
    val names: List<String> by lazy {
        val shared = classes.keys intersect topLevel.keys
        check(shared.isEmpty()) { "SDK classes and top-level declarations share the names $shared" }

        // Overloaded functions share a name, but a value cannot share its name with anything
        val valuesSharingNames = topLevel.filterValues { group -> group.size > 1 && group.any { it is KProperty<*> } }.keys
        check(valuesSharingNames.isEmpty()) { "Top-level values of the SDK share the names $valuesSharingNames" }

        (classes.keys + topLevel.keys).sorted()
    }

    /** Resolves [name] to a class, an object, a top-level value or a top-level function. */
    fun member(name: String): Attribute {
        val kotlinName = resolveName(name, names, "opendc")
        val kotlinClass = classes[kotlinName]
        if (kotlinClass != null) {
            return kotlinClass.objectInstance?.let { Attribute.Value(it) } ?: Attribute.Type(kotlinClass.java)
        }

        val value = topLevel.getValue(kotlinName).filterIsInstance<KProperty<*>>().singleOrNull()
        return if (value != null) Attribute.Value(value.getter.call()) else Attribute.TopLevelFunction(kotlinName)
    }

    /** The overloads of the top-level function named [name]. */
    fun topLevelFunctions(name: String): List<KFunction<*>> {
        val kotlinName = resolveName(name, topLevel.keys, "opendc")
        val functions = topLevel.getValue(kotlinName).filterIsInstance<KFunction<*>>()
        if (functions.isEmpty()) {
            throw ArgumentBindingException("opendc.$kotlinName is a value, not a function")
        }
        return functions
    }

    /** The extension functions and properties that an instance of [type] can be the receiver of. */
    fun extensionsOf(type: Class<*>): List<KCallable<*>> =
        extensions.filter { extension ->
            val receiver = extension.extensionReceiverParameter?.type?.classifier as? KClass<*>
            receiver != null && receiver.java.isAssignableFrom(type)
        }

    private fun kindOf(declaration: Class<*>): Int = declaration.getAnnotation(Metadata::class.java)?.kind ?: 0

    /** The top-level classes of [scanned], read from the directory or jar that its anchor was loaded from. */
    private fun classesIn(scanned: ScannedPackage): List<Class<*>> {
        val location = Path.of(scanned.anchor.protectionDomain.codeSource.location.toURI())
        val names =
            if (Files.isDirectory(location)) {
                classNamesIn(location, scanned)
            } else {
                FileSystems.newFileSystem(location).use { classNamesIn(it.getPath("/"), scanned) }
            }

        return names.map { Class.forName(it, false, scanned.anchor.classLoader) }
    }

    /** The top-level classes of the package under [root], a directory or the root of a jar. */
    private fun classNamesIn(
        root: Path,
        scanned: ScannedPackage,
    ): List<String> =
        Files.walk(root.resolve(scanned.path)).use { paths ->
            paths
                .map { root.relativize(it).joinToString("/") }
                .filter { it.endsWith(".class") && '$' !in it && scanned.excluded.none(it::startsWith) }
                .map { it.removeSuffix(".class").replace('/', '.') }
                .toList()
        }
}
