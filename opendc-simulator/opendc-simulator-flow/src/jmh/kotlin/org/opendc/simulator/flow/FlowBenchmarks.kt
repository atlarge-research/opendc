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

import kotlinx.coroutines.launch
import org.opendc.simulator.flow.mux.ForwardingFlowMultiplexer
import org.opendc.simulator.flow.mux.MaxMinFlowMultiplexer
import org.opendc.simulator.flow.source.TraceFlowSource
import org.opendc.simulator.kotlin.runSimulation
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
class FlowBenchmarks {
    private lateinit var trace: Sequence<TraceFlowSource.Fragment>

    @Setup
    fun setUp() {
        val random = ThreadLocalRandom.current()
        val entries = List(10000) { TraceFlowSource.Fragment(1000, random.nextDouble(0.0, 4500.0)) }
        trace = entries.asSequence()
    }

    @Benchmark
    fun benchmarkSink() {
        return runSimulation {
            val engine = FlowEngine(coroutineContext, clock)
            val provider = FlowSink(engine, 4200.0)
            return@runSimulation provider.consume(TraceFlowSource(trace))
        }
    }

    @Benchmark
    fun benchmarkForward() {
        return runSimulation {
            val engine = FlowEngine(coroutineContext, clock)
            val provider = FlowSink(engine, 4200.0)
            val forwarder = FlowForwarder(engine)
            provider.startConsumer(forwarder)
            return@runSimulation forwarder.consume(TraceFlowSource(trace))
        }
    }

    @Benchmark
    fun benchmarkMuxMaxMinSingleSource() {
        return runSimulation {
            val engine = FlowEngine(coroutineContext, clock)
            val switch = MaxMinFlowMultiplexer(engine)

            FlowSink(engine, 3000.0).startConsumer(switch.newOutput())
            FlowSink(engine, 3000.0).startConsumer(switch.newOutput())

            val provider = switch.newInput()
            return@runSimulation provider.consume(TraceFlowSource(trace))
        }
    }

    @Benchmark
    fun benchmarkMuxMaxMinTripleSource() {
        return runSimulation {
            val engine = FlowEngine(coroutineContext, clock)
            val switch = MaxMinFlowMultiplexer(engine)

            FlowSink(engine, 3000.0).startConsumer(switch.newOutput())
            FlowSink(engine, 3000.0).startConsumer(switch.newOutput())

            repeat(3) {
                launch {
                    val provider = switch.newInput()
                    provider.consume(TraceFlowSource(trace))
                }
            }
        }
    }

    @Benchmark
    fun benchmarkMuxExclusiveSingleSource() {
        return runSimulation {
            val engine = FlowEngine(coroutineContext, clock)
            val switch = ForwardingFlowMultiplexer(engine)

            FlowSink(engine, 3000.0).startConsumer(switch.newOutput())
            FlowSink(engine, 3000.0).startConsumer(switch.newOutput())

            val provider = switch.newInput()
            return@runSimulation provider.consume(TraceFlowSource(trace))
        }
    }

    @Benchmark
    fun benchmarkMuxExclusiveTripleSource() {
        return runSimulation {
            val engine = FlowEngine(coroutineContext, clock)
            val switch = ForwardingFlowMultiplexer(engine)

            FlowSink(engine, 3000.0).startConsumer(switch.newOutput())
            FlowSink(engine, 3000.0).startConsumer(switch.newOutput())

            repeat(2) {
                launch {
                    val provider = switch.newInput()
                    provider.consume(TraceFlowSource(trace))
                }
            }
        }
    }
}
