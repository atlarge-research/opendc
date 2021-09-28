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

import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.simulator.resources.consumer.SimWorkConsumer
import org.opendc.simulator.resources.impl.SimResourceInterpreterImpl

/**
 * A test suite for the [SimResourceTransformer] class.
 */
internal class SimResourceTransformerTest {
    @Test
    fun testCancelImmediately() = runBlockingSimulation {
        val forwarder = SimResourceForwarder()
        val scheduler = SimResourceInterpreterImpl(coroutineContext, clock)
        val source = SimResourceSource(2000.0, scheduler)

        launch { source.consume(forwarder) }

        forwarder.consume(object : SimResourceConsumer {
            override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
                ctx.close()
                return Long.MAX_VALUE
            }
        })

        forwarder.close()
        source.cancel()
    }

    @Test
    fun testCancel() = runBlockingSimulation {
        val forwarder = SimResourceForwarder()
        val scheduler = SimResourceInterpreterImpl(coroutineContext, clock)
        val source = SimResourceSource(2000.0, scheduler)

        launch { source.consume(forwarder) }

        forwarder.consume(object : SimResourceConsumer {
            var isFirst = true

            override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
                return if (isFirst) {
                    isFirst = false
                    ctx.push(1.0)
                    10 * 1000
                } else {
                    ctx.close()
                    Long.MAX_VALUE
                }
            }
        })

        forwarder.close()
        source.cancel()
    }

    @Test
    fun testState() = runBlockingSimulation {
        val forwarder = SimResourceForwarder()
        val consumer = object : SimResourceConsumer {
            override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
                ctx.close()
                return Long.MAX_VALUE
            }
        }

        assertFalse(forwarder.isActive)

        forwarder.startConsumer(consumer)
        assertTrue(forwarder.isActive)

        assertThrows<IllegalStateException> { forwarder.startConsumer(consumer) }

        forwarder.cancel()
        assertFalse(forwarder.isActive)

        forwarder.close()
        assertFalse(forwarder.isActive)
    }

    @Test
    fun testCancelPendingDelegate() = runBlockingSimulation {
        val forwarder = SimResourceForwarder()

        val consumer = spyk(object : SimResourceConsumer {
            override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
                ctx.close()
                return Long.MAX_VALUE
            }
        })

        forwarder.startConsumer(consumer)
        forwarder.cancel()

        verify(exactly = 0) { consumer.onEvent(any(), SimResourceEvent.Exit) }
    }

    @Test
    fun testCancelStartedDelegate() = runBlockingSimulation {
        val forwarder = SimResourceForwarder()
        val scheduler = SimResourceInterpreterImpl(coroutineContext, clock)
        val source = SimResourceSource(2000.0, scheduler)

        val consumer = spyk(SimWorkConsumer(2000.0, 1.0))

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
        val scheduler = SimResourceInterpreterImpl(coroutineContext, clock)
        val source = SimResourceSource(2000.0, scheduler)

        val consumer = spyk(SimWorkConsumer(2000.0, 1.0))

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
        val scheduler = SimResourceInterpreterImpl(coroutineContext, clock)
        val source = SimResourceSource(2000.0, scheduler)

        val consumer = object : SimResourceConsumer {
            override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
                ctx.close()
                return Long.MAX_VALUE
            }
        }

        source.startConsumer(forwarder)
        forwarder.consume(consumer)
        yield()

        assertFalse(forwarder.isActive)
    }

    @Test
    fun testAdjustCapacity() = runBlockingSimulation {
        val forwarder = SimResourceForwarder()
        val scheduler = SimResourceInterpreterImpl(coroutineContext, clock)
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
        val forwarder = SimResourceTransformer { ctx, _ -> ctx.close(); Long.MAX_VALUE }
        val scheduler = SimResourceInterpreterImpl(coroutineContext, clock)
        val source = SimResourceSource(1.0, scheduler)

        val consumer = spyk(SimWorkConsumer(2.0, 1.0))
        source.startConsumer(forwarder)
        forwarder.consume(consumer)

        assertEquals(0, clock.millis())
        verify(exactly = 1) { consumer.onNext(any(), any(), any()) }
    }

    @Test
    fun testCounters() = runBlockingSimulation {
        val forwarder = SimResourceForwarder()
        val scheduler = SimResourceInterpreterImpl(coroutineContext, clock)
        val source = SimResourceSource(1.0, scheduler)

        val consumer = SimWorkConsumer(2.0, 1.0)
        source.startConsumer(forwarder)

        forwarder.consume(consumer)

        assertEquals(source.counters.actual, forwarder.counters.actual) { "Actual work" }
        assertEquals(source.counters.demand, forwarder.counters.demand) { "Work demand" }
        assertEquals(source.counters.overcommit, forwarder.counters.overcommit) { "Overcommitted work" }
        assertEquals(2000, clock.millis())
    }
}
