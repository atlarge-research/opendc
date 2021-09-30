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

package org.opendc.simulator.compute.kernel

import org.opendc.simulator.compute.SimMachine
import org.opendc.simulator.compute.SimMachineContext
import org.opendc.simulator.compute.kernel.cpufreq.ScalingGovernor
import org.opendc.simulator.compute.kernel.interference.VmInterferenceDomain
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.flow.FlowConvergenceListener
import org.opendc.simulator.flow.FlowEngine
import org.opendc.simulator.flow.mux.FlowMultiplexer
import org.opendc.simulator.flow.mux.MaxMinFlowMultiplexer

/**
 * A [SimHypervisor] that distributes the computing requirements of multiple [SimWorkload]s on a single [SimMachine]
 * concurrently using weighted fair sharing.
 *
 * @param engine The [FlowEngine] to manage the machine's resources.
 * @param parent The parent simulation system.
 * @param scalingGovernor The CPU frequency scaling governor to use for the hypervisor.
 * @param interferenceDomain The resource interference domain to which the hypervisor belongs.
 * @param listener The hypervisor listener to use.
 */
public class SimFairShareHypervisor(
    engine: FlowEngine,
    private val parent: FlowConvergenceListener? = null,
    scalingGovernor: ScalingGovernor? = null,
    interferenceDomain: VmInterferenceDomain? = null,
    private val listener: SimHypervisor.Listener? = null
) : SimAbstractHypervisor(engine, scalingGovernor, interferenceDomain) {

    override fun canFit(model: MachineModel, switch: FlowMultiplexer): Boolean = true

    override fun createMultiplexer(ctx: SimMachineContext): FlowMultiplexer {
        return SwitchSystem(ctx).switch
    }

    private inner class SwitchSystem(private val ctx: SimMachineContext) : FlowConvergenceListener {
        val switch = MaxMinFlowMultiplexer(engine, this, interferenceDomain)

        private var lastCpuUsage = 0.0
        private var lastCpuDemand = 0.0
        private var lastDemand = 0.0
        private var lastActual = 0.0
        private var lastOvercommit = 0.0
        private var lastInterference = 0.0
        private var lastReport = Long.MIN_VALUE

        override fun onConverge(now: Long, delta: Long) {
            val listener = listener ?: return
            val counters = switch.counters

            if (now > lastReport) {
                listener.onSliceFinish(
                    this@SimFairShareHypervisor,
                    counters.demand - lastDemand,
                    counters.actual - lastActual,
                    counters.overcommit - lastOvercommit,
                    counters.interference - lastInterference,
                    lastCpuUsage,
                    lastCpuDemand
                )
            }
            lastReport = now

            lastCpuDemand = switch.outputs.sumOf { it.demand }
            lastCpuUsage = switch.outputs.sumOf { it.rate }
            lastDemand = counters.demand
            lastActual = counters.actual
            lastOvercommit = counters.overcommit
            lastInterference = counters.interference

            val load = lastCpuDemand / ctx.cpus.sumOf { it.model.frequency }
            triggerGovernors(load)

            parent?.onConverge(now, delta)
        }
    }
}
