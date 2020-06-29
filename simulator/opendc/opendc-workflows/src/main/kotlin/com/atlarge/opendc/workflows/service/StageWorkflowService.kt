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

import com.atlarge.odcsim.Domain
import com.atlarge.odcsim.flow.EventFlow
import com.atlarge.odcsim.simulationContext
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerEvent
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.compute.metal.service.ProvisioningService
import com.atlarge.opendc.workflows.service.stage.job.JobAdmissionPolicy
import com.atlarge.opendc.workflows.service.stage.job.JobOrderPolicy
import com.atlarge.opendc.workflows.service.stage.resource.ResourceFilterPolicy
import com.atlarge.opendc.workflows.service.stage.resource.ResourceSelectionPolicy
import com.atlarge.opendc.workflows.service.stage.task.TaskEligibilityPolicy
import com.atlarge.opendc.workflows.service.stage.task.TaskOrderPolicy
import com.atlarge.opendc.workflows.workload.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.PriorityQueue
import java.util.Queue
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A [WorkflowService] that distributes work through a multi-stage process based on the Reference Architecture for
 * Datacenter Scheduling.
 */
class StageWorkflowService(
    private val domain: Domain,
    private val provisioningService: ProvisioningService,
    mode: WorkflowSchedulerMode,
    jobAdmissionPolicy: JobAdmissionPolicy,
    jobOrderPolicy: JobOrderPolicy,
    taskEligibilityPolicy: TaskEligibilityPolicy,
    taskOrderPolicy: TaskOrderPolicy,
    resourceFilterPolicy: ResourceFilterPolicy,
    resourceSelectionPolicy: ResourceSelectionPolicy
) : WorkflowService, CoroutineScope by domain {

    /**
     * The incoming jobs ready to be processed by the scheduler.
     */
    internal val incomingJobs: MutableSet<JobState> = linkedSetOf()

    /**
     * The incoming tasks ready to be processed by the scheduler.
     */
    internal val incomingTasks: MutableSet<TaskState> = linkedSetOf()

    /**
     * The job queue.
     */
    internal val jobQueue: Queue<JobState>

    /**
     * The task queue.
     */
    internal val taskQueue: Queue<TaskState>

    /**
     * The active jobs in the system.
     */
    internal val activeJobs: MutableSet<JobState> = mutableSetOf()

    /**
     * The active tasks in the system.
     */
    internal val activeTasks: MutableSet<TaskState> = mutableSetOf()

    /**
     * The running tasks by [Server].
     */
    internal val taskByServer = mutableMapOf<Server, TaskState>()

    /**
     * The nodes that are controlled by the service.
     */
    internal lateinit var nodes: List<Node>

    /**
     * The available nodes.
     */
    internal val available: MutableSet<Node> = mutableSetOf()

    /**
     * The maximum number of incoming jobs.
     */
    private val throttleLimit: Int = 20000

    /**
     * The load of the system.
     */
    internal val load: Double
        get() = (available.size / nodes.size.toDouble())

    /**
     * The root listener of this scheduler.
     */
    private val rootListener = object : StageWorkflowSchedulerListener {
        /**
         * The listeners to delegate to.
         */
        val listeners = mutableSetOf<StageWorkflowSchedulerListener>()

        override fun cycleStarted(scheduler: StageWorkflowService) {
            listeners.forEach { it.cycleStarted(scheduler) }
        }

        override fun cycleFinished(scheduler: StageWorkflowService) {
            listeners.forEach { it.cycleFinished(scheduler) }
        }

        override fun jobSubmitted(job: JobState) {
            listeners.forEach { it.jobSubmitted(job) }
        }

        override fun jobStarted(job: JobState) {
            listeners.forEach { it.jobStarted(job) }
        }

        override fun jobFinished(job: JobState) {
            listeners.forEach { it.jobFinished(job) }
        }

        override fun taskReady(task: TaskState) {
            listeners.forEach { it.taskReady(task) }
        }

        override fun taskAssigned(task: TaskState) {
            listeners.forEach { it.taskAssigned(task) }
        }

        override fun taskStarted(task: TaskState) {
            listeners.forEach { it.taskStarted(task) }
        }

        override fun taskFinished(task: TaskState) {
            listeners.forEach { it.taskFinished(task) }
        }
    }

    private val mode: WorkflowSchedulerMode.Logic
    private val jobAdmissionPolicy: JobAdmissionPolicy.Logic
    private val taskEligibilityPolicy: TaskEligibilityPolicy.Logic
    private val resourceFilterPolicy: ResourceFilterPolicy.Logic
    private val resourceSelectionPolicy: Comparator<Node>
    private val eventFlow = EventFlow<WorkflowEvent>()

    init {
        domain.launch {
            nodes = provisioningService.nodes().toList()
            available.addAll(nodes)
        }

        this.mode = mode(this)
        this.jobAdmissionPolicy = jobAdmissionPolicy(this)
        this.jobQueue = PriorityQueue(100, jobOrderPolicy(this).thenBy { it.job.uid })
        this.taskEligibilityPolicy = taskEligibilityPolicy(this)
        this.taskQueue = PriorityQueue(1000, taskOrderPolicy(this).thenBy { it.task.uid })
        this.resourceFilterPolicy = resourceFilterPolicy(this)
        this.resourceSelectionPolicy = resourceSelectionPolicy(this)
    }

    override val events: Flow<WorkflowEvent> = eventFlow

    override suspend fun submit(job: Job) = withContext(domain.coroutineContext) {
        // J1 Incoming Jobs
        val jobInstance = JobState(job, simulationContext.clock.millis())
        val instances = job.tasks.associateWith {
            TaskState(jobInstance, it)
        }

        for ((task, instance) in instances) {
            instance.dependencies.addAll(task.dependencies.map { instances[it]!! })
            task.dependencies.forEach {
                instances[it]!!.dependents.add(instance)
            }

            // If the task has no dependency, it is a root task and can immediately be evaluated
            if (instance.isRoot) {
                instance.state = TaskStatus.READY
            }
        }

        instances.values.toCollection(jobInstance.tasks)
        incomingJobs += jobInstance
        rootListener.jobSubmitted(jobInstance)

        requestCycle()
    }

    /**
     * Indicate to the scheduler that a scheduling cycle is needed.
     */
    private suspend fun requestCycle() = mode.requestCycle()

    /**
     * Perform a scheduling cycle immediately.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    internal suspend fun schedule() {
        // J2 Create list of eligible jobs
        val iterator = incomingJobs.iterator()
        while (iterator.hasNext()) {
            val jobInstance = iterator.next()
            val advice = jobAdmissionPolicy(jobInstance)
            if (advice.stop) {
                break
            } else if (!advice.admit) {
                continue
            }

            iterator.remove()
            jobQueue.add(jobInstance)
            activeJobs += jobInstance
            eventFlow.emit(WorkflowEvent.JobStarted(this, jobInstance.job, simulationContext.clock.millis()))
            rootListener.jobStarted(jobInstance)
        }

        // J4 Per job
        while (jobQueue.isNotEmpty()) {
            val jobInstance = jobQueue.poll()

            // Edge-case: job has no tasks
            if (jobInstance.isFinished) {
                finishJob(jobInstance)
            }

            // Add job roots to the scheduling queue
            for (task in jobInstance.tasks) {
                if (task.state != TaskStatus.READY) {
                    continue
                }

                incomingTasks += task
                rootListener.taskReady(task)
            }
        }

        // T1 Create list of eligible tasks
        val taskIterator = incomingTasks.iterator()
        while (taskIterator.hasNext()) {
            val taskInstance = taskIterator.next()
            val advice = taskEligibilityPolicy(taskInstance)
            if (advice.stop) {
                break
            } else if (!advice.admit) {
                continue
            }

            taskIterator.remove()
            taskQueue.add(taskInstance)
        }

        // T3 Per task
        while (taskQueue.isNotEmpty()) {
            val instance = taskQueue.peek()
            val host: Node? = available.firstOrNull()

            if (host != null) {
                // T4 Submit task to machine
                available -= host
                instance.state = TaskStatus.ACTIVE
                val newHost = provisioningService.deploy(host, instance.task.image)
                val server = newHost.server!!
                instance.host = newHost
                taskByServer[server] = instance
                server.events
                    .onEach { event -> if (event is ServerEvent.StateChanged) stateChanged(event.server) }
                    .launchIn(this)

                activeTasks += instance
                taskQueue.poll()
                rootListener.taskAssigned(instance)
            } else {
                break
            }
        }
    }

    private suspend fun stateChanged(server: Server) {
        when (server.state) {
            ServerState.ACTIVE -> {
                val task = taskByServer.getValue(server)
                task.startedAt = simulationContext.clock.millis()
                eventFlow.emit(WorkflowEvent.TaskStarted(this@StageWorkflowService, task.job.job, task.task, simulationContext.clock.millis()))
                rootListener.taskStarted(task)
            }
            ServerState.SHUTOFF, ServerState.ERROR -> {
                val task = taskByServer.remove(server) ?: throw IllegalStateException()
                val job = task.job
                task.state = TaskStatus.FINISHED
                task.finishedAt = simulationContext.clock.millis()
                job.tasks.remove(task)
                available += task.host!!
                activeTasks -= task
                eventFlow.emit(WorkflowEvent.TaskFinished(this@StageWorkflowService, task.job.job, task.task, simulationContext.clock.millis()))
                rootListener.taskFinished(task)

                // Add job roots to the scheduling queue
                for (dependent in task.dependents) {
                    if (dependent.state != TaskStatus.READY) {
                        continue
                    }

                    incomingTasks += dependent
                    rootListener.taskReady(dependent)
                }

                if (job.isFinished) {
                    finishJob(job)
                }

                requestCycle()
            }
            else -> throw IllegalStateException()
        }
    }

    private suspend fun finishJob(job: JobState) {
        activeJobs -= job
        eventFlow.emit(WorkflowEvent.JobFinished(this, job.job, simulationContext.clock.millis()))
        rootListener.jobFinished(job)
    }

    fun addListener(listener: StageWorkflowSchedulerListener) {
        rootListener.listeners += listener
    }

    fun removeListener(listener: StageWorkflowSchedulerListener) {
        rootListener.listeners -= listener
    }
}
