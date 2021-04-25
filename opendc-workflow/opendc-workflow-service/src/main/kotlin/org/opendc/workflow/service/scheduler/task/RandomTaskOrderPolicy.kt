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

import org.opendc.workflow.api.Task
import org.opendc.workflow.service.internal.TaskState
import org.opendc.workflow.service.internal.WorkflowSchedulerListener
import org.opendc.workflow.service.internal.WorkflowServiceImpl
import kotlin.random.Random

/**
 * A [TaskOrderPolicy] that orders the tasks randomly.
 */
public object RandomTaskOrderPolicy : TaskOrderPolicy {
    override fun invoke(scheduler: WorkflowServiceImpl): Comparator<TaskState> =
        object : Comparator<TaskState>, WorkflowSchedulerListener {
            private val random = Random(123)
            private val ids = HashMap<Task, Int>()

            init {
                scheduler.addListener(this)
            }

            override fun taskReady(task: TaskState) {
                ids[task.task] = random.nextInt()
            }

            override fun taskFinished(task: TaskState) {
                ids.remove(task.task)
            }

            override fun compare(o1: TaskState, o2: TaskState): Int {
                return compareValuesBy(o1, o2) { ids.getValue(it.task) }
            }
        }

    override fun toString(): String {
        return "Random"
    }
}
