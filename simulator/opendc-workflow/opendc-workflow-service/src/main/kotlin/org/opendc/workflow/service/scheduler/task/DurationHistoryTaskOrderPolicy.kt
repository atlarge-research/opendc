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
 * A [TaskOrderPolicy] that orders tasks based on the average duration of the preceding tasks in the job.
 */
public data class DurationHistoryTaskOrderPolicy(public val ascending: Boolean = true) : TaskOrderPolicy {
    override fun invoke(scheduler: WorkflowServiceImpl): Comparator<TaskState> =
        object : Comparator<TaskState>, WorkflowSchedulerListener {
            private val results = HashMap<JobState, MutableList<Long>>()

            init {
                scheduler.addListener(this)
            }

            override fun jobStarted(job: JobState) {
                results[job] = mutableListOf()
            }

            override fun jobFinished(job: JobState) {
                results.remove(job)
            }

            override fun taskFinished(task: TaskState) {
                results.getValue(task.job) += task.finishedAt - task.startedAt
            }

            override fun compare(o1: TaskState, o2: TaskState): Int {
                return compareValuesBy(o1, o2) { key ->
                    val history = results.getValue(key.job)
                    if (history.isEmpty()) {
                        Long.MAX_VALUE
                    } else {
                        history.average()
                    }
                }.let { if (ascending) it else -it }
            }
        }

    override fun toString(): String {
        return "Task-Duration-History(${if (ascending) "asc" else "desc"})"
    }
}
