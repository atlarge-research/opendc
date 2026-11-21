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
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jboss.resteasy.reactive.RestPath
import org.jboss.resteasy.reactive.RestQuery
import org.jboss.resteasy.reactive.RestResponse
import org.opendc.web.server.auth.Identity
import org.opendc.web.server.auth.ProjectPermission
import org.opendc.web.server.auth.projectFor
import org.opendc.web.server.model.Project
import org.opendc.web.server.model.ProjectMember
import org.opendc.web.server.model.ProjectRole
import org.opendc.web.server.service.SubmissionPipeline
import java.time.Instant

@Path("projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class ProjectsResource(
    private val identity: Identity,
    private val pipeline: SubmissionPipeline,
) {
    @GET
    fun list(
        @RestQuery filter: String?,
        @RestQuery q: String?,
    ): List<ProjectSummary> {
        val user = identity.currentUser()
        val memberships = ProjectMember.findByUser(user.id)
        val filtered =
            when (filter ?: "all") {
                "", "all" -> memberships
                "own" -> memberships.filter { it.role == ProjectRole.OWNER }
                "shared" -> memberships.filter { it.role != ProjectRole.OWNER }
                else -> throw invalidDocument(
                    "Unknown project filter",
                    listOf(DocumentIssue("filter", "must be one of all, own, shared")),
                )
            }
        val matched =
            if (q.isNullOrBlank()) {
                filtered
            } else {
                filtered.filter { it.project.name.contains(q, ignoreCase = true) }
            }
        return matched
            .sortedByDescending { it.project.updatedAt }
            .map { it.toSummary() }
    }

    @POST
    @Transactional
    fun create(request: ProjectRequest): RestResponse<ProjectSummary> {
        val name = validName(request.name, "Project")
        val user = identity.currentUser()
        val now = Instant.now()
        val project = Project()
        project.name = name
        project.createdAt = now
        project.updatedAt = now
        project.persist()
        val member = ProjectMember()
        member.project = project
        member.user = user
        member.role = ProjectRole.OWNER
        member.persist()
        return RestResponse.status(RestResponse.Status.CREATED, member.toSummary())
    }

    @GET
    @Path("{id}")
    fun get(
        @RestPath id: String,
    ): ProjectSummary = projectFor(identity.currentUser(), id, ProjectPermission.READ).toSummary()

    @PATCH
    @Path("{id}")
    @Transactional
    fun rename(
        @RestPath id: String,
        request: ProjectRequest,
    ): ProjectSummary {
        val name = validName(request.name, "Project")
        val member = projectFor(identity.currentUser(), id, ProjectPermission.EDIT)
        val project = member.project
        project.name = name
        project.updatedAt = Instant.now()
        return member.toSummary()
    }

    @DELETE
    @Path("{id}")
    @Transactional
    fun delete(
        @RestPath id: String,
    ) {
        val member = projectFor(identity.currentUser(), id, ProjectPermission.MANAGE)
        // What the experiments hold on a platform or in the store; the rows go through the cascades.
        pipeline.discardAll(member.project)
        // The loaded membership still points at the project, and Hibernate will not flush a live row
        // referencing one being deleted.
        member.delete()
        member.project.delete()
    }
}

@Serializable
enum class ProjectRoleWire {
    @SerialName("owner")
    OWNER,

    @SerialName("editor")
    EDITOR,

    @SerialName("viewer")
    VIEWER,
}

fun ProjectRole.toWire(): ProjectRoleWire =
    when (this) {
        ProjectRole.OWNER -> ProjectRoleWire.OWNER
        ProjectRole.EDITOR -> ProjectRoleWire.EDITOR
        ProjectRole.VIEWER -> ProjectRoleWire.VIEWER
    }

/** A project as the caller sees it: identity plus the caller's role in it. */
@Serializable
data class ProjectSummary(
    val id: String,
    val name: String,
    val role: ProjectRoleWire,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ProjectRequest(
    val name: String,
)

private fun ProjectMember.toSummary(): ProjectSummary =
    ProjectSummary(
        id = project.publicId.toString(),
        name = project.name,
        role = role.toWire(),
        createdAt = project.createdAt.toString(),
        updatedAt = project.updatedAt.toString(),
    )
