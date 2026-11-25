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

import org.opendc.sdk.model.experiment.ExperimentSpec
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.resource.ResourceReference
import org.opendc.sdk.model.resource.UriReference

/** Returns this experiment with each reference whose [key] is in [slugs] renamed to the library trace it maps to. */
fun ExperimentSpec.withLibraryNames(slugs: Map<String, String>): ExperimentSpec =
    mapReferences { use -> slugs[use.reference.key()]?.let(::NamedReference) ?: use.reference }

/** The key Python passes a trace under: the name of a [NamedReference] or the URI of a [UriReference]. */
fun ResourceReference.key(): String =
    when (this) {
        is NamedReference -> name
        is UriReference -> uri
    }
