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

package com.atlarge.opendc.model.resources.compute.host

import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.join
import com.atlarge.odcsim.receiveMessage
import com.atlarge.odcsim.same
import com.atlarge.odcsim.setup
import com.atlarge.odcsim.unhandled
import com.atlarge.odcsim.withTimers
import com.atlarge.opendc.model.resources.compute.Machine
import com.atlarge.opendc.model.resources.compute.MachineMessage
import com.atlarge.opendc.model.resources.compute.ProcessingElement
import com.atlarge.opendc.model.resources.compute.scheduling.MachineScheduler
import com.atlarge.opendc.model.resources.compute.supervision.MachineSupervisionEvent
import com.atlarge.opendc.model.workload.application.Application
import com.atlarge.opendc.model.workload.application.ProcessSupervisor
import java.util.UUID

/**
 * A physical compute node in a datacenter that is able to run [Application]s.
 *
 * @property uid The unique identifier of this machine.
 * @property name The name of the machine.
 * @property scheduler The process scheduler of this machine.
 * @property cores The list of processing elements in the machine.
 * @property details The details of this host.
 */
data class Host(
    override val uid: UUID,
    override val name: String,
    val scheduler: MachineScheduler,
    val cores: List<ProcessingElement>,
    override val details: Map<String, Any> = emptyMap()
) : Machine {
    /**
     * Build the [Behavior] for a physical machine.
     */
    override fun invoke(supervisor: ActorRef<MachineSupervisionEvent>): Behavior<MachineMessage> = setup { ctx ->
        ctx.send(supervisor, MachineSupervisionEvent.Announce(this, ctx.self))
        ctx.send(supervisor, MachineSupervisionEvent.Up(ctx.self))

        withTimers { timers ->
            val sched = scheduler(this, ctx, timers)
            sched.updateResources(cores)
            receiveMessage<Any> { msg ->
                when (msg) {
                    is MachineMessage.Submit -> {
                        sched.submit(msg.application, msg.key, msg.broker)
                        same()
                    }
                    else ->
                        unhandled()
                }
            }.join(ProcessSupervisor(sched).unsafeCast()).narrow()
        }
    }

    override fun equals(other: Any?): Boolean = other is Machine && uid == other.uid

    override fun hashCode(): Int = uid.hashCode()
}
