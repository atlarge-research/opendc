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

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.opendc.simulator.utils.DelayControllerClockAdapter
import org.opendc.utils.TimerScheduler

/**
 * A test suite for the [SimResourceSource] class.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SimResourceSourceTest {
    data class SimCpu(val speed: Double) : SimResource {
        override val capacity: Double
            get() = speed
    }

    @Test
    fun testSpeed() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)
        val provider = SimResourceSource(SimCpu(4200.0), clock, scheduler)

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Consume(1000 * ctx.resource.speed, ctx.resource.speed)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                return SimResourceCommand.Exit
            }
        }

        try {
            val res = mutableListOf<Double>()
            val job = launch { provider.speed.toList(res) }

            provider.consume(consumer)

            job.cancel()
            assertEquals(listOf(0.0, provider.resource.speed, 0.0), res) { "Speed is reported correctly" }
        } finally {
            scheduler.close()
            provider.close()
        }
    }

    @Test
    fun testSpeedLimit() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)
        val provider = SimResourceSource(SimCpu(4200.0), clock, scheduler)

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Consume(1000 * ctx.resource.speed, 2 * ctx.resource.speed)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                return SimResourceCommand.Exit
            }
        }

        try {
            val res = mutableListOf<Double>()
            val job = launch { provider.speed.toList(res) }

            provider.consume(consumer)

            job.cancel()
            assertEquals(listOf(0.0, provider.resource.speed, 0.0), res) { "Speed is reported correctly" }
        } finally {
            scheduler.close()
            provider.close()
        }
    }

    /**
     * Test to see whether no infinite recursion occurs when interrupting during [SimResourceConsumer.onStart] or
     * [SimResourceConsumer.onNext].
     */
    @Test
    fun testIntermediateInterrupt() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)
        val provider = SimResourceSource(SimCpu(4200.0), clock, scheduler)

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                ctx.interrupt()
                return SimResourceCommand.Exit
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                throw IllegalStateException()
            }
        }

        try {
            provider.consume(consumer)
        } finally {
            scheduler.close()
            provider.close()
        }
    }

    @Test
    fun testInterrupt() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)
        val provider = SimResourceSource(SimCpu(4200.0), clock, scheduler)
        lateinit var resCtx: SimResourceContext<SimCpu>

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                resCtx = ctx
                return SimResourceCommand.Consume(4.0, 1.0)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                assertEquals(0.0, remainingWork)
                return SimResourceCommand.Exit
            }
        }

        try {
            launch {
                yield()
                resCtx.interrupt()
            }
            provider.consume(consumer)

            assertEquals(0, currentTime)
        } finally {
            scheduler.close()
            provider.close()
        }
    }

    @Test
    fun testFailure() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)
        val provider = SimResourceSource(SimCpu(4200.0), clock, scheduler)

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                throw IllegalStateException()
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                throw IllegalStateException()
            }
        }

        try {
            assertThrows<IllegalStateException> {
                provider.consume(consumer)
            }
        } finally {
            scheduler.close()
            provider.close()
        }
    }

    @Test
    fun testExceptionPropagationOnNext() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)
        val provider = SimResourceSource(SimCpu(4200.0), clock, scheduler)

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Consume(1.0, 1.0)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                throw IllegalStateException()
            }
        }

        try {
            assertThrows<IllegalStateException> {
                provider.consume(consumer)
            }
        } finally {
            scheduler.close()
            provider.close()
        }
    }

    @Test
    fun testConcurrentConsumption() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)
        val provider = SimResourceSource(SimCpu(4200.0), clock, scheduler)

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Consume(1.0, 1.0)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                throw IllegalStateException()
            }
        }

        try {
            assertThrows<IllegalStateException> {
                coroutineScope {
                    launch { provider.consume(consumer) }
                    provider.consume(consumer)
                }
            }
        } finally {
            scheduler.close()
            provider.close()
        }
    }

    @Test
    fun testClosedConsumption() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)
        val provider = SimResourceSource(SimCpu(4200.0), clock, scheduler)

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Consume(1.0, 1.0)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                throw IllegalStateException()
            }
        }

        try {
            assertThrows<IllegalStateException> {
                provider.close()
                provider.consume(consumer)
            }
        } finally {
            scheduler.close()
            provider.close()
        }
    }

    @Test
    fun testCloseDuringConsumption() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)
        val provider = SimResourceSource(SimCpu(4200.0), clock, scheduler)

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Consume(1.0, 1.0)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                throw IllegalStateException()
            }
        }

        try {
            launch { provider.consume(consumer) }
            delay(500)
            provider.close()

            assertEquals(500, currentTime)
        } finally {
            scheduler.close()
            provider.close()
        }
    }

    @Test
    fun testIdle() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)
        val provider = SimResourceSource(SimCpu(4200.0), clock, scheduler)

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Idle(ctx.clock.millis() + 500)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                return SimResourceCommand.Exit
            }
        }

        try {
            provider.consume(consumer)

            assertEquals(500, currentTime)
        } finally {
            scheduler.close()
            provider.close()
        }
    }

    @Test
    fun testInfiniteSleep() {
        assertThrows<IllegalStateException> {
            runBlockingTest {
                val clock = DelayControllerClockAdapter(this)
                val scheduler = TimerScheduler<Any>(coroutineContext, clock)
                val provider = SimResourceSource(SimCpu(4200.0), clock, scheduler)

                val consumer = object : SimResourceConsumer<SimCpu> {
                    override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                        return SimResourceCommand.Idle()
                    }

                    override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                        return SimResourceCommand.Exit
                    }
                }

                try {
                    provider.consume(consumer)
                } finally {
                    scheduler.close()
                    provider.close()
                }
            }
        }
    }
}
