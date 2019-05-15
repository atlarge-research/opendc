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

package com.atlarge.opendc.model.resources.compute

import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.Behavior
import com.atlarge.opendc.model.Identity
import com.atlarge.opendc.model.resources.compute.supervision.MachineSupervisionEvent
import com.atlarge.opendc.model.workload.application.Application
import com.atlarge.opendc.model.workload.application.Pid

/**
 * A generic representation of a compute node (either physical or virtual) that is able to run [Application]s.
 */
interface Machine : Identity {
    /**
     * The details of the machine in key/value pairs.
     */
    val details: Map<String, Any>

    /**
     * Build the runtime [Behavior] of this compute resource, accepting messages of [MachineMessage].
     *
     * @param supervisor The supervisor of the machine.
     */
    operator fun invoke(supervisor: ActorRef<MachineSupervisionEvent>): Behavior<MachineMessage>
}

/**
 * A reference to a machine instance that accepts messages of type [MachineMessage].
 */
typealias MachineRef = ActorRef<MachineMessage>

/**
 * A message protocol for communicating with machine instances.
 */
sealed class MachineMessage {
    /**
     * Launch the specified [Application] on the machine instance.
     *
     * @property application The application to submit.
     * @property key The key to identify this submission.
     * @property broker The broker of the process to spawn.
     */
    data class Submit(
        val application: Application,
        val key: Any,
        val broker: ActorRef<MachineEvent>
    ) : MachineMessage()
}

/**
 * A message protocol used by machine instances to respond to [MachineMessage]s.
 */
sealed class MachineEvent {
    /**
     * Indicate that an [Application] was spawned on a machine instance.
     *
     * @property instance The machine instance to which the application was submitted.
     * @property application The application that has been submitted.
     * @property key The key used to identify the submission.
     * @property pid The spawned application instance.
     */
    data class Submitted(
        val instance: MachineRef,
        val application: Application,
        val key: Any,
        val pid: Pid
    ) : MachineEvent()

    /**
     * Indicate that an [Application] has terminated on the specified machine.
     *
     * @property instance The machine instance to which the application was submitted.
     * @property pid The reference to the application instance that has terminated.
     * @property status The exit code of the task, where zero means successful.
     */
    data class Terminated(
        val instance: MachineRef,
        val pid: Pid,
        val status: Int = 0
    ) : MachineEvent()
}
