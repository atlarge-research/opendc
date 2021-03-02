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
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.workload.SimResourceCommand
import org.opendc.simulator.compute.workload.SimWorkload
import java.time.Clock
import java.util.ArrayDeque
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min

/**
 * A [SimHypervisor] that allocates its sub-resources exclusively for the virtual machine that it hosts.
 *
 * @param listener The hypervisor listener to use.
 */
public class SimSpaceSharedHypervisor(private val listener: SimHypervisor.Listener? = null) : SimHypervisor {
    /**
     * The execution context in which the hypervisor runs.
     */
    private lateinit var ctx: SimExecutionContext

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

    override fun onStart(ctx: SimExecutionContext) {
        this.ctx = ctx
        this.vcpus = arrayOfNulls(ctx.machine.cpus.size)
        this.availableCpus.addAll(ctx.machine.cpus.indices)
    }

    override fun onStart(ctx: SimExecutionContext, cpu: Int): SimResourceCommand {
        return onNext(ctx, cpu, 0.0)
    }

    override fun onNext(ctx: SimExecutionContext, cpu: Int, remainingWork: Double): SimResourceCommand {
        return vcpus[cpu]?.next(0.0) ?: SimResourceCommand.Idle()
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
         * Run the specified [SimWorkload] on this machine and suspend execution util the workload has finished.
         */
        override suspend fun run(workload: SimWorkload, meta: Map<String, Any>) {
            require(!isTerminated) { "Machine is terminated" }
            require(cont == null) { "Run should not be called concurrently" }

            val ctx = object : SimExecutionContext {
                override val machine: SimMachineModel
                    get() = model

                override val clock: Clock
                    get() = this@SimSpaceSharedHypervisor.ctx.clock

                override val meta: Map<String, Any>
                    get() = meta

                override fun interrupt(cpu: Int) {
                    require(cpu < cpus.size) { "Invalid CPU identifier" }
                    cpus[cpu].interrupt()
                }
            }

            workload.onStart(ctx)

            return suspendCancellableCoroutine { cont ->
                this.cont = cont
                this.cpus = model.cpus.mapIndexed { index, model -> VCpu(this, ctx, model, workload, pCPUs[index]) }

                for (cpu in cpus) {
                    cpu.start()
                }
            }
        }

        override fun close() {
            isTerminated = true
            for (pCPU in pCPUs) {
                vcpus[pCPU] = null
                availableCpus.add(pCPU)
            }
        }

        /**
         * Update the usage of the VM.
         */
        fun updateUsage() {
            usage.value = cpus.sumByDouble { it.speed } / cpus.sumByDouble { it.model.frequency }
        }

        /**
         * This method is invoked when one of the CPUs has exited.
         */
        fun onCpuExit(cpu: Int) {
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
    private inner class VCpu(val vm: SimVm, val ctx: SimExecutionContext, val model: ProcessingUnit, val workload: SimWorkload, val pCPU: Int) {
        /**
         * The processing speed of the vCPU.
         */
        var speed: Double = 0.0
            set(value) {
                field = value
                vm.updateUsage()
            }

        /**
         * A flag to indicate that the CPU has exited.
         */
        var hasExited: Boolean = false

        /**
         * A flag to indicate that the CPU was started.
         */
        var hasStarted: Boolean = false

        /**
         * Process the specified [SimResourceCommand] for this CPU.
         */
        fun process(command: SimResourceCommand): SimResourceCommand {
            return when (command) {
                is SimResourceCommand.Idle -> {
                    speed = 0.0
                    command
                }
                is SimResourceCommand.Consume -> {
                    speed = min(model.frequency, command.limit)
                    command
                }
                is SimResourceCommand.Exit -> {
                    speed = 0.0
                    hasExited = true

                    vm.onCpuExit(model.id)

                    SimResourceCommand.Idle()
                }
            }
        }

        /**
         * Start the CPU.
         */
        fun start() {
            vcpus[pCPU] = this
            interrupt()
        }

        /**
         * Request the workload for more work.
         */
        fun next(remainingWork: Double): SimResourceCommand {
            return try {
                val command =
                    if (hasStarted) {
                        workload.onNext(ctx, model.id, remainingWork)
                    } else {
                        hasStarted = true
                        workload.onStart(ctx, model.id)
                    }
                process(command)
            } catch (e: Throwable) {
                fail(e)
            }
        }

        /**
         * Interrupt the CPU.
         */
        fun interrupt() {
            this@SimSpaceSharedHypervisor.ctx.interrupt(pCPU)
        }

        /**
         * Fail the CPU.
         */
        fun fail(e: Throwable): SimResourceCommand {
            hasExited = true

            vm.onCpuFailure(e)

            return SimResourceCommand.Idle()
        }
    }
}
