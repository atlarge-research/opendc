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

import jakarta.transaction.Transactional
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.web.server.auth.Download
import org.opendc.web.server.auth.DownloadLink
import org.opendc.web.server.auth.DownloadLinks
import org.opendc.web.server.auth.Identity
import org.opendc.web.server.model.ExperimentResource
import org.opendc.web.server.model.Trace
import org.opendc.web.server.model.TraceGrant
import org.opendc.web.server.model.TraceKind
import org.opendc.web.server.model.TraceOrigin
import org.opendc.web.server.model.TracePart
import org.opendc.web.server.model.UserAccount
import org.opendc.web.server.storage.ObjectStore
import org.opendc.web.server.storage.UploadTarget
import org.opendc.web.server.storage.traceKey
import org.opendc.web.server.traces.TraceDisposal
import org.opendc.web.server.traces.TraceIngest
import java.io.InputStream
import java.time.Instant

@Serializable
enum class TraceKindWire {
    @SerialName("workload")
    WORKLOAD,

    @SerialName("carbon")
    CARBON,

    @SerialName("failure")
    FAILURE,
}

/** How the caller came by a trace, which decides whether they may change it. */
@Serializable
enum class TraceAccessWire {
    @SerialName("builtin")
    BUILTIN,

    @SerialName("owned")
    OWNED,

    @SerialName("shared")
    SHARED,
}

@Serializable
data class TraceTable(
    val name: String,
    val sizeBytes: Long,
    val rowCount: Long? = null,
)

// The slug is what an experiment references and may be corrected while nothing does; the public id
// never changes, so it is what URLs address.
@Serializable
data class TraceWire(
    val id: String,
    val slug: String,
    val kind: TraceKindWire,
    val access: TraceAccessWire,
    val description: String? = null,
    val sizeBytes: Long,
    val tables: List<TraceTable>,
    val createdAt: String,
)

/** A kind of trace and the tables one is made of. */
@Serializable
data class TraceKindTables(
    val kind: TraceKindWire,
    val tables: List<String>,
)

/** A file about to be sent. Its size decides how it is cut into parts when the targets are handed out. */
@Serializable
data class UploadedFile(
    val table: String,
    val sizeBytes: Long,
)

@Serializable
data class TraceRegistration(
    val kind: String,
    val name: String,
    val description: String? = null,
    val files: List<UploadedFile> = emptyList(),
)

/** One stretch of a table's file and where it goes. A relative path means this server takes it. */
@Serializable
data class UploadPart(
    val url: String,
    val offset: Long,
    val length: Long,
)

/** Where to send one table's bytes. Several parts may be sent at once. */
@Serializable
data class UploadSlot(
    val table: String,
    val parts: List<UploadPart>,
    val direct: Boolean,
)

@Serializable
data class RegisteredTrace(
    val trace: TraceWire,
    val uploads: List<UploadSlot>,
)

@Serializable
data class TraceEdit(
    val name: String? = null,
    val description: String? = null,
)

/** Somebody a trace has been shared with. */
@Serializable
data class TraceShare(
    val handle: String,
    val displayName: String,
)

@Serializable
data class ShareRequest(
    val handle: String,
)

@Path("traces")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class TracesResource(
    private val identity: Identity,
    private val store: ObjectStore,
    private val ingest: TraceIngest,
    private val disposal: TraceDisposal,
    private val links: DownloadLinks,
) {
    /**
     * The caller's library: what the deployment ships, what they uploaded, what others shared. A trace
     * whose upload never finished is not among them.
     */
    @GET
    fun list(
        @QueryParam("kind") kind: String?,
    ): List<TraceWire> {
        val user = identity.currentUser()
        val wanted = kind?.let(::traceKind)
        val found =
            (
                Trace.findBuiltIns().map { it to TraceAccessWire.BUILTIN } +
                    Trace.findOwnedBy(user.id).map { it to TraceAccessWire.OWNED } +
                    TraceGrant.findSharedWith(user.id).map { it.trace to TraceAccessWire.SHARED }
            ).filter { (trace, _) -> wanted == null || trace.kind == wanted }

        // One query answers both what each trace holds and whether it is finished.
        val tables = tablesOf(found.map { (trace, _) -> trace.id })
        return found
            .filter { (trace, _) -> tables.holdsAll(trace) }
            .sortedBy { (trace, _) -> trace.slug }
            .map { (trace, access) -> trace.toWire(access, tables[trace.id].orEmpty()) }
    }

    /** Which tables each kind of trace is made of, so an upload form needs no copy of its own. */
    @GET
    @Path("kinds")
    fun kinds(): List<TraceKindTables> = TraceKind.entries.map { TraceKindTables(it.toWire(), it.tables) }

    @GET
    @Path("{id}")
    fun get(
        @PathParam("id") id: String,
    ): TraceWire {
        val trace = visible(id)
        return trace.toWire(accessTo(trace))
    }

    /**
     * Claims a name and hands back somewhere to put each table: object storage directly where the
     * browser can reach it, otherwise [putTable] on this server.
     */
    @POST
    @Transactional
    fun register(request: TraceRegistration): Response {
        val kind = traceKind(request.kind)
        val owner = identity.currentUser()
        val slug = "${owner.handle}/${traceName(request.name)}"
        // Before anything is created: a target for a large file opens an upload in storage that a
        // refused request would leave open.
        val sizes = declaredSizes(kind, request.files)
        disposal.claim(slug, owner)
        val trace = Trace.unfinished(slug, kind, owner, request.description.cleaned(), Instant.now())

        val uploads = kind.tables.map { table -> slotFor(trace, table, sizes.getValue(table)) }
        return Response.status(201).entity(RegisteredTrace(trace.toWire(TraceAccessWire.OWNED), uploads)).build()
    }

    /**
     * Takes one table's bytes, for a store a browser cannot write to directly. Any content type, since
     * a browser names none for a .parquet file.
     */
    @PUT
    @Path("{id}/tables/{table}")
    @Consumes(MediaType.WILDCARD)
    fun putTable(
        @PathParam("id") id: String,
        @PathParam("table") table: String,
        body: InputStream,
    ): Response {
        val trace = owned(id)
        if (table !in trace.kind.tables) {
            throw notFound("Table $table")
        }
        body.use { store.put(traceKey(trace.publicId, table), it) }
        return Response.status(204).build()
    }

    /**
     * Checks that what was uploaded is what it was filed as, reading only the parquet footers, and
     * makes the trace usable.
     */
    @POST
    @Path("{id}/complete")
    @Transactional
    fun complete(
        @PathParam("id") id: String,
    ): TraceWire {
        val trace = owned(id)
        // A table sent in parts becomes an object only now that the browser has sent them all.
        val missing = trace.kind.tables.filterNot { store.completeUpload(traceKey(trace.publicId, it)) }
        if (missing.isNotEmpty()) {
            throw conflict("The bytes for ${missing.joinToString(", ")} never arrived")
        }

        ingest.record(trace)
        return trace.toWire(TraceAccessWire.OWNED)
    }

    /** The name is what documents reference, so it may only be corrected while nothing references it. */
    @PATCH
    @Path("{id}")
    @Transactional
    fun edit(
        @PathParam("id") id: String,
        edit: TraceEdit,
    ): TraceWire {
        val trace = owned(id)
        edit.name?.let { requested ->
            val slug = "${trace.owner?.handle}/${traceName(requested)}"
            if (slug != trace.slug) {
                if (ExperimentResource.referencesTrace(trace.slug)) {
                    throw conflict("This trace is referenced by an experiment and can no longer be renamed")
                }
                if (Trace.findBySlug(slug) != null) {
                    throw conflict("You already have a trace called $slug")
                }
                trace.slug = slug
            }
        }
        edit.description?.let { trace.description = it.cleaned() }
        trace.updatedAt = Instant.now()
        return trace.toWire(TraceAccessWire.OWNED)
    }

    @DELETE
    @Path("{id}")
    @Transactional
    fun delete(
        @PathParam("id") id: String,
    ): Response {
        val trace = owned(id)
        if (ExperimentResource.referencesTrace(trace.slug)) {
            throw conflict("This trace is referenced by an experiment and cannot be deleted")
        }
        disposal.discard(trace)
        return Response.status(204).build()
    }

    @GET
    @Path("{id}/content")
    @Produces(APPLICATION_ZIP)
    fun content(
        @PathParam("id") id: String,
    ): Response = traceContentResponse(visible(id), store)

    /** A link a browser can follow to the trace's content; see the experiment archive's. */
    @POST
    @Path("{id}/content/link")
    fun contentLink(
        @PathParam("id") id: String,
    ): DownloadLink {
        val trace = visible(id)
        if (TracePart.findByTrace(trace.id).isEmpty()) {
            throw notFound("Trace content")
        }
        return links.sign(Download.TraceContent(trace.publicId.toString()), Instant.now())
    }

    @GET
    @Path("{id}/shares")
    fun shares(
        @PathParam("id") id: String,
    ): List<TraceShare> = TraceGrant.findByTrace(owned(id).id).map { TraceShare(it.grantee.handle, it.grantee.displayName) }

    @POST
    @Path("{id}/shares")
    @Transactional
    fun share(
        @PathParam("id") id: String,
        request: ShareRequest,
    ): Response {
        val trace = owned(id)
        val grantee = UserAccount.findActiveByHandle(request.handle.trim()) ?: throw notFound("Account ${request.handle}")
        if (grantee.id == trace.owner?.id) {
            throw conflict("This trace is already yours")
        }
        if (TraceGrant.findGrant(trace.id, grantee.id) == null) {
            val grant = TraceGrant()
            grant.trace = trace
            grant.grantee = grantee
            grant.grantedAt = Instant.now()
            grant.persist()
        }
        return Response.status(201).entity(TraceShare(grantee.handle, grantee.displayName)).build()
    }

    @DELETE
    @Path("{id}/shares/{handle}")
    @Transactional
    fun revoke(
        @PathParam("id") id: String,
        @PathParam("handle") handle: String,
    ): Response {
        val trace = owned(id)
        val grantee = UserAccount.findByHandle(handle) ?: throw notFound("Account $handle")
        TraceGrant.findGrant(trace.id, grantee.id)?.delete()
        return Response.status(204).build()
    }

    private fun slotFor(
        trace: Trace,
        table: String,
        sizeBytes: Long,
    ): UploadSlot =
        when (val target = store.uploadTarget(traceKey(trace.publicId, table), sizeBytes)) {
            is UploadTarget.Direct ->
                UploadSlot(table, target.parts.map { UploadPart(it.url, it.offset, it.length) }, direct = true)
            UploadTarget.ThroughServer ->
                UploadSlot(
                    table,
                    listOf(UploadPart("$API_ROOT/traces/${trace.publicId}/tables/$table", 0, sizeBytes)),
                    direct = false,
                )
        }

    /** A trace the caller may read, whether it is the deployment's, theirs, or shared with them. */
    private fun visible(id: String): Trace {
        val trace = Trace.findByPublicId(publicId(id, "Trace")) ?: throw notFound("Trace")
        accessTo(trace)
        return trace
    }

    /** A trace the caller may change. One they can see but do not own is a 403, not a 404. */
    private fun owned(id: String): Trace {
        val trace = visible(id)
        return when (accessTo(trace)) {
            TraceAccessWire.OWNED -> trace
            TraceAccessWire.BUILTIN -> throw forbidden("Built-in traces belong to the deployment")
            TraceAccessWire.SHARED -> throw forbidden("This trace belongs to somebody else")
        }
    }

    private fun accessTo(trace: Trace): TraceAccessWire {
        val user = identity.currentUser()
        return when {
            trace.origin == TraceOrigin.BUILTIN -> TraceAccessWire.BUILTIN
            trace.owner?.id == user.id -> TraceAccessWire.OWNED
            TraceGrant.findGrant(trace.id, user.id) != null -> TraceAccessWire.SHARED
            else -> throw notFound("Trace")
        }
    }

    private fun tablesOf(traceIds: List<Long>): Map<Long, List<TracePart>> =
        if (traceIds.isEmpty()) emptyMap() else TracePart.list("trace.id in ?1", traceIds).groupBy { it.trace.id }

    private fun Map<Long, List<TracePart>>.holdsAll(trace: Trace): Boolean =
        this[trace.id].orEmpty().map { it.tableName }.containsAll(trace.kind.tables)

    private fun Trace.toWire(
        access: TraceAccessWire,
        stored: List<TracePart> = TracePart.findByTrace(id),
    ): TraceWire {
        val parts = stored.map { TraceTable(it.tableName, it.sizeBytes, it.rowCount) }
        return TraceWire(
            id = publicId.toString(),
            slug = slug,
            kind = kind.toWire(),
            access = access,
            description = description,
            sizeBytes = parts.sumOf { it.sizeBytes },
            tables = parts.sortedBy { it.name },
            createdAt = createdAt.toString(),
        )
    }
}

// A misspelled kind is refused rather than answered with an empty library.
fun traceKind(raw: String): TraceKind =
    when (raw) {
        "workload" -> TraceKind.WORKLOAD
        "carbon" -> TraceKind.CARBON
        "failure" -> TraceKind.FAILURE
        else ->
            throw invalidDocument(
                "Unknown trace kind",
                listOf(DocumentIssue("kind", "must be one of workload, carbon, failure")),
            )
    }

/** How large each of [kind]'s tables is said to be, refusing any that is missing or empty. */
private fun declaredSizes(
    kind: TraceKind,
    files: List<UploadedFile>,
): Map<String, Long> {
    val given = files.associate { it.table to it.sizeBytes }
    val unaccounted = kind.tables.filter { (given[it] ?: 0L) <= 0L }
    if (unaccounted.isNotEmpty()) {
        throw invalidDocument(
            "Every file has to be accounted for before any of it is sent",
            unaccounted.map { DocumentIssue(it, "needs a size in bytes") },
        )
    }
    return given
}

// What documents, URLs and object keys all accept. The owner's handle and a slash are added in front.
private val TRACE_NAME = Regex("[a-z0-9][a-z0-9._-]{0,99}")

fun traceName(raw: String): String {
    val name = raw.trim().lowercase()
    if (!TRACE_NAME.matches(name)) {
        throw invalidDocument(
            "That is not a usable trace name",
            listOf(DocumentIssue("name", "use lower-case letters, digits, dots, dashes and underscores")),
        )
    }
    return name
}

fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

fun TraceKind.toWire(): TraceKindWire =
    when (this) {
        TraceKind.WORKLOAD -> TraceKindWire.WORKLOAD
        TraceKind.CARBON -> TraceKindWire.CARBON
        TraceKind.FAILURE -> TraceKindWire.FAILURE
    }
