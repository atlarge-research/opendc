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

package org.opendc.simulator.flow.source

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.opendc.simulator.flow.FlowSink
import org.opendc.simulator.flow.consume
import org.opendc.simulator.flow.internal.FlowEngineImpl
import org.opendc.simulator.kotlin.runSimulation

/**
 * A test suite for the [FixedFlowSource] class.
 */
internal class FixedFlowSourceTest {
    @Test
    fun testSmoke() = runSimulation {
        val scheduler = FlowEngineImpl(coroutineContext, clock)
        val provider = FlowSink(scheduler, 1.0)

        val consumer = FixedFlowSource(1.0, 1.0)

        provider.consume(consumer)
        assertEquals(1000, clock.millis())
    }

    @Test
    fun testUtilization() = runSimulation {
        val scheduler = FlowEngineImpl(coroutineContext, clock)
        val provider = FlowSink(scheduler, 1.0)

        val consumer = FixedFlowSource(1.0, 0.5)

        provider.consume(consumer)
        assertEquals(2000, clock.millis())
    }
}
