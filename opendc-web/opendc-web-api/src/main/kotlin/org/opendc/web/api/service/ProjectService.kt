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

package org.opendc.web.api.service

import org.opendc.web.api.model.*
import org.opendc.web.api.repository.ProjectRepository
import org.opendc.web.proto.user.Project
import org.opendc.web.proto.user.ProjectRole
import java.time.Instant
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Service for managing [Project]s.
 */
@ApplicationScoped
class ProjectService @Inject constructor(private val repository: ProjectRepository) {
    /**
     * List all projects for the user with the specified [userId].
     */
    fun findWithUser(userId: String): List<Project> {
        return repository.findAll(userId).map { it.toUserDto() }
    }

    /**
     * Obtain the project with the specified [id] for the user with the specified [userId].
     */
    fun findWithUser(userId: String, id: Long): Project? {
        return repository.findOne(userId, id)?.toUserDto()
    }

    /**
     * Create a new [Project] for the user with the specified [userId].
     */
    fun createForUser(userId: String, name: String): Project {
        val now = Instant.now()
        val entity = Project(0, name, now)
        repository.save(entity)

        val authorization = ProjectAuthorization(ProjectAuthorizationKey(userId, entity.id), entity, ProjectRole.OWNER)

        entity.authorizations.add(authorization)
        repository.save(authorization)

        return authorization.toUserDto()
    }

    /**
     * Delete a project by its identifier.
     *
     * @param userId The user that invokes the action.
     * @param id The identifier of the project.
     */
    fun deleteWithUser(userId: String, id: Long): Project? {
        val auth = repository.findOne(userId, id) ?: return null

        if (!auth.role.canDelete) {
            throw IllegalArgumentException("Not allowed to delete project")
        }

        val now = Instant.now()
        val project = auth.toUserDto().copy(updatedAt = now)
        repository.delete(auth.project)
        return project
    }
}
