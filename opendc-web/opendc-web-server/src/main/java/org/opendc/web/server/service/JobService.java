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
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import org.opendc.web.proto.JobState;
import org.opendc.web.server.model.Job;

/**
 * Service for managing {@link Job}s.
 */
@ApplicationScoped
public final class JobService {
    /**
     * The service for managing the user accounting.
     */
    private final UserAccountingService accountingService;

    /**
     * Construct a {@link JobService} instance.
     *
     * @param accountingService The {@link UserAccountingService} instance to use.
     */
    public JobService(UserAccountingService accountingService) {
        this.accountingService = accountingService;
    }

    /**
     * Query the pending simulation jobs.
     */
    public List<org.opendc.web.proto.runner.Job> listPending() {
        return Job.findByState(JobState.PENDING).stream()
                .map(JobService::toRunnerDto)
                .toList();
    }

    /**
     * Find a job by its identifier.
     */
    public org.opendc.web.proto.runner.Job findById(long id) {
        return toRunnerDto(Job.findById(id));
    }

    /**
     * Atomically update the state of a {@link Job}.
     *
     * @param id The identifier of the job.
     * @param newState The next state for the job.
     * @param runtime The runtime of the job (in seconds).
     * @param results The potential results of the job.
     */
    public org.opendc.web.proto.runner.Job updateState(
            long id, JobState newState, int runtime, Map<String, ?> results) {
        Job entity = Job.findById(id);
        if (entity == null) {
            return null;
        }

        JobState state = entity.state;
        if (!isTransitionLegal(state, newState)) {
            throw new IllegalArgumentException("Invalid transition from %s to %s".formatted(state, newState));
        }

        Instant now = Instant.now();
        JobState nextState = newState;
        int consumedBudget = Math.min(1, runtime - entity.runtime);

        // Check whether the user still has any simulation budget left
        if (accountingService.consumeSimulationBudget(entity.createdBy, consumedBudget)
                && nextState == JobState.RUNNING) {
            nextState = JobState.FAILED; // User has consumed all their budget; cancel the job
        }

        if (!entity.updateAtomically(nextState, now, runtime, results)) {
            throw new IllegalStateException("Conflicting update");
        }

        return toRunnerDto(entity);
    }

    /**
     * Determine whether the transition from [this] to [newState] is legal.
     */
    public static boolean isTransitionLegal(JobState currentState, JobState newState) {
        // Note that we always allow transitions from the state
        return newState == currentState
                || switch (currentState) {
                    case PENDING -> newState == JobState.CLAIMED;
                    case CLAIMED -> newState == JobState.RUNNING || newState == JobState.FAILED;
                    case RUNNING -> newState == JobState.FINISHED || newState == JobState.FAILED;
                    case FINISHED, FAILED -> false;
                };
    }

    /**
     * Convert a {@link Job} entity into a {@link org.opendc.web.proto.user.Job} DTO.
     */
    public static org.opendc.web.proto.user.Job toUserDto(Job job) {
        return new org.opendc.web.proto.user.Job(job.id, job.state, job.createdAt, job.updatedAt, job.results);
    }

    /**
     * Convert a {@link Job} into a runner-facing DTO.
     */
    public static org.opendc.web.proto.runner.Job toRunnerDto(Job job) {
        return new org.opendc.web.proto.runner.Job(
                job.id,
                ScenarioService.toRunnerDto(job.scenario),
                job.state,
                job.createdAt,
                job.updatedAt,
                job.runtime,
                job.results);
    }
}
