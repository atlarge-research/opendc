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

import org.opendc.workflow.service.WorkflowService

/**
 * Interface for listening to events emitted by the [WorkflowService].
 */
public interface WorkflowSchedulerListener {
    /**
     * This method is invoked when [job] is submitted to the service.
     */
    public fun jobSubmitted(job: JobState) {}

    /**
     * This method is invoked when [job] is started by the service.
     */
    public fun jobStarted(job: JobState) {}

    /**
     * This method is invoked when [job] finishes.
     */
    public fun jobFinished(job: JobState) {}

    /**
     * This method is invoked when [task] becomes ready to be scheduled.
     */
    public fun taskReady(task: TaskState) {}

    /**
     * This method is invoked when [task] is assigned to a machine.
     */
    public fun taskAssigned(task: TaskState) {}

    /**
     * This method is invoked when [task] is started.
     */
    public fun taskStarted(task: TaskState) {}

    /**
     * This method is invoked when [task] finishes.
     */
    public fun taskFinished(task: TaskState) {}
}
