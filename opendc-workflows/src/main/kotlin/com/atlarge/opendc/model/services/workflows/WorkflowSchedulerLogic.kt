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
import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.TimerScheduler
import com.atlarge.opendc.model.resources.compute.scheduling.ProcessObserver
import com.atlarge.opendc.model.services.provisioning.ProvisioningResponse
import com.atlarge.opendc.model.workload.workflow.Job

/**
 * A workflow scheduler interface that schedules jobs across machines.
 *
 * @property ctx The context in which the scheduler runs.
 * @property timers The timer scheduler to use.
 * @property lease The resource lease to use.
 */
abstract class WorkflowSchedulerLogic(
    protected val ctx: ActorContext<WorkflowMessage>,
    protected val timers: TimerScheduler<WorkflowMessage>,
    protected val lease: ProvisioningResponse.Lease
) : ProcessObserver {
    /**
     * Submit the specified workflow for scheduling.
     */
    abstract fun submit(job: Job, handler: ActorRef<WorkflowEvent>)

    /**
     * Trigger an immediate scheduling cycle.
     */
    abstract fun schedule()
}
