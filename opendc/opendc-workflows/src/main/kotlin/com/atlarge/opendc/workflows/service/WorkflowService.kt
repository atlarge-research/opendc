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

import com.atlarge.odcsim.Channel
import com.atlarge.odcsim.ProcessContext
import com.atlarge.odcsim.SendRef
import com.atlarge.odcsim.ask
import com.atlarge.opendc.core.Zone
import com.atlarge.opendc.core.ZoneMessage
import com.atlarge.opendc.core.find
import com.atlarge.opendc.core.resources.compute.MachineEvent
import com.atlarge.opendc.core.services.AbstractService
import com.atlarge.opendc.core.services.Service
import com.atlarge.opendc.core.services.ServiceProvider
import com.atlarge.opendc.core.services.provisioning.ProvisioningMessage
import com.atlarge.opendc.core.services.provisioning.ProvisioningResponse
import com.atlarge.opendc.core.services.provisioning.ProvisioningService
import com.atlarge.opendc.workflows.workload.Job
import com.atlarge.opendc.workflows.workload.Task
import java.util.UUID
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * A service for cloud workflow management.
 *
 * The workflow scheduler is modelled after the Reference Architecture for Datacenter Scheduling by Andreadis et al.
 */
class WorkflowService(val scheduler: WorkflowScheduler) : ServiceProvider {
    override val uid: UUID = UUID.randomUUID()
    override val name: String = "workflows"
    override val provides: Set<Service<*>> = setOf(WorkflowService)
    override val dependencies: Set<Service<*>> = setOf(ProvisioningService)

    /**
     * Build the runtime [Behavior] for the workflow service, responding to messages of shape [WorkflowMessage].
     */
    override suspend fun invoke(ctx: ProcessContext, zone: Zone, zoneRef: SendRef<ZoneMessage>, main: Channel<Any>) {
        coroutineScope {
            val inlet = ctx.listen(main.receive)
            val provisioner = zoneRef.find(ProvisioningService)
            // Wait for 0.1 sec before asking the provisioner to allow it to initialize. Will return empty response if asked
            // immediately.
            delay(10)
            val lease: ProvisioningResponse.Lease = provisioner.ask { ProvisioningMessage.Request(Int.MAX_VALUE, it) }
            val schedulerLogic = scheduler(ctx, main.send, this, lease)

            while (isActive) {
                when (val msg = inlet.receive()) {
                    is WorkflowMessage.Submit -> {
                        schedulerLogic.submit(msg.job, msg.broker)
                    }
                    is MachineEvent.Submitted -> {
                        schedulerLogic.onSubmission(msg.instance, msg.application, msg.key, msg.pid)
                    }
                    is MachineEvent.Terminated -> {
                        schedulerLogic.onTermination(msg.instance, msg.pid, msg.status)
                    }
                }
            }
        }
    }

    companion object : AbstractService<WorkflowMessage>(UUID.randomUUID(), "workflows")
}

/**
 * A reference to the workflow service instance.
 */
typealias WorkflowServiceRef = SendRef<WorkflowMessage>

/**
 * A message protocol for communicating to the workflow service.
 */
sealed class WorkflowMessage {
    /**
     * Submit the specified [Job] to the workflow service for scheduling.
     *
     * @property job The workflow to submit for scheduling.
     * @property broker The broker that has submitted this workflow on behalf of a user and that needs to be kept
     * up-to-date.
     */
    data class Submit(val job: Job, val broker: SendRef<WorkflowEvent>) : WorkflowMessage()
}

/**
 * A message protocol used by the workflow service to respond to [WorkflowMessage]s.
 */
sealed class WorkflowEvent {
    /**
     * Indicate that the specified [Job] was submitted to the workflow service.
     *
     * @property service The reference to the service the job was submitted to.
     * @property job The job that has been submitted.
     * @property time A timestamp of the moment the job was received.
     */
    data class JobSubmitted(
        val service: WorkflowServiceRef,
        val job: Job,
        val time: Long
    ) : WorkflowEvent()

    /**
     * Indicate that the specified [Job] has become active.
     *
     * @property service The reference to the service the job was submitted to.
     * @property job The job that has been submitted.
     * @property time A timestamp of the moment the job started.
     */
    data class JobStarted(
        val service: WorkflowServiceRef,
        val job: Job,
        val time: Long
    ) : WorkflowEvent()

    /**
     * Indicate that the specified [Task] has started processing.
     *
     * @property service The reference to the service the job was submitted to.
     * @property job The job that contains this task.
     * @property task The task that has started processing.
     * @property time A timestamp of the moment the task started.
     */
    data class TaskStarted(
        val service: WorkflowServiceRef,
        val job: Job,
        val task: Task,
        val time: Long
    ) : WorkflowEvent()

    /**
     * Indicate that the specified [Task] has started processing.
     *
     * @property service The reference to the service the job was submitted to.
     * @property job The job that contains this task.
     * @property task The task that has started processing.
     * @property status The exit code of the task, where zero means successful.
     * @property time A timestamp of the moment the task finished.
     */
    data class TaskFinished(
        val service: WorkflowServiceRef,
        val job: Job,
        val task: Task,
        val status: Int,
        val time: Long
    ) : WorkflowEvent()

    /**
     * Indicate that the specified [Job] has finished processing.
     *
     * @property service The reference to the service the job was submitted to.
     * @property job The job that has finished processing.
     * @property time A timestamp of the moment the task finished.
     */
    data class JobFinished(
        val service: WorkflowServiceRef,
        val job: Job,
        val time: Long
    ) : WorkflowEvent()
}
