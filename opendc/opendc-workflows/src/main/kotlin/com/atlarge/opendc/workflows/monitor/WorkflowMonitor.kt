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

package com.atlarge.opendc.workflows.monitor

import com.atlarge.opendc.workflows.workload.Job
import com.atlarge.opendc.workflows.workload.Task

/**
 * An interface for monitoring the progression of workflows.
 */
public interface WorkflowMonitor {
    /**
     * This method is invoked when a job has become active.
     */
    public suspend fun onJobStart(job: Job, time: Long)

    /**
     * This method is invoked when a job has finished processing.
     */
    public suspend fun onJobFinish(job: Job, time: Long)

    /**
     * This method is invoked when a task of a job has started processing.
     */
    public suspend fun onTaskStart(job: Job, task: Task, time: Long)

    /**
     * This method is invoked when a task has finished processing.
     */
    public suspend fun onTaskFinish(job: Job, task: Task, status: Int, time: Long)
}
