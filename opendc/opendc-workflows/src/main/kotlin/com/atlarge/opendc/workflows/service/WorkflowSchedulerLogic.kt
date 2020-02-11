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

import com.atlarge.odcsim.ProcessContext
import com.atlarge.odcsim.SendRef
import com.atlarge.opendc.core.resources.compute.scheduling.ProcessObserver
import com.atlarge.opendc.core.services.provisioning.ProvisioningResponse
import com.atlarge.opendc.workflows.workload.Job
import kotlinx.coroutines.CoroutineScope

/**
 * A workflow scheduler interface that schedules jobs across machines.
 *
 * @property ctx The context in which the scheduler runs.
 * @property timers The timer scheduler to use.
 * @property lease The resource lease to use.
 */
abstract class WorkflowSchedulerLogic(
    protected val ctx: ProcessContext,
    protected val self: WorkflowServiceRef,
    protected val coroutineScope: CoroutineScope,
    protected val lease: ProvisioningResponse.Lease
) : ProcessObserver {
    /**
     * Submit the specified workflow for scheduling.
     */
    abstract suspend fun submit(job: Job, handler: SendRef<WorkflowEvent>)

    /**
     * Trigger an immediate scheduling cycle.
     */
    abstract suspend fun schedule()
}
