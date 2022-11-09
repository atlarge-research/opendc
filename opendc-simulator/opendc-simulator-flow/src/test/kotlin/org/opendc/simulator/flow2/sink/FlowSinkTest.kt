/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.simulator.flow2.sink

import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.opendc.simulator.flow2.FlowEngine
import org.opendc.simulator.flow2.source.SimpleFlowSource
import org.opendc.simulator.flow2.source.TraceFlowSource
import org.opendc.simulator.kotlin.runSimulation
import java.util.concurrent.ThreadLocalRandom

/**
 * Test suite for the [SimpleFlowSink] class.
 */
class FlowSinkTest {
    @Test
    fun testSmoke() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val sink = SimpleFlowSink(graph, 1.0f)
        val source = SimpleFlowSource(graph, 2.0f, 1.0f)

        graph.connect(source.output, sink.input)
        advanceUntilIdle()

        assertEquals(2000, timeSource.millis())
    }

    @Test
    fun testAdjustCapacity() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val sink = SimpleFlowSink(graph, 1.0f)
        val source = SimpleFlowSource(graph, 2.0f, 1.0f)

        graph.connect(source.output, sink.input)

        delay(1000)
        sink.capacity = 0.5f

        advanceUntilIdle()

        assertEquals(3000, timeSource.millis())
    }

    @Test
    fun testUtilization() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val sink = SimpleFlowSink(graph, 1.0f)
        val source = SimpleFlowSource(graph, 2.0f, 0.5f)

        graph.connect(source.output, sink.input)
        advanceUntilIdle()

        assertEquals(4000, timeSource.millis())
    }

    @Test
    fun testFragments() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val sink = SimpleFlowSink(graph, 1.0f)
        val trace = TraceFlowSource.Trace(
            longArrayOf(1000, 2000, 3000, 4000),
            floatArrayOf(1.0f, 0.5f, 2.0f, 1.0f),
            4
        )
        val source = TraceFlowSource(
            graph,
            trace
        )

        graph.connect(source.output, sink.input)
        advanceUntilIdle()

        assertEquals(4000, timeSource.millis())
    }

    @Test
    fun benchmarkSink() {
        val random = ThreadLocalRandom.current()
        val traceSize = 10000000
        val trace = TraceFlowSource.Trace(
            LongArray(traceSize) { it * 1000L },
            FloatArray(traceSize) { random.nextDouble(0.0, 4500.0).toFloat() },
            traceSize
        )

        return runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()
            val sink = SimpleFlowSink(graph, 4200.0f)
            val source = TraceFlowSource(graph, trace)
            graph.connect(source.output, sink.input)
        }
    }
}
