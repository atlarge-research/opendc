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

import org.opendc.web.api.model.Job
import org.opendc.web.proto.JobState
import java.time.Instant
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.persistence.EntityManager

/**
 * A repository to manage [Job] entities.
 */
@ApplicationScoped
class JobRepository @Inject constructor(private val em: EntityManager) {
    /**
     * Find all jobs currently residing in [state].
     *
     * @param state The state in which the jobs should be.
     * @return The list of jobs in state [state].
     */
    fun findAll(state: JobState): List<Job> {
        return em.createNamedQuery("Job.findAll", Job::class.java)
            .setParameter("state", state)
            .resultList
    }

    /**
     * Find the [Job] with the specified [id].
     *
     * @param id The unique identifier of the job.
     * @return The trace or `null` if it does not exist.
     */
    fun findOne(id: Long): Job? {
        return em.find(Job::class.java, id)
    }

    /**
     * Delete the specified [job].
     */
    fun delete(job: Job) {
        em.remove(job)
    }

    /**
     * Save the specified [job] to the database.
     */
    fun save(job: Job) {
        em.persist(job)
    }

    /**
     * Atomically update the specified [job].
     *
     * @param job The job to update atomically.
     * @param newState The new state to enter into.
     * @param time The time at which the update occurs.
     * @param results The results to possible set.
     * @return `true` when the update succeeded`, `false` when there was a conflict.
     */
    fun updateOne(job: Job, newState: JobState, time: Instant, results: Map<String, Any>?): Boolean {
        val count = em.createNamedQuery("Job.updateOne")
            .setParameter("id", job.id)
            .setParameter("oldState", job.state)
            .setParameter("newState", newState)
            .setParameter("updatedAt", Instant.now())
            .setParameter("results", results)
            .executeUpdate()
        em.refresh(job)
        return count > 0
    }
}
