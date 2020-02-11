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

package com.atlarge.opendc.core.resources.compute.supervision

import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.ProcessContext
import com.atlarge.odcsim.ReceiveRef
import com.atlarge.opendc.core.resources.compute.Machine
import com.atlarge.opendc.core.resources.compute.MachineRef
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

/**
 * An interface for supervising [Machine] instances.
 */
interface MachineSupervisor {
    /**
     * This method is invoked when a new machine is introduced to the supervisor by specifying its static information
     * and address.
     *
     * @param machine The machine that is being announced.
     * @param ref The address to talk to the host.
     */
    fun onAnnounce(machine: Machine, ref: MachineRef)

    /**
     * This method is invoked when a process exits.
     *
     * @param ref The address to talk to the machine.
     */
    fun onUp(ref: MachineRef)

    companion object {
        /**
         * Create the [Behavior] for a [MachineSupervisor].
         *
         * @param supervisor The supervisor to create the behavior for.
         */
        suspend operator fun invoke(ctx: ProcessContext, supervisor: MachineSupervisor, main: ReceiveRef<MachineSupervisionEvent>) {
            val inlet = ctx.listen(main)

            coroutineScope {
                while (isActive) {
                    when (val msg = inlet.receive()) {
                        is MachineSupervisionEvent.Announce -> supervisor.onAnnounce(msg.machine, msg.ref)
                        is MachineSupervisionEvent.Up -> supervisor.onUp(msg.ref)
                    }
                }
            }

            inlet.close()
        }
    }
}
