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

package org.opendc.compute.simulator.service

import mu.KotlinLogging
import org.opendc.compute.api.TaskState
import org.opendc.compute.simulator.TaskWatcher
import org.opendc.compute.simulator.infrastructure.SimHost
import org.opendc.compute.simulator.scheduler.SchedulingRequest
import org.opendc.simulator.compute.workload.ChainWorkload
import org.opendc.simulator.compute.workload.VirtualMachine
import org.opendc.simulator.compute.workload.Workload

/**
 * A task managed by the [ComputeService], which runs on a [SimHost] once it is scheduled.
 *
 * Millions of instances can be alive during a simulation, so the fields are kept as small as possible: primitive
 * arrays instead of lists, and narrow types behind wider public properties. Avoid nullable primitives (`Long?`,
 * `Int?`) and `List<Int>` here, as they box every value.
 */
public class SimTask(
    public val id: Int,
    submissionTime: Long,
    public val duration: Long,
    cpuCoreCount: Int,
    public val cpuCapacity: Double,
    // memorySize and gpuMemorySize (in MB) are Ints instead of Longs to save space, as there can be millions of tasks.
    // An Int can still hold up to ~2 petabytes, which should be enough for any task.
    public val memorySize: Int,
    gpuCoreCount: Int,
    public val gpuCapacity: Double,
    public val gpuMemorySize: Int,
    workload: Workload?,
    public val deferrable: Boolean,
    public var deadline: Long,
    parents: IntArray?,
    children: IntArray?,
) {
    // ==================================================================================
    // Fields
    // Specification, dependencies, state, placement, and statistics of the task.
    // ==================================================================================

    // The core counts are stored as Shorts but exposed as Ints. The public properties have no backing field, so they
    // do not add to the size of a task.
    private val _cpuCoreCount: Short = cpuCoreCount.toShort()
    public val cpuCoreCount: Int get() = _cpuCoreCount.toInt()

    private val _gpuCoreCount: Short = gpuCoreCount.toShort()
    public val gpuCoreCount: Int get() = _gpuCoreCount.toInt()

    /**
     * Ids of the parent tasks that must complete before this task may start.
     * `null` means no (remaining) parents. Stored as a primitive array (instead of a
     * boxed `List<Int>`) to avoid per-element boxing and collection overhead, since
     * a large fraction of tasks in a workload have no dependencies at all.
     */
    public var parents: IntArray? = if (parents == null || parents.isEmpty()) null else parents
        private set

    /**
     * Ids of the child tasks that depend on this task. `null` means no children.
     * Never mutated after construction, so unlike [parents] this can be a `val`.
     */
    public val children: IntArray? = if (children == null || children.isEmpty()) null else children

    /**
     * The service this task is submitted to. Set by [ComputeService.submitTask].
     */
    public var service: ComputeService? = null
        // Keep the plain JVM name, so ComputeService (Java) can call setService() without name mangling.
        @JvmName("setService")
        internal set

    /**
     * The workload of the task. Replaced by a snapshot when the task is rescheduled, and cleared when it is deleted.
     */
    public var workload: Workload? = workload
        private set

    /**
     * The scheduling request of this task, while it waits in the queue of the [service].
     */
    private var request: SchedulingRequest? = null

    /**
     * A task only ever has a single watcher in practice, so this is stored directly instead of
     * in a `List`, avoiding an extra `ArrayList` + backing array allocation per task.
     */
    private var watcher: TaskWatcher? = null

    /**
     * The state of the task. Every change is reported to the [watcher], and failures and pauses are counted.
     */
    public var state: TaskState = TaskState.CREATED
        private set(newState) {
            if (field == newState) {
                return
            }

            watcher?.onStateChanged(this, newState)
            if (newState == TaskState.FAILED) {
                _numFailures++
            } else if (newState == TaskState.PAUSED) {
                _numPauses++
            }

            if (newState == TaskState.COMPLETED || newState == TaskState.FAILED || newState == TaskState.TERMINATED) {
                finishedAt = service!!.clock.millis()
            }

            field = newState
        }

    /**
     * The host this task is placed on, or `null` when the task is not placed on a host.
     */
    public var host: SimHost? = null
        internal set(newHost) {
            field = newHost
            if (newHost != null) {
                hostName = newHost.name
            }
        }

    // TODO: This is currently needed because host gets deleted before the final exporting. When exporting has been
    // updated, remove hostName.
    public var hostName: String? = null
        private set

    /**
     * The virtual machine running this task on its [host], or `null` when the task is not running on a host.
     */
    public var virtualMachine: VirtualMachine? = null
        internal set

    // The submission time is shifted by the workload loaders, so unlike the other statistics it can be set from outside.
    public var submittedAt: Long = submissionTime
    public var scheduledAt: Long = 0
        private set
    public var finishedAt: Long = 0
        private set
    public var schedulingDelay: Long = 0
        private set

    private var _numFailures: Short = 0
    public val numFailures: Int get() = _numFailures.toInt()

    private var _numPauses: Short = 0
    public val numPauses: Int get() = _numPauses.toInt()

    // ==================================================================================
    // Lifecycle
    // Scheduling, rescheduling, terminating, and deleting the task. Driven by the ComputeService.
    // ==================================================================================

    /**
     * Put the task in the scheduling queue of the [service]. Called when the task is submitted, and when it is
     * rescheduled after a pause or failure.
     */
    @JvmName("start")
    internal fun start() {
        when (state) {
            TaskState.PROVISIONING -> {
                LOGGER.debug { "Tried to start task $id, but it is already waiting to be scheduled" }
            }
            TaskState.RUNNING -> {
                LOGGER.debug { "Tried to start task $id, but it is already running" }
            }
            TaskState.COMPLETED, TaskState.TERMINATED -> {
                LOGGER.warn { "Tried to start task $id, but it has already finished" }
                throw IllegalStateException("Task $id has already finished")
            }
            TaskState.CREATED -> {
                LOGGER.info { "Scheduling task $id" }
                state = TaskState.PROVISIONING
                assert(request == null) { "Scheduling request already active" }
                request = service!!.schedule(this)
            }
            TaskState.PAUSED, TaskState.FAILED -> {
                LOGGER.info { "Rescheduling task $id after it was ${state.name.lowercase()}" }
                state = TaskState.PROVISIONING
                request = service!!.schedule(this)
            }
            else -> {}
        }
    }

    /**
     * Reschedule the task, continuing from [workload]. Called by [ComputeService.rescheduleTask].
     */
    @JvmName("reschedule")
    internal fun reschedule(workload: Workload) {
        host = null
        this.workload = workload
        start()
    }

    /**
     * Place the task on [host], right before it is spawned there. [queuedAt] is the time the task entered the
     * scheduling queue, which is added to the [schedulingDelay].
     */
    @JvmName("onScheduled")
    internal fun onScheduled(
        host: SimHost,
        queuedAt: Long,
    ) {
        val now = service!!.clock.millis()

        this.host = host
        scheduledAt = now
        schedulingDelay += now - queuedAt
    }

    /**
     * Terminate the task, because it failed too often or does not fit on any host, or because one of its parents
     * was terminated.
     */
    @JvmName("terminate")
    internal fun terminate() {
        state = TaskState.TERMINATED
    }

    /**
     * Remove the task from its host and from the [service]. Called by [ComputeService.deleteTask].
     */
    @JvmName("delete")
    internal fun delete() {
        cancelProvisioningRequest()
        host?.delete(this)
        service!!.unregisterTask(this)

        workload = null

        state = TaskState.DELETED

        watcher = null
    }

    // ==================================================================================
    // Runs
    // A run is one execution of this task on a host: it starts in SimHost.spawn and ends when the workload stops, or
    // when the task is removed from the host. A task can have several runs, for example after a host failure.
    // ==================================================================================

    /**
     * Start running this task on the machine of [host]. Called by [SimHost.spawn].
     */
    internal fun startRun(host: SimHost) {
        assert(virtualMachine == null) { "Concurrent job is already running" }

        state = TaskState.RUNNING
        host.onTaskStateChanged(this)

        val workload = checkNotNull(workload) { "Task $id has no workload" }
        val chainWorkload =
            workload as? ChainWorkload
                ?: ChainWorkload(
                    ArrayList(listOf(workload)),
                    workload.checkpointInterval(),
                    workload.checkpointDuration(),
                    workload.checkpointIntervalScaling(),
                )

        // The machine calls the callback once, when the workload stops. This can happen before startWorkload returns,
        // in which case vm is still null and the run must not be recorded afterwards.
        var vm: VirtualMachine? = null
        var stopped = false
        vm =
            host.simMachine.startWorkload(chainWorkload) { cause ->
                // Ignore the callback of an earlier run, which could arrive after the task was removed from the host.
                if (vm != null && vm !== virtualMachine) {
                    return@startWorkload
                }
                stopped = true
                onRunStopped(host, if (cause != null) TaskState.FAILED else TaskState.COMPLETED)
            }

        if (!stopped) {
            virtualMachine = vm
        }
    }

    /**
     * Stop the current run and put the task into [target] state, [TaskState.FAILED] or [TaskState.PAUSED].
     * Does nothing if the task is not running. Called by [SimHost] when the host fails or pauses its tasks.
     */
    internal fun stopRun(target: TaskState) {
        if (state != TaskState.RUNNING) {
            return
        }

        assert(virtualMachine != null) { "Invalid job state" }
        val virtualMachine = this.virtualMachine ?: return

        // Set the state before stopping the machine: the machine reports the stop as a completion, which
        // onRunStopped ignores because the task is no longer running.
        state = target
        if (target == TaskState.FAILED) {
            virtualMachine.stopWorkload(Exception("Task has failed"))
        } else {
            virtualMachine.stopWorkload()
        }

        this.virtualMachine = null
    }

    /**
     * Called when the workload of the current run on [host] stopped, with [target] the state the machine reports.
     */
    private fun onRunStopped(
        host: SimHost,
        target: TaskState,
    ) {
        // If the task is no longer running, the run was stopped through stopRun and that state is kept.
        if (state == TaskState.RUNNING) {
            state = target
        }
        host.onTaskStateChanged(this)
    }

    // ==================================================================================
    // Dependencies
    // Tracking the parent tasks that must complete before this task can be scheduled.
    // ==================================================================================

    public fun hasParents(): Boolean = parents?.isNotEmpty() == true

    public fun hasChildren(): Boolean = children != null && children.isNotEmpty()

    /**
     * Remove [completedTask] from the parents of this task, as it no longer has to wait for it.
     */
    @JvmName("removeFromParents")
    internal fun removeFromParents(completedTask: Int) {
        val current = parents ?: return

        val idx = current.indexOf(completedTask)
        if (idx == -1) {
            return
        }

        if (current.size == 1) {
            parents = null
            return
        }

        val updated = IntArray(current.size - 1)
        System.arraycopy(current, 0, updated, 0, idx)
        System.arraycopy(current, idx + 1, updated, idx, current.size - idx - 1)
        parents = updated
    }

    // ==================================================================================
    // Watchers
    // Notifying an external TaskWatcher of state changes.
    // ==================================================================================

    public fun watch(watcher: TaskWatcher) {
        this.watcher = watcher
    }

    // ==================================================================================
    // Object overrides
    // ==================================================================================

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as SimTask
        return service == other.service && id == other.id
    }

    // Deliberately not Objects.hash(service, id): that allocates a varargs array and boxes the id
    // on every call, and tasks are used as HashMap keys on hot lookup paths. Ids are unique within
    // a service, so they alone satisfy the equals/hashCode contract.
    override fun hashCode(): Int = id

    override fun toString(): String = "Task[uid=$id,state=$state]"

    // ==================================================================================
    // Internals
    // ==================================================================================

    /**
     * Cancel the provisioning request if active.
     */
    private fun cancelProvisioningRequest() {
        val request = this.request ?: return
        this.request = null
        request.isCancelled = true
    }

    private companion object {
        @JvmStatic
        private val LOGGER = KotlinLogging.logger {}
    }
}
