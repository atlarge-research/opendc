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
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import org.opendc.web.proto.JobState;
import org.opendc.web.server.model.Job;

/**
 * A service for managing the lifecycle of a job and ensuring that the user does not consume
 * too much simulation resources.
 */
@ApplicationScoped
public final class JobService {
    /**
     * The {@link UserAccountingService} responsible for accounting the simulation time of users.
     */
    private final UserAccountingService accountingService;

    /**
     * Construct a {@link JobService} instance.
     *
     * @param accountingService The {@link UserAccountingService} for accounting the simulation time of users.
     */
    public JobService(UserAccountingService accountingService) {
        this.accountingService = accountingService;
    }

    /**
     * Update the job state.
     *
     * @param job The {@link Job} to update.
     * @param newState The new state to transition the job to.
     * @param runtime The runtime (in seconds) consumed by the simulation jbo so far.
     * @param results The results to attach to the job.
     * @throws IllegalArgumentException if the state transition is invalid.
     * @throws IllegalStateException if someone tries to update the job concurrently.
     */
    public void updateJob(Job job, JobState newState, int runtime, Map<String, ?> results) {
        JobState state = job.state;

        if (!job.canTransitionTo(newState)) {
            throw new IllegalArgumentException("Invalid transition from %s to %s".formatted(state, newState));
        }

        Instant now = Instant.now();
        JobState nextState = newState;
        int consumedBudget = Math.min(1, runtime - job.runtime);

        // Check whether the user still has any simulation budget left
        if (accountingService.consumeSimulationBudget(job.createdBy, consumedBudget) && nextState == JobState.RUNNING) {
            nextState = JobState.FAILED; // User has consumed all their budget; cancel the job
        }

        if (!job.updateAtomically(nextState, now, runtime, results)) {
            throw new IllegalStateException("Conflicting update");
        }
    }
}
