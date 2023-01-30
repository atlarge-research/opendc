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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import org.opendc.web.server.model.ProjectAuthorization;
import org.opendc.web.server.model.Scenario;

/**
 * A resource representing the scenarios of a portfolio.
 */
@Produces("application/json")
@Path("/projects/{project}/scenarios")
@RolesAllowed("openid")
public final class ScenarioResource {
    /**
     * The identity of the current user.
     */
    private final SecurityIdentity identity;

    /**
     * Construct a {@link ScenarioResource}.
     *
     * @param identity The {@link SecurityIdentity} of the current user.
     */
    public ScenarioResource(SecurityIdentity identity) {
        this.identity = identity;
    }

    /**
     * Obtain the scenarios belonging to a project.
     */
    @GET
    public List<org.opendc.web.proto.user.Scenario> getAll(@PathParam("project") long projectId) {
        // User must have access to project
        ProjectAuthorization auth =
                ProjectAuthorization.findByUser(identity.getPrincipal().getName(), projectId);

        if (auth == null) {
            throw new WebApplicationException("Project not found", 404);
        }

        return Scenario.findByProject(projectId).list().stream()
                .map((s) -> UserProtocol.toDto(s, auth))
                .toList();
    }

    /**
     * Obtain a scenario by its identifier.
     */
    @GET
    @Path("{scenario}")
    public org.opendc.web.proto.user.Scenario get(
            @PathParam("project") long projectId, @PathParam("scenario") int number) {
        // User must have access to project
        ProjectAuthorization auth =
                ProjectAuthorization.findByUser(identity.getPrincipal().getName(), projectId);

        if (auth == null) {
            throw new WebApplicationException("Project not found", 404);
        }

        Scenario scenario = Scenario.findByProject(projectId, number);

        if (scenario == null) {
            throw new WebApplicationException("Scenario not found", 404);
        }

        return UserProtocol.toDto(scenario, auth);
    }

    /**
     * Delete a scenario.
     */
    @DELETE
    @Path("{scenario}")
    @Transactional
    public org.opendc.web.proto.user.Scenario delete(
            @PathParam("project") long projectId, @PathParam("scenario") int number) {
        // User must have access to project
        ProjectAuthorization auth =
                ProjectAuthorization.findByUser(identity.getPrincipal().getName(), projectId);

        if (auth == null) {
            throw new WebApplicationException("Project not found", 404);
        } else if (!auth.canEdit()) {
            throw new WebApplicationException("Not permitted to edit project", 403);
        }

        Scenario entity = Scenario.findByProject(projectId, number);
        if (entity == null) {
            throw new WebApplicationException("Scenario not found", 404);
        }

        entity.delete();
        return UserProtocol.toDto(entity, auth);
    }
}
