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

package org.opendc.web.server.traces

import org.opendc.sdk.model.experiment.ExperimentSpec
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.resource.UriReference
import org.opendc.web.server.model.Trace
import org.opendc.web.server.model.TraceGrant
import org.opendc.web.server.model.TraceKind
import org.opendc.web.server.model.TraceOrigin
import org.opendc.web.server.model.UserAccount
import org.opendc.web.server.rest.DocumentIssue
import org.opendc.web.server.rest.invalidDocument

/** The traces a document may use, by name, and what is wrong with the references it may not. */
class TraceResolution(
    val usable: Map<String, Trace>,
    val issues: List<DocumentIssue>,
)

/**
 * What a document's trace references resolve to for [user]: a trace they may use is built in, their
 * own, or shared with them, of the kind the reference asks for, and whole.
 *
 * Someone else's private trace reads exactly like one that does not exist, so its name cannot be
 * probed. A URI is never usable: it would be read from the dispatch host's disk or network.
 */
fun resolveTraces(
    spec: ExperimentSpec,
    user: UserAccount,
): TraceResolution {
    val library =
        (Trace.findBuiltIns() + Trace.findOwnedBy(user.id) + TraceGrant.findSharedWith(user.id).map { it.trace }).associateBy { it.slug }
    val usable = mutableMapOf<String, Trace>()
    val issues = mutableListOf<DocumentIssue>()
    for (use in spec.references()) {
        when (val reference = use.reference) {
            is UriReference -> issues += DocumentIssue(use.path, URI_MESSAGE)
            is NamedReference -> {
                val name = reference.name
                val trace = library[name]
                val problem =
                    when {
                        trace == null -> "no trace called $name in your library"
                        trace.kind != TraceKind.of(use.role) -> "$name is a ${trace.kind.name.lowercase()} trace"
                        !trace.isComplete() && trace.origin == TraceOrigin.BUILTIN -> "$name is not available on this deployment"
                        !trace.isComplete() -> "$name is still uploading"
                        else -> {
                            usable[name] = trace
                            null
                        }
                    }
                problem?.let { issues += DocumentIssue(use.path, it) }
            }
        }
    }
    return TraceResolution(usable, issues)
}

/** Refuses a document that points at a URI, which no draft may hold even while it is being written. */
fun refuseUriReferences(spec: ExperimentSpec) {
    val uris = spec.references().filter { it.reference is UriReference }.map { DocumentIssue(it.path, URI_MESSAGE) }
    if (uris.isNotEmpty()) {
        throw invalidDocument("Experiments may only use traces from your library", uris)
    }
}

private const val URI_MESSAGE = "must name a trace from your library"
