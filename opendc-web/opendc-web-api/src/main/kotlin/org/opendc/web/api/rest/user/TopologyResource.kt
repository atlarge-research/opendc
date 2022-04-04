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
import org.opendc.web.api.service.TopologyService
import org.opendc.web.proto.user.Topology
import javax.annotation.security.RolesAllowed
import javax.inject.Inject
import javax.transaction.Transactional
import javax.validation.Valid
import javax.ws.rs.*

/**
 * A resource representing the constructed datacenter topologies.
 */
@Path("/projects/{project}/topologies")
@RolesAllowed("openid")
class TopologyResource @Inject constructor(
    private val topologyService: TopologyService,
    private val identity: SecurityIdentity
) {
    /**
     * Get all topologies that belong to the specified project.
     */
    @GET
    fun getAll(@PathParam("project") projectId: Long): List<Topology> {
        return topologyService.findAll(identity.principal.name, projectId)
    }

    /**
     * Create a topology for this project.
     */
    @POST
    @Transactional
    fun create(@PathParam("project") projectId: Long, @Valid request: Topology.Create): Topology {
        return topologyService.create(identity.principal.name, projectId, request) ?: throw WebApplicationException("Topology not found", 404)
    }

    /**
     * Obtain a topology by its number.
     */
    @GET
    @Path("{topology}")
    fun get(@PathParam("project") projectId: Long, @PathParam("topology") number: Int): Topology {
        return topologyService.findOne(identity.principal.name, projectId, number) ?: throw WebApplicationException("Topology not found", 404)
    }

    /**
     * Update the specified topology by its number.
     */
    @PUT
    @Path("{topology}")
    @Transactional
    fun update(@PathParam("project") projectId: Long, @PathParam("topology") number: Int, @Valid request: Topology.Update): Topology {
        return topologyService.update(identity.principal.name, projectId, number, request) ?: throw WebApplicationException("Topology not found", 404)
    }

    /**
     * Delete the specified topology.
     */
    @Path("{topology}")
    @DELETE
    @Transactional
    fun delete(@PathParam("project") projectId: Long, @PathParam("topology") number: Int): Topology {
        return topologyService.delete(identity.principal.name, projectId, number) ?: throw WebApplicationException("Topology not found", 404)
    }
}
