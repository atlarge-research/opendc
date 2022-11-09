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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.opendc.simulator.flow2.FlowEngine
import org.opendc.simulator.flow2.sink.SimpleFlowSink
import org.opendc.simulator.flow2.source.SimpleFlowSource
import org.opendc.simulator.kotlin.runSimulation

/**
 * Test suite for the [MaxMinFlowMultiplexer] class.
 */
class MaxMinFlowMultiplexerTest {
    @Test
    fun testSmoke() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()
        val switch = MaxMinFlowMultiplexer(graph)

        val sinks = List(2) { SimpleFlowSink(graph, 2000.0f) }
        for (source in sinks) {
            graph.connect(switch.newOutput(), source.input)
        }

        val source = SimpleFlowSource(graph, 2000.0f, 1.0f)
        graph.connect(source.output, switch.newInput())

        advanceUntilIdle()

        assertEquals(500, timeSource.millis())
    }
}
