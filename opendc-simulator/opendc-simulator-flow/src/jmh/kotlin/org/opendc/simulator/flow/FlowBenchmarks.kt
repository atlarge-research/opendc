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

package org.opendc.simulator.flow

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.opendc.simulator.core.SimulationCoroutineScope
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.simulator.flow.mux.ForwardingFlowMultiplexer
import org.opendc.simulator.flow.mux.MaxMinFlowMultiplexer
import org.opendc.simulator.flow.source.TraceFlowSource
import org.openjdk.jmh.annotations.*
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@OptIn(ExperimentalCoroutinesApi::class)
class FlowBenchmarks {
    private lateinit var scope: SimulationCoroutineScope
    private lateinit var engine: FlowEngine

    @Setup
    fun setUp() {
        scope = SimulationCoroutineScope()
        engine = FlowEngine(scope.coroutineContext, scope.clock)
    }

    @State(Scope.Thread)
    class Workload {
        lateinit var trace: Sequence<TraceFlowSource.Fragment>

        @Setup
        fun setUp() {
            val random = ThreadLocalRandom.current()
            val entries = List(10000) { TraceFlowSource.Fragment(1000, random.nextDouble(0.0, 4500.0)) }
            trace = entries.asSequence()
        }
    }

    @Benchmark
    fun benchmarkSink(state: Workload) {
        return scope.runBlockingSimulation {
            val provider = FlowSink(engine, 4200.0)
            return@runBlockingSimulation provider.consume(TraceFlowSource(state.trace))
        }
    }

    @Benchmark
    fun benchmarkForward(state: Workload) {
        return scope.runBlockingSimulation {
            val provider = FlowSink(engine, 4200.0)
            val forwarder = FlowForwarder(engine)
            provider.startConsumer(forwarder)
            return@runBlockingSimulation forwarder.consume(TraceFlowSource(state.trace))
        }
    }

    @Benchmark
    fun benchmarkMuxMaxMinSingleSource(state: Workload) {
        return scope.runBlockingSimulation {
            val switch = MaxMinFlowMultiplexer(engine)

            FlowSink(engine, 3000.0).startConsumer(switch.newOutput())
            FlowSink(engine, 3000.0).startConsumer(switch.newOutput())

            val provider = switch.newInput()
            return@runBlockingSimulation provider.consume(TraceFlowSource(state.trace))
        }
    }

    @Benchmark
    fun benchmarkMuxMaxMinTripleSource(state: Workload) {
        return scope.runBlockingSimulation {
            val switch = MaxMinFlowMultiplexer(engine)

            FlowSink(engine, 3000.0).startConsumer(switch.newOutput())
            FlowSink(engine, 3000.0).startConsumer(switch.newOutput())

            repeat(3) {
                launch {
                    val provider = switch.newInput()
                    provider.consume(TraceFlowSource(state.trace))
                }
            }
        }
    }

    @Benchmark
    fun benchmarkMuxExclusiveSingleSource(state: Workload) {
        return scope.runBlockingSimulation {
            val switch = ForwardingFlowMultiplexer(engine)

            FlowSink(engine, 3000.0).startConsumer(switch.newOutput())
            FlowSink(engine, 3000.0).startConsumer(switch.newOutput())

            val provider = switch.newInput()
            return@runBlockingSimulation provider.consume(TraceFlowSource(state.trace))
        }
    }

    @Benchmark
    fun benchmarkMuxExclusiveTripleSource(state: Workload) {
        return scope.runBlockingSimulation {
            val switch = ForwardingFlowMultiplexer(engine)

            FlowSink(engine, 3000.0).startConsumer(switch.newOutput())
            FlowSink(engine, 3000.0).startConsumer(switch.newOutput())

            repeat(2) {
                launch {
                    val provider = switch.newInput()
                    provider.consume(TraceFlowSource(state.trace))
                }
            }
        }
    }
}
