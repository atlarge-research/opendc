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

import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.Instant
import com.atlarge.odcsim.TimerScheduler
import com.atlarge.odcsim.coroutines.actorContext
import com.atlarge.odcsim.coroutines.dsl.ask
import com.atlarge.odcsim.coroutines.suspending
import com.atlarge.odcsim.receiveMessage
import com.atlarge.odcsim.same
import com.atlarge.odcsim.unhandled
import com.atlarge.odcsim.withTimers
import com.atlarge.opendc.model.Zone
import com.atlarge.opendc.model.ZoneRef
import com.atlarge.opendc.model.find
import com.atlarge.opendc.model.resources.compute.MachineEvent
import com.atlarge.opendc.model.services.AbstractService
import com.atlarge.opendc.model.services.Service
import com.atlarge.opendc.model.services.ServiceProvider
import com.atlarge.opendc.model.services.provisioning.ProvisioningMessage
import com.atlarge.opendc.model.services.provisioning.ProvisioningResponse
import com.atlarge.opendc.model.services.provisioning.ProvisioningService
import com.atlarge.opendc.model.workload.workflow.Job
import com.atlarge.opendc.model.workload.workflow.Task
import java.util.UUID

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
    override fun invoke(zone: Zone, ref: ZoneRef): Behavior<WorkflowMessage> = suspending { ctx ->
        val provisioner = ref.find(ProvisioningService)
        // Wait for 0.1 sec before asking the provisioner to allow it to initialize. Will return empty response if asked
        // immediately.
        val lease: ProvisioningResponse.Lease = actorContext<ProvisioningResponse>().ask(provisioner, after = 0.1) { ProvisioningMessage.Request(Int.MAX_VALUE, it) }

        withTimers<Any> { timers ->
            @Suppress("UNCHECKED_CAST")
            val schedulerLogic = scheduler(ctx, timers as TimerScheduler<WorkflowMessage>, lease)

            receiveMessage { msg ->
                when (msg) {
                    is WorkflowMessage.Submit -> {
                        schedulerLogic.submit(msg.job, msg.broker)
                        same()
                    }
                    is MachineEvent.Submitted -> {
                        schedulerLogic.onSubmission(msg.instance, msg.application, msg.key, msg.pid)
                        same()
                    }
                    is MachineEvent.Terminated -> {
                        schedulerLogic.onTermination(msg.instance, msg.pid, msg.status)
                        same()
                    }
                    else ->
                        unhandled()
                }
            }
        }.narrow()
    }

    companion object : AbstractService<WorkflowMessage>(UUID.randomUUID(), "workflows")
}

/**
 * A reference to the workflow service instance.
 */
typealias WorkflowServiceRef = ActorRef<WorkflowMessage>

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
    data class Submit(val job: Job, val broker: ActorRef<WorkflowEvent>) : WorkflowMessage()
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
        val time: Instant
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
        val time: Instant
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
        val time: Instant
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
        val time: Instant
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
        val time: Instant
    ) : WorkflowEvent()
}
