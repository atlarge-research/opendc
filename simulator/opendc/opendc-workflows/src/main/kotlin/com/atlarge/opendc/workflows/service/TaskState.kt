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

package com.atlarge.opendc.workflows.service

import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.workflows.workload.Task

class TaskState(val job: JobState, val task: Task) {
    /**
     * The moment in time the task was started.
     */
    var startedAt: Long = Long.MIN_VALUE

    /**
     * The moment in time the task was finished.
     */
    var finishedAt: Long = Long.MIN_VALUE

    /**
     * The dependencies of this task.
     */
    val dependencies = HashSet<TaskState>()

    /**
     * The dependents of this task.
     */
    val dependents = HashSet<TaskState>()

    /**
     * A flag to indicate whether this workflow task instance is a workflow root.
     */
    val isRoot: Boolean
        get() = dependencies.isEmpty()

    var state: TaskStatus = TaskStatus.CREATED
        set(value) {
            field = value

            // Mark the process as terminated in the graph
            if (value == TaskStatus.FINISHED) {
                markTerminated()
            }
        }

    var host: Node? = null

    /**
     * Mark the specified [TaskView] as terminated.
     */
    private fun markTerminated() {
        for (dependent in dependents) {
            dependent.dependencies.remove(this)

            if (dependent.isRoot) {
                dependent.state = TaskStatus.READY
            }
        }
    }

    override fun equals(other: Any?): Boolean = other is TaskState && other.job == job && other.task == task

    override fun hashCode(): Int = task.hashCode()
}
