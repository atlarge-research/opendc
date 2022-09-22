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

import org.opendc.simulator.compute.kernel.cpufreq.ScalingGovernor
import org.opendc.simulator.compute.kernel.interference.VmInterferenceDomain
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.flow.FlowEngine
import org.opendc.simulator.flow.mux.FlowMultiplexer
import org.opendc.simulator.flow.mux.ForwardingFlowMultiplexer
import java.util.SplittableRandom

/**
 * A [SimHypervisor] that allocates its sub-resources exclusively for the virtual machine that it hosts.
 *
 * @param engine The [FlowEngine] to drive the simulation.
 * @param random A randomness generator for the interference calculations.
 * @param scalingGovernor The scaling governor to use for scaling the CPU frequency of the underlying hardware.
 * @param interferenceDomain The interference domain to which the hypervisor belongs.
 */
public class SimSpaceSharedHypervisor(
    engine: FlowEngine,
    random: SplittableRandom,
    scalingGovernor: ScalingGovernor?,
    interferenceDomain: VmInterferenceDomain = VmInterferenceDomain()
) : SimAbstractHypervisor(engine, random, scalingGovernor, interferenceDomain) {
    override val mux: FlowMultiplexer = ForwardingFlowMultiplexer(engine, this)

    override fun canFit(model: MachineModel): Boolean {
        return mux.outputs.size - mux.inputs.size >= model.cpus.size
    }
}
