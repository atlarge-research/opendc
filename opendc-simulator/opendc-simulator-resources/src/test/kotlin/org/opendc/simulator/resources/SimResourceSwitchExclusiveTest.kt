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

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.simulator.resources.consumer.SimSpeedConsumerAdapter
import org.opendc.simulator.resources.consumer.SimTraceConsumer
import org.opendc.simulator.resources.impl.SimResourceInterpreterImpl

/**
 * Test suite for the [SimResourceSwitchExclusive] class.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SimResourceSwitchExclusiveTest {
    /**
     * Test a trace workload.
     */
    @Test
    fun testTrace() = runBlockingSimulation {
        val scheduler = SimResourceInterpreterImpl(coroutineContext, clock)

        val speed = mutableListOf<Double>()

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

        val switch = SimResourceSwitchExclusive()
        val source = SimResourceSource(3200.0, scheduler)
        val forwarder = SimResourceForwarder()
        val adapter = SimSpeedConsumerAdapter(forwarder, speed::add)
        source.startConsumer(adapter)
        switch.addInput(forwarder)

        val provider = switch.newOutput()

        try {
            provider.consume(workload)
            yield()
        } finally {
            provider.close()
        }

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
        val scheduler = SimResourceInterpreterImpl(coroutineContext, clock)

        val duration = 5 * 60L * 1000
        val workload = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { workload.onNext(any()) } returns SimResourceCommand.Consume(duration / 1000.0, 1.0) andThen SimResourceCommand.Exit

        val switch = SimResourceSwitchExclusive()
        val source = SimResourceSource(3200.0, scheduler)

        switch.addInput(source)

        val provider = switch.newOutput()

        try {
            provider.consume(workload)
            yield()
        } finally {
            provider.close()
        }
        assertEquals(duration, clock.millis()) { "Took enough time" }
    }

    /**
     * Test two workloads running sequentially.
     */
    @Test
    fun testTwoWorkloads() = runBlockingSimulation {
        val scheduler = SimResourceInterpreterImpl(coroutineContext, clock)

        val duration = 5 * 60L * 1000
        val workload = object : SimResourceConsumer {
            var isFirst = true

            override fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {
                when (event) {
                    SimResourceEvent.Start -> isFirst = true
                    else -> {}
                }
            }

            override fun onNext(ctx: SimResourceContext): SimResourceCommand {
                return if (isFirst) {
                    isFirst = false
                    SimResourceCommand.Consume(duration / 1000.0, 1.0)
                } else {
                    SimResourceCommand.Exit
                }
            }
        }

        val switch = SimResourceSwitchExclusive()
        val source = SimResourceSource(3200.0, scheduler)

        switch.addInput(source)

        val provider = switch.newOutput()

        try {
            provider.consume(workload)
            yield()
            provider.consume(workload)
        } finally {
            provider.close()
        }
        assertEquals(duration * 2, clock.millis()) { "Took enough time" }
    }

    /**
     * Test concurrent workloads on the machine.
     */
    @Test
    fun testConcurrentWorkloadFails() = runBlockingSimulation {
        val scheduler = SimResourceInterpreterImpl(coroutineContext, clock)

        val duration = 5 * 60L * 1000
        val workload = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { workload.onNext(any()) } returns SimResourceCommand.Consume(duration / 1000.0, 1.0) andThen SimResourceCommand.Exit

        val switch = SimResourceSwitchExclusive()
        val source = SimResourceSource(3200.0, scheduler)

        switch.addInput(source)

        switch.newOutput()
        assertThrows<IllegalStateException> { switch.newOutput() }
    }
}
