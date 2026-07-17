/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.model.serialization

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.opendc.sdk.model.experiment.ExperimentSpec
import org.opendc.sdk.model.experiment.ScenarioSpec
import java.io.InputStream

/**
 * Central JSON facade for the SDK model. Sealed hierarchies serialize automatically through their
 * discriminated subtypes, so no manual serializers module is required.
 */
public object SdkJson {
    /** Shared [Json] instance: tolerant of unknown keys, emits defaults, and pretty-prints. */
    public val json: Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }

    /**
     * Strict counterpart of [json] that rejects an unknown key instead of dropping it silently, so a
     * typo or a stale field fails the parse rather than passing unnoticed. It differs from [json] in
     * nothing else. Selected by the `opendc --strict` flag.
     */
    public val strictJson: Json = Json(from = json) { ignoreUnknownKeys = false }

    /** Serialize an [ExperimentSpec] to a JSON string. */
    public fun encodeToString(experiment: ExperimentSpec): String = json.encodeToString(experiment)

    /** Serialize a [ScenarioSpec] to a JSON string. */
    public fun encodeToString(scenario: ScenarioSpec): String = json.encodeToString(scenario)

    /** Parse an [ExperimentSpec] from a JSON string. */
    public fun decodeExperiment(text: String): ExperimentSpec = json.decodeFromString(text)

    /** Parse an [ExperimentSpec] from a UTF-8 encoded JSON stream. */
    public fun decodeExperiment(input: InputStream): ExperimentSpec = decodeExperiment(input.readText())

    /** Parse a [ScenarioSpec] from a JSON string. */
    public fun decodeScenario(text: String): ScenarioSpec = json.decodeFromString(text)

    /** Parse a [ScenarioSpec] from a UTF-8 encoded JSON stream. */
    public fun decodeScenario(input: InputStream): ScenarioSpec = decodeScenario(input.readText())

    /** Convert an [ExperimentSpec] into a [JsonElement] tree, suitable for jsonb storage. */
    public fun toJsonElement(experiment: ExperimentSpec): JsonElement = json.encodeToJsonElement(experiment)

    /**
     * Reconstruct an [ExperimentSpec] from a [JsonElement] tree. When [strict], an unknown key fails
     * the decode instead of being ignored.
     */
    public fun fromJsonElement(
        element: JsonElement,
        strict: Boolean = false,
    ): ExperimentSpec = (if (strict) strictJson else json).decodeFromJsonElement(element)

    private fun InputStream.readText(): String = readBytes().decodeToString()
}
