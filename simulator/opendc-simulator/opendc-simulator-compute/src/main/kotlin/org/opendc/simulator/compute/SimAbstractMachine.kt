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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.opendc.simulator.compute.model.SimMemoryUnit
import org.opendc.simulator.compute.model.SimProcessingUnit
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.resources.SimResource
import org.opendc.simulator.resources.SimResourceProvider
import org.opendc.simulator.resources.SimResourceSource
import java.time.Clock
import kotlin.coroutines.CoroutineContext

/**
 * Abstract implementation of the [SimMachine] interface.
 */
public abstract class SimAbstractMachine(private val clock: Clock) : SimMachine {
    private val _usage = MutableStateFlow(0.0)
    override val usage: StateFlow<Double>
        get() = _usage

    /**
     * The speed of the CPU cores.
     */
    public val speed: List<Double>
        get() = _speed
    private var _speed = mutableListOf<Double>()

    /**
     * A flag to indicate that the machine is terminated.
     */
    private var isTerminated = false

    /**
     * The [CoroutineContext] to run in.
     */
    protected abstract val context: CoroutineContext

    /**
     * The resources allocated for this machine.
     */
    protected abstract val resources: Map<SimProcessingUnit, SimResourceSource<SimProcessingUnit>>

    /**
     * The execution context in which the workload runs.
     */
    private inner class Context(
        val sources: Map<SimProcessingUnit, SimResourceProvider<SimProcessingUnit>>,
        override val meta: Map<String, Any>
    ) : SimMachineContext {
        override val clock: Clock
            get() = this@SimAbstractMachine.clock

        override val cpus: List<SimProcessingUnit> = model.cpus

        override val memory: List<SimMemoryUnit> = model.memory

        override fun interrupt(resource: SimResource) {
            checkNotNull(sources[resource]) { "Invalid resource" }.interrupt()
        }
    }

    /**
     * Run the specified [SimWorkload] on this machine and suspend execution util the workload has finished.
     */
    override suspend fun run(workload: SimWorkload, meta: Map<String, Any>): Unit = withContext(context) {
        val resources = resources
        require(!isTerminated) { "Machine is terminated" }
        val ctx = Context(resources, meta + mapOf("coroutine-context" to context))
        val totalCapacity = model.cpus.sumByDouble { it.frequency }

        _speed = MutableList(model.cpus.size) { 0.0 }

        workload.onStart(ctx)

        for ((cpu, source) in resources) {
            val consumer = workload.getConsumer(ctx, cpu)
            val job = source.speed
                .onEach {
                    _speed[cpu.id] = source.speed.value
                    _usage.value = _speed.sum() / totalCapacity
                }
                .launchIn(this)

            launch {
                source.consume(consumer)
                job.cancel()
            }
        }
    }

    override fun close() {
        if (!isTerminated) {
            resources.forEach { (_, provider) -> provider.close() }
        } else {
            isTerminated = true
        }
    }
}
