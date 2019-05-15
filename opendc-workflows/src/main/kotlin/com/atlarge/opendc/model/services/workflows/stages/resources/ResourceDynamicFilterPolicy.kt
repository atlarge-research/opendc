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

package com.atlarge.opendc.model.services.workflows.stages.resources

import com.atlarge.opendc.model.services.resources.HostView
import com.atlarge.opendc.model.services.workflows.StageWorkflowSchedulerLogic

/**
 * This interface represents the **R4** stage of the Reference Architecture for Schedulers and acts as a filter yielding
 * a list of resources with sufficient resource-capacities, based on fixed or dynamic requirements, and on predicted or
 * monitored information about processing unit availability, memory occupancy, etc.
 */
interface ResourceDynamicFilterPolicy {
    /**
     * Filter the list of machines based on dynamic information.
     *
     * @param scheduler The scheduler to filter the machines.
     * @param machines The list of machines in the system.
     * @param task The task that is to be scheduled.
     * @return The machines on which the task can be scheduled.
     */
    operator fun invoke(
        scheduler: StageWorkflowSchedulerLogic,
        machines: List<HostView>,
        task: StageWorkflowSchedulerLogic.TaskView
    ): List<HostView>
}
