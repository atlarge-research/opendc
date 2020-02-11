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
import com.atlarge.odcsim.SendPort
import com.atlarge.odcsim.SendRef
import com.atlarge.odcsim.sendOnce
import com.atlarge.opendc.core.resources.compute.MachineEvent
import com.atlarge.opendc.core.resources.compute.MachineMessage
import com.atlarge.opendc.core.resources.compute.MachineRef
import com.atlarge.opendc.core.resources.compute.scheduling.ProcessObserver
import com.atlarge.opendc.core.resources.compute.scheduling.ProcessState
import com.atlarge.opendc.core.services.provisioning.ProvisioningResponse
import com.atlarge.opendc.core.services.resources.HostView
import com.atlarge.opendc.core.workload.application.Application
import com.atlarge.opendc.core.workload.application.Pid
import com.atlarge.opendc.workflows.service.stage.job.JobAdmissionPolicy
import com.atlarge.opendc.workflows.service.stage.job.JobSortingPolicy
import com.atlarge.opendc.workflows.service.stage.resource.ResourceDynamicFilterPolicy
import com.atlarge.opendc.workflows.service.stage.resource.ResourceSelectionPolicy
import com.atlarge.opendc.workflows.service.stage.task.TaskEligibilityPolicy
import com.atlarge.opendc.workflows.service.stage.task.TaskSortingPolicy
import com.atlarge.opendc.workflows.workload.Job
import com.atlarge.opendc.workflows.workload.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Logic of the [StageWorkflowScheduler].
 */
class StageWorkflowSchedulerLogic(
    ctx: ProcessContext,
    self: WorkflowServiceRef,
    coroutineScope: CoroutineScope,
    lease: ProvisioningResponse.Lease,
    private val mode: WorkflowSchedulerMode,
    private val jobAdmissionPolicy: JobAdmissionPolicy,
    private val jobSortingPolicy: JobSortingPolicy,
    private val taskEligibilityPolicy: TaskEligibilityPolicy,
    private val taskSortingPolicy: TaskSortingPolicy,
    private val resourceDynamicFilterPolicy: ResourceDynamicFilterPolicy,
    private val resourceSelectionPolicy: ResourceSelectionPolicy
) : WorkflowSchedulerLogic(ctx, self, coroutineScope, lease) {

    /**
     * The incoming jobs ready to be processed by the scheduler.
     */
    internal val incomingJobs: MutableSet<JobView> = mutableSetOf()

    /**
     * The active jobs in the system.
     */
    internal val activeJobs: MutableSet<JobView> = mutableSetOf()

    /**
     * The running tasks by [Pid].
     */
    internal val taskByPid = mutableMapOf<Pid, TaskView>()

    /**
     * The available processor cores on the leased machines.
     */
    internal val machineCores: MutableMap<HostView, Int> = HashMap()

    private val brokers: MutableMap<SendRef<WorkflowEvent>, SendPort<WorkflowEvent>> = HashMap()
    private val channel = ctx.open<MachineEvent>()

    init {
        lease.hosts.forEach { machineCores[it] = it.host.cores.count() }
        coroutineScope.launch {
            ProcessObserver(ctx, this@StageWorkflowSchedulerLogic, channel.receive)
        }
    }

    override suspend fun submit(job: Job, handler: SendRef<WorkflowEvent>) {
        val broker = brokers.getOrPut(handler) { ctx.connect(handler) }

        // J1 Incoming Jobs
        val jobInstance = JobView(job, handler)
        val instances = job.tasks.associateWith {
            TaskView(jobInstance, it)
        }

        for ((task, instance) in instances) {
            instance.dependencies.addAll(task.dependencies.map { instances[it]!! })
            task.dependencies.forEach {
                instances[it]!!.dependents.add(instance)
            }

            // If the task has no dependency, it is a root task and can immediately be evaluated
            if (instance.isRoot) {
                instance.state = ProcessState.READY
            }
        }

        jobInstance.tasks = instances.values.toMutableSet()
        incomingJobs += jobInstance
        broker.send(WorkflowEvent.JobSubmitted(self, job, ctx.clock.millis()))
        requestCycle()
    }

    private var next: kotlinx.coroutines.Job? = null

    /**
     * Indicate to the scheduler that a scheduling cycle is needed.
     */
    private fun requestCycle() {
        when (mode) {
            is WorkflowSchedulerMode.Interactive -> {
                coroutineScope.launch {
                    schedule()
                }
            }
            is WorkflowSchedulerMode.Batch -> {
                if (next == null) {
                    val job = coroutineScope.launch {
                        delay(mode.quantum)
                        schedule()
                    }
                    next = job
                    job.invokeOnCompletion {
                        next = null
                    }
                }
            }
        }
    }

    /**
     * Perform a scheduling cycle immediately.
     */
    override suspend fun schedule() {
        // J2 Create list of eligible jobs
        jobAdmissionPolicy.startCycle(this)
        val eligibleJobs = incomingJobs.filter { jobAdmissionPolicy.shouldAdmit(this, it) }
        for (jobInstance in eligibleJobs) {
            incomingJobs -= jobInstance
            activeJobs += jobInstance
            brokers.getValue(jobInstance.broker).send(WorkflowEvent.JobStarted(self, jobInstance.job, ctx.clock.millis()))
        }

        // J3 Sort jobs on criterion
        val sortedJobs = jobSortingPolicy(this, activeJobs)

        // J4 Per job
        for (jobInstance in sortedJobs) {
            // T1 Create list of eligible tasks
            taskEligibilityPolicy.startCycle(this)
            val eligibleTasks = jobInstance.tasks.filter { taskEligibilityPolicy.isEligible(this, it) }

            // T2 Sort tasks on criterion
            val sortedTasks = taskSortingPolicy(this, eligibleTasks)

            // T3 Per task
            for (instance in sortedTasks) {
                val hosts = resourceDynamicFilterPolicy(this, lease.hosts, instance)
                val host = resourceSelectionPolicy.select(this, hosts, instance)

                if (host != null) {
                    // T4 Submit task to machine
                    host.ref.sendOnce(MachineMessage.Submit(instance.task.application, instance, channel.send))
                    instance.host = host
                    instance.state = ProcessState.RUNNING // Assume the application starts running
                    machineCores.merge(host, instance.task.application.cores, Int::minus)
                } else {
                    return
                }
            }
        }
    }

    override fun onSubmission(instance: MachineRef, application: Application, key: Any, pid: Pid) {
        val task = key as TaskView
        task.pid = pid
        taskByPid[pid] = task

        brokers.getValue(task.job.broker).send(WorkflowEvent.TaskStarted(self, task.job.job, task.task, ctx.clock.millis()))
    }

    override fun onTermination(instance: MachineRef, pid: Pid, status: Int) {
        val task = taskByPid.remove(pid) ?: throw IllegalStateException()

        val job = task.job
        task.state = ProcessState.TERMINATED
        job.tasks.remove(task)
        machineCores.merge(task.host!!, task.task.application.cores, Int::plus)
        brokers.getValue(job.broker).send(WorkflowEvent.TaskFinished(self, job.job, task.task, status, ctx.clock.millis()))

        if (job.isFinished) {
            activeJobs -= job
            brokers.getValue(job.broker).send(WorkflowEvent.JobFinished(self, job.job, ctx.clock.millis()))
        }

        requestCycle()
    }

    class JobView(val job: Job, val broker: SendRef<WorkflowEvent>) {
        /**
         * A flag to indicate whether this job is finished.
         */
        val isFinished: Boolean
            get() = tasks.isEmpty()

        lateinit var tasks: MutableSet<TaskView>
    }

    class TaskView(val job: JobView, val task: Task) {
        /**
         * The dependencies of this task.
         */
        val dependencies = HashSet<TaskView>()

        /**
         * The dependents of this task.
         */
        val dependents = HashSet<TaskView>()

        /**
         * A flag to indicate whether this workflow task instance is a workflow root.
         */
        val isRoot: Boolean
            get() = dependencies.isEmpty()

        var state: ProcessState = ProcessState.CREATED
            set(value) {
                field = value

                // Mark the process as terminated in the graph
                if (value == ProcessState.TERMINATED) {
                    markTerminated()
                }
            }

        var pid: Pid? = null

        var host: HostView? = null

        /**
         * Mark the specified [TaskView] as terminated.
         */
        private fun markTerminated() {
            for (dependent in dependents) {
                dependent.dependencies.remove(this)

                if (dependent.isRoot) {
                    dependent.state = ProcessState.READY
                }
            }
        }
    }
}
