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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Rewrites a topology written in the deprecated `opendc-compute-topology` JSON format into the shape
 * of the SDK's `Topology`.
 *
 * The two formats disagree about *names*, never about *magnitudes*: both decode their numbers with
 * the same `opendc-common` unit serializers, so a bare `2100` still means 2100 MHz and a bare
 * `100000` still means 100000 MiB. Every value is therefore carried over untouched and only keys and
 * discriminators are translated.
 */
internal fun JsonObject.toSdkTopology(): JsonObject =
    buildJsonObject {
        put("clusters", JsonArray(arrayAt("clusters").map { it.asObject("a cluster").toSdkCluster() }))
    }

/**
 * The legacy format parsed `count` but never acted on it, so a cluster was always instantiated once;
 * the SDK honours it and replicates the cluster. It is carried over rather than pinned to 1 because
 * that is what the field always claimed to mean, and no topology in the repository sets it.
 */
private fun JsonObject.toSdkCluster(): JsonObject =
    buildJsonObject {
        keep(this@toSdkCluster, "name", "count")
        put("hosts", JsonArray(arrayAt("hosts").map { it.asObject("a host").toSdkHost() }))
        optionalObjectAt("powerSource")?.let { put("powerSource", it.toSdkPowerSource()) }
        optionalObjectAt("battery")?.let { put("battery", it.toSdkBattery()) }
    }

private fun JsonObject.toSdkHost(): JsonObject =
    buildJsonObject {
        keep(this@toSdkHost, "name", "count")
        put("cpu", objectAt("cpu").toSdkCpu())
        put("memory", objectAt("memory").toSdkMemory())
        optionalObjectAt("gpu")?.let { put("gpu", it.toSdkGpu()) }
        optionalObjectAt("cpuPowerModel")?.let { put("cpuPowerModel", it.toSdkPowerModel()) }
        optionalObjectAt("gpuPowerModel")?.let { put("gpuPowerModel", it.toSdkPowerModel()) }
        optionalObjectAt("cpuDistributionPolicy")?.let { put("cpuDistribution", it.toSdkDistributionPolicy()) }
        optionalObjectAt("gpuDistributionPolicy")?.let { put("gpuDistribution", it.toSdkDistributionPolicy()) }
    }

private fun JsonObject.toSdkCpu(): JsonObject =
    buildJsonObject {
        keep(this@toSdkCpu, "coreCount", "coreSpeed", "count", "vendor", "modelName")
        rename(this@toSdkCpu, from = "arch", to = "architecture")
    }

private fun JsonObject.toSdkMemory(): JsonObject =
    buildJsonObject {
        keep(this@toSdkMemory, "vendor", "modelName")
        rename(this@toSdkMemory, from = "memorySize", to = "size")
        rename(this@toSdkMemory, from = "memorySpeed", to = "speed")
        rename(this@toSdkMemory, from = "arch", to = "architecture")
    }

private fun JsonObject.toSdkGpu(): JsonObject =
    buildJsonObject {
        keep(this@toSdkGpu, "coreCount", "coreSpeed", "count", "memoryBandwidth", "vendor", "modelName", "architecture")
        rename(this@toSdkGpu, from = "memorySize", to = "memory")
        optionalObjectAt("virtualizationOverHeadModel")?.let { put("virtualizationOverhead", it.toSdkVirtualizationOverhead()) }
    }

/** The legacy discriminators of a virtualization overhead model, keyed by their SDK spelling. */
private val OVERHEAD_MODELS = mapOf("NONE" to "none", "CONSTANT" to "constant", "SHARE_BASED" to "shareBased")

/**
 * `percentageOverhead` is a 0.0–1.0 fraction in both formats. The legacy `-1.0` sentinel meant
 * "unset", which the SDK expresses as an absent (null) value.
 */
private fun JsonObject.toSdkVirtualizationOverhead(): JsonObject =
    buildJsonObject {
        put("type", JsonPrimitive(translate(tag("a virtualization overhead model"), OVERHEAD_MODELS, "virtualization overhead model")))
        this@toSdkVirtualizationOverhead["percentageOverhead"]
            ?.takeUnless { it is JsonPrimitive && it.content.toDoubleOrNull() == -1.0 }
            ?.let { put("percentageOverhead", it) }
    }

/** The fields each power model reads, keyed by its `modelType`, which both formats spell alike. */
private val POWER_MODELS =
    listOf("maxPower", "idlePower").let { maxIdle ->
        mapOf(
            "constant" to listOf("power"),
            "linear" to maxIdle,
            "square" to maxIdle,
            "cubic" to maxIdle,
            "sqrt" to maxIdle,
            "mse" to maxIdle + "calibrationFactor",
            "asymptotic" to maxIdle + listOf("asymUtil", "dvfs"),
        )
    }

/** The legacy defaults of the fields the SDK requires but the legacy format did not. */
private val POWER_MODEL_DEFAULTS =
    mapOf(
        "power" to JsonPrimitive(400),
        "calibrationFactor" to JsonPrimitive(1.0),
        "asymUtil" to JsonPrimitive(0.0),
    )

/**
 * The legacy format gave every power model the same fields, whether its `modelType` read them or not,
 * so only the fields of its own SDK type are carried over. A field the legacy format defaulted but the SDK requires
 * is written out with its legacy default.
 */
private fun JsonObject.toSdkPowerModel(): JsonObject =
    buildJsonObject {
        val modelType =
            stringAt("modelType") ?: throw LegacyFormatException("a power model is missing its 'modelType'")
        val fields =
            POWER_MODELS[modelType] ?: throw LegacyFormatException(
                "unknown power model '$modelType' (expected one of ${POWER_MODELS.keys.joinToString(", ")})",
            )
        put("type", JsonPrimitive(modelType))
        for (field in fields) {
            (this@toSdkPowerModel[field] ?: POWER_MODEL_DEFAULTS[field])?.let { put(field, it) }
        }
    }

private fun JsonObject.toSdkPowerSource(): JsonObject =
    buildJsonObject {
        keep(this@toSdkPowerSource, "name", "maxPower")
        stringAt("carbonTracePath")?.let { put("carbon", namedReference(it)) }
    }

/**
 * Battery policies already share their discriminators and their fields, so they are copied verbatim. The legacy
 * capacity and charge count kWh, while a bare number in an SDK `Energy` counts Joule, so their unit is spelled out.
 */
private fun JsonObject.toSdkBattery(): JsonObject =
    buildJsonObject {
        keep(this@toSdkBattery, "name", "embodiedCarbon", "expectedLifetime")
        keepIn(this@toSdkBattery, "kWh", "capacity", "initialCharge")
        keepIn(this@toSdkBattery, "W", "chargingSpeed")
        rename(this@toSdkBattery, from = "batteryPolicy", to = "policy")
    }

/** The legacy discriminators of a distribution policy, keyed by their SDK spelling. */
private val DISTRIBUTION_POLICIES =
    mapOf(
        "MAX_MIN_FAIRNESS" to "maxMinFairness",
        "EQUAL_SHARE" to "equalShare",
        "FIRST_FIT" to "firstFit",
        "FIXED_SHARE" to "fixedShare",
        "BEST_EFFORT" to "bestEffort",
    )

private fun JsonObject.toSdkDistributionPolicy(): JsonObject =
    buildJsonObject {
        put("type", JsonPrimitive(translate(tag("a distribution policy"), DISTRIBUTION_POLICIES, "distribution policy")))
        keep(this@toSdkDistributionPolicy, "shareRatio")
        rename(this@toSdkDistributionPolicy, from = "updateIntervalLength", to = "updateIntervalMs")
    }
