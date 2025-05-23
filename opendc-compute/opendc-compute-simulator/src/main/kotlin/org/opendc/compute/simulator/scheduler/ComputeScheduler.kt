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

package org.opendc.compute.simulator.scheduler

import org.opendc.compute.simulator.service.HostView
import org.opendc.compute.simulator.service.ServiceTask

/**
 * A generic scheduler interface used by the [ComputeService] to select hosts to place [ServiceTask]s on.
 */
public interface ComputeScheduler {
    /**
     * Register the specified [host] to be used for scheduling.
     */
    public fun addHost(host: HostView)

    /**
     * Remove the specified [host] to be removed from the scheduling pool.
     */
    public fun removeHost(host: HostView)

    /**
     * Select a host for the specified [iter].
     * We implicity assume that the task has been scheduled onto the host.
     *
     * @param iter The server to select a host for.
     * @return The host to schedule the server on or `null` if no server is available.
     */
    public fun select(iter: MutableIterator<SchedulingRequest>): SchedulingResult

    /**
     * Inform the scheduler that a [task] has been removed from the [host].
     * Could be due to completion or failure.
     */
    public fun removeTask(
        task: ServiceTask,
        host: HostView?,
    )
}

/**
 * A request to schedule a [ServiceTask] onto one of the [SimHost]s.
 */
public data class SchedulingRequest internal constructor(
    public val task: ServiceTask,
    public val submitTime: Long,
) {
    public var isCancelled: Boolean = false
    public var timesSkipped: Int = 0
}

public enum class SchedulingResultType {
    SUCCESS,
    FAILURE,
    EMPTY,
}

public data class SchedulingResult(
    val resultType: SchedulingResultType,
    val host: HostView? = null,
    val req: SchedulingRequest? = null,
)
