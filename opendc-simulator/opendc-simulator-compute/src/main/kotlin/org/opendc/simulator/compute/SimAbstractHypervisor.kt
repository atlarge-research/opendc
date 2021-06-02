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

import org.opendc.simulator.compute.interference.PerformanceInterferenceModel
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.resources.*
import org.opendc.simulator.resources.SimResourceSwitch

/**
 * Abstract implementation of the [SimHypervisor] interface.
 */
public abstract class SimAbstractHypervisor(private val interpreter: SimResourceInterpreter) : SimHypervisor {
    /**
     * The machine on which the hypervisor runs.
     */
    private lateinit var context: SimMachineContext

    /**
     * The resource switch to use.
     */
    private lateinit var switch: SimResourceSwitch

    /**
     * The virtual machines running on this hypervisor.
     */
    private val _vms = mutableSetOf<VirtualMachine>()
    override val vms: Set<SimMachine>
        get() = _vms

    /**
     * Construct the [SimResourceSwitch] implementation that performs the actual scheduling of the CPUs.
     */
    public abstract fun createSwitch(ctx: SimMachineContext): SimResourceSwitch

    /**
     * Check whether the specified machine model fits on this hypervisor.
     */
    public abstract fun canFit(model: SimMachineModel, switch: SimResourceSwitch): Boolean

    override fun canFit(model: SimMachineModel): Boolean {
        return canFit(model, switch)
    }

    override fun createMachine(
        model: SimMachineModel,
        performanceInterferenceModel: PerformanceInterferenceModel?
    ): SimMachine {
        require(canFit(model)) { "Machine does not fit" }
        val vm = VirtualMachine(model, performanceInterferenceModel)
        _vms.add(vm)
        return vm
    }

    /**
     * A virtual machine running on the hypervisor.
     *
     * @property model The machine model of the virtual machine.
     * @property performanceInterferenceModel The performance interference model to utilize.
     */
    private inner class VirtualMachine(
        model: SimMachineModel,
        val performanceInterferenceModel: PerformanceInterferenceModel? = null,
    ) : SimAbstractMachine(interpreter, parent = null, model) {
        /**
         * The vCPUs of the machine.
         */
        override val cpus = model.cpus.map { VCpu(switch.newOutput(), it) }

        override fun close() {
            super.close()

            _vms.remove(this)
        }
    }

    override fun onStart(ctx: SimMachineContext) {
        context = ctx
        switch = createSwitch(ctx)

        for (cpu in ctx.cpus) {
            switch.addInput(cpu)
        }
    }

    /**
     * A [SimProcessingUnit] of a virtual machine.
     */
    private class VCpu(
        private val source: SimResourceProvider,
        override val model: ProcessingUnit
    ) : SimProcessingUnit, SimResourceProvider by source {
        override fun toString(): String = "SimAbstractHypervisor.VCpu[model=$model]"
    }
}
