/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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
import com.atlarge.opendc.workflows.service.StageWorkflowService

/**
 * A [JobAdmissionPolicy] that limits the amount of active jobs in the system.
 *
 * @property limit The maximum number of concurrent jobs in the system.
 */
data class LimitJobAdmissionPolicy(val limit: Int) : JobAdmissionPolicy {
    override fun invoke(scheduler: StageWorkflowService) = object : JobAdmissionPolicy.Logic {
        override fun invoke(
            job: JobState
        ): JobAdmissionPolicy.Advice =
            if (scheduler.activeJobs.size < limit)
                JobAdmissionPolicy.Advice.ADMIT
            else
                JobAdmissionPolicy.Advice.STOP
    }

    override fun toString(): String = "Limit-Active($limit)"
}
