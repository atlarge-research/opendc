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
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.opendc.simulator.utils.DelayControllerClockAdapter

/**
 * A test suite for the [SimAbstractResourceContext] class.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SimResourceContextTest {
    data class SimCpu(val speed: Double) : SimResource {
        override val capacity: Double
            get() = speed
    }

    @Test
    fun testFlushWithoutCommand() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)

        val resource = SimCpu(4200.0)

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Consume(10.0, 1.0)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                return SimResourceCommand.Exit
            }
        }

        val context = object : SimAbstractResourceContext<SimCpu>(resource, clock, consumer) {
            override fun onIdle(deadline: Long) {
            }

            override fun onConsume(work: Double, limit: Double, deadline: Long) {
            }

            override fun onFinish() {
            }

            override fun onFailure(cause: Throwable) {
            }
        }

        context.flush()
    }

    @Test
    fun testIntermediateFlush() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val resource = SimCpu(4200.0)

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Consume(10.0, 1.0)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                return SimResourceCommand.Exit
            }
        }

        var counter = 0
        val context = object : SimAbstractResourceContext<SimCpu>(resource, clock, consumer) {
            override fun onIdle(deadline: Long) {
            }

            override fun onConsume(work: Double, limit: Double, deadline: Long) {
                counter++
            }

            override fun onFinish() {
            }

            override fun onFailure(cause: Throwable) {
            }
        }

        context.start()
        delay(1) // Delay 1 ms to prevent hitting the fast path
        context.flush(isIntermediate = true)
        assertEquals(2, counter)
    }

    @Test
    fun testIntermediateFlushIdle() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val resource = SimCpu(4200.0)

        val consumer = object : SimResourceConsumer<SimCpu> {
            override fun onStart(ctx: SimResourceContext<SimCpu>): SimResourceCommand {
                return SimResourceCommand.Idle(10)
            }

            override fun onNext(ctx: SimResourceContext<SimCpu>, remainingWork: Double): SimResourceCommand {
                return SimResourceCommand.Exit
            }
        }

        var counter = 0
        var isFinished = false
        val context = object : SimAbstractResourceContext<SimCpu>(resource, clock, consumer) {
            override fun onIdle(deadline: Long) {
                counter++
            }

            override fun onConsume(work: Double, limit: Double, deadline: Long) {
            }

            override fun onFinish() {
                isFinished = true
            }

            override fun onFailure(cause: Throwable) {
            }
        }

        context.start()
        delay(5)
        context.flush(isIntermediate = true)
        delay(5)
        context.flush(isIntermediate = true)

        assertAll(
            { assertEquals(1, counter) },
            { assertTrue(isFinished) }
        )
    }
}
