/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.workflows.service.stage.job

import com.atlarge.opendc.workflows.service.JobState
import com.atlarge.opendc.workflows.service.stage.StagePolicy

/**
 * A policy interface for admitting [JobState]s to a scheduling cycle.
 */
interface JobAdmissionPolicy : StagePolicy<JobAdmissionPolicy.Logic> {
    interface Logic {
        /**
         * Determine whether the specified [JobState] should be admitted to the scheduling cycle.
         *
         * @param job The workflow that has been submitted.
         * @return The advice for admitting the job.
         */
        operator fun invoke(job: JobState): Advice
    }

    /**
     * The advice given to the scheduler by an admission policy.
     *
     * @property admit A flag to indicate to the scheduler that the job should be admitted.
     * @property stop A flag to indicate the scheduler should immediately stop admitting jobs to the scheduling queue and wait
     * for the next scheduling cycle.
     */
    enum class Advice(val admit: Boolean, val stop: Boolean) {
        /**
         * Admit the current job to the scheduling queue and continue admitting jobs.
         */
        ADMIT(true, false),

        /**
         * Admit the current job to the scheduling queue and stop admitting jobs.
         */
        ADMIT_LAST(true, true),

        /**
         * Deny the current job, but continue admitting jobs.
         */
        DENY(false, false),

        /**
         * Deny the current job and also stop admitting jobs.
         */
        STOP(false, true)
    }
}
