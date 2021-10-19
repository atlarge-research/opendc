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
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.flow.*
import org.opendc.simulator.flow.interference.InterferenceKey
import org.opendc.simulator.flow.mux.FlowMultiplexer
import kotlin.math.roundToLong

/**
 * Abstract implementation of the [SimHypervisor] interface.
 *
 * @param engine The [FlowEngine] to drive the simulation.
 * @param scalingGovernor The scaling governor to use for scaling the CPU frequency of the underlying hardware.
 */
public abstract class SimAbstractHypervisor(
    protected val engine: FlowEngine,
    private val listener: FlowConvergenceListener?,
    private val scalingGovernor: ScalingGovernor?,
    protected val interferenceDomain: VmInterferenceDomain? = null
) : SimHypervisor, FlowConvergenceListener {
    /**
     * The machine on which the hypervisor runs.
     */
    protected lateinit var context: SimMachineContext

    /**
     * The resource switch to use.
     */
    protected abstract val mux: FlowMultiplexer

    /**
     * The virtual machines running on this hypervisor.
     */
    private val _vms = mutableSetOf<VirtualMachine>()
    override val vms: Set<SimMachine>
        get() = _vms

    /**
     * The resource counters associated with the hypervisor.
     */
    public override val counters: SimHypervisorCounters
        get() = _counters
    private val _counters = CountersImpl(this)

    /**
     * The CPU capacity of the hypervisor in MHz.
     */
    override val cpuCapacity: Double
        get() = mux.capacity

    /**
     * The CPU demand of the hypervisor in MHz.
     */
    override val cpuDemand: Double
        get() = mux.demand

    /**
     * The CPU usage of the hypervisor in MHz.
     */
    override val cpuUsage: Double
        get() = mux.rate

    /**
     * The scaling governors attached to the physical CPUs backing this hypervisor.
     */
    private val governors = mutableListOf<ScalingGovernor.Logic>()

    /* SimHypervisor */
    override fun newMachine(model: MachineModel, interferenceId: String?): SimVirtualMachine {
        require(canFit(model)) { "Machine does not fit" }
        val vm = VirtualMachine(model, interferenceId)
        _vms.add(vm)
        return vm
    }

    override fun removeMachine(machine: SimVirtualMachine) {
        if (_vms.remove(machine)) {
            // This cast must always succeed, since `_vms` only contains `VirtualMachine` types.
            (machine as VirtualMachine).close()
        }
    }

    /* SimWorkload */
    override fun onStart(ctx: SimMachineContext) {
        context = ctx

        _cpuCount = ctx.cpus.size
        _cpuCapacity = ctx.cpus.sumOf { it.model.frequency }
        _counters.d = _cpuCount / _cpuCapacity * 1000L

        // Clear the existing outputs of the multiplexer
        mux.clearOutputs()

        for (cpu in ctx.cpus) {
            val governor = scalingGovernor?.createLogic(ScalingPolicyImpl(cpu))
            if (governor != null) {
                governors.add(governor)
                governor.onStart()
            }

            cpu.startConsumer(mux.newOutput())
        }
    }

    override fun onStop(ctx: SimMachineContext) {}

    private var _cpuCount = 0
    private var _cpuCapacity = 0.0

    /* FlowConvergenceListener */
    override fun onConverge(now: Long, delta: Long) {
        _counters.record()

        val load = cpuDemand / cpuCapacity
        for (governor in governors) {
            governor.onLimit(load)
        }

        listener?.onConverge(now, delta)
    }

    /**
     * A virtual machine running on the hypervisor.
     *
     * @param model The machine model of the virtual machine.
     */
    private inner class VirtualMachine(
        model: MachineModel,
        interferenceId: String? = null
    ) : SimAbstractMachine(engine, parent = null, model), SimVirtualMachine, AutoCloseable {
        /**
         * A flag to indicate that the machine is closed.
         */
        private var isClosed = false

        /**
         * The interference key of this virtual machine.
         */
        private val interferenceKey: InterferenceKey? = interferenceId?.let { interferenceDomain?.createKey(it) }

        /**
         * The vCPUs of the machine.
         */
        override val cpus = model.cpus.map { cpu -> VCpu(mux, mux.newInput(cpu.frequency, interferenceKey), cpu) }

        /**
         * The resource counters associated with the hypervisor.
         */
        override val counters: SimHypervisorCounters
            get() = _counters
        private val _counters = VmCountersImpl(cpus)

        /**
         * The CPU capacity of the hypervisor in MHz.
         */
        override val cpuCapacity: Double
            get() = cpus.sumOf(FlowConsumer::capacity)

        /**
         * The CPU demand of the hypervisor in MHz.
         */
        override val cpuDemand: Double
            get() = cpus.sumOf(FlowConsumer::demand)

        /**
         * The CPU usage of the hypervisor in MHz.
         */
        override val cpuUsage: Double
            get() = cpus.sumOf(FlowConsumer::rate)

        override fun startWorkload(workload: SimWorkload, meta: Map<String, Any>): SimMachineContext {
            check(!isClosed) { "Machine is closed" }

            return super.startWorkload(
                object : SimWorkload {
                    override fun onStart(ctx: SimMachineContext) {
                        try {
                            joinInterferenceDomain()
                            workload.onStart(ctx)
                        } catch (cause: Throwable) {
                            leaveInterferenceDomain()
                            throw cause
                        }
                    }

                    override fun onStop(ctx: SimMachineContext) {
                        leaveInterferenceDomain()
                        workload.onStop(ctx)
                    }
                },
                meta
            )
        }

        override fun close() {
            if (isClosed) {
                return
            }

            isClosed = true
            cancel()

            for (cpu in cpus) {
                cpu.close()
            }
        }

        /**
         * Join the interference domain of the hypervisor.
         */
        private fun joinInterferenceDomain() {
            val interferenceKey = interferenceKey
            if (interferenceKey != null) {
                interferenceDomain?.join(interferenceKey)
            }
        }

        /**
         * Leave the interference domain of the hypervisor.
         */
        private fun leaveInterferenceDomain() {
            val interferenceKey = interferenceKey
            if (interferenceKey != null) {
                interferenceDomain?.leave(interferenceKey)
            }
        }
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
            set(_) = TODO("Capacity changes on vCPU not supported")

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

    /**
     * Implementation of [SimHypervisorCounters].
     */
    private class CountersImpl(private val hv: SimAbstractHypervisor) : SimHypervisorCounters {
        @JvmField var d = 1.0 // Number of CPUs divided by total CPU capacity

        override val cpuActiveTime: Long
            get() = _cpuTime[0]
        override val cpuIdleTime: Long
            get() = _cpuTime[1]
        override val cpuStealTime: Long
            get() = _cpuTime[2]
        override val cpuLostTime: Long
            get() = _cpuTime[3]

        private val _cpuTime = LongArray(4)
        private val _previous = DoubleArray(4)

        /**
         * Record the CPU time of the hypervisor.
         */
        fun record() {
            val cpuTime = _cpuTime
            val previous = _previous
            val counters = hv.mux.counters

            val demand = counters.demand
            val actual = counters.actual
            val remaining = counters.remaining
            val interference = counters.interference

            val demandDelta = demand - previous[0]
            val actualDelta = actual - previous[1]
            val remainingDelta = remaining - previous[2]
            val interferenceDelta = interference - previous[3]

            previous[0] = demand
            previous[1] = actual
            previous[2] = remaining
            previous[3] = interference

            cpuTime[0] += (actualDelta * d).roundToLong()
            cpuTime[1] += (remainingDelta * d).roundToLong()
            cpuTime[2] += ((demandDelta - actualDelta) * d).roundToLong()
            cpuTime[3] += (interferenceDelta * d).roundToLong()
        }
    }

    /**
     * A [SimHypervisorCounters] implementation for a virtual machine.
     */
    private class VmCountersImpl(private val cpus: List<VCpu>) : SimHypervisorCounters {
        private val d = cpus.size / cpus.sumOf { it.model.frequency } * 1000

        override val cpuActiveTime: Long
            get() = (cpus.sumOf { it.counters.actual } * d).roundToLong()
        override val cpuIdleTime: Long
            get() = (cpus.sumOf { it.counters.actual + it.counters.remaining } * d).roundToLong()
        override val cpuStealTime: Long
            get() = (cpus.sumOf { it.counters.demand - it.counters.actual } * d).roundToLong()
        override val cpuLostTime: Long
            get() = (cpus.sumOf { it.counters.interference } * d).roundToLong()
    }
}
