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
import com.atlarge.opendc.workflows.service.StageWorkflowSchedulerListener
import com.atlarge.opendc.workflows.service.StageWorkflowService
import com.atlarge.opendc.workflows.workload.Job
import java.util.Random

/**
 * A [JobOrderPolicy] that randomly orders jobs.
 */
object RandomJobOrderPolicy : JobOrderPolicy {
    override fun invoke(scheduler: StageWorkflowService): Comparator<JobState> =
        object : Comparator<JobState>, StageWorkflowSchedulerListener {
            private val random = Random(123)
            private val ids = HashMap<Job, Int>()

            init {
                scheduler.addListener(this)
            }

            override fun jobSubmitted(job: JobState) {
                ids[job.job] = random.nextInt()
            }

            override fun jobFinished(job: JobState) {
                ids.remove(job.job)
            }

            override fun compare(o1: JobState, o2: JobState): Int {
                return compareValuesBy(o1, o2) { ids.getValue(it.job) }
            }
        }

    override fun toString(): String {
        return "Random"
    }
}
