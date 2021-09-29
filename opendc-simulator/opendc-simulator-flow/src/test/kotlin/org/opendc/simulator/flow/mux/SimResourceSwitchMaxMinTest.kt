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

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.simulator.flow.FlowSink
import org.opendc.simulator.flow.consume
import org.opendc.simulator.flow.internal.FlowEngineImpl
import org.opendc.simulator.flow.source.FixedFlowSource
import org.opendc.simulator.flow.source.TraceFlowSource

/**
 * Test suite for the [FlowMultiplexer] implementations
 */
internal class SimResourceSwitchMaxMinTest {
    @Test
    fun testSmoke() = runBlockingSimulation {
        val scheduler = FlowEngineImpl(coroutineContext, clock)
        val switch = MaxMinFlowMultiplexer(scheduler)

        val sources = List(2) { FlowSink(scheduler, 2000.0) }
        sources.forEach { switch.addOutput(it) }

        val provider = switch.newInput()
        val consumer = FixedFlowSource(2000.0, 1.0)

        try {
            provider.consume(consumer)
            yield()
        } finally {
            switch.clear()
        }
    }

    /**
     * Test overcommitting of resources via the hypervisor with a single VM.
     */
    @Test
    fun testOvercommittedSingle() = runBlockingSimulation {
        val scheduler = FlowEngineImpl(coroutineContext, clock)

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

        val switch = MaxMinFlowMultiplexer(scheduler)
        val provider = switch.newInput()

        try {
            switch.addOutput(FlowSink(scheduler, 3200.0))
            provider.consume(workload)
            yield()
        } finally {
            switch.clear()
        }

        assertAll(
            { assertEquals(1113300.0, switch.counters.demand, "Requested work does not match") },
            { assertEquals(1023300.0, switch.counters.actual, "Actual work does not match") },
            { assertEquals(90000.0, switch.counters.overcommit, "Overcommitted work does not match") },
            { assertEquals(1200000, clock.millis()) }
        )
    }

    /**
     * Test overcommitting of resources via the hypervisor with two VMs.
     */
    @Test
    fun testOvercommittedDual() = runBlockingSimulation {
        val scheduler = FlowEngineImpl(coroutineContext, clock)

        val duration = 5 * 60L
        val workloadA =
            TraceFlowSource(
                sequenceOf(
                    TraceFlowSource.Fragment(duration * 1000, 28.0),
                    TraceFlowSource.Fragment(duration * 1000, 3500.0),
                    TraceFlowSource.Fragment(duration * 1000, 0.0),
                    TraceFlowSource.Fragment(duration * 1000, 183.0)
                ),
            )
        val workloadB =
            TraceFlowSource(
                sequenceOf(
                    TraceFlowSource.Fragment(duration * 1000, 28.0),
                    TraceFlowSource.Fragment(duration * 1000, 3100.0),
                    TraceFlowSource.Fragment(duration * 1000, 0.0),
                    TraceFlowSource.Fragment(duration * 1000, 73.0)
                )
            )

        val switch = MaxMinFlowMultiplexer(scheduler)
        val providerA = switch.newInput()
        val providerB = switch.newInput()

        try {
            switch.addOutput(FlowSink(scheduler, 3200.0))

            coroutineScope {
                launch { providerA.consume(workloadA) }
                providerB.consume(workloadB)
            }

            yield()
        } finally {
            switch.clear()
        }
        assertAll(
            { assertEquals(2073600.0, switch.counters.demand, "Requested work does not match") },
            { assertEquals(1053600.0, switch.counters.actual, "Granted work does not match") },
            { assertEquals(1020000.0, switch.counters.overcommit, "Overcommitted work does not match") },
            { assertEquals(1200000, clock.millis()) }
        )
    }
}
