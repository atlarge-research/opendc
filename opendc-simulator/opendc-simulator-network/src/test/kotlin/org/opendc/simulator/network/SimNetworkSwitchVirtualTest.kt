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

package org.opendc.simulator.network

import io.mockk.verify
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.flow2.FlowEngine
import org.opendc.simulator.kotlin.runSimulation

/**
 * Test suite for the [SimNetworkSwitchVirtual] class.
 */
class SimNetworkSwitchVirtualTest {
    @Test
    fun testConnect() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()
        val sink = SimNetworkSink(graph, /*capacity*/ 100.0f)
        val source = TestSource(graph)
        val switch = SimNetworkSwitchVirtual(graph)

        switch.newPort().connect(sink)
        switch.newPort().connect(source)

        yield()

        assertAll(
            { assertTrue(sink.isConnected) },
            { assertTrue(source.isConnected) },
            { assertEquals(100.0f, source.outlet.capacity) }
        )

        verify { source.logic.onUpdate(any(), any()) }
    }

    @Test
    fun testConnectClosedPort() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()
        val sink = SimNetworkSink(graph, /*capacity*/ 100.0f)
        val switch = SimNetworkSwitchVirtual(graph)

        val port = switch.newPort()
        port.close()

        assertThrows<IllegalStateException> {
            port.connect(sink)
        }
    }
}
