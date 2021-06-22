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

import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.simulator.resources.*
import org.opendc.simulator.resources.consumer.SimWorkConsumer

/**
 * Test suite for the [SimNetworkSwitchVirtual] class.
 */
class SimNetworkSwitchVirtualTest {
    @Test
    fun testConnect() = runBlockingSimulation {
        val interpreter = SimResourceInterpreter(coroutineContext, clock)
        val sink = SimNetworkSink(interpreter, capacity = 100.0)
        val source = spyk(Source(interpreter))
        val switch = SimNetworkSwitchVirtual(interpreter)
        val consumer = source.consumer

        switch.newPort().connect(sink)
        switch.newPort().connect(source)

        assertTrue(sink.isConnected)
        assertTrue(source.isConnected)

        verify { source.createConsumer() }
        verify { consumer.onEvent(any(), SimResourceEvent.Start) }
    }

    @Test
    fun testConnectClosedPort() = runBlockingSimulation {
        val interpreter = SimResourceInterpreter(coroutineContext, clock)
        val sink = SimNetworkSink(interpreter, capacity = 100.0)
        val switch = SimNetworkSwitchVirtual(interpreter)

        val port = switch.newPort()
        port.close()

        assertThrows<IllegalStateException> {
            port.connect(sink)
        }
    }

    private class Source(interpreter: SimResourceInterpreter) : SimNetworkPort() {
        val consumer = spyk(SimWorkConsumer(Double.POSITIVE_INFINITY, utilization = 0.8))

        public override fun createConsumer(): SimResourceConsumer = consumer

        override val provider: SimResourceProvider = SimResourceSource(0.0, interpreter)
    }
}
