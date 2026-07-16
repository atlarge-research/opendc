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

package org.opendc.sdk.model.failure

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.sdk.model.resource.ResourceReference
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue
import org.opendc.sdk.model.validation.prefixed

/** Describes how host failures are injected into a simulation. */
@Serializable
public sealed interface FailureModel : Validatable {
    override fun validate(): List<ValidationIssue> = emptyList()
}

/** No failures are injected. */
@Serializable
@SerialName("none")
public data object NoFailure : FailureModel

/** Failures replayed from an external availability trace. */
@Serializable
@SerialName("traceBased")
public data class TraceBasedFailure(
    /** Reference to the trace supplying the failure events. */
    public val source: ResourceReference,
    /** Relative position in the trace at which replay starts, in the range [0.0, 1.0). */
    public val startPoint: Double = 0.0,
    /** Whether the trace is replayed from the beginning once exhausted. */
    public val repeat: Boolean = true,
) : FailureModel {
    override fun validate(): List<ValidationIssue> =
        if (startPoint >= 0.0 && startPoint < 1.0) {
            emptyList()
        } else {
            listOf(ValidationIssue("startPoint", "must be in [0.0, 1.0)"))
        }
}

/** A built-in failure model selected from a set of empirically fitted [FailurePrefab]s. */
@Serializable
@SerialName("prefab")
public data class PrefabFailure(
    /** The built-in failure model to use. */
    public val prefabName: FailurePrefab,
) : FailureModel

/** A fully custom failure model built from three sampling distributions. */
@Serializable
@SerialName("custom")
public data class CustomFailure(
    /** Distribution of the time between successive failures. */
    public val interArrival: Distribution,
    /** Distribution of the duration of each failure. */
    public val duration: Distribution,
    /** Distribution of the fraction of hosts affected by each failure. */
    public val hostFraction: Distribution,
) : FailureModel {
    override fun validate(): List<ValidationIssue> =
        buildList {
            addAll(interArrival.validate().prefixed("interArrival"))
            addAll(duration.validate().prefixed("duration"))
            addAll(hostFraction.validate().prefixed("hostFraction"))
        }
}
