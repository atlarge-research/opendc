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

package org.opendc.simulator.flow2.mux

import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.simulator.flow2.FlowEngine
import org.opendc.simulator.flow2.sink.SimpleFlowSink
import org.opendc.simulator.flow2.source.TraceFlowSource
import org.opendc.simulator.kotlin.runSimulation

/**
 * Test suite for the [ForwardingFlowMultiplexer] class.
 */
class ForwardingFlowMultiplexerTest {
    /**
     * Test a trace workload.
     */
    @Test
    fun testTrace() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val switch = ForwardingFlowMultiplexer(graph)
        val sink = SimpleFlowSink(graph, 3200.0f)
        graph.connect(switch.newOutput(), sink.input)

        yield()

        assertEquals(sink.capacity, switch.capacity) { "Capacity is not detected" }

        val workload =
            TraceFlowSource(
                graph,
                TraceFlowSource.Trace(
                    longArrayOf(1000, 2000, 3000, 4000),
                    floatArrayOf(28.0f, 3500.0f, 0.0f, 183.0f),
                    4
                )
            )
        graph.connect(workload.output, switch.newInput())

        advanceUntilIdle()

        assertAll(
            { assertEquals(4000, timeSource.millis()) { "Took enough time" } }
        )
    }
}
