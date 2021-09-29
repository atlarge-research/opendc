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
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.simulator.flow.*
import org.opendc.simulator.flow.source.FixedFlowSource

/**
 * Test suite for the [SimNetworkSink] class.
 */
class SimNetworkSinkTest {
    @Test
    fun testInitialState() = runBlockingSimulation {
        val engine = FlowEngine(coroutineContext, clock)
        val sink = SimNetworkSink(engine, capacity = 100.0)

        assertFalse(sink.isConnected)
        assertNull(sink.link)
        assertEquals(100.0, sink.capacity)
    }

    @Test
    fun testDisconnectIdempotent() = runBlockingSimulation {
        val engine = FlowEngine(coroutineContext, clock)
        val sink = SimNetworkSink(engine, capacity = 100.0)

        assertDoesNotThrow { sink.disconnect() }
        assertFalse(sink.isConnected)
    }

    @Test
    fun testConnectCircular() = runBlockingSimulation {
        val engine = FlowEngine(coroutineContext, clock)
        val sink = SimNetworkSink(engine, capacity = 100.0)

        assertThrows<IllegalArgumentException> {
            sink.connect(sink)
        }
    }

    @Test
    fun testConnectAlreadyConnectedTarget() = runBlockingSimulation {
        val engine = FlowEngine(coroutineContext, clock)
        val sink = SimNetworkSink(engine, capacity = 100.0)
        val source = mockk<SimNetworkPort>(relaxUnitFun = true)
        every { source.isConnected } returns true

        assertThrows<IllegalStateException> {
            sink.connect(source)
        }
    }

    @Test
    fun testConnectAlreadyConnected() = runBlockingSimulation {
        val engine = FlowEngine(coroutineContext, clock)
        val sink = SimNetworkSink(engine, capacity = 100.0)
        val source1 = Source(engine)

        val source2 = mockk<SimNetworkPort>(relaxUnitFun = true)

        every { source2.isConnected } returns false

        sink.connect(source1)
        assertThrows<IllegalStateException> {
            sink.connect(source2)
        }
    }

    @Test
    fun testConnect() = runBlockingSimulation {
        val engine = FlowEngine(coroutineContext, clock)
        val sink = SimNetworkSink(engine, capacity = 100.0)
        val source = spyk(Source(engine))
        val consumer = source.consumer

        sink.connect(source)

        assertTrue(sink.isConnected)
        assertTrue(source.isConnected)

        verify { source.createConsumer() }
        verify { consumer.onEvent(any(), any(), FlowEvent.Start) }
    }

    @Test
    fun testDisconnect() = runBlockingSimulation {
        val engine = FlowEngine(coroutineContext, clock)
        val sink = SimNetworkSink(engine, capacity = 100.0)
        val source = spyk(Source(engine))
        val consumer = source.consumer

        sink.connect(source)
        sink.disconnect()

        assertFalse(sink.isConnected)
        assertFalse(source.isConnected)

        verify { consumer.onEvent(any(), any(), FlowEvent.Exit) }
    }

    private class Source(engine: FlowEngine) : SimNetworkPort() {
        val consumer = spyk(FixedFlowSource(Double.POSITIVE_INFINITY, utilization = 0.8))

        public override fun createConsumer(): FlowSource = consumer

        override val provider: FlowConsumer = FlowSink(engine, 0.0)
    }
}
