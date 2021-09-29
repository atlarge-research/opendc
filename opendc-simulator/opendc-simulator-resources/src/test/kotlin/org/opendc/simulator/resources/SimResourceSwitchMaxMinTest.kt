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

package org.opendc.simulator.resources

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.simulator.resources.consumer.SimTraceConsumer
import org.opendc.simulator.resources.consumer.SimWorkConsumer
import org.opendc.simulator.resources.impl.SimResourceInterpreterImpl

/**
 * Test suite for the [SimResourceSwitch] implementations
 */
internal class SimResourceSwitchMaxMinTest {
    @Test
    fun testSmoke() = runBlockingSimulation {
        val scheduler = SimResourceInterpreterImpl(coroutineContext, clock)
        val switch = SimResourceSwitchMaxMin(scheduler)

        val sources = List(2) { SimResourceSource(2000.0, scheduler) }
        sources.forEach { switch.addInput(it) }

        val provider = switch.newOutput()
        val consumer = SimWorkConsumer(2000.0, 1.0)

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
        val scheduler = SimResourceInterpreterImpl(coroutineContext, clock)

        val duration = 5 * 60L
        val workload =
            SimTraceConsumer(
                sequenceOf(
                    SimTraceConsumer.Fragment(duration * 1000, 28.0),
                    SimTraceConsumer.Fragment(duration * 1000, 3500.0),
                    SimTraceConsumer.Fragment(duration * 1000, 0.0),
                    SimTraceConsumer.Fragment(duration * 1000, 183.0)
                ),
            )

        val switch = SimResourceSwitchMaxMin(scheduler)
        val provider = switch.newOutput()

        try {
            switch.addInput(SimResourceSource(3200.0, scheduler))
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
        val scheduler = SimResourceInterpreterImpl(coroutineContext, clock)

        val duration = 5 * 60L
        val workloadA =
            SimTraceConsumer(
                sequenceOf(
                    SimTraceConsumer.Fragment(duration * 1000, 28.0),
                    SimTraceConsumer.Fragment(duration * 1000, 3500.0),
                    SimTraceConsumer.Fragment(duration * 1000, 0.0),
                    SimTraceConsumer.Fragment(duration * 1000, 183.0)
                ),
            )
        val workloadB =
            SimTraceConsumer(
                sequenceOf(
                    SimTraceConsumer.Fragment(duration * 1000, 28.0),
                    SimTraceConsumer.Fragment(duration * 1000, 3100.0),
                    SimTraceConsumer.Fragment(duration * 1000, 0.0),
                    SimTraceConsumer.Fragment(duration * 1000, 73.0)
                )
            )

        val switch = SimResourceSwitchMaxMin(scheduler)
        val providerA = switch.newOutput()
        val providerB = switch.newOutput()

        try {
            switch.addInput(SimResourceSource(3200.0, scheduler))

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
