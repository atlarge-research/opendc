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

package org.opendc.cli

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.opendc.sdk.model.experiment.ExperimentSpec
import org.opendc.sdk.model.serialization.SdkJson
import java.io.File

/** The key that names the file an object takes the rest of its fields from. */
private const val IMPORT_KEY = "importFrom"

/**
 * Reads an SDK-model experiment, resolving the `importFrom` references it composes itself from.
 *
 * Any object in the document — the experiment itself, a topology, a cluster, a host, a workload — may
 * name a file to take the rest of its fields from:
 *
 * ```json
 * "topologies": [ { "importFrom": "topologies/surfsara.json" } ]
 * ```
 *
 * The imported file supplies the object's fields; the keys written alongside `importFrom` supply the
 * rest and win where the two disagree, so an import can be adopted and then adjusted:
 *
 * ```json
 * { "importFrom": "hosts/big-host.json", "count": 64 }
 * ```
 *
 * A key is overridden whole — an object-valued key replaces the imported object rather than merging
 * into it — so what a document says locally is always what it means. Imports nest, and each file's
 * relative paths are read against *its own* directory, so an imported file composes further files
 * exactly as the experiment does. This is a pure source-level composition: once resolved, the document
 * is ordinary SDK JSON and goes through [SdkJson] unchanged.
 *
 * Composition is source-level, so [strict] is checked only once, against the fully resolved document:
 * an unknown key anywhere in it — the experiment or any file it imports — fails the read.
 *
 * @throws ImportException if an import is missing, is not a JSON object, or forms a cycle.
 */
internal fun readExperiment(
    file: File,
    strict: Boolean = false,
): ExperimentSpec {
    val start = file.canonicalFile
    val document = SdkJson.json.parseToJsonElement(start.readText()).asImportable(start)
    return SdkJson.fromJsonElement(document.resolveImports(start.parentFile, listOf(start)), strict)
}

/** Signals an `importFrom` that cannot be resolved. */
internal class ImportException(message: String) : IllegalArgumentException(message)

/**
 * Resolves every `importFrom` at or below this element. [baseDir] is the directory of the file that
 * declares them and [chain] the files being resolved, innermost last, which is what makes a cycle
 * visible rather than infinite.
 */
private fun JsonElement.resolveImports(
    baseDir: File,
    chain: List<File>,
): JsonElement =
    when (this) {
        is JsonObject -> resolveImports(baseDir, chain)
        is JsonArray -> JsonArray(map { it.resolveImports(baseDir, chain) })
        else -> this
    }

private fun JsonObject.resolveImports(
    baseDir: File,
    chain: List<File>,
): JsonObject {
    val local =
        JsonObject(
            filterKeys { it != IMPORT_KEY }
                .mapValues { (_, value) -> value.resolveImports(baseDir, chain) },
        )
    val path = importPath() ?: return local
    // The import supplies the fields the object does not state itself, so the local keys go on last.
    return JsonObject(import(path, baseDir, chain) + local)
}

/** The file this object imports the rest of its fields from, or `null` when it names none. */
private fun JsonObject.importPath(): String? =
    when (val value = this[IMPORT_KEY]) {
        null -> null
        is JsonPrimitive -> if (value.isString) value.content else throw ImportException("'$IMPORT_KEY' must be a path")
        else -> throw ImportException("'$IMPORT_KEY' must be a path")
    }

/** Reads the file at [path] and resolves the imports *it* declares, against its own directory. */
private fun import(
    path: String,
    baseDir: File,
    chain: List<File>,
): JsonObject {
    val file = File(path).let { if (it.isAbsolute) it else File(baseDir, path) }.canonicalFile
    if (file in chain) {
        throw ImportException("'$path' imports itself: ${(chain + file).joinToString(" -> ") { it.name }}")
    }
    if (!file.isFile) {
        throw ImportException("the imported file '$path' does not exist (looked in ${file.path})")
    }
    val imported = SdkJson.json.parseToJsonElement(file.readText()).asImportable(file)
    return imported.resolveImports(file.parentFile, chain + file)
}

private fun JsonElement.asImportable(file: File): JsonObject =
    this as? JsonObject ?: throw ImportException("'${file.name}' must contain a JSON object")
