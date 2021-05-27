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

package org.opendc.simulator.resources

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.opendc.simulator.core.SimulationCoroutineScope
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.simulator.resources.consumer.SimTraceConsumer
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@OptIn(ExperimentalCoroutinesApi::class)
class SimResourceBenchmarks {
    private lateinit var scope: SimulationCoroutineScope
    private lateinit var interpreter: SimResourceInterpreter

    @Setup
    fun setUp() {
        scope = SimulationCoroutineScope()
        interpreter = SimResourceInterpreter(scope.coroutineContext, scope.clock)
    }

    @State(Scope.Thread)
    class Workload {
        lateinit var trace: Sequence<SimTraceConsumer.Fragment>

        @Setup
        fun setUp() {
            trace = sequenceOf(
                SimTraceConsumer.Fragment(1000, 28.0),
                SimTraceConsumer.Fragment(1000, 3500.0),
                SimTraceConsumer.Fragment(1000, 0.0),
                SimTraceConsumer.Fragment(1000, 183.0),
                SimTraceConsumer.Fragment(1000, 400.0),
                SimTraceConsumer.Fragment(1000, 100.0),
                SimTraceConsumer.Fragment(1000, 3000.0),
                SimTraceConsumer.Fragment(1000, 4500.0),
            )
        }
    }

    @Benchmark
    fun benchmarkSource(state: Workload) {
        return scope.runBlockingSimulation {
            val provider = SimResourceSource(4200.0, interpreter)
            return@runBlockingSimulation provider.consume(SimTraceConsumer(state.trace))
        }
    }

    @Benchmark
    fun benchmarkForwardOverhead(state: Workload) {
        return scope.runBlockingSimulation {
            val provider = SimResourceSource(4200.0, interpreter)
            val forwarder = SimResourceForwarder()
            provider.startConsumer(forwarder)
            return@runBlockingSimulation forwarder.consume(SimTraceConsumer(state.trace))
        }
    }

    @Benchmark
    fun benchmarkSwitchMaxMinSingleConsumer(state: Workload) {
        return scope.runBlockingSimulation {
            val switch = SimResourceSwitchMaxMin(interpreter)

            switch.addInput(SimResourceSource(3000.0, interpreter))
            switch.addInput(SimResourceSource(3000.0, interpreter))

            val provider = switch.addOutput(3500.0)
            return@runBlockingSimulation provider.consume(SimTraceConsumer(state.trace))
        }
    }

    @Benchmark
    fun benchmarkSwitchMaxMinTripleConsumer(state: Workload) {
        return scope.runBlockingSimulation {
            val switch = SimResourceSwitchMaxMin(interpreter)

            switch.addInput(SimResourceSource(3000.0, interpreter))
            switch.addInput(SimResourceSource(3000.0, interpreter))

            repeat(3) {
                launch {
                    val provider = switch.addOutput(3500.0)
                    provider.consume(SimTraceConsumer(state.trace))
                }
            }
        }
    }

    @Benchmark
    fun benchmarkSwitchExclusiveSingleConsumer(state: Workload) {
        return scope.runBlockingSimulation {
            val switch = SimResourceSwitchExclusive()

            switch.addInput(SimResourceSource(3000.0, interpreter))
            switch.addInput(SimResourceSource(3000.0, interpreter))

            val provider = switch.addOutput(3500.0)
            return@runBlockingSimulation provider.consume(SimTraceConsumer(state.trace))
        }
    }

    @Benchmark
    fun benchmarkSwitchExclusiveTripleConsumer(state: Workload) {
        return scope.runBlockingSimulation {
            val switch = SimResourceSwitchExclusive()

            switch.addInput(SimResourceSource(3000.0, interpreter))
            switch.addInput(SimResourceSource(3000.0, interpreter))

            repeat(2) {
                launch {
                    val provider = switch.addOutput(3500.0)
                    provider.consume(SimTraceConsumer(state.trace))
                }
            }
        }
    }
}
