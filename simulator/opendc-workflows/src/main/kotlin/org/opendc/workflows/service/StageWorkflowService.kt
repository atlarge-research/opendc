/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.workflows.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.opendc.compute.core.Server
import org.opendc.compute.core.ServerEvent
import org.opendc.compute.core.ServerState
import org.opendc.compute.core.metal.Node
import org.opendc.compute.core.metal.service.ProvisioningService
import org.opendc.trace.core.EventTracer
import org.opendc.trace.core.consumeAsFlow
import org.opendc.trace.core.enable
import org.opendc.workflows.service.stage.job.JobAdmissionPolicy
import org.opendc.workflows.service.stage.job.JobOrderPolicy
import org.opendc.workflows.service.stage.resource.ResourceFilterPolicy
import org.opendc.workflows.service.stage.resource.ResourceSelectionPolicy
import org.opendc.workflows.service.stage.task.TaskEligibilityPolicy
import org.opendc.workflows.service.stage.task.TaskOrderPolicy
import org.opendc.workflows.workload.Job
import java.time.Clock
import java.util.*

/**
 * A [WorkflowService] that distributes work through a multi-stage process based on the Reference Architecture for
 * Topology Scheduling.
 */
public class StageWorkflowService(
    internal val coroutineScope: CoroutineScope,
    internal val clock: Clock,
    internal val tracer: EventTracer,
    private val provisioningService: ProvisioningService,
    mode: WorkflowSchedulerMode,
    jobAdmissionPolicy: JobAdmissionPolicy,
    jobOrderPolicy: JobOrderPolicy,
    taskEligibilityPolicy: TaskEligibilityPolicy,
    taskOrderPolicy: TaskOrderPolicy,
    resourceFilterPolicy: ResourceFilterPolicy,
    resourceSelectionPolicy: ResourceSelectionPolicy
) : WorkflowService {

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

    init {
        coroutineScope.launch {
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

    override val events: Flow<WorkflowEvent> = tracer.openRecording().let {
        it.enable<WorkflowEvent.JobSubmitted>()
        it.enable<WorkflowEvent.JobStarted>()
        it.enable<WorkflowEvent.JobFinished>()
        it.enable<WorkflowEvent.TaskStarted>()
        it.enable<WorkflowEvent.TaskFinished>()
        it.consumeAsFlow().map { event -> event as WorkflowEvent }
    }

    override suspend fun submit(job: Job) {
        // J1 Incoming Jobs
        val jobInstance = JobState(job, clock.millis())
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
        tracer.commit(WorkflowEvent.JobSubmitted(this, jobInstance.job))

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
            tracer.commit(WorkflowEvent.JobStarted(this, jobInstance.job))
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
                    .launchIn(coroutineScope)

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
                task.startedAt = clock.millis()
                tracer.commit(
                    WorkflowEvent.TaskStarted(
                        this@StageWorkflowService,
                        task.job.job,
                        task.task
                    )
                )
                rootListener.taskStarted(task)
            }
            ServerState.SHUTOFF, ServerState.ERROR -> {
                val task = taskByServer.remove(server) ?: throw IllegalStateException()
                val job = task.job
                task.state = TaskStatus.FINISHED
                task.finishedAt = clock.millis()
                job.tasks.remove(task)
                available += task.host!!
                activeTasks -= task
                tracer.commit(
                    WorkflowEvent.TaskFinished(
                        this@StageWorkflowService,
                        task.job.job,
                        task.task
                    )
                )
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
        tracer.commit(WorkflowEvent.JobFinished(this, job.job))
        rootListener.jobFinished(job)
    }

    public fun addListener(listener: StageWorkflowSchedulerListener) {
        rootListener.listeners += listener
    }

    public fun removeListener(listener: StageWorkflowSchedulerListener) {
        rootListener.listeners -= listener
    }
}
