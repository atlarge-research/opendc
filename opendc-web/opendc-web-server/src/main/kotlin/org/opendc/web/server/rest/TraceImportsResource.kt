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

package org.opendc.web.server.rest

import jakarta.enterprise.event.Event
import jakarta.transaction.Transactional
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.web.server.auth.Identity
import org.opendc.web.server.model.ImportProgress
import org.opendc.web.server.model.Trace
import org.opendc.web.server.model.TraceImport
import org.opendc.web.server.traces.ImportRequested
import org.opendc.web.server.traces.SourceCheck
import org.opendc.web.server.traces.TraceDisposal
import org.opendc.web.server.traces.checkShape
import java.net.URI
import java.net.URISyntaxException
import java.time.Instant

@Serializable
data class TraceImportRequest(
    val kind: String,
    val name: String,
    val description: String? = null,
    /** One URL per table the kind is made of, keyed by the table's name. */
    val sources: Map<String, String>,
)

@Serializable
sealed interface ImportProgressWire {
    @Serializable
    @SerialName("running")
    data class Running(val since: String) : ImportProgressWire

    @Serializable
    @SerialName("succeeded")
    data class Succeeded(val at: String) : ImportProgressWire

    @Serializable
    @SerialName("failed")
    data class Failed(
        val at: String,
        val reason: String,
    ) : ImportProgressWire
}

@Serializable
data class TraceImportWire(
    val id: String,
    val traceId: String,
    val slug: String,
    val kind: TraceKindWire,
    val progress: ImportProgressWire,
)

/**
 * Traces the server fetches from public URLs in the background. The trace joins its owner's library
 * once every table has arrived and passed the checks an upload gets.
 */
@Path("traces/imports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class TraceImportsResource(
    private val identity: Identity,
    private val disposal: TraceDisposal,
    private val requests: Event<ImportRequested>,
) {
    /** The caller's imports, newest first, until they are dismissed or their trace is deleted. */
    @GET
    fun list(): List<TraceImportWire> = TraceImport.findOwnedBy(identity.currentUser().id).map { it.toWire() }

    @POST
    @Transactional
    fun start(request: TraceImportRequest): Response {
        val kind = traceKind(request.kind)
        val sources = sourcesOf(kind.tables, request.sources)
        val owner = identity.currentUser()
        val slug = "${owner.handle}/${traceName(request.name)}"
        disposal.claim(slug, owner)

        val now = Instant.now()
        val trace = Trace.unfinished(slug, kind, owner, request.description.cleaned(), now)
        val import = TraceImport()
        import.trace = trace
        import.createdAt = now
        import.persist()
        requests.fire(ImportRequested(import.publicId, trace.publicId, sources))
        return Response.status(202).entity(import.toWire()).build()
    }

    /** Clears an import from the list. A failed one takes its unfinished trace with it, freeing the name. */
    @DELETE
    @Path("{id}")
    @Transactional
    fun dismiss(
        @PathParam("id") id: String,
    ): Response {
        val import = TraceImport.lockByPublicId(publicId(id, "Import")) ?: throw notFound("Import")
        if (import.trace.owner?.id != identity.currentUser().id) {
            throw notFound("Import")
        }
        when (import.progress) {
            is ImportProgress.Running -> throw conflict("This import is still running")
            is ImportProgress.Succeeded -> import.delete()
            is ImportProgress.Failed -> disposal.discard(import.trace)
        }
        return Response.status(204).build()
    }
}

/** One fetchable URL for each of [tables], and nothing for a table the kind does not have. */
private fun sourcesOf(
    tables: List<String>,
    raw: Map<String, String>,
): Map<String, URI> {
    val issues =
        tables.filter { it !in raw }.map { DocumentIssue("sources.$it", "is required") } +
            raw.keys.filter { it !in tables }.map { DocumentIssue("sources.$it", "is not a table of this kind") } +
            raw.filterKeys { it in tables }.flatMap { (table, url) ->
                when (val check = checkUrl(url)) {
                    SourceCheck.Allowed -> emptyList()
                    is SourceCheck.Refused -> listOf(DocumentIssue("sources.$table", check.reason))
                }
            }
    if (issues.isNotEmpty()) {
        throw invalidDocument("These URLs cannot be imported", issues)
    }
    return tables.associateWith { URI(raw.getValue(it).trim()) }
}

private fun checkUrl(url: String): SourceCheck =
    try {
        checkShape(URI(url.trim()))
    } catch (e: URISyntaxException) {
        SourceCheck.Refused("is not a URL")
    }

private fun TraceImport.toWire(): TraceImportWire =
    TraceImportWire(
        id = publicId.toString(),
        traceId = trace.publicId.toString(),
        slug = trace.slug,
        kind = trace.kind.toWire(),
        progress =
            when (val progress = progress) {
                is ImportProgress.Running -> ImportProgressWire.Running(progress.since.toString())
                is ImportProgress.Succeeded -> ImportProgressWire.Succeeded(progress.at.toString())
                is ImportProgress.Failed -> ImportProgressWire.Failed(progress.at.toString(), progress.reason)
            },
    )
