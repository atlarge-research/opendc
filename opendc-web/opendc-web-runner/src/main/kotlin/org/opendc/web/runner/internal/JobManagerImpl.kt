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

package org.opendc.web.runner.internal

import org.opendc.web.client.runner.OpenDCRunnerClient
import org.opendc.web.proto.JobState
import org.opendc.web.proto.runner.Job
import org.opendc.web.runner.JobManager

/**
 * Default implementation of [JobManager] that uses the OpenDC client to receive jobs.
 */
internal class JobManagerImpl(private val client: OpenDCRunnerClient) : JobManager {
    override fun findNext(): Job? {
        return client.jobs.queryPending().firstOrNull()
    }

    override fun claim(id: Long): Boolean {
        return try {
            client.jobs.update(id, Job.Update(JobState.CLAIMED, 0))
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

    override fun heartbeat(id: Long, runtime: Int): Boolean {
        val res = client.jobs.update(id, Job.Update(JobState.RUNNING, runtime))
        return res?.state != JobState.FAILED
    }

    override fun fail(id: Long, runtime: Int) {
        client.jobs.update(id, Job.Update(JobState.FAILED, runtime))
    }

    override fun finish(id: Long, runtime: Int, results: Map<String, Any>) {
        client.jobs.update(id, Job.Update(JobState.FINISHED, runtime))
    }
}
