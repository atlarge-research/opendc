/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.workflow.service.scheduler.telemetry

/**
 * Statistics about the workflow scheduler.
 *
 * @property workflowsSubmitted The number of workflows submitted to the scheduler.
 * @property workflowsRunning The number of workflows that are currently running.
 * @property workflowsFinished The number of workflows that have completed since the scheduler started.
 * @property tasksSubmitted The number of tasks submitted to the scheduler.
 * @property tasksRunning The number of tasks that are currently running.
 * @property tasksFinished The number of tasks that have completed.
 */
public data class SchedulerStats(
    val workflowsSubmitted: Int,
    val workflowsRunning: Int,
    val workflowsFinished: Int,
    val tasksSubmitted: Int,
    val tasksRunning: Int,
    val tasksFinished: Int
)
