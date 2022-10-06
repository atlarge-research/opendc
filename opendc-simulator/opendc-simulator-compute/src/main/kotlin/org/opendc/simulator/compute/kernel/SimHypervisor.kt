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

import org.opendc.simulator.compute.SimAbstractMachine
import org.opendc.simulator.compute.SimMachine
import org.opendc.simulator.compute.SimMachineContext
import org.opendc.simulator.compute.SimProcessingUnit
import org.opendc.simulator.compute.kernel.cpufreq.ScalingGovernor
import org.opendc.simulator.compute.kernel.cpufreq.ScalingPolicy
import org.opendc.simulator.compute.kernel.interference.VmInterferenceDomain
import org.opendc.simulator.compute.kernel.interference.VmInterferenceMember
import org.opendc.simulator.compute.kernel.interference.VmInterferenceProfile
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.flow.FlowConsumer
import org.opendc.simulator.flow.FlowConvergenceListener
import org.opendc.simulator.flow.FlowEngine
import org.opendc.simulator.flow.mux.FlowMultiplexer
import org.opendc.simulator.flow.mux.FlowMultiplexerFactory
import java.util.SplittableRandom
import kotlin.math.roundToLong

/**
 * A SimHypervisor facilitates the execution of multiple concurrent [SimWorkload]s, while acting as a single workload
 * to another [SimMachine].
 *
 * @param engine The [FlowEngine] to drive the simulation.
 * @param muxFactory The factor for the [FlowMultiplexer] to multiplex the workloads.
 * @param random A randomness generator for the interference calculations.
 * @param scalingGovernor The scaling governor to use for scaling the CPU frequency of the underlying hardware.
 * @param interferenceDomain The interference domain to which the hypervisor belongs.
 */
public class SimHypervisor(
    private val engine: FlowEngine,
    muxFactory: FlowMultiplexerFactory,
    private val random: SplittableRandom,
    private val scalingGovernor: ScalingGovernor? = null,
    private val interferenceDomain: VmInterferenceDomain = VmInterferenceDomain()
) : SimWorkload, FlowConvergenceListener {
    /**
     * The [FlowMultiplexer] to multiplex the virtual machines.
     */
    private val mux = muxFactory.newMultiplexer(engine, this)

    /**
     * The virtual machines running on this hypervisor.
     */
    private val _vms = mutableSetOf<VirtualMachine>()
    public val vms: Set<SimMachine>
        get() = _vms

    /**
     * The resource counters associated with the hypervisor.
     */
    public val counters: SimHypervisorCounters
        get() = _counters
    private val _counters = CountersImpl(this)

    /**
     * The CPU capacity of the hypervisor in MHz.
     */
    public val cpuCapacity: Double
        get() = mux.capacity

    /**
     * The CPU demand of the hypervisor in MHz.
     */
    public val cpuDemand: Double
        get() = mux.demand

    /**
     * The CPU usage of the hypervisor in MHz.
     */
    public val cpuUsage: Double
        get() = mux.rate

    /**
     * The machine on which the hypervisor runs.
     */
    private lateinit var context: SimMachineContext

    /**
     * The scaling governors attached to the physical CPUs backing this hypervisor.
     */
    private val governors = mutableListOf<ScalingGovernor.Logic>()

    /* SimHypervisor */
    /**
     * Create a [SimMachine] instance on which users may run a [SimWorkload].
     *
     * @param model The machine to create.
     */
    public fun newMachine(model: MachineModel): SimVirtualMachine {
        require(canFit(model)) { "Machine does not fit" }
        val vm = VirtualMachine(model)
        _vms.add(vm)
        return vm
    }

    /**
     * Remove the specified [machine] from the hypervisor.
     *
     * @param machine The machine to remove.
     */
    public fun removeMachine(machine: SimVirtualMachine) {
        if (_vms.remove(machine)) {
            // This cast must always succeed, since `_vms` only contains `VirtualMachine` types.
            (machine as VirtualMachine).close()
        }
    }

    /**
     * Determine whether the specified machine characterized by [model] can fit on this hypervisor at this moment.
     */
    public fun canFit(model: MachineModel): Boolean {
        return (mux.maxInputs - mux.inputs.size) >= model.cpus.size
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
    private var _lastConverge = engine.clock.millis()

    /* FlowConvergenceListener */
    override fun onConverge(now: Long) {
        val lastConverge = _lastConverge
        _lastConverge = now
        val delta = now - lastConverge

        if (delta > 0) {
            _counters.record()

            val mux = mux
            val load = mux.rate / mux.capacity.coerceAtLeast(1.0)
            val random = random

            for (vm in _vms) {
                vm._counters.record(random, load)
            }
        }

        val load = cpuDemand / cpuCapacity
        for (governor in governors) {
            governor.onLimit(load)
        }
    }

    /**
     * A virtual machine running on the hypervisor.
     *
     * @param model The machine model of the virtual machine.
     */
    private inner class VirtualMachine(model: MachineModel) : SimAbstractMachine(engine, model), SimVirtualMachine, AutoCloseable {
        /**
         * A flag to indicate that the machine is closed.
         */
        private var isClosed = false

        /**
         * The vCPUs of the machine.
         */
        override val cpus = model.cpus.map { cpu -> VCpu(mux, mux.newInput(cpu.frequency), cpu) }

        /**
         * The resource counters associated with the hypervisor.
         */
        override val counters: SimHypervisorCounters
            get() = _counters
        @JvmField val _counters = VmCountersImpl(cpus, null)

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

            val profile = meta["interference-profile"] as? VmInterferenceProfile
            val interferenceMember = if (profile != null) interferenceDomain.join(profile) else null

            val counters = _counters
            counters.member = interferenceMember

            return super.startWorkload(
                object : SimWorkload {
                    override fun onStart(ctx: SimMachineContext) {
                        try {
                            interferenceMember?.activate()
                            workload.onStart(ctx)
                        } catch (cause: Throwable) {
                            interferenceMember?.deactivate()
                            throw cause
                        }
                    }

                    override fun onStop(ctx: SimMachineContext) {
                        interferenceMember?.deactivate()
                        counters.member = null
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

        fun flush() {
            switch.flushCounters(source)
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
    private class CountersImpl(private val hv: SimHypervisor) : SimHypervisorCounters {
        @JvmField var d = 1.0 // Number of CPUs divided by total CPU capacity

        override val cpuActiveTime: Long
            get() = _cpuTime[0]
        override val cpuIdleTime: Long
            get() = _cpuTime[1]
        override val cpuStealTime: Long
            get() = _cpuTime[2]
        override val cpuLostTime: Long
            get() = _cpuTime[3]

        val _cpuTime = LongArray(4)
        private val _previous = DoubleArray(3)

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

            val demandDelta = demand - previous[0]
            val actualDelta = actual - previous[1]
            val remainingDelta = remaining - previous[2]

            previous[0] = demand
            previous[1] = actual
            previous[2] = remaining

            cpuTime[0] += (actualDelta * d).roundToLong()
            cpuTime[1] += (remainingDelta * d).roundToLong()
            cpuTime[2] += ((demandDelta - actualDelta) * d).roundToLong()
        }

        override fun flush() {
            hv.mux.flushCounters()
            record()
        }
    }

    /**
     * A [SimHypervisorCounters] implementation for a virtual machine.
     */
    private inner class VmCountersImpl(
        private val cpus: List<VCpu>,
        @JvmField var member: VmInterferenceMember?
    ) : SimHypervisorCounters {
        private val d = cpus.size / cpus.sumOf { it.model.frequency } * 1000

        override val cpuActiveTime: Long
            get() = _cpuTime[0]
        override val cpuIdleTime: Long
            get() = _cpuTime[1]
        override val cpuStealTime: Long
            get() = _cpuTime[2]
        override val cpuLostTime: Long
            get() = _cpuTime[3]

        private val _cpuTime = LongArray(4)
        private val _previous = DoubleArray(3)

        /**
         * Record the CPU time of the hypervisor.
         */
        fun record(random: SplittableRandom, load: Double) {
            val cpuTime = _cpuTime
            val previous = _previous

            var demand = 0.0
            var actual = 0.0
            var remaining = 0.0

            for (cpu in cpus) {
                val counters = cpu.counters

                actual += counters.actual
                demand += counters.demand
                remaining += counters.remaining
            }

            val demandDelta = demand - previous[0]
            val actualDelta = actual - previous[1]
            val remainingDelta = remaining - previous[2]

            previous[0] = demand
            previous[1] = actual
            previous[2] = remaining

            val d = d
            cpuTime[0] += (actualDelta * d).roundToLong()
            cpuTime[1] += (remainingDelta * d).roundToLong()
            cpuTime[2] += ((demandDelta - actualDelta) * d).roundToLong()

            // Compute the performance penalty due to flow interference
            val member = member
            if (member != null) {
                val penalty = 1 - member.apply(random, load)
                val interference = (actualDelta * d * penalty).roundToLong()

                if (interference > 0) {
                    cpuTime[3] += interference
                    _counters._cpuTime[3] += interference
                }
            }
        }

        override fun flush() {
            for (cpu in cpus) {
                cpu.flush()
            }
        }
    }
}
