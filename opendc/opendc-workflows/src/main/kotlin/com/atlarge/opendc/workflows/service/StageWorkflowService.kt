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
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.compute.metal.service.ProvisioningService
import com.atlarge.opendc.workflows.monitor.WorkflowMonitor
import com.atlarge.opendc.workflows.service.stage.job.JobAdmissionPolicy
import com.atlarge.opendc.workflows.service.stage.job.JobSortingPolicy
import com.atlarge.opendc.workflows.service.stage.resource.ResourceDynamicFilterPolicy
import com.atlarge.opendc.workflows.service.stage.resource.ResourceSelectionPolicy
import com.atlarge.opendc.workflows.service.stage.task.TaskEligibilityPolicy
import com.atlarge.opendc.workflows.service.stage.task.TaskSortingPolicy
import com.atlarge.opendc.workflows.workload.Job
import com.atlarge.opendc.workflows.workload.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A [WorkflowService] that distributes work through a multi-stage process based on the Reference Architecture for
 * Datacenter Scheduling.
 */
class StageWorkflowService(
    private val ctx: ProcessContext,
    private val provisioningService: ProvisioningService,
    private val mode: WorkflowSchedulerMode,
    private val jobAdmissionPolicy: JobAdmissionPolicy,
    private val jobSortingPolicy: JobSortingPolicy,
    private val taskEligibilityPolicy: TaskEligibilityPolicy,
    private val taskSortingPolicy: TaskSortingPolicy,
    private val resourceDynamicFilterPolicy: ResourceDynamicFilterPolicy,
    private val resourceSelectionPolicy: ResourceSelectionPolicy
) : WorkflowService, ServerMonitor {

    /**
     * The incoming jobs ready to be processed by the scheduler.
     */
    internal val incomingJobs: MutableSet<JobView> = mutableSetOf()

    /**
     * The active jobs in the system.
     */
    internal val activeJobs: MutableSet<JobView> = mutableSetOf()

    /**
     * The running tasks by [Server].
     */
    internal val taskByServer = mutableMapOf<Server, TaskView>()

    /**
     * The nodes that are controlled by the service.
     */
    internal lateinit var nodes: List<Node>

    /**
     * The available nodes.
     */
    internal val available: MutableSet<Node> = mutableSetOf()

    init {
        ctx.launch {
            nodes = provisioningService.nodes().toList()
            available.addAll(nodes)
        }
    }

    override suspend fun submit(job: Job, monitor: WorkflowMonitor) {
        // J1 Incoming Jobs
        val jobInstance = JobView(job, monitor)
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
                instance.state = TaskState.READY
            }
        }

        jobInstance.tasks = instances.values.toMutableSet()
        incomingJobs += jobInstance
        requestCycle()
    }

    private var next: kotlinx.coroutines.Job? = null

    /**
     * Indicate to the scheduler that a scheduling cycle is needed.
     */
    private fun requestCycle() {
        when (mode) {
            is WorkflowSchedulerMode.Interactive -> {
                ctx.launch {
                    schedule()
                }
            }
            is WorkflowSchedulerMode.Batch -> {
                if (next == null) {
                    val job = ctx.launch {
                        delay(mode.quantum)
                        next = null
                        schedule()
                    }
                    next = job
                }
            }
        }
    }

    /**
     * Perform a scheduling cycle immediately.
     */
    private suspend fun schedule() {
        // J2 Create list of eligible jobs
        jobAdmissionPolicy.startCycle(this)
        val eligibleJobs = incomingJobs.filter { jobAdmissionPolicy.shouldAdmit(this, it) }

        for (jobInstance in eligibleJobs) {
            incomingJobs -= jobInstance
            activeJobs += jobInstance
            jobInstance.monitor.onJobStart(jobInstance.job, ctx.clock.millis())
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
                val hosts = resourceDynamicFilterPolicy(this, nodes, instance)
                val host = resourceSelectionPolicy.select(this, hosts, instance)

                if (host != null) {
                    // T4 Submit task to machine
                    available -= host
                    instance.state = TaskState.ACTIVE

                    val newHost = provisioningService.deploy(host, instance.task.image, this)
                    instance.host = newHost
                    taskByServer[newHost.server!!] = instance
                } else {
                    return
                }
            }
        }
    }

    override suspend fun onUpdate(server: Server, previousState: ServerState) {
        when (server.state) {
            ServerState.ACTIVE -> {
                val task = taskByServer.getValue(server)
                task.job.monitor.onTaskStart(task.job.job, task.task, ctx.clock.millis())
            }
            ServerState.SHUTOFF, ServerState.ERROR -> {
                val task = taskByServer.remove(server) ?: throw IllegalStateException()
                val job = task.job
                task.state = TaskState.FINISHED
                job.tasks.remove(task)
                available += task.host!!
                job.monitor.onTaskFinish(job.job, task.task, 0, ctx.clock.millis())

                if (job.isFinished) {
                    activeJobs -= job
                    job.monitor.onJobFinish(job.job, ctx.clock.millis())
                }

                requestCycle()
            }
            else -> throw IllegalStateException()
        }
    }

    class JobView(val job: Job, val monitor: WorkflowMonitor) {
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

        var state: TaskState = TaskState.CREATED
            set(value) {
                field = value

                // Mark the process as terminated in the graph
                if (value == TaskState.FINISHED) {
                    markTerminated()
                }
            }

        var host: Node? = null

        /**
         * Mark the specified [TaskView] as terminated.
         */
        private fun markTerminated() {
            for (dependent in dependents) {
                dependent.dependencies.remove(this)

                if (dependent.isRoot) {
                    dependent.state = TaskState.READY
                }
            }
        }
    }
}
