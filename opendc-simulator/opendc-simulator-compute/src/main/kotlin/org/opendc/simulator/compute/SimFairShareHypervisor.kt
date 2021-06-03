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

import org.opendc.simulator.compute.cpufreq.ScalingGovernor
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.resources.SimResourceInterpreter
import org.opendc.simulator.resources.SimResourceSwitch
import org.opendc.simulator.resources.SimResourceSwitchMaxMin
import org.opendc.simulator.resources.SimResourceSystem

/**
 * A [SimHypervisor] that distributes the computing requirements of multiple [SimWorkload] on a single
 * [SimBareMetalMachine] concurrently using weighted fair sharing.
 *
 * @param interpreter The interpreter to manage the machine's resources.
 * @param parent The parent simulation system.
 * @param scalingGovernor The CPU frequency scaling governor to use for the hypervisor.
 * @param listener The hypervisor listener to use.
 */
public class SimFairShareHypervisor(
    private val interpreter: SimResourceInterpreter,
    private val parent: SimResourceSystem? = null,
    scalingGovernor: ScalingGovernor? = null,
    private val listener: SimHypervisor.Listener? = null
) : SimAbstractHypervisor(interpreter, scalingGovernor) {

    override fun canFit(model: SimMachineModel, switch: SimResourceSwitch): Boolean = true

    override fun createSwitch(ctx: SimMachineContext): SimResourceSwitch {
        return SwitchSystem(ctx).switch
    }

    private inner class SwitchSystem(private val ctx: SimMachineContext) : SimResourceSystem {
        val switch = SimResourceSwitchMaxMin(interpreter, this)

        override val parent: SimResourceSystem? = this@SimFairShareHypervisor.parent

        private var lastCpuUsage = 0.0
        private var lastCpuDemand = 0.0
        private var lastDemand = 0.0
        private var lastActual = 0.0
        private var lastOvercommit = 0.0
        private var lastReport = Long.MIN_VALUE

        override fun onConverge(timestamp: Long) {
            val listener = listener ?: return
            val counters = switch.counters

            if (timestamp > lastReport) {
                listener.onSliceFinish(
                    this@SimFairShareHypervisor,
                    (counters.demand - lastDemand).toLong(),
                    (counters.actual - lastActual).toLong(),
                    (counters.overcommit - lastOvercommit).toLong(),
                    0L,
                    lastCpuUsage,
                    lastCpuDemand
                )
            }
            lastReport = timestamp

            lastCpuDemand = switch.inputs.sumOf { it.demand }
            lastCpuUsage = switch.inputs.sumOf { it.speed }
            lastDemand = counters.demand
            lastActual = counters.actual
            lastOvercommit = counters.overcommit

            val load = lastCpuDemand / ctx.cpus.sumOf { it.model.frequency }
            triggerGovernors(load)
        }
    }
}
