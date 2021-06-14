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
import org.opendc.simulator.compute.cpufreq.ScalingPolicy
import org.opendc.simulator.compute.interference.PerformanceInterferenceModel
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.resources.*
import org.opendc.simulator.resources.SimResourceSwitch

/**
 * Abstract implementation of the [SimHypervisor] interface.
 *
 * @param interpreter The resource interpreter to use.
 * @param scalingGovernor The scaling governor to use for scaling the CPU frequency of the underlying hardware.
 */
public abstract class SimAbstractHypervisor(
    private val interpreter: SimResourceInterpreter,
    private val scalingGovernor: ScalingGovernor?
) : SimHypervisor {
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
     * The scaling governors attached to the physical CPUs backing this hypervisor.
     */
    private val governors = mutableListOf<ScalingGovernor.Logic>()

    /**
     * Construct the [SimResourceSwitch] implementation that performs the actual scheduling of the CPUs.
     */
    public abstract fun createSwitch(ctx: SimMachineContext): SimResourceSwitch

    /**
     * Check whether the specified machine model fits on this hypervisor.
     */
    public abstract fun canFit(model: SimMachineModel, switch: SimResourceSwitch): Boolean

    /**
     * Trigger the governors to recompute the scaling limits.
     */
    protected fun triggerGovernors(load: Double) {
        for (governor in governors) {
            governor.onLimit(load)
        }
    }

    /* SimHypervisor */
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

    /* SimWorkload */
    override fun onStart(ctx: SimMachineContext) {
        context = ctx
        switch = createSwitch(ctx)

        for (cpu in ctx.cpus) {
            val governor = scalingGovernor?.createLogic(ScalingPolicyImpl(cpu))
            if (governor != null) {
                governors.add(governor)
                governor.onStart()
            }

            switch.addInput(cpu)
        }
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

    /**
     * A [SimProcessingUnit] of a virtual machine.
     */
    private class VCpu(
        private val source: SimResourceProvider,
        override val model: ProcessingUnit
    ) : SimProcessingUnit, SimResourceProvider by source {
        override var capacity: Double
            get() = source.capacity
            set(_) {
                // Ignore capacity changes
            }

        override fun toString(): String = "SimAbstractHypervisor.VCpu[model=$model]"
    }

    /**
     * A [ScalingPolicy] for a physical CPU of the hypervisor.
     */
    private class ScalingPolicyImpl(override val cpu: SimProcessingUnit) : ScalingPolicy {
        override var target: Double
            get() = cpu.capacity
            set(value) {
                cpu.capacity = value
            }

        override val max: Double = cpu.model.frequency

        override val min: Double = 0.0
    }
}
