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
import org.opendc.web.proto.user.ProjectRole;
import org.opendc.web.server.model.Project;
import org.opendc.web.server.model.ProjectAuthorization;

/**
 * Service for managing {@link Project}s.
 */
@ApplicationScoped
public final class ProjectService {
    /**
     * List all projects for the user with the specified <code>userId</code>.
     */
    public List<org.opendc.web.proto.user.Project> findByUser(String userId) {
        return ProjectAuthorization.findByUser(userId).stream()
                .map(ProjectService::toUserDto)
                .toList();
    }

    /**
     * Obtain the project with the specified <code>id</code> for the user with the specified <code>userId</code>.
     */
    public org.opendc.web.proto.user.Project findByUser(String userId, long id) {
        ProjectAuthorization auth = ProjectAuthorization.findByUser(userId, id);

        if (auth == null) {
            return null;
        }

        return toUserDto(auth);
    }

    /**
     * Create a new {@link Project} for the user with the specified <code>userId</code>.
     */
    public org.opendc.web.proto.user.Project create(String userId, String name) {
        Instant now = Instant.now();
        Project entity = new Project(name, now);
        entity.persist();

        ProjectAuthorization authorization = new ProjectAuthorization(entity, userId, ProjectRole.OWNER);

        entity.authorizations.add(authorization);
        authorization.persist();

        return toUserDto(authorization);
    }

    /**
     * Delete a project by its identifier.
     *
     * @param userId The user that invokes the action.
     * @param id The identifier of the project.
     */
    public org.opendc.web.proto.user.Project delete(String userId, long id) {
        ProjectAuthorization auth = ProjectAuthorization.findByUser(userId, id);

        if (auth == null) {
            return null;
        }

        if (!auth.canDelete()) {
            throw new IllegalArgumentException("Not allowed to delete project");
        }

        auth.project.updatedAt = Instant.now();
        org.opendc.web.proto.user.Project project = toUserDto(auth);
        auth.project.delete();
        return project;
    }

    /**
     * Convert a {@link ProjectAuthorization} entity into a {@link Project} DTO.
     */
    public static org.opendc.web.proto.user.Project toUserDto(ProjectAuthorization auth) {
        Project project = auth.project;
        return new org.opendc.web.proto.user.Project(
                project.id, project.name, project.createdAt, project.updatedAt, auth.role);
    }
}
