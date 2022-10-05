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

package org.opendc.simulator.flow.mux

import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.flow.*
import org.opendc.simulator.flow.internal.FlowEngineImpl
import org.opendc.simulator.flow.source.FixedFlowSource
import org.opendc.simulator.flow.source.FlowSourceRateAdapter
import org.opendc.simulator.flow.source.TraceFlowSource
import org.opendc.simulator.kotlin.runBlockingSimulation

/**
 * Test suite for the [ForwardingFlowMultiplexer] class.
 */
internal class ForwardingFlowMultiplexerTest {
    /**
     * Test a trace workload.
     */
    @Test
    fun testTrace() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)

        val speed = mutableListOf<Double>()

        val duration = 5 * 60L
        val workload =
            TraceFlowSource(
                sequenceOf(
                    TraceFlowSource.Fragment(duration * 1000, 28.0),
                    TraceFlowSource.Fragment(duration * 1000, 3500.0),
                    TraceFlowSource.Fragment(duration * 1000, 0.0),
                    TraceFlowSource.Fragment(duration * 1000, 183.0)
                ),
            )

        val switch = ForwardingFlowMultiplexer(engine)
        val source = FlowSink(engine, 3200.0)
        val forwarder = FlowForwarder(engine)
        val adapter = FlowSourceRateAdapter(forwarder, speed::add)
        source.startConsumer(adapter)
        forwarder.startConsumer(switch.newOutput())

        val provider = switch.newInput()
        provider.consume(workload)
        yield()

        assertAll(
            { assertEquals(listOf(0.0, 28.0, 3200.0, 0.0, 183.0, 0.0), speed) { "Correct speed" } },
            { assertEquals(5 * 60L * 4000, clock.millis()) { "Took enough time" } }
        )
    }

    /**
     * Test runtime workload on hypervisor.
     */
    @Test
    fun testRuntimeWorkload() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)

        val duration = 5 * 60L * 1000
        val workload = FixedFlowSource(duration * 3.2, 1.0)

        val switch = ForwardingFlowMultiplexer(engine)
        val source = FlowSink(engine, 3200.0)

        source.startConsumer(switch.newOutput())

        val provider = switch.newInput()
        provider.consume(workload)
        yield()

        assertEquals(duration, clock.millis()) { "Took enough time" }
    }

    /**
     * Test two workloads running sequentially.
     */
    @Test
    fun testTwoWorkloads() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)

        val duration = 5 * 60L * 1000
        val workload = object : FlowSource {
            var isFirst = true

            override fun onStart(conn: FlowConnection, now: Long) {
                isFirst = true
            }

            override fun onPull(conn: FlowConnection, now: Long): Long {
                return if (isFirst) {
                    isFirst = false
                    conn.push(1.0)
                    duration
                } else {
                    conn.close()
                    Long.MAX_VALUE
                }
            }
        }

        val switch = ForwardingFlowMultiplexer(engine)
        val source = FlowSink(engine, 3200.0)

        source.startConsumer(switch.newOutput())

        val provider = switch.newInput()
        provider.consume(workload)
        yield()
        provider.consume(workload)
        assertEquals(duration * 2, clock.millis()) { "Took enough time" }
    }

    /**
     * Test concurrent workloads on the machine.
     */
    @Test
    fun testConcurrentWorkloadFails() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)

        val switch = ForwardingFlowMultiplexer(engine)
        val source = FlowSink(engine, 3200.0)

        source.startConsumer(switch.newOutput())

        switch.newInput()
        assertThrows<IllegalStateException> { switch.newInput() }
    }
}
