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

import org.opendc.web.api.model.Topology
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.persistence.EntityManager

/**
 * A repository to manage [Topology] entities.
 */
@ApplicationScoped
class TopologyRepository @Inject constructor(private val em: EntityManager) {
    /**
     * Find all [Topology]s that belong to [project][projectId].
     *
     * @param projectId The unique identifier of the project.
     * @return The list of topologies that belong to the specified project.
     */
    fun findAll(projectId: Long): List<Topology> {
        return em.createNamedQuery("Topology.findAll", Topology::class.java)
            .setParameter("projectId", projectId)
            .resultList
    }

    /**
     * Find the [Topology] with the specified [number] belonging to [project][projectId].
     *
     * @param projectId The unique identifier of the project.
     * @param number The number of the topology.
     * @return The topology or `null` if it does not exist.
     */
    fun findOne(projectId: Long, number: Int): Topology? {
        return em.createNamedQuery("Topology.findOne", Topology::class.java)
            .setParameter("projectId", projectId)
            .setParameter("number", number)
            .setMaxResults(1)
            .resultList
            .firstOrNull()
    }

    /**
     * Find the [Topology] with the specified [id].
     *
     * @param id Unique identifier of the topology.
     * @return The topology or `null` if it does not exist.
     */
    fun findOne(id: Long): Topology? {
        return em.find(Topology::class.java, id)
    }

    /**
     * Delete the specified [topology].
     */
    fun delete(topology: Topology) {
        em.remove(topology)
    }

    /**
     * Save the specified [topology] to the database.
     */
    fun save(topology: Topology) {
        em.persist(topology)
    }
}
