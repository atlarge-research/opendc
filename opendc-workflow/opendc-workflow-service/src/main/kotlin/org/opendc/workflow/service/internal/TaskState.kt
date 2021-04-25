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

package org.opendc.workflow.service.internal

import org.opendc.compute.api.Server
import org.opendc.workflow.api.Task

public class TaskState(public val job: JobState, public val task: Task) {
    /**
     * The moment in time the task was started.
     */
    public var startedAt: Long = Long.MIN_VALUE

    /**
     * The moment in time the task was finished.
     */
    public var finishedAt: Long = Long.MIN_VALUE

    /**
     * The dependencies of this task.
     */
    public val dependencies: HashSet<TaskState> = HashSet()

    /**
     * The dependents of this task.
     */
    public val dependents: HashSet<TaskState> = HashSet()

    /**
     * A flag to indicate whether this workflow task instance is a workflow root.
     */
    public val isRoot: Boolean
        get() = dependencies.isEmpty()

    public var state: TaskStatus = TaskStatus.CREATED
        set(value) {
            field = value

            // Mark the process as terminated in the graph
            if (value == TaskStatus.FINISHED) {
                markTerminated()
            }
        }

    public var server: Server? = null

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
