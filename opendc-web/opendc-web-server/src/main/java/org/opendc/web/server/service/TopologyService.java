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

package org.opendc.web.server.service;

import java.time.Instant;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import org.opendc.web.server.model.Project;
import org.opendc.web.server.model.ProjectAuthorization;
import org.opendc.web.server.model.Topology;

/**
 * Service for managing {@link Topology}s.
 */
@ApplicationScoped
public final class TopologyService {
    /**
     * List all {@link Topology}s that belong a certain project.
     */
    public List<org.opendc.web.proto.user.Topology> findAll(String userId, long projectId) {
        // User must have access to project
        ProjectAuthorization auth = ProjectAuthorization.findByUser(userId, projectId);

        if (auth == null) {
            return List.of();
        }

        return Topology.findByProject(projectId).stream()
                .map((t) -> toUserDto(t, auth))
                .toList();
    }

    /**
     * Find the {@link Topology} with the specified <code>number</code> belonging to <code>projectId</code>.
     */
    public org.opendc.web.proto.user.Topology findOne(String userId, long projectId, int number) {
        // User must have access to project
        ProjectAuthorization auth = ProjectAuthorization.findByUser(userId, projectId);

        if (auth == null) {
            return null;
        }

        Topology topology = Topology.findByProject(projectId, number);

        if (topology == null) {
            return null;
        }

        return toUserDto(topology, auth);
    }

    /**
     * Delete the {@link Topology} with the specified <code>number</code> belonging to <code>projectId</code>
     */
    public org.opendc.web.proto.user.Topology delete(String userId, long projectId, int number) {
        // User must have access to project
        ProjectAuthorization auth = ProjectAuthorization.findByUser(userId, projectId);

        if (auth == null) {
            return null;
        } else if (!auth.canEdit()) {
            throw new IllegalStateException("Not permitted to edit project");
        }

        Topology entity = Topology.findByProject(projectId, number);

        if (entity == null) {
            return null;
        }

        entity.updatedAt = Instant.now();
        entity.delete();
        return toUserDto(entity, auth);
    }

    /**
     * Update a {@link Topology} with the specified <code>number</code> belonging to <code>projectId</code>.
     */
    public org.opendc.web.proto.user.Topology update(
            String userId, long projectId, int number, org.opendc.web.proto.user.Topology.Update request) {
        // User must have access to project
        ProjectAuthorization auth = ProjectAuthorization.findByUser(userId, projectId);

        if (auth == null) {
            return null;
        } else if (!auth.canEdit()) {
            throw new IllegalStateException("Not permitted to edit project");
        }

        Topology entity = Topology.findByProject(projectId, number);

        if (entity == null) {
            return null;
        }

        entity.updatedAt = Instant.now();
        entity.rooms = request.getRooms();

        return toUserDto(entity, auth);
    }

    /**
     * Construct a new {@link Topology} with the specified name.
     */
    public org.opendc.web.proto.user.Topology create(
            String userId, long projectId, org.opendc.web.proto.user.Topology.Create request) {
        // User must have access to project
        ProjectAuthorization auth = ProjectAuthorization.findByUser(userId, projectId);

        if (auth == null) {
            return null;
        } else if (!auth.canEdit()) {
            throw new IllegalStateException("Not permitted to edit project");
        }

        Instant now = Instant.now();
        Project project = auth.project;
        int number = project.allocateTopology(now);

        Topology topology = new Topology(project, number, request.getName(), now, request.getRooms());

        project.topologies.add(topology);
        topology.persist();

        return toUserDto(topology, auth);
    }

    /**
     * Convert a {@link Topology} entity into a {@link org.opendc.web.proto.user.Topology} DTO.
     */
    public static org.opendc.web.proto.user.Topology toUserDto(Topology topology, ProjectAuthorization auth) {
        return new org.opendc.web.proto.user.Topology(
                topology.id,
                topology.number,
                ProjectService.toUserDto(auth),
                topology.name,
                topology.rooms,
                topology.createdAt,
                topology.updatedAt);
    }

    /**
     * Convert a {@link Topology} entity into a {@link org.opendc.web.proto.user.Topology.Summary} DTO.
     */
    public static org.opendc.web.proto.user.Topology.Summary toSummaryDto(Topology topology) {
        return new org.opendc.web.proto.user.Topology.Summary(
                topology.id, topology.number, topology.name, topology.createdAt, topology.updatedAt);
    }

    /**
     * Convert a {@link Topology} into a runner-facing DTO.
     */
    public static org.opendc.web.proto.runner.Topology toRunnerDto(Topology topology) {
        return new org.opendc.web.proto.runner.Topology(
                topology.id, topology.number, topology.name, topology.rooms, topology.createdAt, topology.updatedAt);
    }
}
