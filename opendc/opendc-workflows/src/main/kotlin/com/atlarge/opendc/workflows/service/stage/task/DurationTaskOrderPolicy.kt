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

package com.atlarge.opendc.workflows.service.stage.task

import com.atlarge.opendc.workflows.service.StageWorkflowSchedulerListener
import com.atlarge.opendc.workflows.service.StageWorkflowService
import com.atlarge.opendc.workflows.service.TaskState
import com.atlarge.opendc.workflows.workload.WORKFLOW_TASK_DEADLINE
import java.util.UUID

/**
 * A [TaskOrderPolicy] orders tasks based on the pre-specified (approximate) duration of the task.
 */
data class DurationTaskOrderPolicy(val ascending: Boolean = true) : TaskOrderPolicy {

    override fun invoke(scheduler: StageWorkflowService): Comparator<TaskState> =
        object : Comparator<TaskState>, StageWorkflowSchedulerListener {
            private val results = HashMap<UUID, Long>()

            init {
                scheduler.addListener(this)
            }

            override fun taskReady(task: TaskState) {
                val deadline = task.task.metadata[WORKFLOW_TASK_DEADLINE] as? Long?
                results[task.task.uid] = deadline ?: Long.MAX_VALUE
            }

            override fun taskFinished(task: TaskState) {
                results -= task.task.uid
            }

            private val TaskState.duration: Long
                get() = results.getValue(task.uid)

            override fun compare(o1: TaskState, o2: TaskState): Int {
                return compareValuesBy(o1, o2) { state -> state.duration }.let {
                    if (ascending) it else -it
                }
            }
        }

    override fun toString(): String {
        return "Task-Duration(${if (ascending) "asc" else "desc"})"
    }
}
