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
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.utils.DelayControllerClockAdapter
import org.opendc.utils.TimerScheduler

/**
 * A test suite for the [SimResourceForwarder] class.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SimResourceForwarderTest {

    data class SimCpu(val speed: Double) : SimResource {
        override val capacity: Double
            get() = speed
    }

    @Test
    fun testExitImmediately() = runBlockingTest {
        val forwarder = SimResourceForwarder(SimCpu(1000.0))
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)
        val source = SimResourceSource(SimCpu(2000.0), clock, scheduler)

        launch {
            source.consume(forwarder)
            source.close()
        }

        forwarder.consume(object : SimResourceConsumer<SimCpu> {
            override fun onNext(
                ctx: SimResourceContext<SimCpu>
            ): SimResourceCommand {
                return SimResourceCommand.Exit
            }
        })

        forwarder.close()
        scheduler.close()
    }

    @Test
    fun testExit() = runBlockingTest {
        val forwarder = SimResourceForwarder(SimCpu(1000.0))
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)
        val source = SimResourceSource(SimCpu(2000.0), clock, scheduler)

        launch {
            source.consume(forwarder)
            source.close()
        }

        forwarder.consume(object : SimResourceConsumer<SimCpu> {
            var isFirst = true

            override fun onNext(
                ctx: SimResourceContext<SimCpu>
            ): SimResourceCommand {
                return if (isFirst) {
                    isFirst = false
                    SimResourceCommand.Consume(10.0, 1.0)
                } else {
                    SimResourceCommand.Exit
                }
            }
        })

        forwarder.close()
    }

    @Test
    fun testState() = runBlockingTest {
        val forwarder = SimResourceForwarder(SimCpu(1000.0))
        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onNext(
                ctx: SimResourceContext<SimCpu>
            ): SimResourceCommand = SimResourceCommand.Exit
        }

        assertEquals(SimResourceState.Pending, forwarder.state)

        forwarder.startConsumer(consumer)
        assertEquals(SimResourceState.Active, forwarder.state)

        assertThrows<IllegalStateException> { forwarder.startConsumer(consumer) }

        forwarder.cancel()
        assertEquals(SimResourceState.Pending, forwarder.state)

        forwarder.close()
        assertEquals(SimResourceState.Stopped, forwarder.state)
    }

    @Test
    fun testCancelPendingDelegate() = runBlockingTest {
        val forwarder = SimResourceForwarder(SimCpu(1000.0))

        val consumer = mockk<SimResourceConsumer<SimCpu>>(relaxUnitFun = true)
        every { consumer.onNext(any()) } returns SimResourceCommand.Exit

        forwarder.startConsumer(consumer)
        forwarder.cancel()

        verify(exactly = 0) { consumer.onFinish(any(), null) }
    }

    @Test
    fun testCancelStartedDelegate() = runBlockingTest {
        val forwarder = SimResourceForwarder(SimCpu(1000.0))
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)
        val source = SimResourceSource(SimCpu(2000.0), clock, scheduler)

        val consumer = mockk<SimResourceConsumer<SimCpu>>(relaxUnitFun = true)
        every { consumer.onNext(any()) } returns SimResourceCommand.Idle(10)

        source.startConsumer(forwarder)
        yield()
        forwarder.startConsumer(consumer)
        yield()
        forwarder.cancel()

        verify(exactly = 1) { consumer.onStart(any()) }
        verify(exactly = 1) { consumer.onFinish(any(), null) }
    }

    @Test
    fun testCancelPropagation() = runBlockingTest {
        val forwarder = SimResourceForwarder(SimCpu(1000.0))
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)
        val source = SimResourceSource(SimCpu(2000.0), clock, scheduler)

        val consumer = mockk<SimResourceConsumer<SimCpu>>(relaxUnitFun = true)
        every { consumer.onNext(any()) } returns SimResourceCommand.Idle(10)

        source.startConsumer(forwarder)
        yield()
        forwarder.startConsumer(consumer)
        yield()
        source.cancel()

        verify(exactly = 1) { consumer.onStart(any()) }
        verify(exactly = 1) { consumer.onFinish(any(), null) }
    }
}
