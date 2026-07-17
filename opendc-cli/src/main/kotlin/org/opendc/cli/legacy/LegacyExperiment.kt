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

package org.opendc.cli.legacy

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.opendc.sdk.model.experiment.Experiment
import org.opendc.sdk.model.serialization.SdkJson
import java.io.File

/**
 * Reads an experiment written in the deprecated `opendc-experiments-base` JSON format, the format the
 * `opendc` CLI accepts under `--legacy`.
 *
 * The old and the new documents describe the same experiment; they disagree about the names of things
 * and about a single unit. Rather than carry a second copy of the data model, the old document is
 * rewritten into the shape of the new one and handed to the SDK parser, so the result is an ordinary,
 * fully validated [Experiment].
 *
 * Two differences are worth knowing about:
 *  - The old format referenced its topologies by path. They are read and inlined here, which is why
 *    this needs the file rather than a stream. A relative path is resolved against [baseDir], which
 *    the CLI sets to the working directory because that is what the deprecated runner resolved
 *    against — an old experiment names its topologies and traces relative to where it is *run from*,
 *    not to where it is *stored*. The workload, carbon and failure traces keep their paths verbatim
 *    and are resolved the same way later, so both halves share one base.
 *  - `outputFolder` has no counterpart in the SDK model and is dropped; `opendc run -o` decides where
 *    results are written.
 *
 * @throws LegacyFormatException if the document cannot be expressed in the SDK model.
 */
internal fun readLegacyExperiment(
    file: File,
    baseDir: File,
): Experiment {
    val document = SdkJson.json.parseToJsonElement(file.readText()).asObject("the experiment")
    return SdkJson.fromJsonElement(document.toSdkExperiment(baseDir))
}

/** Signals a legacy document that cannot be expressed in the SDK model. */
internal class LegacyFormatException(message: String) : IllegalArgumentException(message)

private fun JsonObject.toSdkExperiment(baseDir: File): JsonObject =
    buildJsonObject {
        keep(this@toSdkExperiment, "name", "runs", "initialSeed", "maxNumFailures")
        put("topologies", JsonArray(arrayAt("topologies").map { it.asObject("a topology").inlineTopology(baseDir) }))
        put("workloads", JsonArray(arrayAt("workloads").map { it.asObject("a workload").toSdkWorkload() }))
        optionalArrayAt("allocationPolicies")?.let { policies ->
            put("allocationPolicies", JsonArray(policies.map { it.asObject("an allocation policy").toSdkAllocationPolicy() }))
        }
        optionalArrayAt("failureModels")?.let { models ->
            put("failureModels", JsonArray(models.map { it.toSdkFailureModel() }))
        }
        optionalArrayAt("checkpointModels")?.let { models ->
            put("checkpointModels", JsonArray(models.map { it.toSdkCheckpointModel() }))
        }
        optionalArrayAt("exportModels")?.let { models ->
            put("exportModels", JsonArray(models.map { it.asObject("an export model").toSdkExportModel() }))
        }
    }

/** Reads the topology this legacy reference points at and inlines it, as the SDK model expects. */
private fun JsonObject.inlineTopology(baseDir: File): JsonObject {
    val path = stringAt("pathToFile") ?: throw LegacyFormatException("a topology is missing its 'pathToFile'")
    val topology = File(path).let { if (it.isAbsolute) it else File(baseDir, path) }
    if (!topology.isFile) {
        throw LegacyFormatException("the topology '$path' does not exist (looked in ${topology.absolutePath})")
    }
    return SdkJson.json.parseToJsonElement(topology.readText()).asObject("the topology '$path'").toSdkTopology()
}

/**
 * Every legacy workload replayed a trace from disk — `ComputeWorkload` was the only kind — so it
 * always becomes the SDK's `trace` workload. `scalingPolicy` is spelled identically in both formats.
 */
private fun JsonObject.toSdkWorkload(): JsonObject =
    buildJsonObject {
        put("type", JsonPrimitive("trace"))
        val path = stringAt("pathToFile") ?: throw LegacyFormatException("a workload is missing its 'pathToFile'")
        put("source", namedReference(path))
        keep(this@toSdkWorkload, "sampleFraction", "submissionTime", "deferAll", "scalingPolicy")
    }

/** The legacy discriminators of a host filter, keyed by their SDK spelling. */
private val HOST_FILTERS =
    mapOf(
        "Compute" to "compute",
        "SameHost" to "sameHost",
        "DifferentHost" to "differentHost",
        "InstanceCount" to "instanceCount",
        "Ram" to "ram",
        "VCpuCapacity" to "vcpuCapacity",
        "VCpu" to "vcpu",
    )

/** The legacy discriminators of a host weigher, keyed by their SDK spelling. */
private val HOST_WEIGHERS =
    mapOf(
        "Ram" to "ram",
        "CoreRam" to "coreRam",
        "InstanceCount" to "instanceCount",
        "VCpuCapacity" to "vcpuCapacity",
        "VCpu" to "vcpu",
    )

/**
 * Scheduler names, the task stopper and the timeshift thresholds are spelled identically in both
 * formats; only the policy's own key for the prefab name and the filter and weigher discriminators
 * differ. A prefab policy without a `policyName` keeps falling back to the default scheduler, exactly
 * as it did before.
 */
private fun JsonObject.toSdkAllocationPolicy(): JsonObject =
    when (val type = tag("an allocation policy")) {
        "prefab" ->
            buildJsonObject {
                put("type", JsonPrimitive("prefab"))
                rename(this@toSdkAllocationPolicy, from = "policyName", to = "prefabName")
            }
        "filter", "timeshift" ->
            buildJsonObject {
                put("type", JsonPrimitive(type))
                keep(
                    this@toSdkAllocationPolicy,
                    "subsetSize", "windowSize", "forecast", "shortForecastThreshold",
                    "longForecastThreshold", "forecastSize", "taskStopper", "memorize",
                )
                optionalArrayAt("filters")?.let { put("filters", it.toSdkHostFilters()) }
                optionalArrayAt("weighers")?.let { put("weighers", it.toSdkHostWeighers()) }
            }
        else ->
            throw LegacyFormatException(
                "unknown legacy allocation policy '$type' (expected one of prefab, filter, timeshift)",
            )
    }

private fun JsonArray.toSdkHostFilters(): JsonArray =
    JsonArray(
        map { element ->
            val legacy = element.asObject("a host filter")
            buildJsonObject {
                put("type", JsonPrimitive(translate(legacy.tag("a host filter"), HOST_FILTERS, "host filter")))
                keep(legacy, "limit", "allocationRatio")
            }
        },
    )

private fun JsonArray.toSdkHostWeighers(): JsonArray =
    JsonArray(
        map { element ->
            val legacy = element.asObject("a host weigher")
            buildJsonObject {
                put("type", JsonPrimitive(translate(legacy.tag("a host weigher"), HOST_WEIGHERS, "host weigher")))
                keep(legacy, "multiplier")
            }
        },
    )

/** The SDK spells "inject no failures" as a model of its own, which is also what a `null` element meant. */
private val NO_FAILURE = buildJsonObject { put("type", JsonPrimitive("none")) }

/** The distributions of a custom failure model already share their discriminators, so they are copied verbatim. */
private fun JsonElement.toSdkFailureModel(): JsonObject {
    if (this is JsonNull) return NO_FAILURE
    val legacy = asObject("a failure model")
    return when (val type = legacy.tag("a failure model")) {
        "no" -> NO_FAILURE
        "trace-based" ->
            buildJsonObject {
                put("type", JsonPrimitive("traceBased"))
                val path =
                    legacy.stringAt("pathToFile")
                        ?: throw LegacyFormatException("a trace-based failure model is missing its 'pathToFile'")
                put("source", namedReference(path))
                keep(legacy, "startPoint", "repeat")
            }
        "prefab" ->
            buildJsonObject {
                put("type", JsonPrimitive("prefab"))
                keep(legacy, "prefabName")
            }
        "custom" ->
            buildJsonObject {
                put("type", JsonPrimitive("custom"))
                rename(legacy, from = "iatSampler", to = "interArrival")
                rename(legacy, from = "durationSampler", to = "duration")
                rename(legacy, from = "nohSampler", to = "hostFraction")
            }
        else ->
            throw LegacyFormatException(
                "unknown legacy failure model '$type' (expected one of no, trace-based, prefab, custom)",
            )
    }
}

/**
 * Checkpoint intervals and durations already counted milliseconds, which is exactly what a bare number
 * means to an SDK `TimeDelta`, so the numbers cross over untouched.
 */
private fun JsonElement.toSdkCheckpointModel(): JsonElement {
    if (this is JsonNull) return JsonNull
    val legacy = asObject("a checkpoint model")
    return buildJsonObject {
        rename(legacy, from = "checkpointInterval", to = "interval")
        rename(legacy, from = "checkpointDuration", to = "duration")
        rename(legacy, from = "checkpointIntervalScaling", to = "intervalScaling")
    }
}

private fun JsonObject.toSdkExportModel(): JsonObject =
    buildJsonObject {
        keep(this@toSdkExportModel, "printFrequency", "filesToExport")
        this@toSdkExportModel["exportInterval"]?.let { put("exportInterval", it.toExportInterval()) }
        optionalObjectAt("computeExportConfig")?.let { put("columns", it.toSdkExportColumns()) }
    }

/**
 * The one magnitude the two formats genuinely disagree about. A legacy export interval counts
 * *seconds*, while a bare number in an SDK `TimeDelta` counts *milliseconds*; spelling the unit out
 * keeps `3600` an hour instead of silently turning it into 3.6 seconds.
 */
private fun JsonElement.toExportInterval(): JsonPrimitive {
    val seconds =
        (this as? JsonPrimitive)?.takeUnless { it.isString }?.content
            ?: throw LegacyFormatException("'exportInterval' must be a number of seconds")
    return JsonPrimitive("$seconds seconds")
}

/**
 * A legacy `computeExportConfig` lists the columns of each output file by hand; the SDK models the
 * same choice as a per-file column selection. Output files the legacy config leaves unmentioned fall
 * back to all of their columns.
 */
private fun JsonObject.toSdkExportColumns(): JsonObject =
    buildJsonObject {
        select(this@toSdkExportColumns, from = "hostExportColumns", to = "host")
        select(this@toSdkExportColumns, from = "taskExportColumns", to = "task")
        select(this@toSdkExportColumns, from = "powerSourceExportColumns", to = "powerSource")
        select(this@toSdkExportColumns, from = "batteryExportColumns", to = "battery")
        select(this@toSdkExportColumns, from = "serviceExportColumns", to = "service")
    }

private fun JsonObjectBuilder.select(
    source: JsonObject,
    from: String,
    to: String,
) {
    val columns = source[from] as? JsonArray ?: return
    put(
        to,
        buildJsonObject {
            put("type", JsonPrimitive("only"))
            put("columns", columns)
        },
    )
}

// The shared vocabulary the two rewrites are written in. Everything below reads a legacy document or
// builds an SDK one; nothing here interprets a value.

/** A `ResourceReference` naming [path], resolved at run time against the experiment's input root. */
internal fun namedReference(path: String): JsonObject =
    buildJsonObject {
        put("type", JsonPrimitive("named"))
        put("name", JsonPrimitive(path))
    }

/** This element as an object; fails describing it as [what] when it is anything else. */
internal fun JsonElement.asObject(what: String): JsonObject =
    this as? JsonObject ?: throw LegacyFormatException("$what must be a JSON object")

/** The object at [key]; fails naming [key] when it is absent or is not an object. */
internal fun JsonObject.objectAt(key: String): JsonObject =
    this[key] as? JsonObject ?: throw LegacyFormatException("'$key' is missing or is not a JSON object")

/** The object at [key], or `null` when it is absent. */
internal fun JsonObject.optionalObjectAt(key: String): JsonObject? = this[key] as? JsonObject

/** The array at [key]; fails naming [key] when it is absent or is not an array. */
internal fun JsonObject.arrayAt(key: String): JsonArray =
    this[key] as? JsonArray ?: throw LegacyFormatException("'$key' is missing or is not a JSON array")

/** The array at [key], or `null` when it is absent. */
internal fun JsonObject.optionalArrayAt(key: String): JsonArray? = this[key] as? JsonArray

/** The string at [key], or `null` when it is absent. */
internal fun JsonObject.stringAt(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

/** The discriminator of this legacy sealed type; fails describing it as [what] when it is absent. */
internal fun JsonObject.tag(what: String): String = stringAt("type") ?: throw LegacyFormatException("$what is missing its 'type'")

/** Maps the legacy [tag] to its SDK spelling, failing with the accepted spellings when it is unknown. */
internal fun translate(
    tag: String,
    tags: Map<String, String>,
    what: String,
): String =
    tags[tag]
        ?: throw LegacyFormatException("unknown legacy $what '$tag' (expected one of ${tags.keys.joinToString(", ")})")

/** Carries [keys] over from [source] verbatim, skipping the ones it does not declare. */
internal fun JsonObjectBuilder.keep(
    source: JsonObject,
    vararg keys: String,
) {
    for (key in keys) source[key]?.let { put(key, it) }
}

/** Carries `source[from]` over under its SDK name [to], skipping it when [from] is absent. */
internal fun JsonObjectBuilder.rename(
    source: JsonObject,
    from: String,
    to: String,
) {
    source[from]?.let { put(to, it) }
}
