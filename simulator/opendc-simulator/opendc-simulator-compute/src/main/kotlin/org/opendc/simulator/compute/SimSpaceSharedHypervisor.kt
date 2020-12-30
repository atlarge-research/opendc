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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.opendc.simulator.compute.interference.PerformanceInterferenceModel
import org.opendc.simulator.compute.model.SimMemoryUnit
import org.opendc.simulator.compute.model.SimProcessingUnit
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.resources.*
import java.time.Clock
import java.util.ArrayDeque
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A [SimHypervisor] that allocates its sub-resources exclusively for the virtual machine that it hosts.
 *
 * @param listener The hypervisor listener to use.
 */
public class SimSpaceSharedHypervisor(private val listener: SimHypervisor.Listener? = null) : SimHypervisor, SimResourceConsumer<SimProcessingUnit> {
    /**
     * The execution context in which the hypervisor runs.
     */
    private lateinit var ctx: SimMachineContext

    /**
     * The mapping from pCPU to vCPU.
     */
    private lateinit var vcpus: Array<VCpu?>

    /**
     * The available physical CPUs to schedule on.
     */
    private val availableCpus = ArrayDeque<Int>()

    override fun canFit(model: SimMachineModel): Boolean = availableCpus.size >= model.cpus.size

    override fun createMachine(
        model: SimMachineModel,
        performanceInterferenceModel: PerformanceInterferenceModel?
    ): SimMachine {
        require(canFit(model)) { "Cannot fit machine" }
        return SimVm(model, performanceInterferenceModel)
    }

    override fun onStart(ctx: SimMachineContext) {
        this.ctx = ctx
        this.vcpus = arrayOfNulls(ctx.cpus.size)
        this.availableCpus.addAll(ctx.cpus.indices)
    }

    override fun getConsumer(ctx: SimMachineContext, cpu: SimProcessingUnit): SimResourceConsumer<SimProcessingUnit> {
        return this
    }

    override fun onStart(ctx: SimResourceContext<SimProcessingUnit>): SimResourceCommand {
        return onNext(ctx, 0.0)
    }

    override fun onNext(ctx: SimResourceContext<SimProcessingUnit>, remainingWork: Double): SimResourceCommand {
        val vcpu = vcpus[ctx.resource.id] ?: return SimResourceCommand.Idle()

        if (vcpu.isStarted) {
            vcpu.remainingWork = remainingWork
            vcpu.flush()
        } else {
            vcpu.isStarted = true
            vcpu.start()
        }

        if (vcpu.hasExited && vcpu != vcpus[ctx.resource.id]) {
            return onNext(ctx, remainingWork)
        }

        return vcpu.activeCommand
    }

    /**
     * A virtual machine running on the hypervisor.
     *
     * @property model The machine model of the virtual machine.
     * @property performanceInterferenceModel The performance interference model to utilize.
     */
    private inner class SimVm(
        override val model: SimMachineModel,
        val performanceInterferenceModel: PerformanceInterferenceModel? = null,
    ) : SimMachine {
        /**
         *  A flag to indicate that the machine is terminated.
         */
        private var isTerminated = false

        /**
         * A [StateFlow] representing the CPU usage of the simulated machine.
         */
        override val usage: MutableStateFlow<Double> = MutableStateFlow(0.0)

        /**
         * The current active workload.
         */
        private var cont: Continuation<Unit>? = null

        /**
         * The physical CPUs that have been allocated.
         */
        private val pCPUs = model.cpus.map { availableCpus.poll() }.toIntArray()

        /**
         * The active CPUs of this virtual machine.
         */
        private var cpus: List<VCpu> = emptyList()

        /**
         * The execution context in which the workload runs.
         */
        inner class Context(override val meta: Map<String, Any>) : SimMachineContext {
            override val cpus: List<SimProcessingUnit>
                get() = model.cpus

            override val memory: List<SimMemoryUnit>
                get() = model.memory

            override val clock: Clock
                get() = this@SimSpaceSharedHypervisor.ctx.clock

            override fun interrupt(resource: SimResource) {
                TODO()
            }
        }

        lateinit var ctx: SimMachineContext

        /**
         * Run the specified [SimWorkload] on this machine and suspend execution util the workload has finished.
         */
        override suspend fun run(workload: SimWorkload, meta: Map<String, Any>) {
            require(!isTerminated) { "Machine is terminated" }
            require(cont == null) { "Run should not be called concurrently" }

            ctx = Context(meta)
            workload.onStart(ctx)

            return suspendCancellableCoroutine { cont ->
                this.cont = cont
                try {
                    this.cpus = model.cpus.map { model -> VCpu(this, model, workload.getConsumer(ctx, model), ctx.clock) }

                    for ((index, pCPU) in pCPUs.withIndex()) {
                        vcpus[pCPU] = cpus[index]
                        this@SimSpaceSharedHypervisor.ctx.interrupt(this@SimSpaceSharedHypervisor.ctx.cpus[pCPU])
                    }
                } catch (e: Throwable) {
                    cont.resumeWithException(e)
                }
            }
        }

        override fun close() {
            isTerminated = true
            for (pCPU in pCPUs) {
                vcpus[pCPU] = null
                availableCpus.add(pCPU)
            }

            val cont = cont
            this.cont = null
            cont?.resume(Unit)
        }

        /**
         * Update the usage of the VM.
         */
        fun updateUsage() {
            usage.value = cpus.sumByDouble { it.speed } / cpus.sumByDouble { it.resource.frequency }
        }

        /**
         * This method is invoked when one of the CPUs has exited.
         */
        fun onCpuExit() {
            // Check whether all other CPUs have finished
            if (cpus.all { it.hasExited }) {
                val cont = cont
                this.cont = null
                cont?.resume(Unit)
            }
        }

        /**
         * This method is invoked when one of the CPUs failed.
         */
        fun onCpuFailure(e: Throwable) {
            // In case the flush fails with an exception, immediately propagate to caller, cancelling all other
            // tasks.
            val cont = cont
            this.cont = null
            cont?.resumeWithException(e)
        }
    }

    /**
     * A CPU of the virtual machine.
     */
    private inner class VCpu(
        val vm: SimVm,
        resource: SimProcessingUnit,
        consumer: SimResourceConsumer<SimProcessingUnit>,
        clock: Clock
    ) : SimAbstractResourceContext<SimProcessingUnit>(resource, clock, consumer) {
        /**
         * Indicates that the vCPU was started.
         */
        var isStarted: Boolean = false

        /**
         * The current command that is processed by the vCPU.
         */
        var activeCommand: SimResourceCommand = SimResourceCommand.Idle()

        /**
         * The processing speed of the vCPU.
         */
        var speed: Double = 0.0
            set(value) {
                field = value
                vm.updateUsage()
            }

        /**
         * The amount of work remaining from the previous consumption.
         */
        var remainingWork: Double = 0.0

        /**
         * A flag to indicate that the CPU has exited.
         */
        var hasExited: Boolean = false

        override fun onIdle(deadline: Long) {
            speed = 0.0
            activeCommand = SimResourceCommand.Idle(deadline)
        }

        override fun onConsume(work: Double, limit: Double, deadline: Long) {
            speed = getSpeed(limit)
            activeCommand = SimResourceCommand.Consume(work, speed, deadline)
        }

        override fun onFinish() {
            speed = 0.0
            hasExited = true
            activeCommand = SimResourceCommand.Idle()
            vm.onCpuExit()
        }

        override fun onFailure(cause: Throwable) {
            speed = 0.0
            hasExited = true
            activeCommand = SimResourceCommand.Idle()
            vm.onCpuFailure(cause)
        }

        override fun getRemainingWork(work: Double, speed: Double, duration: Long, isInterrupted: Boolean): Double {
            return remainingWork
        }
    }
}
