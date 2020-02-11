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

package com.atlarge.opendc.core.workload.application

import com.atlarge.odcsim.SendRef
import com.atlarge.opendc.core.resources.compute.Machine
import com.atlarge.opendc.core.resources.compute.ProcessingElement

/**
 * The process id (pid) is a reference to the application instance (process) that accepts messages of
 * type [ProcessMessage].
 */
typealias Pid = SendRef<ProcessMessage>

/**
 * A message protocol for actors to communicate with task instances (called processes).
 */
sealed class ProcessMessage {
    /**
     * Indicate that the task should be installed to the specified machine.
     *
     * @property machine The machine to install the task.
     * @property ref The reference to the machine instance.
     */
    data class Setup(val machine: Machine, val ref: SendRef<ProcessEvent>) : ProcessMessage()

    /**
     * Indicate an allocation of compute resources on a machine for a certain duration.
     * The task may assume that the reservation occurs after installation on the same machine.
     *
     * @property resources The cpu cores (and the utilization percentages) allocated for the task.
     * @property until The point in time till which the reservation is valid.
     */
    data class Allocation(val resources: Map<ProcessingElement, Double>, val until: Long) : ProcessMessage()
}

/**
 * The message protocol used by application instances respond to [ProcessMessage]s.
 */
sealed class ProcessEvent {
    /**
     * Indicate that the process is ready to start processing.
     *
     * @property pid A reference to the application instance.
     */
    data class Ready(val pid: Pid) : ProcessEvent()

    /**
     * Indicate the estimated resource utilization of the task until a specified point in time.
     *
     * @property pid A reference to the application instance of the represented utilization.
     * @property utilization The utilization of the cpu cores as a percentage.
     * @property until The point in time until which the utilization is valid.
     */
    data class Consume(
        val pid: Pid,
        val utilization: Map<ProcessingElement, Double>,
        val until: Long
    ) : ProcessEvent()

    /**
     * Indicate that a process has been terminated.
     *
     * @property pid A reference to the application instance.
     * @property status The exit code of the task, where zero means successful.
     */
    data class Exit(val pid: Pid, val status: Int) : ProcessEvent()
}
