/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.workflow.service.scheduler.task

import org.opendc.workflow.service.internal.JobState
import org.opendc.workflow.service.internal.TaskState
import org.opendc.workflow.service.internal.WorkflowSchedulerListener
import org.opendc.workflow.service.internal.WorkflowServiceImpl

/**
 * A [TaskOrderPolicy] that orders tasks based on the number of completed relative tasks.
 */
public data class CompletionTaskOrderPolicy(public val ascending: Boolean = true) : TaskOrderPolicy {
    override fun invoke(scheduler: WorkflowServiceImpl): Comparator<TaskState> =
        object : Comparator<TaskState>, WorkflowSchedulerListener {
            private val finished = mutableMapOf<JobState, Int>()

            init {
                scheduler.addListener(this)
            }

            override fun jobStarted(job: JobState) {
                finished[job] = 0
            }

            override fun jobFinished(job: JobState) {
                finished.remove(job)
            }

            override fun taskFinished(task: TaskState) {
                finished.merge(task.job, 1, Int::plus)
            }

            override fun compare(o1: TaskState, o2: TaskState): Int {
                return compareValuesBy(o1, o2) { finished.getValue(it.job) / it.job.tasks.size.toDouble() }.let {
                    if (ascending) it else -it
                }
            }
        }

    override fun toString(): String {
        return "Job-Completion(${if (ascending) "asc" else "desc"})"
    }
}
