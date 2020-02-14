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

import com.atlarge.opendc.workflows.service.StageWorkflowService

/**
 * A policy interface for determining the eligibility of tasks in a scheduling cycle.
 */
interface TaskEligibilityPolicy {
    /**
     * A method that is invoked at the start of each scheduling cycle.
     *
     * @param scheduler The scheduler that started the cycle.
     */
    fun startCycle(scheduler: StageWorkflowService) {}

    /**
     * Determine whether the specified [StageWorkflowService.TaskView] is eligible to be scheduled.
     *
     * @param scheduler The scheduler that is determining whether the task is eligible.
     * @param task The task instance to schedule.
     * @return `true` if the task eligible to be scheduled, `false` otherwise.
     */
    fun isEligible(scheduler: StageWorkflowService, task: StageWorkflowService.TaskView): Boolean
}
