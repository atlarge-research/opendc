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
import org.opendc.simulator.resources.SimResourceInterpreter
import org.opendc.simulator.resources.SimResourceSystem
import org.opendc.simulator.resources.batch
import org.opendc.simulator.resources.consume
import java.time.Clock

/**
 * Abstract implementation of the [SimMachine] interface.
 *
 * @param interpreter The interpreter to manage the machine's resources.
 */
public abstract class SimAbstractMachine(protected val interpreter: SimResourceInterpreter) : SimMachine, SimResourceSystem {
    private val _usage = MutableStateFlow(0.0)
    override val usage: StateFlow<Double>
        get() = _usage

    /**
     * The speed of the CPU cores.
     */
    public val speed: DoubleArray
        get() = _speed
    private var _speed = doubleArrayOf()

    /**
     * A flag to indicate that the machine is terminated.
     */
    private var isTerminated = false

    /**
     * The resources allocated for this machine.
     */
    protected abstract val cpus: List<SimProcessingUnit>

    /**
     * The execution context in which the workload runs.
     */
    private inner class Context(override val meta: Map<String, Any>) : SimMachineContext {
        override val clock: Clock
            get() = interpreter.clock

        override val cpus: List<SimProcessingUnit> = this@SimAbstractMachine.cpus

        override val memory: List<MemoryUnit> = model.memory
    }

    /**
     * Run the specified [SimWorkload] on this machine and suspend execution util the workload has finished.
     */
    override suspend fun run(workload: SimWorkload, meta: Map<String, Any>): Unit = coroutineScope {
        check(!isTerminated) { "Machine is terminated" }
        val ctx = Context(meta)

        // Before the workload starts, initialize the initial power draw
        _speed = DoubleArray(model.cpus.size) { 0.0 }
        updateUsage(0.0)

        interpreter.batch {
            workload.onStart(ctx)

            for (cpu in cpus) {
                val model = cpu.model
                val consumer = workload.getConsumer(ctx, model)
                launch { cpu.consume(consumer) }
            }
        }
    }

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

    override fun close() {
        if (isTerminated) {
            return
        }

        isTerminated = true
        cpus.forEach(SimProcessingUnit::close)
    }
}
