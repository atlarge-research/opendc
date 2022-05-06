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
import io.opentelemetry.api.metrics.MeterProvider
import kotlinx.coroutines.*
import org.opendc.common.util.Pacer
import org.opendc.compute.api.*
import org.opendc.workflow.api.Job
import org.opendc.workflow.api.WORKFLOW_TASK_CORES
import org.opendc.workflow.service.*
import org.opendc.workflow.service.scheduler.job.JobAdmissionPolicy
import org.opendc.workflow.service.scheduler.job.JobOrderPolicy
import org.opendc.workflow.service.scheduler.task.TaskEligibilityPolicy
import org.opendc.workflow.service.scheduler.task.TaskOrderPolicy
import org.opendc.workflow.service.telemetry.SchedulerStats
import java.time.Clock
import java.time.Duration
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

/**
 * A [WorkflowService] that distributes work through a multi-stage process based on the Reference Architecture for
 * Datacenter Scheduling.
 */
public class WorkflowServiceImpl(
    context: CoroutineContext,
    private val clock: Clock,
    meterProvider: MeterProvider,
    private val computeClient: ComputeClient,
    private val schedulingQuantum: Duration,
    jobAdmissionPolicy: JobAdmissionPolicy,
    jobOrderPolicy: JobOrderPolicy,
    taskEligibilityPolicy: TaskEligibilityPolicy,
    taskOrderPolicy: TaskOrderPolicy
) : WorkflowService, ServerWatcher {
    /**
     * The [CoroutineScope] of the service bounded by the lifecycle of the service.
     */
    private val scope = CoroutineScope(context + Job())

    /**
     * The [Meter] to collect metrics of this service.
     */
    private val meter = meterProvider.get("org.opendc.workflow.service")

    /**
     * The incoming jobs ready to be processed by the scheduler.
     */
    private val incomingJobs: MutableSet<JobState> = linkedSetOf()

    /**
     * The incoming tasks ready to be processed by the scheduler.
     */
    private val incomingTasks: MutableSet<TaskState> = linkedSetOf()

    /**
     * The job queue.
     */
    private val jobQueue: Queue<JobState>

    /**
     * The task queue.
     */
    private val taskQueue: Queue<TaskState>

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
    private val taskByServer = mutableMapOf<Server, TaskState>()

    /**
     * The root listener of this scheduler.
     */
    private val rootListener = object : WorkflowSchedulerListener {
        /**
         * The listeners to delegate to.
         */
        val listeners = mutableListOf<WorkflowSchedulerListener>()

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
    private var _workflowsSubmitted: Int = 0

    /**
     * The number of jobs that are running.
     */
    private val runningJobs = meter.upDownCounterBuilder("jobs.active")
        .setDescription("Number of jobs running")
        .setUnit("1")
        .build()
    private var _workflowsRunning: Int = 0

    /**
     * The number of jobs that have finished running.
     */
    private val finishedJobs = meter.counterBuilder("jobs.finished")
        .setDescription("Number of jobs that finished running")
        .setUnit("1")
        .build()
    private var _workflowsFinished: Int = 0

    /**
     * The number of tasks that have been submitted to the service.
     */
    private val submittedTasks = meter.counterBuilder("tasks.submitted")
        .setDescription("Number of submitted tasks")
        .setUnit("1")
        .build()
    private var _tasksSubmitted: Int = 0

    /**
     * The number of jobs that are running.
     */
    private val runningTasks = meter.upDownCounterBuilder("tasks.active")
        .setDescription("Number of tasks running")
        .setUnit("1")
        .build()
    private var _tasksRunning: Int = 0

    /**
     * The number of jobs that have finished running.
     */
    private val finishedTasks = meter.counterBuilder("tasks.finished")
        .setDescription("Number of tasks that finished running")
        .setUnit("1")
        .build()
    private var _tasksFinished: Int = 0

    /**
     * The [Pacer] to use for scheduling the scheduler cycles.
     */
    private val pacer = Pacer(scope.coroutineContext, clock, schedulingQuantum.toMillis()) { doSchedule() }

    private val jobAdmissionPolicy: JobAdmissionPolicy.Logic
    private val taskEligibilityPolicy: TaskEligibilityPolicy.Logic
    private lateinit var image: Image

    init {
        this.jobAdmissionPolicy = jobAdmissionPolicy(this)
        this.jobQueue = PriorityQueue(100, jobOrderPolicy(this).thenBy { it.job.uid })
        this.taskEligibilityPolicy = taskEligibilityPolicy(this)
        this.taskQueue = PriorityQueue(1000, taskOrderPolicy(this).thenBy { it.task.uid })

        scope.launch { image = computeClient.newImage("workflow-runner") }
    }

    override suspend fun invoke(job: Job): Unit = suspendCancellableCoroutine { cont ->
        // J1 Incoming Jobs
        val jobInstance = JobState(job, clock.millis(), cont)
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
            _tasksSubmitted++
        }

        instances.values.toCollection(jobInstance.tasks)
        incomingJobs += jobInstance
        rootListener.jobSubmitted(jobInstance)
        submittedJobs.add(1)
        _workflowsSubmitted++

        pacer.enqueue()
    }

    override fun getSchedulerStats(): SchedulerStats {
        return SchedulerStats(_workflowsSubmitted, _workflowsRunning, _workflowsFinished, _tasksSubmitted, _tasksRunning, _tasksFinished)
    }

    override fun close() {
        scope.cancel()
    }

    /**
     * Register a [WorkflowSchedulerListener].
     */
    public fun addListener(listener: WorkflowSchedulerListener) {
        rootListener.listeners += listener
    }

    /**
     * Unregister a [WorkflowSchedulerListener].
     */
    public fun removeListener(listener: WorkflowSchedulerListener) {
        rootListener.listeners -= listener
    }

    /**
     * Perform a scheduling cycle immediately.
     */
    private fun doSchedule() {
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
            _workflowsRunning++
            rootListener.jobStarted(jobInstance)
        }

        // J4 Per job
        while (true) {
            val jobInstance = jobQueue.poll() ?: break

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

    override fun onStateChanged(server: Server, newState: ServerState) {
        when (newState) {
            ServerState.PROVISIONING -> {}
            ServerState.RUNNING -> {
                val task = taskByServer.getValue(server)
                task.startedAt = clock.millis()
                runningTasks.add(1)
                _tasksRunning++
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
                _tasksRunning--
                _tasksFinished++
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

                pacer.enqueue()
            }
            ServerState.DELETED -> {}
            else -> throw IllegalStateException()
        }
    }

    private fun finishJob(job: JobState) {
        activeJobs -= job
        runningJobs.add(-1)
        finishedJobs.add(1)
        _workflowsRunning--
        _workflowsFinished++
        rootListener.jobFinished(job)

        job.cont.resume(Unit)
    }
}
