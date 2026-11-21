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
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.serialization.Serializable
import org.opendc.web.server.auth.Identity
import org.opendc.web.server.auth.ProjectPermission
import org.opendc.web.server.auth.projectFor
import org.opendc.web.server.model.Project
import org.opendc.web.server.model.ProjectMember
import org.opendc.web.server.model.ProjectRole
import org.opendc.web.server.model.UserAccount
import java.time.Instant

/**
 * Who works in a project, addressed by handle. Changes take turns on the project row, so two owners
 * demoting each other cannot leave it with none.
 */
@Path("projects/{id}/members")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class ProjectMembersResource(private val identity: Identity) {
    @GET
    fun list(
        @PathParam("id") id: String,
    ): List<ProjectMemberWire> {
        val project = projectFor(identity.currentUser(), id, ProjectPermission.READ).project
        return ProjectMember
            .findMembers(project.id)
            .sortedWith(compareBy({ it.role != ProjectRole.OWNER }, { it.user.handle }))
            .map { it.toWire() }
    }

    @POST
    @Transactional
    fun invite(
        @PathParam("id") id: String,
        invite: MemberInvite,
    ): Response {
        val project = locked(id, ProjectPermission.MANAGE)
        val handle = invite.handle.trim()
        val user = UserAccount.findActiveByHandle(handle) ?: throw notFound("Account $handle")
        if (ProjectMember.findMembership(project.id, user.id) != null) {
            throw conflict("$handle is already in this project")
        }
        val member = ProjectMember()
        member.project = project
        member.user = user
        member.role = invite.role.toModel()
        member.persist()
        project.updatedAt = Instant.now()
        return Response.status(201).entity(member.toWire()).build()
    }

    @PATCH
    @Path("{handle}")
    @Transactional
    fun changeRole(
        @PathParam("id") id: String,
        @PathParam("handle") handle: String,
        change: RoleChange,
    ): ProjectMemberWire {
        val project = locked(id, ProjectPermission.MANAGE)
        val member = memberOf(project, handle)
        val role = change.role.toModel()
        if (member.role == ProjectRole.OWNER && role != ProjectRole.OWNER) {
            keepAnOwner(project)
        }
        member.role = role
        return member.toWire()
    }

    /** Removes someone, which an owner may do to anyone and anyone may do to themselves. */
    @DELETE
    @Path("{handle}")
    @Transactional
    fun remove(
        @PathParam("id") id: String,
        @PathParam("handle") handle: String,
    ): Response {
        val caller = identity.currentUser()
        val leaving = caller.handle == handle
        val project = locked(id, if (leaving) ProjectPermission.READ else ProjectPermission.MANAGE)
        val member = memberOf(project, handle)
        if (member.role == ProjectRole.OWNER) {
            keepAnOwner(project)
        }
        member.delete()
        return Response.noContent().build()
    }

    // Locked before the caller's role is read, so a role changed in the meantime is the one checked.
    private fun locked(
        id: String,
        needs: ProjectPermission,
    ): Project {
        val project = Project.lockByPublicId(publicId(id, "Project")) ?: throw notFound("Project")
        projectFor(identity.currentUser(), project.publicId, needs)
        return project
    }

    private fun memberOf(
        project: Project,
        handle: String,
    ): ProjectMember {
        val user = UserAccount.findByHandle(handle) ?: throw notFound("Member $handle")
        return ProjectMember.findMembership(project.id, user.id) ?: throw notFound("Member $handle")
    }

    private fun keepAnOwner(project: Project) {
        if (ProjectMember.countOwners(project.id) <= 1) {
            throw conflict("A project needs at least one owner")
        }
    }
}

private fun ProjectMember.toWire(): ProjectMemberWire = ProjectMemberWire(user.handle, user.displayName, role.toWire())

private fun ProjectRoleWire.toModel(): ProjectRole =
    when (this) {
        ProjectRoleWire.OWNER -> ProjectRole.OWNER
        ProjectRoleWire.EDITOR -> ProjectRole.EDITOR
        ProjectRoleWire.VIEWER -> ProjectRole.VIEWER
    }

@Serializable
data class ProjectMemberWire(
    val handle: String,
    val displayName: String,
    val role: ProjectRoleWire,
)

@Serializable
data class MemberInvite(
    val handle: String,
    val role: ProjectRoleWire,
)

@Serializable
data class RoleChange(val role: ProjectRoleWire)
