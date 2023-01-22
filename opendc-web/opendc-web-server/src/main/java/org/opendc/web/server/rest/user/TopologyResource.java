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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import org.opendc.web.server.service.TopologyService;

/**
 * A resource representing the constructed datacenter topologies.
 */
@Produces("application/json")
@Path("/projects/{project}/topologies")
@RolesAllowed("openid")
public final class TopologyResource {
    /**
     * The service for managing the user topologies.
     */
    private final TopologyService topologyService;

    /**
     * The identity of the current user.
     */
    private final SecurityIdentity identity;

    /**
     * Construct a {@link TopologyResource}.
     *
     * @param topologyService The {@link TopologyService} instance to use.
     * @param identity The {@link SecurityIdentity} of the current user.
     */
    public TopologyResource(TopologyService topologyService, SecurityIdentity identity) {
        this.topologyService = topologyService;
        this.identity = identity;
    }

    /**
     * Get all topologies that belong to the specified project.
     */
    @GET
    public List<org.opendc.web.proto.user.Topology> getAll(@PathParam("project") long projectId) {
        return topologyService.findAll(identity.getPrincipal().getName(), projectId);
    }

    /**
     * Create a topology for this project.
     */
    @POST
    @Consumes("application/json")
    @Transactional
    public org.opendc.web.proto.user.Topology create(
            @PathParam("project") long projectId, @Valid org.opendc.web.proto.user.Topology.Create request) {
        var topology = topologyService.create(identity.getPrincipal().getName(), projectId, request);

        if (topology == null) {
            throw new WebApplicationException("Topology not found", 404);
        }

        return topology;
    }

    /**
     * Obtain a topology by its number.
     */
    @GET
    @Path("{topology}")
    public org.opendc.web.proto.user.Topology get(
            @PathParam("project") long projectId, @PathParam("topology") int number) {
        var topology = topologyService.findOne(identity.getPrincipal().getName(), projectId, number);

        if (topology == null) {
            throw new WebApplicationException("Topology not found", 404);
        }

        return topology;
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
        var topology = topologyService.update(identity.getPrincipal().getName(), projectId, number, request);

        if (topology == null) {
            throw new WebApplicationException("Topology not found", 404);
        }

        return topology;
    }

    /**
     * Delete the specified topology.
     */
    @Path("{topology}")
    @DELETE
    @Transactional
    public org.opendc.web.proto.user.Topology delete(
            @PathParam("project") long projectId, @PathParam("topology") int number) {
        var topology = topologyService.delete(identity.getPrincipal().getName(), projectId, number);

        if (topology == null) {
            throw new WebApplicationException("Topology not found", 404);
        }

        return topology;
    }
}
