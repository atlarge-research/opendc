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
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.jboss.resteasy.reactive.RestResponse
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.web.server.auth.Identity
import org.opendc.web.server.auth.ProjectPermission
import org.opendc.web.server.auth.projectFor
import org.opendc.web.server.auth.topologyFor
import org.opendc.web.server.model.TopologyTemplate
import org.opendc.web.server.service.SpecCodec
import java.time.Instant

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class TopologyTemplateWire(
    val id: String,
    val projectId: String,
    val name: String,
    val topology: JsonElement,
    val topologyHash: String,
    // Absent rather than null for a topology nobody has arranged, so the client derives a floor plan.
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val layout: JsonElement? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class TopologyCreateRequest(
    val projectId: String,
    val name: String,
    val topology: JsonElement,
    val layout: JsonElement? = null,
)

@Serializable
data class TopologyUpdateRequest(
    val name: String,
    val topology: JsonElement,
    val layout: JsonElement? = null,
)

/**
 * Topology templates as flat resources: the project is a query or body field, never a path segment.
 * The topology is stored validated and canonical; the layout is opaque and stored verbatim.
 */
@Path("topologies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class TopologiesResource(
    private val identity: Identity,
    private val codec: SpecCodec,
) {
    @GET
    fun list(
        @QueryParam("project") projectId: String?,
    ): List<TopologyTemplateWire> {
        val raw =
            projectId
                ?: throw invalidDocument(
                    "The project query parameter is required",
                    listOf(DocumentIssue("project", "must be provided")),
                )
        val project = projectFor(identity.currentUser(), raw, ProjectPermission.READ).project
        return TopologyTemplate.findByProject(project.id).map { it.toWire() }
    }

    @POST
    @Transactional
    fun create(request: TopologyCreateRequest): RestResponse<TopologyTemplateWire> {
        val project = projectFor(identity.currentUser(), request.projectId, ProjectPermission.EDIT).project
        val canonical = codec.canonical(decodeValidated(request.topology))
        val now = Instant.now()
        project.updatedAt = now
        val template = TopologyTemplate()
        template.project = project
        template.name = validName(request.name, "Topology")
        template.topology = canonical
        template.topologyHash = codec.hash(canonical)
        template.layout = request.layout?.let { codec.json.encodeToString(JsonElement.serializer(), it) }
        template.createdAt = now
        template.updatedAt = now
        template.persist()
        return RestResponse.ResponseBuilder
            .create<TopologyTemplateWire>(201)
            .entity(template.toWire())
            .build()
    }

    @GET
    @Path("{id}")
    fun get(
        @PathParam("id") id: String,
    ): TopologyTemplateWire = topologyFor(identity.currentUser(), id, ProjectPermission.READ).toWire()

    @PUT
    @Path("{id}")
    @Transactional
    fun replace(
        @PathParam("id") id: String,
        request: TopologyUpdateRequest,
    ): TopologyTemplateWire {
        val template = topologyFor(identity.currentUser(), id, ProjectPermission.EDIT)
        val canonical = codec.canonical(decodeValidated(request.topology))
        template.name = validName(request.name, "Topology")
        template.topology = canonical
        template.topologyHash = codec.hash(canonical)
        template.layout = request.layout?.let { codec.json.encodeToString(JsonElement.serializer(), it) }
        template.updatedAt = Instant.now()
        template.project.updatedAt = template.updatedAt
        return template.toWire()
    }

    @DELETE
    @Path("{id}")
    @Transactional
    fun delete(
        @PathParam("id") id: String,
    ) {
        val template = topologyFor(identity.currentUser(), id, ProjectPermission.EDIT)
        template.project.updatedAt = Instant.now()
        template.delete()
    }

    private fun decodeValidated(document: JsonElement): TopologySpec {
        val spec = codec.decodeTopology(document)
        val issues = spec.validate()
        if (issues.isNotEmpty()) {
            throw invalidDocument("The topology document is invalid", issues.toWire())
        }
        return spec
    }

    private fun TopologyTemplate.toWire(): TopologyTemplateWire =
        TopologyTemplateWire(
            id = publicId.toString(),
            projectId = project.publicId.toString(),
            name = name,
            topology = codec.parseStored(topology),
            topologyHash = topologyHash,
            layout = layout?.let { codec.parseStored(it) },
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString(),
        )
}
