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

package org.opendc.web.runner

import org.opendc.web.client.runner.OpenDCRunnerClient
import org.opendc.web.proto.runner.Job
import org.opendc.web.runner.internal.JobManagerImpl

/**
 * Interface used by the [OpenDCRunner] to manage the available jobs to be processed.
 */
public interface JobManager {
    /**
     * Find the next job that the simulator needs to process.
     */
    public fun findNext(): Job?

    /**
     * Claim the simulation job with the specified id.
     */
    public fun claim(id: Long): Boolean

    /**
     * Update the heartbeat of the specified job.
     *
     * @param id The identifier of the job.
     * @param runtime The total runtime of the job.
     * @return `true` if the job can continue, `false` if the job has been cancelled.
     */
    public fun heartbeat(id: Long, runtime: Int): Boolean

    /**
     * Mark the job as failed.
     */
    public fun fail(id: Long, runtime: Int)

    /**
     * Persist the specified results for the specified job.
     */
    public fun finish(id: Long, runtime: Int, results: Map<String, Any>)

    public companion object {
        /**
         * Create a [JobManager] given the specified [client].
         */
        @JvmStatic
        @JvmName("create")
        public operator fun invoke(client: OpenDCRunnerClient): JobManager {
            return JobManagerImpl(client)
        }
    }
}
