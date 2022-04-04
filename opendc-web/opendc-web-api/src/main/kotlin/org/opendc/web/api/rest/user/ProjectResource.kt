/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.web.api.rest.user

import io.quarkus.security.identity.SecurityIdentity
import org.opendc.web.api.service.ProjectService
import org.opendc.web.proto.user.Project
import javax.annotation.security.RolesAllowed
import javax.inject.Inject
import javax.transaction.Transactional
import javax.validation.Valid
import javax.ws.rs.*

/**
 * A resource representing the created projects.
 */
@Path("/projects")
@RolesAllowed("openid")
class ProjectResource @Inject constructor(
    private val projectService: ProjectService,
    private val identity: SecurityIdentity
) {
    /**
     * Obtain all the projects of the current user.
     */
    @GET
    fun getAll(): List<Project> {
        return projectService.findWithUser(identity.principal.name)
    }

    /**
     * Create a new project for the current user.
     */
    @POST
    @Transactional
    fun create(@Valid request: Project.Create): Project {
        return projectService.createForUser(identity.principal.name, request.name)
    }

    /**
     * Obtain a single project by its identifier.
     */
    @GET
    @Path("{project}")
    fun get(@PathParam("project") id: Long): Project {
        return projectService.findWithUser(identity.principal.name, id) ?: throw WebApplicationException("Project not found", 404)
    }

    /**
     * Delete a project.
     */
    @DELETE
    @Path("{project}")
    @Transactional
    fun delete(@PathParam("project") id: Long): Project {
        try {
            return projectService.deleteWithUser(identity.principal.name, id) ?: throw WebApplicationException("Project not found", 404)
        } catch (e: IllegalArgumentException) {
            throw WebApplicationException(e.message, 403)
        }
    }
}
