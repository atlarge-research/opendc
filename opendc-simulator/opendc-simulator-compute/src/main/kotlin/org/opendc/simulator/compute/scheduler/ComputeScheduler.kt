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

package org.opendc.simulator.compute.scheduler

import org.opendc.common.units.Timestamp
import org.opendc.simulator.compute.infrastructure.SimHost
import org.opendc.simulator.compute.scheduler.priority.TaskPriority
import org.opendc.simulator.compute.task.SimTask

/**
 * Decides which [SimHost] each [SimTask] is placed on, for the [org.opendc.simulator.compute.service.ComputeService].
 *
 * The service tells the scheduler about every host and every change to one, so the scheduler can keep its own
 * bookkeeping, and asks it to select a host for the tasks in its queue.
 */
public interface ComputeScheduler {
    /**
     * Add [host] to the hosts tasks are placed on.
     */
    public fun addHost(host: SimHost)

    /**
     * Remove [host] from the hosts tasks are placed on.
     */
    public fun removeHost(host: SimHost)

    /**
     * [host] failed: place no tasks on it until it is restarted.
     */
    public fun failHost(host: SimHost)

    /**
     * [host] is available again after a failure.
     */
    public fun restartHost(host: SimHost)

    /**
     * The resources in use on [host] changed, because a task was placed on it or left it.
     */
    public fun updateHost(host: SimHost)

    /**
     * Select a host for one of the requests in [iter], which walks the queue of the service in order. The scheduler
     * removes the requests it places, and the cancelled requests it comes across, through [iter].
     *
     * @return [SchedulingResultType.SUCCESS] with the request and the host to place it on;
     *   [SchedulingResultType.FAILURE] with the request that cannot be placed now; or [SchedulingResultType.EMPTY] when
     *   no request should be placed now.
     */
    public fun select(iter: MutableIterator<SchedulingRequest>): SchedulingResult

    /**
     * The priority of the tasks waiting to be placed, or `null` (the default) to place them first in, first out. The
     * service keeps waiting tasks from the highest to the lowest priority, and tasks with equal priority in their order
     * of arrival. It reads the priority of a task once, in the first scheduling cycle after the task is queued, so
     * tasks submitted at the same moment are all known by then.
     */
    public val taskPriority: TaskPriority? get() = null
}

/**
 * A request to schedule a [SimTask] onto one of the [SimHost]s.
 */
public data class SchedulingRequest internal constructor(
    public val task: SimTask,
    public val submitTime: Timestamp,
) {
    public var isCancelled: Boolean = false

    /**
     * The position of the request among requests with the same priority: lower goes first. Set by the service when
     * the request is queued.
     */
    internal var sequence: Long = 0

    /**
     * The [ComputeScheduler.taskPriority] of the task, read once when the request enters the ordered queue.
     */
    internal var priority: Double = 0.0
}

public enum class SchedulingResultType {
    SUCCESS,
    FAILURE,
    EMPTY,
}

public data class SchedulingResult(
    val resultType: SchedulingResultType,
    val host: SimHost? = null,
    val req: SchedulingRequest? = null,
)
