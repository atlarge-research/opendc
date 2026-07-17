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

package org.opendc.sdk.model.topology

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models the performance penalty incurred when a resource is shared through virtualization.
 */
@Serializable
public sealed interface VirtualizationOverheadSpec

/** No virtualization overhead is applied. */
@Serializable
@SerialName("none")
public data object NoVirtualizationOverheadSpec : VirtualizationOverheadSpec

/**
 * A fixed virtualization overhead.
 *
 * @property percentageOverhead The overhead as a percentage, or `null` to use the model default.
 */
@Serializable
@SerialName("constant")
public data class ConstantVirtualizationOverheadSpec(
    public val percentageOverhead: Double? = null,
) : VirtualizationOverheadSpec

/** Virtualization overhead that scales with the number of consumers sharing the resource. */
@Serializable
@SerialName("shareBased")
public data object ShareBasedVirtualizationOverheadSpec : VirtualizationOverheadSpec
