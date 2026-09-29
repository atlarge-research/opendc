/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.simulator.compute.service

import mu.KotlinLogging
import org.opendc.simulator.compute.carbon.CarbonNode
import org.opendc.simulator.compute.carbon.CarbonReceiver
import org.opendc.simulator.compute.infrastructure.SimCluster
import org.opendc.simulator.compute.infrastructure.SimDataCenter
import org.opendc.simulator.compute.infrastructure.SimHost
import org.opendc.simulator.compute.models.HostListener
import org.opendc.simulator.compute.models.HostState
import org.opendc.simulator.compute.power.PowerSourceNode
import org.opendc.simulator.compute.power.batteries.BatteryNode
import org.opendc.simulator.compute.scheduler.ComputeScheduler
import org.opendc.simulator.compute.scheduler.SchedulingRequest
import org.opendc.simulator.compute.scheduler.SchedulingResultType
import org.opendc.simulator.compute.task.SimTask
import org.opendc.simulator.compute.task.TaskState
import org.opendc.simulator.compute.telemetry.TaskListener
import org.opendc.simulator.compute.workload.Workload
import org.opendc.simulator.core.Dispatcher
import org.opendc.simulator.core.Pacer
import java.time.Duration
import java.time.InstantSource
import java.util.ArrayDeque
import java.util.Collections

/**
 * The [ComputeService] hosts the API implementation of the OpenDC Compute Engine.
 *
 * @param dispatcher The [Dispatcher] that drives the scheduling cycles and provides the (simulation) clock.
 * @param scheduler The [ComputeScheduler] responsible for placing the tasks onto hosts.
 * @param quantum The scheduling quantum of the service: scheduling cycles run at most once per quantum.
 * @param maxNumFailures The number of times a task may fail before it is terminated.
 */
public class ComputeService(
    dispatcher: Dispatcher,
    private val scheduler: ComputeScheduler,
    quantum: Duration = Duration.ofMillis(1),
    private val maxNumFailures: Int = 10,
) : AutoCloseable, CarbonReceiver {
    // ==================================================================================
    // Fields
    // Internal state: infrastructure registries, task bookkeeping, and scheduling counters.
    // ==================================================================================

    /**
     * The [InstantSource] representing the clock tracking the (simulation) time.
     */
    public val clock: InstantSource = dispatcher.timeSource

    /**
     * The [Pacer] used to pace the scheduling requests.
     */
    private val pacer = Pacer(dispatcher, quantum.toMillis()) { doSchedule() }

    /**
     * A flag to indicate that the service is closed.
     */
    private var isClosed = false

    /**
     * The hosts registered with this service.
     */
    private val _hosts = HashSet<SimHost>()

    /**
     * The available hypervisors.
     */
    private val availableHosts = HashSet<SimHost>()

    /**
     * The available clusters
     */
    private val _clusters = HashSet<SimCluster>()

    /**
     * The available dataCenters
     */
    private val _dataCenters = HashSet<SimDataCenter>()

    /**
     * The available powerSources
     */
    private val _powerSources = HashSet<PowerSourceNode>()

    /**
     * The available batteries
     */
    private val _batteries = HashSet<BatteryNode>()

    /**
     * The tasks that should be launched by the service.
     */
    private val taskQueue = ArrayDeque<SchedulingRequest>()

    private val blockedTasks = HashMap<Int, SchedulingRequest>()

    /**
     * The active tasks in the system.
     */
    private val activeTasks = HashSet<SimTask>()

    /**
     * The registered tasks for this compute service.
     */
    private val taskById = HashMap<Int, SimTask>()

    private val taskListeners = ArrayList<TaskListener>()

    private var maxCores = 0
    private var maxMemory = 0L

    // ==================================================================================
    // Lifecycle
    // All functions related to changing the ComputeService state during its lifecycle
    // ==================================================================================

    override fun close() {
        if (isClosed) {
            return
        }

        isClosed = true
        pacer.cancel()
    }

    // ==================================================================================
    // Task API
    // Public surface for submitting, looking up, and rescheduling tasks.
    // ==================================================================================

    /**
     * Submit a [SimTask] to be scheduled by this service.
     */
    public fun submitTask(task: SimTask): SimTask {
        check(!isClosed) { "Service is closed" }

        task.service = this

        taskById[task.id] = task

        tasksTotal++

        task.start()

        return task
    }

    /**
     * Find the [SimTask] with the specified id, or `null` if no such task exists.
     */
    public fun findTask(id: Int): SimTask? = taskById[id]

    /**
     * Reschedule the given [SimTask] with a new [Workload].
     */
    public fun rescheduleTask(
        task: SimTask,
        workload: Workload,
    ) {
        task.reschedule(workload)
    }

    /**
     * The [SimTask]s hosted by this service.
     */
    public val tasks: Map<Int, SimTask> = Collections.unmodifiableMap(taskById)

    /**
     * Notify the listeners that the given [SimTask] is done, and delete it.
     */
    public fun deleteTask(task: SimTask) {
        for (listener in taskListeners) {
            listener.onTaskDeletion(task)
        }

        task.delete()
    }

    public fun addTaskListener(listener: TaskListener) {
        taskListeners.add(listener)
    }

    // ==================================================================================
    // Host management
    // Registering hosts with the scheduling pool and reacting to their availability.
    // ==================================================================================

    /**
     * Add a [SimHost] to the scheduling pool of the compute service.
     */
    public fun addHost(host: SimHost) {
        // Check if host is already known
        if (host in _hosts) {
            return
        }

        val model = host.model

        maxCores = maxOf(maxCores, model.coreCount)
        maxMemory = maxOf(maxMemory, model.memoryCapacity)
        _hosts.add(host)

        if (host.state == HostState.UP) {
            availableHosts.add(host)
        }

        scheduler.addHost(host)
        host.addListener(hostListener)
    }

    /**
     * Remove a [SimHost] from the scheduling pool of the compute service.
     */
    public fun removeHost(host: SimHost) {
        if (_hosts.remove(host)) {
            availableHosts.remove(host)
            scheduler.removeHost(host)
            host.removeListener(hostListener)
        }
    }

    public fun updateHost(host: SimHost) {
        if (host !in _hosts) {
            return
        }

        scheduler.updateHost(host)
    }

    public fun failHost(host: SimHost) {
        scheduler.failHost(host)
    }

    public fun restartHost(host: SimHost) {
        scheduler.restartHost(host)
    }

    /**
     * The [SimHost]s that are registered with this service.
     */
    public val hosts: Set<SimHost> = Collections.unmodifiableSet(_hosts)

    // ==================================================================================
    // Power & energy infrastructure
    // Registering the clusters, data centers, power sources, and batteries backing the service.
    // ==================================================================================

    public fun addCluster(cluster: SimCluster) {
        _clusters.add(cluster)
    }

    public fun removeCluster(cluster: SimCluster) {
        _clusters.remove(cluster)
    }

    public fun addDataCenter(dataCenter: SimDataCenter) {
        _dataCenters.add(dataCenter)
    }

    public fun removeDataCenter(dataCenter: SimDataCenter) {
        _dataCenters.remove(dataCenter)
    }

    public fun addPowerSource(powerSource: PowerSourceNode) {
        _powerSources.add(powerSource)
    }

    public fun addBattery(battery: BatteryNode) {
        _batteries.add(battery)
    }

    public val clusters: Set<SimCluster> = Collections.unmodifiableSet(_clusters)

    public val dataCenters: Set<SimDataCenter> = Collections.unmodifiableSet(_dataCenters)

    public val powerSources: Set<PowerSourceNode> = Collections.unmodifiableSet(_powerSources)

    public val batteries: Set<BatteryNode> = Collections.unmodifiableSet(_batteries)

    // ==================================================================================
    // Carbon receiver
    // Implementation of CarbonReceiver; carbon intensity itself is modeled per power source.
    // ==================================================================================

    override fun updateCarbonIntensity(newCarbonIntensity: Double) {
        requestSchedulingCycle()
    }

    // ComputeService does not hold a carbon node itself; carbon intensity is modeled per power source.
    override fun setCarbonNode(carbonNode: CarbonNode?) {}

    override fun removeCarbonNode(carbonNode: CarbonNode?) {}

    // ==================================================================================
    // Statistics
    // Read-only counters exposing scheduling and task outcomes, for monitoring.
    // ==================================================================================

    public val hostsAvailable: Int get() = availableHosts.size

    public val hostsUnavailable: Int get() = _hosts.size - availableHosts.size

    public var attemptsSuccess: Long = 0L
        private set

    public var attemptsFailure: Long = 0L
        private set

    // Number of tasks seen by the service
    public var tasksTotal: Int = 0
        private set

    public val tasksPending: Int get() = taskQueue.size

    public val tasksActive: Int get() = activeTasks.size

    // Number of tasks completed successfully
    public var tasksCompleted: Int = 0
        private set

    // Number of tasks that were terminated due to too much failures
    public var tasksTerminated: Int = 0
        private set

    // ==================================================================================
    // Scheduling internals
    // Module-internal machinery that queues, selects, and deploys tasks onto hosts.
    // ==================================================================================

    /**
     * A [HostListener] used to track the active tasks.
     */
    private val hostListener =
        object : HostListener {
            override fun onStateChanged(
                host: SimHost,
                newState: HostState,
            ) {
                LOGGER.debug { "Host $host state changed: $newState" }

                if (host in _hosts) {
                    if (newState == HostState.UP) {
                        availableHosts.add(host)
                        restartHost(host)
                    } else {
                        availableHosts.remove(host)
                        failHost(host)
                    }
                }

                // Re-schedule on the new machine
                requestSchedulingCycle()
            }

            override fun onStateChanged(
                host: SimHost,
                task: SimTask,
                newState: TaskState,
            ) {
                // Identity comparison on purpose: SimHost.equals compares by id.
                if (task.host !== host) {
                    // This can happen when a task is rescheduled and started on another machine, while being deleted from
                    // the old machine.
                    return
                }

                if (newState == TaskState.COMPLETED ||
                    newState == TaskState.PAUSED ||
                    newState == TaskState.TERMINATED ||
                    newState == TaskState.FAILED
                ) {
                    LOGGER.info { "task ${task.id} finished" }

                    activeTasks.remove(task)

                    val isKnownHost = host in _hosts
                    if (!isKnownHost) {
                        LOGGER.error { "Unknown host $host" }
                    }

                    // Deleting the task also releases the capacity it reserved on the host
                    host.delete(task)

                    updateHost(host)

                    if (newState == TaskState.COMPLETED) {
                        tasksCompleted++
                        addCompletedTask(task)
                    }
                    if (newState == TaskState.TERMINATED) {
                        tasksTerminated++
                        addTerminatedTask(task)
                    }

                    if (task.state == TaskState.COMPLETED || task.state == TaskState.TERMINATED) {
                        deleteTask(task)
                    }

                    // Try to reschedule if needed
                    requestSchedulingCycle()
                }
            }
        }

    /**
     * Enqueue the specified [task] to be scheduled onto a host, at the front of the queue if [atFront] is set.
     * Returns `null` if the task was terminated, or is blocked until its parents complete.
     */
    internal fun schedule(
        task: SimTask,
        atFront: Boolean = false,
    ): SchedulingRequest? {
        LOGGER.debug { "Enqueueing task ${task.id} to be assigned to host" }

        if (task.numFailures >= maxNumFailures) {
            LOGGER.warn { "task $task has been terminated because it failed ${task.numFailures} times" }

            tasksTerminated++
            task.terminate()

            addTerminatedTask(task)

            deleteTask(task)
            return null
        }

        val now = clock.millis()
        val request = SchedulingRequest(task, now)

        // If the task has parents, put in blocked tasks
        if (task.hasParents()) {
            blockedTasks[task.id] = request
            return null
        }

        // Add the request at the front or the back of the queue
        if (atFront) {
            taskQueue.addFirst(request)
        } else {
            taskQueue.add(request)
        }

        requestSchedulingCycle()
        return request
    }

    private fun addCompletedTask(completedTask: SimTask) {
        val children = completedTask.children ?: return
        val parentId = completedTask.id

        for (childTaskId in children) {
            val childRequest = blockedTasks[childTaskId] ?: continue
            val childTask = childRequest.task
            childTask.removeFromParents(parentId)

            // If the child task has no more parents, it can be scheduled
            if (!childTask.hasParents()) {
                taskQueue.add(childRequest)
                blockedTasks.remove(childTaskId)
            }
        }
    }

    private fun addTerminatedTask(task: SimTask) {
        val children = task.children ?: return

        for (childTaskId in children) {
            val request = blockedTasks[childTaskId] ?: continue
            val childTask = request.task

            tasksTerminated++
            childTask.terminate()

            addTerminatedTask(childTask)

            deleteTask(childTask)

            blockedTasks.remove(childTask.id)
        }
    }

    /**
     * Unregister a [SimTask] that has deleted itself.
     */
    internal fun unregisterTask(task: SimTask) {
        taskById.remove(task.id)
    }

    /**
     * Indicate that a new scheduling cycle is needed due to a change to the service's state.
     */
    private fun requestSchedulingCycle() {
        // Bail out in case the queue is empty.
        if (taskQueue.isEmpty()) {
            return
        }

        pacer.enqueue()
    }

    /**
     * Run a single scheduling iteration.
     */
    private fun doSchedule() {
        // The scheduler may remove requests from the queue, so every selection gets a fresh iterator.
        while (taskQueue.isNotEmpty()) {
            val result = scheduler.select(taskQueue.iterator())
            if (result.resultType == SchedulingResultType.EMPTY) {
                break
            }

            val req = result.req!!
            val task = req.task

            if (result.resultType == SchedulingResultType.FAILURE) {
                LOGGER.trace { "Task $task selected for scheduling but no capacity available for it at the moment" }

                // Check if the task will every fit on any of the hosts.
                // If not, terminate the host
                if (task.memorySize > maxMemory || task.cpuCoreCount > maxCores) {
                    terminateOversizedTask(task, req)
                    continue
                } else {
                    break
                }
            }

            deployTask(task, result.host!!, req)
        }
    }

    /**
     * Terminate a task that exceeds the capacity of every host, and remove it from the queue.
     */
    private fun terminateOversizedTask(
        task: SimTask,
        req: SchedulingRequest,
    ) {
        // Remove the incoming image
        taskQueue.remove(req)
        tasksTerminated++

        LOGGER.warn { "Failed to spawn $task: does not fit" }

        task.terminate()

        addTerminatedTask(task)

        deleteTask(task)
    }

    /**
     * Deploy the given task onto the host selected for it.
     */
    private fun deployTask(
        task: SimTask,
        host: SimHost,
        req: SchedulingRequest,
    ) {
        LOGGER.info { "Assigned task $task to host $host" }

        try {
            task.onScheduled(host, req.submitTime)

            host.spawn(task)

            attemptsSuccess++

            activeTasks.add(task)

            updateHost(host)
        } catch (cause: Exception) {
            LOGGER.error(cause) { "Failed to deploy VM" }
            attemptsFailure++
        }
    }

    private companion object {
        @JvmStatic
        private val LOGGER = KotlinLogging.logger {}
    }
}
