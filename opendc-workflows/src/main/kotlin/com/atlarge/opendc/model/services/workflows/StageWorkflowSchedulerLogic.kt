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
import com.atlarge.odcsim.unsafeCast
import com.atlarge.opendc.model.resources.compute.MachineMessage
import com.atlarge.opendc.model.resources.compute.MachineRef
import com.atlarge.opendc.model.resources.compute.scheduling.ProcessState
import com.atlarge.opendc.model.services.provisioning.ProvisioningResponse
import com.atlarge.opendc.model.services.resources.HostView
import com.atlarge.opendc.model.services.workflows.stages.job.JobAdmissionPolicy
import com.atlarge.opendc.model.services.workflows.stages.job.JobSortingPolicy
import com.atlarge.opendc.model.services.workflows.stages.resources.ResourceDynamicFilterPolicy
import com.atlarge.opendc.model.services.workflows.stages.resources.ResourceSelectionPolicy
import com.atlarge.opendc.model.services.workflows.stages.task.TaskEligibilityPolicy
import com.atlarge.opendc.model.services.workflows.stages.task.TaskSortingPolicy
import com.atlarge.opendc.model.workload.application.Application
import com.atlarge.opendc.model.workload.application.Pid
import com.atlarge.opendc.model.workload.workflow.Job
import com.atlarge.opendc.model.workload.workflow.Task

/**
 * Logic of the [StageWorkflowScheduler].
 */
class StageWorkflowSchedulerLogic(
    ctx: ActorContext<WorkflowMessage>,
    timers: TimerScheduler<WorkflowMessage>,
    lease: ProvisioningResponse.Lease,
    private val mode: WorkflowSchedulerMode,
    private val jobAdmissionPolicy: JobAdmissionPolicy,
    private val jobSortingPolicy: JobSortingPolicy,
    private val taskEligibilityPolicy: TaskEligibilityPolicy,
    private val taskSortingPolicy: TaskSortingPolicy,
    private val resourceDynamicFilterPolicy: ResourceDynamicFilterPolicy,
    private val resourceSelectionPolicy: ResourceSelectionPolicy
) : WorkflowSchedulerLogic(ctx, timers, lease) {

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

    init {
        lease.hosts.forEach { machineCores[it] = it.host.cores.count() }
    }

    override fun submit(job: Job, handler: ActorRef<WorkflowEvent>) {
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
        ctx.send(handler, WorkflowEvent.JobSubmitted(ctx.self, job, ctx.time))
        requestCycle()
    }

    /**
     * Indicate to the scheduler that a scheduling cycle is needed.
     */
    private fun requestCycle() {
        when (mode) {
            is WorkflowSchedulerMode.Interactive -> {
                schedule()
            }
            is WorkflowSchedulerMode.Batch -> {
                timers.after(mode, mode.quantum) {
                    schedule()
                }
            }
        }
    }

    /**
     * Perform a scheduling cycle immediately.
     */
    override fun schedule() {
        // J2 Create list of eligible jobs
        jobAdmissionPolicy.startCycle(this)
        val eligibleJobs = incomingJobs.filter { jobAdmissionPolicy.shouldAdmit(this, it) }
        for (jobInstance in eligibleJobs) {
            incomingJobs -= jobInstance
            activeJobs += jobInstance
            ctx.send(jobInstance.broker, WorkflowEvent.JobStarted(ctx.self, jobInstance.job, ctx.time))
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
                    ctx.send(host.ref, MachineMessage.Submit(instance.task.application, instance, ctx.self.unsafeCast()))
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
        ctx.send(task.job.broker, WorkflowEvent.TaskStarted(ctx.self, task.job.job, task.task, ctx.time))
    }

    override fun onTermination(instance: MachineRef, pid: Pid, status: Int) {
        val task = taskByPid.remove(pid)!!
        val job = task.job
        task.state = ProcessState.TERMINATED
        job.tasks.remove(task)
        machineCores.merge(task.host!!, task.task.application.cores, Int::plus)
        ctx.send(job.broker, WorkflowEvent.TaskFinished(ctx.self, job.job, task.task, status, ctx.time))

        if (job.isFinished) {
            activeJobs -= job
            ctx.send(job.broker, WorkflowEvent.JobFinished(ctx.self, job.job, ctx.time))
        }

        requestCycle()
    }

    class JobView(val job: Job, val broker: ActorRef<WorkflowEvent>) {
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
