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

package org.opendc.simulator.flow2

import kotlinx.coroutines.launch
import org.opendc.simulator.flow2.mux.MaxMinFlowMultiplexer
import org.opendc.simulator.flow2.sink.SimpleFlowSink
import org.opendc.simulator.flow2.source.TraceFlowSource
import org.opendc.simulator.flow2.util.FlowTransformer
import org.opendc.simulator.flow2.util.FlowTransforms
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
    private lateinit var trace: TraceFlowSource.Trace

    @Setup
    fun setUp() {
        val random = ThreadLocalRandom.current()
        val traceSize = 10_000_000
        trace = TraceFlowSource.Trace(
            LongArray(traceSize) { (it + 1) * 1000L },
            FloatArray(traceSize) { random.nextFloat(0.0f, 4500.0f) },
            traceSize
        )
    }

    @Benchmark
    fun benchmarkSink() {
        return runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()
            val sink = SimpleFlowSink(graph, 4200.0f)
            val source = TraceFlowSource(graph, trace)
            graph.connect(source.output, sink.input)
        }
    }

    @Benchmark
    fun benchmarkForward() {
        return runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()
            val sink = SimpleFlowSink(graph, 4200.0f)
            val source = TraceFlowSource(graph, trace)
            val forwarder = FlowTransformer(graph, FlowTransforms.noop())

            graph.connect(source.output, forwarder.input)
            graph.connect(forwarder.output, sink.input)
        }
    }

    @Benchmark
    fun benchmarkMuxMaxMinSingleSource() {
        return runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()
            val switch = MaxMinFlowMultiplexer(graph)

            val sinkA = SimpleFlowSink(graph, 3000.0f)
            val sinkB = SimpleFlowSink(graph, 3000.0f)

            graph.connect(switch.newOutput(), sinkA.input)
            graph.connect(switch.newOutput(), sinkB.input)

            val source = TraceFlowSource(graph, trace)
            graph.connect(source.output, switch.newInput())
        }
    }

    @Benchmark
    fun benchmarkMuxMaxMinTripleSource() {
        return runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()
            val switch = MaxMinFlowMultiplexer(graph)

            val sinkA = SimpleFlowSink(graph, 3000.0f)
            val sinkB = SimpleFlowSink(graph, 3000.0f)

            graph.connect(switch.newOutput(), sinkA.input)
            graph.connect(switch.newOutput(), sinkB.input)

            repeat(3) {
                launch {
                    val source = TraceFlowSource(graph, trace)
                    graph.connect(source.output, switch.newInput())
                }
            }
        }
    }
}
