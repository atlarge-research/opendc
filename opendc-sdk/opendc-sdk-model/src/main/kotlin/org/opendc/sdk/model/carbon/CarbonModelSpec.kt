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

package org.opendc.sdk.model.carbon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject
import mu.KotlinLogging
import org.opendc.common.units.Timestamp
import org.opendc.sdk.model.resource.ResourceReference
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue
import org.opendc.sdk.model.validation.validateEach

/** Describes the carbon intensity of the energy that a power source delivers over time. */
@Serializable
public sealed interface CarbonModelSpec : Validatable {
    override fun validate(): List<ValidationIssue> = emptyList()
}

/** A carbon intensity read from an external carbon trace. */
@Serializable
@SerialName("trace")
public data class TraceBasedCarbonSpec(
    /** Reference to the trace supplying the carbon intensities. */
    public val source: ResourceReference,
) : CarbonModelSpec

/**
 * A carbon intensity read from a trace that is given inline, instead of read from a file. Like the entries of a carbon
 * trace, each carbon intensity holds from its timestamp until the next one starts. The first carbon intensity also
 * holds before its timestamp, and the last one until the end of the simulation.
 */
@Serializable
@SerialName("inline-trace")
public data class InlineTraceBasedCarbonSpec(
    /** The carbon intensities of the trace, in any order. */
    public val intensities: List<CarbonIntensitySpec>,
) : CarbonModelSpec {
    override fun validate(): List<ValidationIssue> =
        buildList {
            if (intensities.isEmpty()) add(ValidationIssue("intensities", "must not be empty"))
            if (intensities.distinctBy { it.timestamp }.size < intensities.size) {
                add(ValidationIssue("intensities", "must not contain two carbon intensities with the same timestamp"))
            }
            addAll(intensities.validateEach("intensities"))
        }
}

/**
 * A single carbon intensity of an [InlineTraceBasedCarbonSpec], the equivalent of one row of a carbon trace.
 *
 * @property timestamp The moment from which this carbon intensity holds, such as `"2022-01-01T00:00:00Z"`.
 * @property carbonIntensity The carbon intensity, in gCO2/kWh.
 */
@Serializable
public data class CarbonIntensitySpec(
    public val timestamp: Timestamp,
    public val carbonIntensity: Double,
) : Validatable {
    override fun validate(): List<ValidationIssue> =
        buildList {
            if (carbonIntensity < 0.0) add(ValidationIssue("carbonIntensity", "must be at least zero"))
        }
}

/**
 * Serializes a [CarbonModelSpec], and also reads the old format in which a carbon trace was given as a bare
 * [ResourceReference], such as `{"type": "named", "name": "..."}`, as a [TraceBasedCarbonSpec].
 * A [CarbonModelSpec] is always written in the new format.
 *
 * TODO: Remove this serializer once the old format is no longer used.
 */
internal object CarbonModelSpecSerializer : JsonTransformingSerializer<CarbonModelSpec>(CarbonModelSpec.serializer()) {
    private val logger = KotlinLogging.logger {}

    /** The types of a [ResourceReference], which mark a carbon trace in the old format. */
    private val referenceTypes = setOf("named", "uri")

    override fun transformDeserialize(element: JsonElement): JsonElement {
        val type = ((element as? JsonObject)?.get("type") as? JsonPrimitive)?.content
        if (type !in referenceTypes) {
            return element
        }

        logger.warn {
            "A carbon trace given as a bare reference is deprecated. Use {\"type\": \"trace\", \"source\": $element} instead."
        }
        return buildJsonObject {
            put("type", JsonPrimitive("trace"))
            put("source", element)
        }
    }
}
