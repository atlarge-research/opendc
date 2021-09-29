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

import org.opendc.simulator.compute.*
import org.opendc.simulator.compute.kernel.cpufreq.ScalingGovernor
import org.opendc.simulator.compute.kernel.cpufreq.ScalingPolicy
import org.opendc.simulator.compute.kernel.interference.VmInterferenceDomain
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.flow.*
import org.opendc.simulator.flow.mux.FlowMultiplexer

/**
 * Abstract implementation of the [SimHypervisor] interface.
 *
 * @param engine The [FlowEngine] to drive the simulation.
 * @param scalingGovernor The scaling governor to use for scaling the CPU frequency of the underlying hardware.
 */
public abstract class SimAbstractHypervisor(
    protected val engine: FlowEngine,
    private val scalingGovernor: ScalingGovernor? = null,
    protected val interferenceDomain: VmInterferenceDomain? = null
) : SimHypervisor {
    /**
     * The machine on which the hypervisor runs.
     */
    private lateinit var context: SimMachineContext

    /**
     * The resource switch to use.
     */
    private lateinit var mux: FlowMultiplexer

    /**
     * The virtual machines running on this hypervisor.
     */
    private val _vms = mutableSetOf<VirtualMachine>()
    override val vms: Set<SimMachine>
        get() = _vms

    /**
     * The resource counters associated with the hypervisor.
     */
    public override val counters: FlowCounters
        get() = mux.counters

    /**
     * The scaling governors attached to the physical CPUs backing this hypervisor.
     */
    private val governors = mutableListOf<ScalingGovernor.Logic>()

    /**
     * Construct the [FlowMultiplexer] implementation that performs the actual scheduling of the CPUs.
     */
    public abstract fun createMultiplexer(ctx: SimMachineContext): FlowMultiplexer

    /**
     * Check whether the specified machine model fits on this hypervisor.
     */
    public abstract fun canFit(model: MachineModel, switch: FlowMultiplexer): Boolean

    /**
     * Trigger the governors to recompute the scaling limits.
     */
    protected fun triggerGovernors(load: Double) {
        for (governor in governors) {
            governor.onLimit(load)
        }
    }

    /* SimHypervisor */
    override fun canFit(model: MachineModel): Boolean {
        return canFit(model, mux)
    }

    override fun createMachine(model: MachineModel, interferenceId: String?): SimMachine {
        require(canFit(model)) { "Machine does not fit" }
        val vm = VirtualMachine(model, interferenceId)
        _vms.add(vm)
        return vm
    }

    /* SimWorkload */
    override fun onStart(ctx: SimMachineContext) {
        context = ctx
        mux = createMultiplexer(ctx)

        for (cpu in ctx.cpus) {
            val governor = scalingGovernor?.createLogic(ScalingPolicyImpl(cpu))
            if (governor != null) {
                governors.add(governor)
                governor.onStart()
            }

            mux.addOutput(cpu)
        }
    }

    /**
     * A virtual machine running on the hypervisor.
     *
     * @param model The machine model of the virtual machine.
     */
    private inner class VirtualMachine(model: MachineModel, interferenceId: String? = null) : SimAbstractMachine(engine, parent = null, model) {
        /**
         * The interference key of this virtual machine.
         */
        private val interferenceKey = interferenceId?.let { interferenceDomain?.join(interferenceId) }

        /**
         * The vCPUs of the machine.
         */
        override val cpus = model.cpus.map { VCpu(mux, mux.newInput(interferenceKey), it) }

        override fun close() {
            super.close()

            for (cpu in cpus) {
                cpu.close()
            }

            _vms.remove(this)
            if (interferenceKey != null) {
                interferenceDomain?.leave(interferenceKey)
            }
        }

        override fun onConverge(timestamp: Long) {}
    }

    /**
     * A [SimProcessingUnit] of a virtual machine.
     */
    private class VCpu(
        private val switch: FlowMultiplexer,
        private val source: FlowConsumer,
        override val model: ProcessingUnit
    ) : SimProcessingUnit, FlowConsumer by source {
        override var capacity: Double
            get() = source.capacity
            set(_) {
                // Ignore capacity changes
            }

        override fun toString(): String = "SimAbstractHypervisor.VCpu[model=$model]"

        /**
         * Close the CPU
         */
        fun close() {
            switch.removeInput(source)
        }
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
