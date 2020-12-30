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
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.opendc.simulator.utils.DelayControllerClockAdapter
import org.opendc.utils.TimerScheduler
import java.time.Clock

/**
 * A test suite for the [SimResourceScheduler] class.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SimResourceSourceTest {

    private lateinit var scope: TestCoroutineScope
    private lateinit var clock: Clock

    data class SimCpu(val speed: Double) : SimResource {
        override val capacity: Double
            get() = speed
    }

    @BeforeEach
    fun setUp() {
        scope = TestCoroutineScope()
        clock = DelayControllerClockAdapter(scope)
    }

    @Test
    fun testSpeed() {
        val resource = SimCpu(4200.0)
        val provider = SimResourceSource(resource, clock, TimerScheduler(scope, clock))

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Consume(1000 * ctx.resource.speed, ctx.resource.speed)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                return SimResourceCommand.Exit
            }
        }

        scope.runBlockingTest {
            val res = mutableListOf<Double>()
            val job = launch { provider.speed.toList(res) }

            provider.consume(consumer)

            job.cancel()
            assertEquals(listOf(0.0, resource.speed, 0.0), res) { "Speed is reported correctly" }
        }
    }

    @Test
    fun testSpeedLimit() {
        val resource = SimCpu(4200.0)
        val provider = SimResourceSource(resource, clock, TimerScheduler(scope, clock))

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Consume(1000 * ctx.resource.speed, 2 * ctx.resource.speed)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                return SimResourceCommand.Exit
            }
        }

        scope.runBlockingTest {
            val res = mutableListOf<Double>()
            val job = launch { provider.speed.toList(res) }

            provider.consume(consumer)

            job.cancel()
            assertEquals(listOf(0.0, resource.speed, 0.0), res) { "Speed is reported correctly" }
        }
    }

    @Test
    fun testInterrupt() {
        val resource = SimCpu(4200.0)
        val provider = SimResourceSource(resource, clock, TimerScheduler(scope, clock))

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                ctx.interrupt()
                return SimResourceCommand.Exit
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                throw IllegalStateException()
            }
        }

        assertDoesNotThrow {
            scope.runBlockingTest {
                provider.consume(consumer)
            }
        }
    }

    @Test
    fun testFailure() {
        val resource = SimCpu(4200.0)
        val provider = SimResourceSource(resource, clock, TimerScheduler(scope, clock))

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                throw IllegalStateException()
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                throw IllegalStateException()
            }
        }

        assertThrows<IllegalStateException> {
            scope.runBlockingTest {
                provider.consume(consumer)
            }
        }
    }

    @Test
    fun testExceptionPropagationOnNext() {
        val resource = SimCpu(4200.0)
        val provider = SimResourceSource(resource, clock, TimerScheduler(scope, clock))

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Consume(1.0, 1.0)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                throw IllegalStateException()
            }
        }

        assertThrows<IllegalStateException> {
            scope.runBlockingTest { provider.consume(consumer) }
        }
    }

    @Test
    fun testConcurrentConsumption() {
        val resource = SimCpu(4200.0)
        val provider = SimResourceSource(resource, clock, TimerScheduler(scope, clock))

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Consume(1.0, 1.0)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                throw IllegalStateException()
            }
        }

        assertThrows<IllegalStateException> {
            scope.runBlockingTest {
                launch { provider.consume(consumer) }
                launch { provider.consume(consumer) }
            }
        }
    }

    @Test
    fun testClosedConsumption() {
        val resource = SimCpu(4200.0)
        val provider = SimResourceSource(resource, clock, TimerScheduler(scope, clock))

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Consume(1.0, 1.0)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                throw IllegalStateException()
            }
        }

        assertThrows<IllegalStateException> {
            scope.runBlockingTest {
                provider.close()
                provider.consume(consumer)
            }
        }
    }

    @Test
    fun testCloseDuringConsumption() {
        val resource = SimCpu(4200.0)
        val provider = SimResourceSource(resource, clock, TimerScheduler(scope, clock))

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Consume(1.0, 1.0)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                throw IllegalStateException()
            }
        }

        scope.runBlockingTest {
            launch { provider.consume(consumer) }
            delay(500)
            provider.close()
        }

        assertEquals(500, scope.currentTime)
    }

    @Test
    fun testIdle() {
        val resource = SimCpu(4200.0)
        val provider = SimResourceSource(resource, clock, TimerScheduler(scope, clock))

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Idle(ctx.clock.millis() + 500)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                return SimResourceCommand.Exit
            }
        }

        scope.runBlockingTest {
            provider.consume(consumer)
        }

        assertEquals(500, scope.currentTime)
    }

    @Test
    fun testInfiniteSleep() {
        val resource = SimCpu(4200.0)
        val provider = SimResourceSource(resource, clock, TimerScheduler(scope, clock))

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Idle()
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                return SimResourceCommand.Exit
            }
        }

        assertThrows<IllegalStateException> {
            scope.runBlockingTest {
                provider.consume(consumer)
            }
        }
    }
}
