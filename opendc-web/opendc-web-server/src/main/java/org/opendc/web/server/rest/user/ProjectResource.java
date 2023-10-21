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
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import java.time.Instant;
import java.util.List;
import org.opendc.web.proto.user.ProjectRole;
import org.opendc.web.server.model.Project;
import org.opendc.web.server.model.ProjectAuthorization;

/**
 * A resource representing the created projects.
 */
@Produces("application/json")
@Path("/projects")
@RolesAllowed("openid")
public final class ProjectResource {
    /**
     * The identity of the current user.
     */
    private final SecurityIdentity identity;

    /**
     * Construct a {@link ProjectResource}.
     *
     * @param identity The {@link SecurityIdentity} of the current user.
     */
    public ProjectResource(SecurityIdentity identity) {
        this.identity = identity;
    }

    /**
     * Obtain all the projects of the current user.
     */
    @GET
    public List<org.opendc.web.proto.user.Project> getAll() {
        return ProjectAuthorization.findByUser(identity.getPrincipal().getName()).list().stream()
                .map(UserProtocol::toDto)
                .toList();
    }

    /**
     * Create a new project for the current user.
     */
    @POST
    @Transactional
    @Consumes("application/json")
    public org.opendc.web.proto.user.Project create(@Valid org.opendc.web.proto.user.Project.Create request) {
        Instant now = Instant.now();
        Project entity = new Project(request.getName(), now);
        entity.persist();

        ProjectAuthorization authorization =
                new ProjectAuthorization(entity, identity.getPrincipal().getName(), ProjectRole.OWNER);

        entity.authorizations.add(authorization);
        authorization.persist();

        return UserProtocol.toDto(authorization);
    }

    /**
     * Obtain a single project by its identifier.
     */
    @GET
    @Path("{project}")
    public org.opendc.web.proto.user.Project get(@PathParam("project") long id) {
        ProjectAuthorization auth =
                ProjectAuthorization.findByUser(identity.getPrincipal().getName(), id);

        if (auth == null) {
            throw new WebApplicationException("Project not found", 404);
        }

        return UserProtocol.toDto(auth);
    }

    /**
     * Delete a project.
     */
    @DELETE
    @Path("{project}")
    @Transactional
    public org.opendc.web.proto.user.Project delete(@PathParam("project") long id) {
        ProjectAuthorization auth =
                ProjectAuthorization.findByUser(identity.getPrincipal().getName(), id);

        if (auth == null) {
            throw new WebApplicationException("Project not found", 404);
        } else if (!auth.canDelete()) {
            throw new WebApplicationException("Not allowed to delete project", 403);
        }

        auth.project.updatedAt = Instant.now();
        org.opendc.web.proto.user.Project project = UserProtocol.toDto(auth);
        auth.project.delete();
        return project;
    }
}
