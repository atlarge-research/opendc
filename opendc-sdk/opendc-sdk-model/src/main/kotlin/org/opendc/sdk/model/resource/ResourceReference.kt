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

package org.opendc.sdk.model.resource

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A path-agnostic handle to an external data resource, resolved at runtime by a [ResourceResolver].
 */
@Serializable
public sealed interface ResourceReference

/**
 * A [ResourceReference] identified by a logical [name] that the resolver maps to concrete data.
 *
 * @property name The logical name of the resource.
 */
@Serializable
@SerialName("named")
public data class NamedReference(public val name: String) : ResourceReference

/**
 * A [ResourceReference] identified by a [uri] pointing at the resource.
 *
 * @property uri The uniform resource identifier of the resource.
 */
@Serializable
@SerialName("uri")
public data class UriReference(public val uri: String) : ResourceReference
