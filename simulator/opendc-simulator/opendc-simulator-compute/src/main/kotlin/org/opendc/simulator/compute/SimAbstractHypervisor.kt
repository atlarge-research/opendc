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

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.opendc.simulator.compute.interference.PerformanceInterferenceModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.resources.*
import java.time.Clock

/**
 * Abstract implementation of the [SimHypervisor] interface.
 */
public abstract class SimAbstractHypervisor : SimHypervisor {
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
        override val model: SimMachineModel,
        val performanceInterferenceModel: PerformanceInterferenceModel? = null,
    ) : SimMachine {
        /**
         * A [StateFlow] representing the CPU usage of the simulated machine.
         */
        override val usage: MutableStateFlow<Double> = MutableStateFlow(0.0)

        /**
         *  A flag to indicate that the machine is terminated.
         */
        private var isTerminated = false

        /**
         * The vCPUs of the machine.
         */
        private val cpus = model.cpus.map { ProcessingUnitImpl(it, switch) }

        /**
         * Run the specified [SimWorkload] on this machine and suspend execution util the workload has finished.
         */
        override suspend fun run(workload: SimWorkload, meta: Map<String, Any>) {
            coroutineScope {
                require(!isTerminated) { "Machine is terminated" }

                val ctx = object : SimMachineContext {
                    override val cpus: List<SimProcessingUnit> = this@VirtualMachine.cpus

                    override val memory: List<MemoryUnit>
                        get() = model.memory

                    override val clock: Clock
                        get() = this@SimAbstractHypervisor.context.clock

                    override val meta: Map<String, Any> = meta
                }

                workload.onStart(ctx)

                for (cpu in cpus) {
                    launch {
                        cpu.consume(workload.getConsumer(ctx, cpu.model))
                    }
                }
            }
        }

        /**
         * Terminate this VM instance.
         */
        override fun close() {
            if (!isTerminated) {
                isTerminated = true

                cpus.forEach(SimProcessingUnit::close)
                _vms.remove(this)
            }
        }
    }

    override fun onStart(ctx: SimMachineContext) {
        context = ctx
        switch = createSwitch(ctx)
    }

    override fun getConsumer(ctx: SimMachineContext, cpu: ProcessingUnit): SimResourceConsumer {
        val forwarder = SimResourceForwarder()
        switch.addInput(forwarder)
        return forwarder
    }

    /**
     * The [SimProcessingUnit] of this machine.
     */
    public inner class ProcessingUnitImpl(override val model: ProcessingUnit, switch: SimResourceSwitch) : SimProcessingUnit {
        /**
         * The actual resource supporting the processing unit.
         */
        private val source = switch.addOutput(model.frequency)

        override val speed: Double = 0.0 /* TODO Implement */

        override val state: SimResourceState
            get() = source.state

        override fun startConsumer(consumer: SimResourceConsumer) {
            source.startConsumer(consumer)
        }

        override fun interrupt() {
            source.interrupt()
        }

        override fun cancel() {
            source.cancel()
        }

        override fun close() {
            source.close()
        }
    }
}
