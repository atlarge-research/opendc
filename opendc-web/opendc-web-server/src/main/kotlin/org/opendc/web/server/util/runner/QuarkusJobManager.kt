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

package org.opendc.web.server.util.runner

import org.opendc.web.proto.JobState
import org.opendc.web.proto.runner.Job
import org.opendc.web.runner.JobManager
import org.opendc.web.server.service.JobService
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.transaction.Transactional

/**
 * Implementation of [JobManager] that interfaces directly with [JobService] without overhead of the REST API.
 */
@ApplicationScoped
class QuarkusJobManager @Inject constructor(private val service: JobService) : JobManager {
    @Transactional
    override fun findNext(): Job? {
        return service.queryPending().firstOrNull()
    }

    @Transactional
    override fun claim(id: Long): Boolean {
        return try {
            service.updateState(id, JobState.CLAIMED, 0, null)
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

    @Transactional
    override fun heartbeat(id: Long, runtime: Int): Boolean {
        val res = service.updateState(id, JobState.RUNNING, runtime, null)
        return res?.state != JobState.FAILED
    }

    @Transactional
    override fun fail(id: Long, runtime: Int) {
        service.updateState(id, JobState.FAILED, runtime, null)
    }

    @Transactional
    override fun finish(id: Long, runtime: Int, results: Map<String, Any>) {
        service.updateState(id, JobState.FINISHED, runtime, results)
    }
}
