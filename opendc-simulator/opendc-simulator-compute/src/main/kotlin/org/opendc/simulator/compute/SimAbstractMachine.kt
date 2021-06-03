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

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.resources.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * Abstract implementation of the [SimMachine] interface.
 *
 * @param interpreter The interpreter to manage the machine's resources.
 * @param parent The parent simulation system.
 * @param model The model of the machine.
 */
public abstract class SimAbstractMachine(
    protected val interpreter: SimResourceInterpreter,
    final override val parent: SimResourceSystem?,
    final override val model: SimMachineModel
) : SimMachine, SimResourceSystem {
    /**
     * A [StateFlow] representing the CPU usage of the simulated machine.
     */
    private val _usage = MutableStateFlow(0.0)
    public final override val usage: StateFlow<Double>
        get() = _usage

    /**
     * The speed of the CPU cores.
     */
    public val speed: DoubleArray
        get() = _speed
    private var _speed = doubleArrayOf()

    /**
     * The resources allocated for this machine.
     */
    protected abstract val cpus: List<SimProcessingUnit>

    /**
     * A flag to indicate that the machine is terminated.
     */
    private var isTerminated = false

    /**
     * The continuation to resume when the virtual machine workload has finished.
     */
    private var cont: Continuation<Unit>? = null

    /**
     * Run the specified [SimWorkload] on this machine and suspend execution util the workload has finished.
     */
    override suspend fun run(workload: SimWorkload, meta: Map<String, Any>) {
        check(!isTerminated) { "Machine is terminated" }
        check(cont == null) { "A machine cannot run concurrently" }

        val ctx = Context(meta)

        // Before the workload starts, initialize the initial power draw
        _speed = DoubleArray(model.cpus.size) { 0.0 }
        updateUsage(0.0)

        return suspendCancellableCoroutine { cont ->
            this.cont = cont

            // Cancel all cpus on cancellation
            cont.invokeOnCancellation {
                this.cont = null

                interpreter.batch {
                    for (cpu in cpus) {
                        cpu.cancel()
                    }
                }
            }

            interpreter.batch { workload.onStart(ctx) }
        }
    }

    override fun close() {
        if (isTerminated) {
            return
        }

        isTerminated = true
        cancel()
        interpreter.batch {
            for (cpu in cpus) {
                cpu.close()
            }
        }
    }

    /* SimResourceSystem */
    override fun onConverge(timestamp: Long) {
        val totalCapacity = model.cpus.sumOf { it.frequency }
        val cpus = cpus
        var totalSpeed = 0.0
        for (cpu in cpus) {
            _speed[cpu.model.id] = cpu.speed
            totalSpeed += cpu.speed
        }

        updateUsage(totalSpeed / totalCapacity)
    }

    /**
     * This method is invoked when the usage of the machine is updated.
     */
    protected open fun updateUsage(usage: Double) {
        _usage.value = usage
    }

    /**
     * Cancel the workload that is currently running on the machine.
     */
    private fun cancel() {
        interpreter.batch {
            for (cpu in cpus) {
                cpu.cancel()
            }
        }

        val cont = cont
        if (cont != null) {
            this.cont = null
            cont.resume(Unit)
        }
    }

    /**
     * The execution context in which the workload runs.
     */
    private inner class Context(override val meta: Map<String, Any>) : SimMachineContext {
        override val interpreter: SimResourceInterpreter
            get() = this@SimAbstractMachine.interpreter

        override val cpus: List<SimProcessingUnit> = this@SimAbstractMachine.cpus

        override val memory: List<MemoryUnit> = model.memory

        override fun close() = cancel()
    }
}
