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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.resources.consumer.SimTraceConsumer
import org.opendc.simulator.utils.DelayControllerClockAdapter
import org.opendc.utils.TimerScheduler

/**
 * Test suite for the [SimResourceSwitchExclusive] class.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SimResourceSwitchExclusiveTest {
    /**
     * Test a trace workload.
     */
    @Test
    fun testTrace() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)

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
        val source = SimResourceSource(3200.0, clock, scheduler)

        switch.addInput(source)

        val provider = switch.addOutput(3200.0)
        val job = launch { source.speed.toList(speed) }

        try {
            provider.consume(workload)
            yield()
        } finally {
            job.cancel()
            provider.close()
        }

        assertAll(
            { assertEquals(listOf(0.0, 28.0, 3200.0, 0.0, 183.0, 0.0), speed) { "Correct speed" } },
            { assertEquals(5 * 60L * 4000, currentTime) { "Took enough time" } }
        )
    }

    /**
     * Test runtime workload on hypervisor.
     */
    @Test
    fun testRuntimeWorkload() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)

        val duration = 5 * 60L * 1000
        val workload = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { workload.onNext(any()) } returns SimResourceCommand.Consume(duration / 1000.0, 1.0) andThen SimResourceCommand.Exit

        val switch = SimResourceSwitchExclusive()
        val source = SimResourceSource(3200.0, clock, scheduler)

        switch.addInput(source)

        val provider = switch.addOutput(3200.0)

        try {
            provider.consume(workload)
            yield()
        } finally {
            provider.close()
        }
        assertEquals(duration, currentTime) { "Took enough time" }
    }

    /**
     * Test two workloads running sequentially.
     */
    @Test
    fun testTwoWorkloads() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)

        val duration = 5 * 60L * 1000
        val workload = object : SimResourceConsumer {
            var isFirst = true

            override fun onStart(ctx: SimResourceContext) {
                isFirst = true
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
        val source = SimResourceSource(3200.0, clock, scheduler)

        switch.addInput(source)

        val provider = switch.addOutput(3200.0)

        try {
            provider.consume(workload)
            yield()
            provider.consume(workload)
        } finally {
            provider.close()
        }
        assertEquals(duration * 2, currentTime) { "Took enough time" }
    }

    /**
     * Test concurrent workloads on the machine.
     */
    @Test
    fun testConcurrentWorkloadFails() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)

        val duration = 5 * 60L * 1000
        val workload = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { workload.onNext(any()) } returns SimResourceCommand.Consume(duration / 1000.0, 1.0) andThen SimResourceCommand.Exit

        val switch = SimResourceSwitchExclusive()
        val source = SimResourceSource(3200.0, clock, scheduler)

        switch.addInput(source)

        switch.addOutput(3200.0)
        assertThrows<IllegalStateException> { switch.addOutput(3200.0) }
    }
}
