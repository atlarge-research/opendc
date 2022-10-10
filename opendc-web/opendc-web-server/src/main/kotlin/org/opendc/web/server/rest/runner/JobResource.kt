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

package org.opendc.web.server.rest.runner

import org.opendc.web.proto.runner.Job
import org.opendc.web.server.service.JobService
import javax.annotation.security.RolesAllowed
import javax.inject.Inject
import javax.transaction.Transactional
import javax.validation.Valid
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.WebApplicationException

/**
 * A resource representing the available simulation jobs.
 */
@Path("/jobs")
@RolesAllowed("runner")
class JobResource @Inject constructor(private val jobService: JobService) {
    /**
     * Obtain all pending simulation jobs.
     */
    @GET
    fun queryPending(): List<Job> {
        return jobService.queryPending()
    }

    /**
     * Get a job by identifier.
     */
    @GET
    @Path("{job}")
    fun get(@PathParam("job") id: Long): Job {
        return jobService.findById(id) ?: throw WebApplicationException("Job not found", 404)
    }

    /**
     * Atomically update the state of a job.
     */
    @POST
    @Path("{job}")
    @Transactional
    fun update(@PathParam("job") id: Long, @Valid update: Job.Update): Job {
        return try {
            jobService.updateState(id, update.state, update.runtime, update.results)
                ?: throw WebApplicationException("Job not found", 404)
        } catch (e: IllegalArgumentException) {
            throw WebApplicationException(e, 400)
        } catch (e: IllegalStateException) {
            throw WebApplicationException(e, 409)
        }
    }
}
