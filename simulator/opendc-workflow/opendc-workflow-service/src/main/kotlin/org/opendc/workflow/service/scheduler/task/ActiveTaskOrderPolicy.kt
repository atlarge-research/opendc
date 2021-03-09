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
 * A [TaskOrderPolicy] that orders tasks based on the number of active relative tasks (w.r.t. its job) in the system.
 */
public data class ActiveTaskOrderPolicy(public val ascending: Boolean = true) : TaskOrderPolicy {
    override fun invoke(scheduler: WorkflowServiceImpl): Comparator<TaskState> =
        object : Comparator<TaskState>, WorkflowSchedulerListener {
            private val active = mutableMapOf<JobState, Int>()

            init {
                scheduler.addListener(this)
            }

            override fun jobStarted(job: JobState) {
                active[job] = 0
            }

            override fun jobFinished(job: JobState) {
                active.remove(job)
            }

            override fun taskAssigned(task: TaskState) {
                active.merge(task.job, 1, Int::plus)
            }

            override fun taskFinished(task: TaskState) {
                active.merge(task.job, -1, Int::plus)
            }

            override fun compare(o1: TaskState, o2: TaskState): Int {
                return compareValuesBy(o1, o2) { active.getValue(it.job) }.let {
                    if (ascending) it else -it
                }
            }
        }

    override fun toString(): String {
        return "Active-Per-Job(${if (ascending) "asc" else "desc"})"
    }
}
