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

package com.atlarge.opendc.model.services.workflows

import com.atlarge.odcsim.ActorContext
import com.atlarge.odcsim.TimerScheduler
import com.atlarge.opendc.model.services.provisioning.ProvisioningResponse
import com.atlarge.opendc.model.services.workflows.stages.job.JobAdmissionPolicy
import com.atlarge.opendc.model.services.workflows.stages.job.JobSortingPolicy
import com.atlarge.opendc.model.services.workflows.stages.resources.ResourceDynamicFilterPolicy
import com.atlarge.opendc.model.services.workflows.stages.resources.ResourceSelectionPolicy
import com.atlarge.opendc.model.services.workflows.stages.task.TaskEligibilityPolicy
import com.atlarge.opendc.model.services.workflows.stages.task.TaskSortingPolicy

/**
 * A [WorkflowScheduler] that distributes work through a multi-stage process based on the Reference Architecture for
 * Datacenter Scheduling.
 */
class StageWorkflowScheduler(
    private val mode: WorkflowSchedulerMode,
    private val jobAdmissionPolicy: JobAdmissionPolicy,
    private val jobSortingPolicy: JobSortingPolicy,
    private val taskEligibilityPolicy: TaskEligibilityPolicy,
    private val taskSortingPolicy: TaskSortingPolicy,
    private val resourceDynamicFilterPolicy: ResourceDynamicFilterPolicy,
    private val resourceSelectionPolicy: ResourceSelectionPolicy
) : WorkflowScheduler {
    override fun invoke(
        ctx: ActorContext<WorkflowMessage>,
        timers: TimerScheduler<WorkflowMessage>,
        lease: ProvisioningResponse.Lease
    ): WorkflowSchedulerLogic {
        return StageWorkflowSchedulerLogic(ctx, timers, lease, mode, jobAdmissionPolicy,
            jobSortingPolicy, taskEligibilityPolicy, taskSortingPolicy, resourceDynamicFilterPolicy, resourceSelectionPolicy)
    }
}
