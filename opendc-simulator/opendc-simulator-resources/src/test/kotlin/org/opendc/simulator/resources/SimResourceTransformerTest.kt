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
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.simulator.resources.consumer.SimWorkConsumer

/**
 * A test suite for the [SimResourceTransformer] class.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SimResourceTransformerTest {
    @Test
    fun testExitImmediately() = runBlockingSimulation {
        val forwarder = SimResourceForwarder()
        val scheduler = SimResourceSchedulerTrampoline(coroutineContext, clock)
        val source = SimResourceSource(2000.0, scheduler)

        launch {
            source.consume(forwarder)
            source.close()
        }

        forwarder.consume(object : SimResourceConsumer {
            override fun onNext(ctx: SimResourceContext): SimResourceCommand {
                return SimResourceCommand.Exit
            }
        })

        forwarder.close()
    }

    @Test
    fun testExit() = runBlockingSimulation {
        val forwarder = SimResourceForwarder()
        val scheduler = SimResourceSchedulerTrampoline(coroutineContext, clock)
        val source = SimResourceSource(2000.0, scheduler)

        launch {
            source.consume(forwarder)
            source.close()
        }

        forwarder.consume(object : SimResourceConsumer {
            var isFirst = true

            override fun onNext(ctx: SimResourceContext): SimResourceCommand {
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
    fun testState() = runBlockingSimulation {
        val forwarder = SimResourceForwarder()
        val consumer = object : SimResourceConsumer {
            override fun onNext(ctx: SimResourceContext): SimResourceCommand = SimResourceCommand.Exit
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
    fun testCancelPendingDelegate() = runBlockingSimulation {
        val forwarder = SimResourceForwarder()

        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any()) } returns SimResourceCommand.Exit

        forwarder.startConsumer(consumer)
        forwarder.cancel()

        verify(exactly = 0) { consumer.onEvent(any(), SimResourceEvent.Exit) }
    }

    @Test
    fun testCancelStartedDelegate() = runBlockingSimulation {
        val forwarder = SimResourceForwarder()
        val scheduler = SimResourceSchedulerTrampoline(coroutineContext, clock)
        val source = SimResourceSource(2000.0, scheduler)

        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any()) } returns SimResourceCommand.Idle(10)

        source.startConsumer(forwarder)
        yield()
        forwarder.startConsumer(consumer)
        yield()
        forwarder.cancel()

        verify(exactly = 1) { consumer.onEvent(any(), SimResourceEvent.Start) }
        verify(exactly = 1) { consumer.onEvent(any(), SimResourceEvent.Exit) }
    }

    @Test
    fun testCancelPropagation() = runBlockingSimulation {
        val forwarder = SimResourceForwarder()
        val scheduler = SimResourceSchedulerTrampoline(coroutineContext, clock)
        val source = SimResourceSource(2000.0, scheduler)

        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any()) } returns SimResourceCommand.Idle(10)

        source.startConsumer(forwarder)
        yield()
        forwarder.startConsumer(consumer)
        yield()
        source.cancel()

        verify(exactly = 1) { consumer.onEvent(any(), SimResourceEvent.Start) }
        verify(exactly = 1) { consumer.onEvent(any(), SimResourceEvent.Exit) }
    }

    @Test
    fun testExitPropagation() = runBlockingSimulation {
        val forwarder = SimResourceForwarder(isCoupled = true)
        val scheduler = SimResourceSchedulerTrampoline(coroutineContext, clock)
        val source = SimResourceSource(2000.0, scheduler)

        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any()) } returns SimResourceCommand.Exit

        source.startConsumer(forwarder)
        forwarder.consume(consumer)
        yield()

        assertEquals(SimResourceState.Pending, source.state)
    }

    @Test
    fun testAdjustCapacity() = runBlockingSimulation {
        val forwarder = SimResourceForwarder()
        val scheduler = SimResourceSchedulerTrampoline(coroutineContext, clock)
        val source = SimResourceSource(1.0, scheduler)

        val consumer = spyk(SimWorkConsumer(2.0, 1.0))
        source.startConsumer(forwarder)

        coroutineScope {
            launch { forwarder.consume(consumer) }
            delay(1000)
            source.capacity = 0.5
        }

        assertEquals(3000, clock.millis())
        verify(exactly = 1) { consumer.onEvent(any(), SimResourceEvent.Capacity) }
    }

    @Test
    fun testTransformExit() = runBlockingSimulation {
        val forwarder = SimResourceTransformer { _, _ -> SimResourceCommand.Exit }
        val scheduler = SimResourceSchedulerTrampoline(coroutineContext, clock)
        val source = SimResourceSource(1.0, scheduler)

        val consumer = spyk(SimWorkConsumer(2.0, 1.0))
        source.startConsumer(forwarder)
        forwarder.consume(consumer)

        assertEquals(0, clock.millis())
        verify(exactly = 1) { consumer.onNext(any()) }
    }
}
