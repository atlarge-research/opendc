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
import org.opendc.sdk.model.experiment.Experiment
import org.opendc.sdk.model.experiment.Scenario
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

    /** Serialize an [Experiment] to a JSON string. */
    public fun encodeToString(experiment: Experiment): String = json.encodeToString(experiment)

    /** Serialize a [Scenario] to a JSON string. */
    public fun encodeToString(scenario: Scenario): String = json.encodeToString(scenario)

    /** Parse an [Experiment] from a JSON string. */
    public fun decodeExperiment(text: String): Experiment = json.decodeFromString(text)

    /** Parse an [Experiment] from a UTF-8 encoded JSON stream. */
    public fun decodeExperiment(input: InputStream): Experiment = decodeExperiment(input.readText())

    /** Parse a [Scenario] from a JSON string. */
    public fun decodeScenario(text: String): Scenario = json.decodeFromString(text)

    /** Parse a [Scenario] from a UTF-8 encoded JSON stream. */
    public fun decodeScenario(input: InputStream): Scenario = decodeScenario(input.readText())

    /** Convert an [Experiment] into a [JsonElement] tree, suitable for jsonb storage. */
    public fun toJsonElement(experiment: Experiment): JsonElement = json.encodeToJsonElement(experiment)

    /** Reconstruct an [Experiment] from a [JsonElement] tree. */
    public fun fromJsonElement(element: JsonElement): Experiment = json.decodeFromJsonElement(element)

    private fun InputStream.readText(): String = readBytes().decodeToString()
}
