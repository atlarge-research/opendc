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

import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opendc.web.proto.JobState;
import org.opendc.web.proto.runner.Job;
import org.opendc.web.runner.JobManager;
import org.opendc.web.server.service.JobService;

/**
 * Implementation of {@link JobManager} that interfaces directly with {@link JobService} without overhead of the REST API.
 */
@ApplicationScoped
public class QuarkusJobManager implements JobManager {
    private final JobService jobService;

    public QuarkusJobManager(JobService jobService) {
        this.jobService = jobService;
    }

    @Transactional
    @Nullable
    @Override
    public Job findNext() {
        var pending = jobService.listPending();
        if (pending.isEmpty()) {
            return null;
        }

        return pending.get(0);
    }

    @Override
    public boolean claim(long id) {
        try {
            jobService.updateState(id, JobState.CLAIMED, 0, null);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public boolean heartbeat(long id, int runtime) {
        Job res = jobService.updateState(id, JobState.RUNNING, runtime, null);
        return res != null && !res.getState().equals(JobState.FAILED);
    }

    @Override
    public void fail(long id, int runtime) {
        jobService.updateState(id, JobState.FAILED, runtime, null);
    }

    @Override
    public void finish(long id, int runtime, @NotNull Map<String, ?> results) {
        jobService.updateState(id, JobState.FINISHED, runtime, results);
    }
}
