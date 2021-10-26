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

package org.opendc.web.api.repository

import org.opendc.web.api.model.Project
import org.opendc.web.api.model.ProjectAuthorization
import org.opendc.web.api.model.ProjectAuthorizationKey
import java.time.Instant
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.persistence.EntityManager

/**
 * A repository to manage [Project] entities.
 */
@ApplicationScoped
class ProjectRepository @Inject constructor(private val em: EntityManager) {
    /**
     * List all projects for the user with the specified [userId].
     *
     * @param userId The identifier of the user that is requesting the list of projects.
     * @return A list of projects that the user has received authorization for.
     */
    fun findAll(userId: String): List<ProjectAuthorization> {
        return em.createNamedQuery("Project.findAll", ProjectAuthorization::class.java)
            .setParameter("userId", userId)
            .resultList
    }

    /**
     * Find the project with [id] for the user with the specified [userId].
     *
     * @param userId The identifier of the user that is requesting the list of projects.
     * @param id The unique identifier of the project.
     * @return The project with the specified identifier or `null` if it does not exist or is not accessible to the
     *         user with the specified identifier.
     */
    fun findOne(userId: String, id: Long): ProjectAuthorization? {
        return em.find(ProjectAuthorization::class.java, ProjectAuthorizationKey(userId, id))
    }

    /**
     * Delete the specified [project].
     */
    fun delete(project: Project) {
        em.remove(project)
    }

    /**
     * Save the specified [project] to the database.
     */
    fun save(project: Project) {
        em.persist(project)
    }

    /**
     * Save the specified [auth] to the database.
     */
    fun save(auth: ProjectAuthorization) {
        em.persist(auth)
    }

    /**
     * Allocate the next portfolio number for the specified [project].
     *
     * @param project The project to allocate the portfolio number for.
     * @param time The time at which the new portfolio is created.
     * @param tries The number of times to try to allocate the number before failing.
     */
    fun allocatePortfolio(project: Project, time: Instant, tries: Int = 4): Int {
        repeat(tries) {
            val count = em.createNamedQuery("Project.allocatePortfolio")
                .setParameter("id", project.id)
                .setParameter("oldState", project.portfoliosCreated)
                .setParameter("now", time)
                .executeUpdate()

            if (count > 0) {
                return project.portfoliosCreated + 1
            } else {
                em.refresh(project)
            }
        }

        throw IllegalStateException("Failed to allocate next portfolio")
    }

    /**
     * Allocate the next topology number for the specified [project].
     *
     * @param project The project to allocate the topology number for.
     * @param time The time at which the new topology is created.
     * @param tries The number of times to try to allocate the number before failing.
     */
    fun allocateTopology(project: Project, time: Instant, tries: Int = 4): Int {
        repeat(tries) {
            val count = em.createNamedQuery("Project.allocateTopology")
                .setParameter("id", project.id)
                .setParameter("oldState", project.topologiesCreated)
                .setParameter("now", time)
                .executeUpdate()

            if (count > 0) {
                return project.topologiesCreated + 1
            } else {
                em.refresh(project)
            }
        }

        throw IllegalStateException("Failed to allocate next topology")
    }

    /**
     * Allocate the next scenario number for the specified [project].
     *
     * @param project The project to allocate the scenario number for.
     * @param time The time at which the new scenario is created.
     * @param tries The number of times to try to allocate the number before failing.
     */
    fun allocateScenario(project: Project, time: Instant, tries: Int = 4): Int {
        repeat(tries) {
            val count = em.createNamedQuery("Project.allocateScenario")
                .setParameter("id", project.id)
                .setParameter("oldState", project.scenariosCreated)
                .setParameter("now", time)
                .executeUpdate()

            if (count > 0) {
                return project.scenariosCreated + 1
            } else {
                em.refresh(project)
            }
        }

        throw IllegalStateException("Failed to allocate next scenario")
    }
}
