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

package org.opendc.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.utils.DelayControllerClockAdapter

/**
 * A test suite for the [TimerScheduler] class.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class TimerSchedulerTest {
    @Test
    fun testBasicTimer() {
        runBlockingTest {
            val clock = DelayControllerClockAdapter(this)
            val scheduler = TimerScheduler<Int>(coroutineContext, clock)

            scheduler.startSingleTimer(0, 1000) {
                scheduler.close()
                assertEquals(1000, clock.millis())
            }
        }
    }

    @Test
    fun testCancelNonExisting() {
        runBlockingTest {
            val clock = DelayControllerClockAdapter(this)
            val scheduler = TimerScheduler<Int>(coroutineContext, clock)

            scheduler.cancel(1)
            scheduler.close()
        }
    }

    @Test
    fun testCancelExisting() {
        runBlockingTest {
            val clock = DelayControllerClockAdapter(this)
            val scheduler = TimerScheduler<Int>(coroutineContext, clock)

            scheduler.startSingleTimer(0, 1000) {
                assertFalse(false)
            }

            scheduler.startSingleTimer(1, 100) {
                scheduler.cancel(0)
                scheduler.close()

                assertEquals(100, clock.millis())
            }
        }
    }

    @Test
    fun testCancelAll() {
        runBlockingTest {
            val clock = DelayControllerClockAdapter(this)
            val scheduler = TimerScheduler<Int>(coroutineContext, clock)

            scheduler.startSingleTimer(0, 1000) {
                assertFalse(false)
            }

            scheduler.startSingleTimer(1, 100) {
                assertFalse(false)
            }

            scheduler.close()
        }
    }

    @Test
    fun testOverride() {
        runBlockingTest {
            val clock = DelayControllerClockAdapter(this)
            val scheduler = TimerScheduler<Int>(coroutineContext, clock)

            scheduler.startSingleTimer(0, 1000) {
                assertFalse(false)
            }

            scheduler.startSingleTimer(0, 200) {
                scheduler.close()

                assertEquals(200, clock.millis())
            }
        }
    }

    @Test
    fun testStopped() {
        runBlockingTest {
            val clock = DelayControllerClockAdapter(this)
            val scheduler = TimerScheduler<Int>(coroutineContext, clock)

            scheduler.close()

            assertThrows<IllegalStateException> {
                scheduler.startSingleTimer(1, 100) {
                    assertFalse(false)
                }
            }
        }
    }

    @Test
    fun testNegativeDelay() {
        runBlockingTest {
            val clock = DelayControllerClockAdapter(this)
            val scheduler = TimerScheduler<Int>(coroutineContext, clock)

            assertThrows<IllegalArgumentException> {
                scheduler.startSingleTimer(1, -1) {
                    assertFalse(false)
                }
            }

            scheduler.close()
        }
    }
}
