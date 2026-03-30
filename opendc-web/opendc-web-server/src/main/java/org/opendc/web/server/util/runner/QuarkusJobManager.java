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

package org.opendc.web.server.util.runner;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opendc.web.proto.JobState;
import org.opendc.web.runner.JobManager;
import org.opendc.web.server.model.Job;
import org.opendc.web.server.rest.runner.RunnerProtocol;
import org.opendc.web.server.service.JobService;

/**
 * Implementation of {@link JobManager} that interfaces directly with the database without overhead of the REST API.
 */
@ApplicationScoped
public class QuarkusJobManager implements JobManager {
    /**
     * The {@link JobService} used to manage the job's lifecycle.
     */
    private final JobService jobService;

    /**
     * Construct a {@link QuarkusJobManager}.
     *
     * @param jobService The {@link JobService} for managing the job's lifecycle.
     */
    public QuarkusJobManager(JobService jobService) {
        this.jobService = jobService;
    }

    @Transactional
    @Nullable
    @Override
    public org.opendc.web.proto.runner.Job findNext() {
        var job = Job.findByState(JobState.PENDING).firstResult();
        if (job == null) {
            return null;
        }

        return RunnerProtocol.toDto(job);
    }

    @Transactional
    @Override
    public boolean claim(long id) {
        return updateState(id, JobState.CLAIMED, 0, null);
    }

    @Transactional
    @Override
    public boolean heartbeat(long id, int runtime) {
        return updateState(id, JobState.RUNNING, runtime, null);
    }

    @Transactional
    @Override
    public void fail(long id, int runtime) {
        updateState(id, JobState.FAILED, runtime, null);
    }

    @Transactional
    @Override
    public void finish(long id, int runtime, @NotNull Map<String, ?> results) {
        updateState(id, JobState.FINISHED, runtime, results);
    }

    /**
     * Helper method to update the state of a job.
     *
     * @param id The unique id of the job.
     * @param newState The new state to transition to.
     * @param runtime The runtime of the job.
     * @param results The results of the job.
     * @return <code>true</code> if the operation succeeded, <code>false</code> otherwise.
     */
    private boolean updateState(long id, JobState newState, int runtime, Map<String, ?> results) {
        Job job = Job.findById(id);

        if (job == null) {
            return false;
        }

        try {
            jobService.updateJob(job, newState, runtime, results);
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            return false;
        }
    }
}
