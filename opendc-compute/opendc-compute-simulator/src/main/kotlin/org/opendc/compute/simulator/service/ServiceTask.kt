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
import org.opendc.simulator.compute.workload.Workload

/**
 * Implementation of [ServiceTask] provided by [ComputeService].
 *
 * Millions of instances can be alive during a simulation, so the fields are kept as small as possible: primitive
 * arrays instead of lists, and narrow types behind wider public properties. Avoid nullable primitives (`Long?`,
 * `Int?`) and `List<Int>` here, as they box every value.
 */
public class ServiceTask(
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
    public var workload: Workload?,
    public val deferrable: Boolean,
    public var deadline: Long,
    parents: IntArray?,
    children: IntArray?,
) {
    public var service: ComputeService? = null

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

    // The core counts are stored as Shorts but exposed as Ints. The public properties have no backing field, so they
    // do not add to the size of a task.
    private val _cpuCoreCount: Short = cpuCoreCount.toShort()
    public val cpuCoreCount: Int get() = _cpuCoreCount.toInt()

    private val _gpuCoreCount: Short = gpuCoreCount.toShort()
    public val gpuCoreCount: Int get() = _gpuCoreCount.toInt()

    /**
     * A task only ever has a single watcher in practice, so this is stored directly instead of
     * in a `List`, avoiding an extra `ArrayList` + backing array allocation per task.
     */
    private var watcher: TaskWatcher? = null

    public var state: TaskState = TaskState.CREATED
        // Keep the plain JVM name, so ComputeService (Java) can call setState() without name mangling.
        @JvmName("setState")
        internal set(newState) {
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

    public var submittedAt: Long = submissionTime
    public var scheduledAt: Long = 0
    public var finishedAt: Long = 0

    public var host: SimHost? = null
        set(newHost) {
            field = newHost
            if (newHost != null) {
                hostName = newHost.name
            }
        }

    // TODO: This is currently needed because host gets deleted before the final exporting. When exporting has been
    // updated, remove hostName.
    public var hostName: String? = null

    public var request: SchedulingRequest? = null

    private var _numFailures: Short = 0
    public var numFailures: Int
        get() = _numFailures.toInt()
        set(value) {
            _numFailures = value.toShort()
        }

    private var _numPauses: Short = 0
    public var numPauses: Int
        get() = _numPauses.toInt()
        set(value) {
            _numPauses = value.toShort()
        }

    public var schedulingDelay: Long = 0

    public fun copy(): ServiceTask =
        ServiceTask(
            id,
            submittedAt,
            duration,
            cpuCoreCount,
            cpuCapacity,
            memorySize,
            gpuCoreCount,
            gpuCapacity,
            0,
            workload,
            deferrable,
            deadline,
            parents?.copyOf(),
            children?.copyOf(),
        )

    public fun start() {
        when (state) {
            TaskState.PROVISIONING -> {
                LOGGER.debug { "User tried to start task but request is already pending: doing nothing" }
                LOGGER.debug { "User tried to start task but task is already running" }
            }
            TaskState.RUNNING -> {
                LOGGER.debug { "User tried to start task but task is already running" }
            }
            TaskState.COMPLETED, TaskState.TERMINATED -> {
                LOGGER.warn { "User tried to start deleted task" }
                throw IllegalStateException("Task is deleted")
            }
            TaskState.CREATED -> {
                LOGGER.info { "User requested to start task $id" }
                state = TaskState.PROVISIONING
                assert(request == null) { "Scheduling request already active" }
                request = service!!.schedule(this)
            }
            TaskState.PAUSED -> {
                LOGGER.info { "User requested to start task after pause $id" }
                state = TaskState.PROVISIONING
                request = service!!.schedule(this, false)
            }
            TaskState.FAILED -> {
                LOGGER.info { "User requested to start task after failure $id" }
                state = TaskState.PROVISIONING
                request = service!!.schedule(this, false)
            }
            else -> {}
        }
    }

    public fun watch(watcher: TaskWatcher) {
        this.watcher = watcher
    }

    public fun unwatch(watcher: TaskWatcher) {
        if (this.watcher === watcher) {
            this.watcher = null
        }
    }

    public fun delete() {
        cancelProvisioningRequest()
        host?.delete(this)
        service!!.unregisterTask(this)

        workload = null

        state = TaskState.DELETED

        watcher = null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as ServiceTask
        return service == other.service && id == other.id
    }

    // Deliberately not Objects.hash(service, id): that allocates a varargs array and boxes the id
    // on every call, and tasks are used as HashMap keys on hot lookup paths. Ids are unique within
    // a service, so they alone satisfy the equals/hashCode contract.
    override fun hashCode(): Int = id

    override fun toString(): String = "Task[uid=$id,state=$state]"

    /**
     * Cancel the provisioning request if active.
     */
    private fun cancelProvisioningRequest() {
        val request = this.request ?: return
        this.request = null
        request.isCancelled = true
    }

    public fun removeFromParents(completedTask: Int) {
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

    public fun hasChildren(): Boolean = children != null && children.isNotEmpty()

    public fun hasParents(): Boolean = parents?.isNotEmpty() == true

    private companion object {
        @JvmStatic
        private val LOGGER = KotlinLogging.logger {}
    }
}
