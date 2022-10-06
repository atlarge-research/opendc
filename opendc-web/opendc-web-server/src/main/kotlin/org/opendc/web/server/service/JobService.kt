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

package org.opendc.web.server.service

import org.opendc.web.proto.JobState
import org.opendc.web.proto.runner.Job
import org.opendc.web.server.repository.JobRepository
import java.time.Instant
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Service for managing [Job]s.
 */
@ApplicationScoped
class JobService @Inject constructor(
    private val repository: JobRepository,
    private val accountingService: UserAccountingService
) {
    /**
     * Query the pending simulation jobs.
     */
    fun queryPending(): List<Job> {
        return repository.findAll(JobState.PENDING).map { it.toRunnerDto() }
    }

    /**
     * Find a job by its identifier.
     */
    fun findById(id: Long): Job? {
        return repository.findOne(id)?.toRunnerDto()
    }

    /**
     * Atomically update the state of a [Job].
     *
     * @param id The identifier of the job.
     * @param newState The next state for the job.
     * @param runtime The runtime of the job (in seconds).
     * @param results The potential results of the job.
     */
    fun updateState(id: Long, newState: JobState, runtime: Int, results: Map<String, Any>?): Job? {
        val entity = repository.findOne(id) ?: return null
        val state = entity.state
        if (!state.isTransitionLegal(newState)) {
            throw IllegalArgumentException("Invalid transition from $state to $newState")
        }

        val now = Instant.now()
        var nextState = newState
        val consumedBudget = (runtime - entity.runtime).coerceAtLeast(1)

        // Check whether the user still has any simulation budget left
        if (accountingService.consumeSimulationBudget(entity.createdBy, consumedBudget) && nextState == JobState.RUNNING) {
            nextState = JobState.FAILED // User has consumed all their budget; cancel the job
        }

        if (!repository.updateOne(entity, nextState, now, runtime, results)) {
            throw IllegalStateException("Conflicting update")
        }

        return entity.toRunnerDto()
    }

    /**
     * Determine whether the transition from [this] to [newState] is legal.
     */
    private fun JobState.isTransitionLegal(newState: JobState): Boolean {
        // Note that we always allow transitions from the state
        return newState == this || when (this) {
            JobState.PENDING -> newState == JobState.CLAIMED
            JobState.CLAIMED -> newState == JobState.RUNNING || newState == JobState.FAILED
            JobState.RUNNING -> newState == JobState.FINISHED || newState == JobState.FAILED
            JobState.FINISHED, JobState.FAILED -> false
        }
    }
}
