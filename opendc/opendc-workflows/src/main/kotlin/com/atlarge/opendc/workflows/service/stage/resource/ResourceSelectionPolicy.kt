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

package com.atlarge.opendc.workflows.service.stage.resource

import com.atlarge.opendc.core.services.resources.HostView
import com.atlarge.opendc.workflows.service.StageWorkflowSchedulerLogic

/**
 * This interface represents the **R5** stage of the Reference Architecture for Schedulers and matches the the selected
 * task with a (set of) resource(s), using policies such as First-Fit, Worst-Fit, and Best-Fit.
 */
interface ResourceSelectionPolicy {
    /**
     * Select a machine on which the task should be scheduled.
     *
     * @param scheduler The scheduler to select the machine.
     * @param machines The list of machines in the system.
     * @param task The task that is to be scheduled.
     * @return The selected machine or `null` if no machine could be found.
     */
    fun select(
        scheduler: StageWorkflowSchedulerLogic,
        machines: List<HostView>,
        task: StageWorkflowSchedulerLogic.TaskView
    ): HostView?
}
