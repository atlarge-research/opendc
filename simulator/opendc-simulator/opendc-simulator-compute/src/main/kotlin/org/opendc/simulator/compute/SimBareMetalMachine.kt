/*
 * Copyright (c) 2020 AtLarge Research
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
import org.opendc.simulator.resources.*
import org.opendc.utils.TimerScheduler
import java.time.Clock
import java.util.*
import kotlin.coroutines.*

/**
 * A simulated bare-metal machine that is able to run a single workload.
 *
 * A [SimBareMetalMachine] is a stateful object and you should be careful when operating this object concurrently. For
 * example. the class expects only a single concurrent call to [run].
 *
 * @param coroutineScope The [CoroutineScope] to run the simulated workload in.
 * @param clock The virtual clock to track the simulation time.
 * @param model The machine model to simulate.
 */
@OptIn(ExperimentalCoroutinesApi::class, InternalCoroutinesApi::class)
public class SimBareMetalMachine(
    private val coroutineScope: CoroutineScope,
    private val clock: Clock,
    override val model: SimMachineModel
) : SimMachine {
    private val _usage = MutableStateFlow(0.0)
    override val usage: StateFlow<Double>
        get() = _usage

    /**
     *  A flag to indicate that the machine is terminated.
     */
    private var isTerminated = false

    /**
     * The [TimerScheduler] to use for scheduling the interrupts.
     */
    private val scheduler = TimerScheduler<Any>(coroutineScope, clock)

    /**
     * The execution context in which the workload runs.
     */
    private inner class Context(val map: Map<SimProcessingUnit, SimResourceContext<SimProcessingUnit>>,
                                override val meta: Map<String, Any>) : SimMachineContext {
        override val clock: Clock
            get() = this@SimBareMetalMachine.clock

        override val cpus: List<SimProcessingUnit> = model.cpus

        override val memory: List<SimMemoryUnit> = model.memory

        override fun interrupt(resource: SimResource) {
            val context = map[resource]
            checkNotNull(context) { "Invalid resource" }
            context.interrupt()
        }
    }

    /**
     * Run the specified [SimWorkload] on this machine and suspend execution util the workload has finished.
     */
    override suspend fun run(workload: SimWorkload, meta: Map<String, Any>): Unit = coroutineScope {
        require(!isTerminated) { "Machine is terminated" }
        val map = mutableMapOf<SimProcessingUnit, SimResourceContext<SimProcessingUnit>>()
        val ctx = Context(map, meta)
        val sources = model.cpus.map { SimResourceSource(it, clock, scheduler) }
        val totalCapacity = model.cpus.sumByDouble { it.frequency }

        workload.onStart(ctx)

        for (source in sources) {
            val consumer = workload.getConsumer(ctx, source.resource)
            val job = source.speed
                .onEach {
                    _usage.value = sources.sumByDouble { it.speed.value } / totalCapacity
                }
                .launchIn(this)

            launch {
                source.consume(object : SimResourceConsumer<SimProcessingUnit> by consumer {
                    override fun onStart(ctx: SimResourceContext<SimProcessingUnit>): SimResourceCommand {
                        map[ctx.resource] = ctx
                        return consumer.onStart(ctx)
                    }
                })
                job.cancel()
            }
        }
    }

    override fun close() {
        isTerminated = true
        scheduler.close()
    }
}
