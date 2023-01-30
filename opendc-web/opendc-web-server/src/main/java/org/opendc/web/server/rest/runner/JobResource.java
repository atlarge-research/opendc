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

package org.opendc.web.server.rest.runner;

import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import org.opendc.web.proto.JobState;
import org.opendc.web.server.model.Job;
import org.opendc.web.server.service.JobService;

/**
 * A resource representing the available simulation jobs.
 */
@Produces("application/json")
@Path("/jobs")
@RolesAllowed("runner")
public final class JobResource {
    /**
     * The {@link JobService} for helping manage the job lifecycle.
     */
    private final JobService jobService;

    /**
     * Construct a {@link JobResource} instance.
     *
     * @param jobService The {@link JobService} for managing the job lifecycle.
     */
    public JobResource(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * Obtain all pending simulation jobs.
     */
    @GET
    public List<org.opendc.web.proto.runner.Job> queryPending() {
        return Job.findByState(JobState.PENDING).list().stream()
                .map(RunnerProtocol::toDto)
                .toList();
    }

    /**
     * Get a job by identifier.
     */
    @GET
    @Path("{job}")
    public org.opendc.web.proto.runner.Job get(@PathParam("job") long id) {
        Job job = Job.findById(id);

        if (job == null) {
            throw new WebApplicationException("Job not found", 404);
        }

        return RunnerProtocol.toDto(job);
    }

    /**
     * Atomically update the state of a job.
     */
    @POST
    @Path("{job}")
    @Consumes("application/json")
    @Transactional
    public org.opendc.web.proto.runner.Job update(
            @PathParam("job") long id, @Valid org.opendc.web.proto.runner.Job.Update update) {
        Job job = Job.findById(id);
        if (job == null) {
            throw new WebApplicationException("Job not found", 404);
        }

        try {
            jobService.updateJob(job, update.getState(), update.getRuntime(), update.getResults());
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e, 400);
        } catch (IllegalStateException e) {
            throw new WebApplicationException(e, 409);
        }

        return RunnerProtocol.toDto(job);
    }
}
