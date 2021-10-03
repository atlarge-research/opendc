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
 * @param listener The listener for the convergence of the system.
 * @param scalingGovernor The CPU frequency scaling governor to use for the hypervisor.
 * @param interferenceDomain The resource interference domain to which the hypervisor belongs.
 */
public class SimFairShareHypervisor(
    engine: FlowEngine,
    listener: FlowConvergenceListener?,
    scalingGovernor: ScalingGovernor?,
    interferenceDomain: VmInterferenceDomain?,
) : SimAbstractHypervisor(engine, listener, scalingGovernor, interferenceDomain) {
    /**
     * The multiplexer that distributes the computing capacity.
     */
    override val mux: FlowMultiplexer = MaxMinFlowMultiplexer(engine, this, interferenceDomain)

    override fun canFit(model: MachineModel): Boolean = true
}
