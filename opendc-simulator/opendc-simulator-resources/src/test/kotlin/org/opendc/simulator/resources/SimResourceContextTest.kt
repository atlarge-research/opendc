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
        val consumer = object : SimResourceConsumer {
            override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
                return if (now == 0L) {
                    ctx.push(1.0)
                    1000
                } else {
                    ctx.close()
                    Long.MAX_VALUE
                }
            }
        }

        val logic = object : SimResourceProviderLogic {}
        val context = SimResourceContextImpl(null, interpreter, consumer, logic)

        interpreter.scheduleSync(interpreter.clock.millis(), context)
    }

    @Test
    fun testIntermediateFlush() = runBlockingSimulation {
        val interpreter = SimResourceInterpreterImpl(coroutineContext, clock)
        val consumer = object : SimResourceConsumer {
            override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
                return if (now == 0L) {
                    ctx.push(4.0)
                    1000
                } else {
                    ctx.close()
                    Long.MAX_VALUE
                }
            }
        }

        val logic = spyk(object : SimResourceProviderLogic {
            override fun onFinish(ctx: SimResourceControllableContext) {}
            override fun onConsume(ctx: SimResourceControllableContext, now: Long, limit: Double, duration: Long): Long = duration
        })
        val context = SimResourceContextImpl(null, interpreter, consumer, logic)

        context.start()
        delay(1) // Delay 1 ms to prevent hitting the fast path
        interpreter.scheduleSync(interpreter.clock.millis(), context)

        verify(exactly = 2) { logic.onConsume(any(), any(), any(), any()) }
    }

    @Test
    fun testIntermediateFlushIdle() = runBlockingSimulation {
        val interpreter = SimResourceInterpreterImpl(coroutineContext, clock)
        val consumer = object : SimResourceConsumer {
            override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
                return if (now == 0L) {
                    ctx.push(0.0)
                    10
                } else {
                    ctx.close()
                    Long.MAX_VALUE
                }
            }
        }

        val logic = spyk(object : SimResourceProviderLogic {})
        val context = SimResourceContextImpl(null, interpreter, consumer, logic)

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
        val consumer = object : SimResourceConsumer {
            override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
                return if (now == 0L) {
                    ctx.push(0.0)
                    1000
                } else {
                    ctx.close()
                    Long.MAX_VALUE
                }
            }
        }

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
        val consumer = spyk(object : SimResourceConsumer {
            override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
                return if (now == 0L) {
                    ctx.push(1.0)
                    1000
                } else {
                    ctx.close()
                    Long.MAX_VALUE
                }
            }
        })

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

        val consumer = spyk(object : SimResourceConsumer {
            override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
                ctx.close()
                return Long.MAX_VALUE
            }

            override fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {
                if (event == SimResourceEvent.Exit) throw IllegalStateException("onEvent")
            }

            override fun onFailure(ctx: SimResourceContext, cause: Throwable) {
                throw IllegalStateException("onFailure")
            }
        })

        val logic = object : SimResourceProviderLogic {}

        val context = SimResourceContextImpl(null, interpreter, consumer, logic)

        context.start()

        delay(1)

        verify(exactly = 1) { consumer.onFailure(any(), any()) }
    }
}
