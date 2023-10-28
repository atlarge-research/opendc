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

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import java.time.Instant;
import java.util.List;
import org.opendc.web.server.model.Project;
import org.opendc.web.server.model.ProjectAuthorization;
import org.opendc.web.server.model.Topology;

/**
 * A resource representing the constructed datacenter topologies.
 */
@Produces("application/json")
@Path("/projects/{project}/topologies")
@RolesAllowed("openid")
public final class TopologyResource {
    /**
     * The identity of the current user.
     */
    private final SecurityIdentity identity;

    /**
     * Construct a {@link TopologyResource}.
     *
     * @param identity The {@link SecurityIdentity} of the current user.
     */
    public TopologyResource(SecurityIdentity identity) {
        this.identity = identity;
    }

    /**
     * Get all topologies that belong to the specified project.
     */
    @GET
    public List<org.opendc.web.proto.user.Topology> getAll(@PathParam("project") long projectId) {
        // User must have access to project
        ProjectAuthorization auth =
                ProjectAuthorization.findByUser(identity.getPrincipal().getName(), projectId);

        if (auth == null) {
            return List.of();
        }

        return Topology.findByProject(projectId).list().stream()
                .map((t) -> UserProtocol.toDto(t, auth))
                .toList();
    }

    /**
     * Create a topology for this project.
     */
    @POST
    @Consumes("application/json")
    @Transactional
    public org.opendc.web.proto.user.Topology create(
            @PathParam("project") long projectId, @Valid org.opendc.web.proto.user.Topology.Create request) {
        // User must have access to project
        ProjectAuthorization auth =
                ProjectAuthorization.findByUser(identity.getPrincipal().getName(), projectId);

        if (auth == null) {
            throw new WebApplicationException("Topology not found", 404);
        } else if (!auth.canEdit()) {
            throw new WebApplicationException("Not permitted to edit project", 403);
        }

        Instant now = Instant.now();
        Project project = auth.project;
        int number = project.allocateTopology(now);

        Topology topology = new Topology(project, number, request.name(), now, request.rooms());

        project.topologies.add(topology);
        topology.persist();

        return UserProtocol.toDto(topology, auth);
    }

    /**
     * Obtain a topology by its number.
     */
    @GET
    @Path("{topology}")
    public org.opendc.web.proto.user.Topology get(
            @PathParam("project") long projectId, @PathParam("topology") int number) {
        // User must have access to project
        ProjectAuthorization auth =
                ProjectAuthorization.findByUser(identity.getPrincipal().getName(), projectId);

        if (auth == null) {
            throw new WebApplicationException("Topology not found", 404);
        }

        Topology topology = Topology.findByProject(projectId, number);

        if (topology == null) {
            throw new WebApplicationException("Topology not found", 404);
        }

        return UserProtocol.toDto(topology, auth);
    }

    /**
     * Update the specified topology by its number.
     */
    @PUT
    @Path("{topology}")
    @Consumes("application/json")
    @Transactional
    public org.opendc.web.proto.user.Topology update(
            @PathParam("project") long projectId,
            @PathParam("topology") int number,
            @Valid org.opendc.web.proto.user.Topology.Update request) {
        // User must have access to project
        ProjectAuthorization auth =
                ProjectAuthorization.findByUser(identity.getPrincipal().getName(), projectId);

        if (auth == null) {
            throw new WebApplicationException("Topology not found", 404);
        } else if (!auth.canEdit()) {
            throw new WebApplicationException("Not permitted to edit project", 403);
        }

        Topology entity = Topology.findByProject(projectId, number);

        if (entity == null) {
            throw new WebApplicationException("Topology not found", 404);
        }

        entity.updatedAt = Instant.now();
        entity.rooms = request.rooms();

        return UserProtocol.toDto(entity, auth);
    }

    /**
     * Delete the specified topology.
     */
    @Path("{topology}")
    @DELETE
    @Transactional
    public org.opendc.web.proto.user.Topology delete(
            @PathParam("project") long projectId, @PathParam("topology") int number) {
        // User must have access to project
        ProjectAuthorization auth =
                ProjectAuthorization.findByUser(identity.getPrincipal().getName(), projectId);

        if (auth == null) {
            throw new WebApplicationException("Topology not found", 404);
        } else if (!auth.canEdit()) {
            throw new WebApplicationException("Not permitted to edit project", 403);
        }

        Topology entity = Topology.findByProject(projectId, number);

        if (entity == null) {
            throw new WebApplicationException("Topology not found", 404);
        }

        entity.updatedAt = Instant.now();
        entity.delete();

        try {
            // Flush the results, so we can check whether the constraints are not violated
            Panache.flush();
        } catch (PersistenceException e) {
            throw new WebApplicationException("Topology is still in use", 403);
        }

        return UserProtocol.toDto(entity, auth);
    }
}
