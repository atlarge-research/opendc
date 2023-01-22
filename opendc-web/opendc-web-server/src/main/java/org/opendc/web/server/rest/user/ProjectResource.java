/*
 * Copyright (c) 2023 AtLarge Research
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

package org.opendc.web.server.rest.user;

import io.quarkus.security.identity.SecurityIdentity;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import org.opendc.web.server.service.ProjectService;

/**
 * A resource representing the created projects.
 */
@Produces("application/json")
@Path("/projects")
@RolesAllowed("openid")
public final class ProjectResource {
    /**
     * The service for managing the user projects.
     */
    private final ProjectService projectService;

    /**
     * The identity of the current user.
     */
    private final SecurityIdentity identity;

    /**
     * Construct a {@link ProjectResource}.
     *
     * @param projectService The {@link ProjectService} instance to use.
     * @param identity The {@link SecurityIdentity} of the current user.
     */
    public ProjectResource(ProjectService projectService, SecurityIdentity identity) {
        this.projectService = projectService;
        this.identity = identity;
    }

    /**
     * Obtain all the projects of the current user.
     */
    @GET
    public List<org.opendc.web.proto.user.Project> getAll() {
        return projectService.findByUser(identity.getPrincipal().getName());
    }

    /**
     * Create a new project for the current user.
     */
    @POST
    @Transactional
    @Consumes("application/json")
    public org.opendc.web.proto.user.Project create(@Valid org.opendc.web.proto.user.Project.Create request) {
        return projectService.create(identity.getPrincipal().getName(), request.getName());
    }

    /**
     * Obtain a single project by its identifier.
     */
    @GET
    @Path("{project}")
    public org.opendc.web.proto.user.Project get(@PathParam("project") long id) {
        var project = projectService.findByUser(identity.getPrincipal().getName(), id);
        if (project == null) {
            throw new WebApplicationException("Project not found", 404);
        }

        return project;
    }

    /**
     * Delete a project.
     */
    @DELETE
    @Path("{project}")
    @Transactional
    public org.opendc.web.proto.user.Project delete(@PathParam("project") long id) {
        try {
            var project = projectService.delete(identity.getPrincipal().getName(), id);
            if (project == null) {
                throw new WebApplicationException("Project not found", 404);
            }

            return project;
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e.getMessage(), 403);
        }
    }
}
