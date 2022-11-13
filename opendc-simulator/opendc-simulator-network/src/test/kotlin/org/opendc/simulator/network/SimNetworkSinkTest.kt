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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.flow2.FlowEngine
import org.opendc.simulator.kotlin.runSimulation

/**
 * Test suite for the [SimNetworkSink] class.
 */
class SimNetworkSinkTest {
    @Test
    fun testInitialState() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()
        val sink = SimNetworkSink(graph, /*capacity*/ 100.0f)

        assertAll(
            { assertFalse(sink.isConnected) },
            { assertNull(sink.link) },
            { assertEquals(100.0f, sink.capacity) }
        )
    }

    @Test
    fun testDisconnectIdempotent() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()
        val sink = SimNetworkSink(graph, /*capacity*/ 100.0f)

        assertDoesNotThrow { sink.disconnect() }
        assertFalse(sink.isConnected)
    }

    @Test
    fun testConnectCircular() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()
        val sink = SimNetworkSink(graph, /*capacity*/ 100.0f)

        assertThrows<IllegalArgumentException> {
            sink.connect(sink)
        }
    }

    @Test
    fun testConnectAlreadyConnectedTarget() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()
        val sink = SimNetworkSink(graph, /*capacity*/ 100.0f)
        val source = mockk<SimNetworkPort>(relaxUnitFun = true)
        every { source.isConnected } returns true

        assertThrows<IllegalStateException> {
            sink.connect(source)
        }
    }

    @Test
    fun testConnectAlreadyConnected() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()
        val sink = SimNetworkSink(graph, /*capacity*/ 100.0f)
        val source1 = TestSource(graph)

        val source2 = mockk<SimNetworkPort>(relaxUnitFun = true)

        every { source2.isConnected } returns false

        sink.connect(source1)
        assertThrows<IllegalStateException> {
            sink.connect(source2)
        }
    }

    @Test
    fun testConnect() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()
        val sink = SimNetworkSink(graph, /*capacity*/ 100.0f)
        val source = TestSource(graph)

        sink.connect(source)

        yield()

        assertAll(
            { assertTrue(sink.isConnected) },
            { assertTrue(source.isConnected) },
            { assertEquals(100.0f, source.outlet.capacity) }
        )

        verify { source.logic.onUpdate(any(), any()) }
    }

    @Test
    fun testDisconnect() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()
        val sink = SimNetworkSink(graph, /*capacity*/ 100.0f)
        val source = TestSource(graph)

        sink.connect(source)
        sink.disconnect()

        yield()

        assertAll(
            { assertFalse(sink.isConnected) },
            { assertFalse(source.isConnected) },
            { assertEquals(0.0f, source.outlet.capacity) }
        )
    }
}
