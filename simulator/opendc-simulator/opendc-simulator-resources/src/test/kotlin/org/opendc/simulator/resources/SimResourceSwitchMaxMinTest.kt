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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.opendc.simulator.resources.consumer.SimTraceConsumer
import org.opendc.simulator.utils.DelayControllerClockAdapter
import org.opendc.utils.TimerScheduler

/**
 * Test suite for the [SimResourceSwitch] implementations
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SimResourceSwitchMaxMinTest {
    @Test
    fun testSmoke() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)
        val switch = SimResourceSwitchMaxMin(clock)

        val sources = List(2) { SimResourceSource(2000.0, clock, scheduler) }
        sources.forEach { switch.addInput(it) }

        val provider = switch.addOutput(1000.0)

        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any()) } returns SimResourceCommand.Consume(1.0, 1.0) andThen SimResourceCommand.Exit

        try {
            provider.consume(consumer)
            yield()
        } finally {
            switch.close()
            scheduler.close()
        }
    }

    /**
     * Test overcommitting of resources via the hypervisor with a single VM.
     */
    @Test
    fun testOvercommittedSingle() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)

        val listener = object : SimResourceSwitchMaxMin.Listener {
            var totalRequestedWork = 0L
            var totalGrantedWork = 0L
            var totalOvercommittedWork = 0L

            override fun onSliceFinish(
                switch: SimResourceSwitchMaxMin,
                requestedWork: Long,
                grantedWork: Long,
                overcommittedWork: Long,
                interferedWork: Long,
                cpuUsage: Double,
                cpuDemand: Double
            ) {
                totalRequestedWork += requestedWork
                totalGrantedWork += grantedWork
                totalOvercommittedWork += overcommittedWork
            }
        }

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

        val switch = SimResourceSwitchMaxMin(clock, listener)
        val provider = switch.addOutput(3200.0)

        try {
            switch.addInput(SimResourceSource(3200.0, clock, scheduler))
            provider.consume(workload)
            yield()
        } finally {
            switch.close()
            scheduler.close()
        }

        assertAll(
            { assertEquals(1113300, listener.totalRequestedWork, "Requested Burst does not match") },
            { assertEquals(1023300, listener.totalGrantedWork, "Granted Burst does not match") },
            { assertEquals(90000, listener.totalOvercommittedWork, "Overcommissioned Burst does not match") },
            { assertEquals(1200000, currentTime) }
        )
    }

    /**
     * Test overcommitting of resources via the hypervisor with two VMs.
     */
    @Test
    fun testOvercommittedDual() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)

        val listener = object : SimResourceSwitchMaxMin.Listener {
            var totalRequestedWork = 0L
            var totalGrantedWork = 0L
            var totalOvercommittedWork = 0L

            override fun onSliceFinish(
                switch: SimResourceSwitchMaxMin,
                requestedWork: Long,
                grantedWork: Long,
                overcommittedWork: Long,
                interferedWork: Long,
                cpuUsage: Double,
                cpuDemand: Double
            ) {
                totalRequestedWork += requestedWork
                totalGrantedWork += grantedWork
                totalOvercommittedWork += overcommittedWork
            }
        }

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

        val switch = SimResourceSwitchMaxMin(clock, listener)
        val providerA = switch.addOutput(3200.0)
        val providerB = switch.addOutput(3200.0)

        try {
            switch.addInput(SimResourceSource(3200.0, clock, scheduler))

            coroutineScope {
                launch { providerA.consume(workloadA) }
                providerB.consume(workloadB)
            }

            yield()
        } finally {
            switch.close()
            scheduler.close()
        }
        assertAll(
            { assertEquals(2082000, listener.totalRequestedWork, "Requested Burst does not match") },
            { assertEquals(1062000, listener.totalGrantedWork, "Granted Burst does not match") },
            { assertEquals(1020000, listener.totalOvercommittedWork, "Overcommissioned Burst does not match") },
            { assertEquals(1200000, currentTime) }
        )
    }
}
