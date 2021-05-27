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

package org.opendc.simulator.compute

import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.resources.SimResourceInterpreter
import org.opendc.simulator.resources.SimResourceSwitch
import org.opendc.simulator.resources.SimResourceSwitchMaxMin

/**
 * A [SimHypervisor] that distributes the computing requirements of multiple [SimWorkload] on a single
 * [SimBareMetalMachine] concurrently using weighted fair sharing.
 *
 * @param listener The hypervisor listener to use.
 */
public class SimFairShareHypervisor(private val interpreter: SimResourceInterpreter, private val listener: SimHypervisor.Listener? = null) : SimAbstractHypervisor(interpreter) {

    override fun canFit(model: SimMachineModel, switch: SimResourceSwitch): Boolean = true

    override fun createSwitch(ctx: SimMachineContext): SimResourceSwitch {
        return SimResourceSwitchMaxMin(
            interpreter, null,
            object : SimResourceSwitchMaxMin.Listener {
                override fun onSliceFinish(
                    switch: SimResourceSwitchMaxMin,
                    requestedWork: Long,
                    grantedWork: Long,
                    overcommittedWork: Long,
                    interferedWork: Long,
                    cpuUsage: Double,
                    cpuDemand: Double
                ) {
                    listener?.onSliceFinish(this@SimFairShareHypervisor, requestedWork, grantedWork, overcommittedWork, interferedWork, cpuUsage, cpuDemand)
                }
            }
        )
    }
}
