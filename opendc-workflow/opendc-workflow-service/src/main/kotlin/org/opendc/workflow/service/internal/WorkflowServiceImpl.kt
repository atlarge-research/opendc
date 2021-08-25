/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.workflow.service.internal

import io.opentelemetry.api.metrics.Meter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.opendc.compute.api.*
import org.opendc.workflow.api.Job
import org.opendc.workflow.api.WORKFLOW_TASK_CORES
import org.opendc.workflow.service.*
import org.opendc.workflow.service.scheduler.WorkflowSchedulerMode
import org.opendc.workflow.service.scheduler.job.JobAdmissionPolicy
import org.opendc.workflow.service.scheduler.job.JobOrderPolicy
import org.opendc.workflow.service.scheduler.task.TaskEligibilityPolicy
import org.opendc.workflow.service.scheduler.task.TaskOrderPolicy
import java.time.Clock
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

/**
 * A [WorkflowService] that distributes work through a multi-stage process based on the Reference Architecture for
 * Datacenter Scheduling.
 */
public class WorkflowServiceImpl(
    context: CoroutineContext,
    internal val clock: Clock,
    private val meter: Meter,
    private val computeClient: ComputeClient,
    mode: WorkflowSchedulerMode,
    jobAdmissionPolicy: JobAdmissionPolicy,
    jobOrderPolicy: JobOrderPolicy,
    taskEligibilityPolicy: TaskEligibilityPolicy,
    taskOrderPolicy: TaskOrderPolicy
) : WorkflowService, ServerWatcher {
    /**
     * The [CoroutineScope] of the service bounded by the lifecycle of the service.
     */
    internal val scope = CoroutineScope(context + Job())

    /**
     * The logger instance to use.
     */
    private val logger = KotlinLogging.logger {}

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
     * The continuation of the jobs.
     */
    private val conts = mutableMapOf<Job, Continuation<Unit>>()

    /**
     * The root listener of this scheduler.
     */
    private val rootListener = object : WorkflowSchedulerListener {
        /**
         * The listeners to delegate to.
         */
        val listeners = mutableSetOf<WorkflowSchedulerListener>()

        override fun cycleStarted(scheduler: WorkflowServiceImpl) {
            listeners.forEach { it.cycleStarted(scheduler) }
        }

        override fun cycleFinished(scheduler: WorkflowServiceImpl) {
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

    /**
     * The number of jobs that have been submitted to the service.
     */
    private val submittedJobs = meter.counterBuilder("jobs.submitted")
        .setDescription("Number of submitted jobs")
        .setUnit("1")
        .build()

    /**
     * The number of jobs that are running.
     */
    private val runningJobs = meter.upDownCounterBuilder("jobs.active")
        .setDescription("Number of jobs running")
        .setUnit("1")
        .build()

    /**
     * The number of jobs that have finished running.
     */
    private val finishedJobs = meter.counterBuilder("jobs.finished")
        .setDescription("Number of jobs that finished running")
        .setUnit("1")
        .build()

    /**
     * The number of tasks that have been submitted to the service.
     */
    private val submittedTasks = meter.counterBuilder("tasks.submitted")
        .setDescription("Number of submitted tasks")
        .setUnit("1")
        .build()

    /**
     * The number of jobs that are running.
     */
    private val runningTasks = meter.upDownCounterBuilder("tasks.active")
        .setDescription("Number of tasks running")
        .setUnit("1")
        .build()

    /**
     * The number of jobs that have finished running.
     */
    private val finishedTasks = meter.counterBuilder("tasks.finished")
        .setDescription("Number of tasks that finished running")
        .setUnit("1")
        .build()

    private val mode: WorkflowSchedulerMode.Logic
    private val jobAdmissionPolicy: JobAdmissionPolicy.Logic
    private val taskEligibilityPolicy: TaskEligibilityPolicy.Logic
    private lateinit var image: Image

    init {
        this.mode = mode(this)
        this.jobAdmissionPolicy = jobAdmissionPolicy(this)
        this.jobQueue = PriorityQueue(100, jobOrderPolicy(this).thenBy { it.job.uid })
        this.taskEligibilityPolicy = taskEligibilityPolicy(this)
        this.taskQueue = PriorityQueue(1000, taskOrderPolicy(this).thenBy { it.task.uid })
        scope.launch {
            image = computeClient.newImage("workflow-runner")
        }
    }

    override suspend fun run(job: Job) {
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

            submittedTasks.add(1)
        }

        return suspendCancellableCoroutine { cont ->
            instances.values.toCollection(jobInstance.tasks)
            incomingJobs += jobInstance
            rootListener.jobSubmitted(jobInstance)
            conts[job] = cont

            submittedJobs.add(1)

            requestCycle()
        }
    }

    override suspend fun submit(job: Job) {
        scope.launch { run(job) }
    }

    override fun close() {
        scope.cancel()
    }

    /**
     * Indicate to the scheduler that a scheduling cycle is needed.
     */
    private fun requestCycle() = mode.requestCycle()

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

            runningJobs.add(1)
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

            val cores = instance.task.metadata[WORKFLOW_TASK_CORES] as? Int ?: 1
            val image = image
            scope.launch {
                val flavor = computeClient.newFlavor(
                    instance.task.name,
                    cores,
                    1000
                ) // TODO How to determine memory usage for workflow task
                val server = computeClient.newServer(
                    instance.task.name,
                    image,
                    flavor,
                    start = false,
                    meta = instance.task.metadata
                )

                instance.state = TaskStatus.ACTIVE
                instance.server = server
                taskByServer[server] = instance

                server.watch(this@WorkflowServiceImpl)
                server.start()
            }

            activeTasks += instance
            taskQueue.poll()
            rootListener.taskAssigned(instance)
        }
    }

    public override fun onStateChanged(server: Server, newState: ServerState) {
        when (newState) {
            ServerState.PROVISIONING -> {}
            ServerState.RUNNING -> {
                val task = taskByServer.getValue(server)
                task.startedAt = clock.millis()
                runningTasks.add(1)
                rootListener.taskStarted(task)
            }
            ServerState.TERMINATED, ServerState.ERROR -> {
                val task = taskByServer.remove(server) ?: throw IllegalStateException()

                scope.launch {
                    server.delete()
                    server.flavor.delete()
                }

                val job = task.job
                task.state = TaskStatus.FINISHED
                task.finishedAt = clock.millis()
                job.tasks.remove(task)
                activeTasks -= task

                runningTasks.add(-1)
                finishedTasks.add(1)
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
            ServerState.DELETED -> {
            }
            else -> throw IllegalStateException()
        }
    }

    private fun finishJob(job: JobState) {
        activeJobs -= job
        runningJobs.add(-1)
        finishedJobs.add(1)
        rootListener.jobFinished(job)

        conts.remove(job.job)?.resume(Unit)
    }

    public fun addListener(listener: WorkflowSchedulerListener) {
        rootListener.listeners += listener
    }

    public fun removeListener(listener: WorkflowSchedulerListener) {
        rootListener.listeners -= listener
    }
}
