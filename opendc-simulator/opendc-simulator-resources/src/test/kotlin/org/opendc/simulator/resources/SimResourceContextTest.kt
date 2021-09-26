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

import io.mockk.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.simulator.resources.impl.SimResourceContextImpl
import org.opendc.simulator.resources.impl.SimResourceInterpreterImpl

/**
 * A test suite for the [SimResourceContextImpl] class.
 */
class SimResourceContextTest {
    @Test
    fun testFlushWithoutCommand() = runBlockingSimulation {
        val interpreter = SimResourceInterpreterImpl(coroutineContext, clock)
        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any(), any(), any()) } returns SimResourceCommand.Consume(1.0) andThen SimResourceCommand.Exit

        val logic = object : SimResourceProviderLogic {}
        val context = SimResourceContextImpl(null, interpreter, consumer, logic)

        context.doUpdate(interpreter.clock.millis())
    }

    @Test
    fun testIntermediateFlush() = runBlockingSimulation {
        val interpreter = SimResourceInterpreterImpl(coroutineContext, clock)
        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any(), any(), any()) } returns SimResourceCommand.Consume(1.0) andThen SimResourceCommand.Exit

        val logic = spyk(object : SimResourceProviderLogic {
            override fun onFinish(ctx: SimResourceControllableContext) {}
            override fun onConsume(ctx: SimResourceControllableContext, now: Long, limit: Double, duration: Long): Long = duration
        })
        val context = spyk(SimResourceContextImpl(null, interpreter, consumer, logic))

        context.start()
        delay(1) // Delay 1 ms to prevent hitting the fast path
        context.doUpdate(interpreter.clock.millis())

        verify(exactly = 2) { logic.onConsume(any(), any(), any(), any()) }
    }

    @Test
    fun testIntermediateFlushIdle() = runBlockingSimulation {
        val interpreter = SimResourceInterpreterImpl(coroutineContext, clock)
        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any(), any(), any()) } returns SimResourceCommand.Consume(0.0, 10) andThen SimResourceCommand.Exit

        val logic = spyk(object : SimResourceProviderLogic {})
        val context = spyk(SimResourceContextImpl(null, interpreter, consumer, logic))

        context.start()
        delay(5)
        context.invalidate()
        delay(5)
        context.invalidate()

        assertAll(
            { verify(exactly = 2) { logic.onConsume(any(), any(), 0.0, any()) } },
            { verify(exactly = 1) { logic.onFinish(any()) } }
        )
    }

    @Test
    fun testDoubleStart() = runBlockingSimulation {
        val interpreter = SimResourceInterpreterImpl(coroutineContext, clock)
        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any(), any(), any()) } returns SimResourceCommand.Consume(0.0, 10) andThen SimResourceCommand.Exit

        val logic = object : SimResourceProviderLogic {}
        val context = SimResourceContextImpl(null, interpreter, consumer, logic)

        context.start()

        assertThrows<IllegalStateException> {
            context.start()
        }
    }

    @Test
    fun testIdempotentCapacityChange() = runBlockingSimulation {
        val interpreter = SimResourceInterpreterImpl(coroutineContext, clock)
        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any(), any(), any()) } returns SimResourceCommand.Consume(1.0) andThen SimResourceCommand.Exit

        val logic = object : SimResourceProviderLogic {}

        val context = SimResourceContextImpl(null, interpreter, consumer, logic)
        context.capacity = 4200.0
        context.start()
        context.capacity = 4200.0

        verify(exactly = 0) { consumer.onEvent(any(), SimResourceEvent.Capacity) }
    }

    @Test
    fun testFailureNoInfiniteLoop() = runBlockingSimulation {
        val interpreter = SimResourceInterpreterImpl(coroutineContext, clock)
        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any(), any(), any()) } returns SimResourceCommand.Exit
        every { consumer.onEvent(any(), SimResourceEvent.Exit) } throws IllegalStateException("onEvent")
        every { consumer.onFailure(any(), any()) } throws IllegalStateException("onFailure")

        val logic = spyk(object : SimResourceProviderLogic {})

        val context = SimResourceContextImpl(null, interpreter, consumer, logic)

        context.start()

        delay(1)

        verify(exactly = 1) { consumer.onFailure(any(), any()) }
    }
}
