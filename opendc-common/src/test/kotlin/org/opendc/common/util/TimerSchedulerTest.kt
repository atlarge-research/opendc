/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.kotlin.runSimulation

/**
 * A test suite for the [TimerScheduler] class.
 */
class TimerSchedulerTest {
    @Test
    fun testBasicTimer() {
        runSimulation {
            val scheduler = TimerScheduler<Int>(dispatcher)

            scheduler.startSingleTimer(0, 1000) {
                assertEquals(1000, timeSource.millis())
            }

            assertTrue(scheduler.isTimerActive(0))
            assertFalse(scheduler.isTimerActive(1))
        }
    }

    @Test
    fun testCancelNonExisting() {
        runSimulation {
            val scheduler = TimerScheduler<Int>(dispatcher)

            scheduler.cancel(1)
        }
    }

    @Test
    fun testCancelExisting() {
        runSimulation {
            val scheduler = TimerScheduler<Int>(dispatcher)

            scheduler.startSingleTimer(0, 1000) {
                fail()
            }

            scheduler.startSingleTimer(1, 100) {
                scheduler.cancel(0)

                assertEquals(100, timeSource.millis())
            }
        }
    }

    @Test
    fun testCancelAll() {
        runSimulation {
            val scheduler = TimerScheduler<Int>(dispatcher)

            scheduler.startSingleTimer(0, 1000) { fail() }
            scheduler.startSingleTimer(1, 100) { fail() }
            scheduler.cancelAll()
        }
    }

    @Test
    fun testOverride() {
        runSimulation {
            val scheduler = TimerScheduler<Int>(dispatcher)

            scheduler.startSingleTimer(0, 1000) { fail() }

            scheduler.startSingleTimer(0, 200) {
                assertEquals(200, timeSource.millis())
            }
        }
    }

    @Test
    fun testOverrideBlock() {
        runSimulation {
            val scheduler = TimerScheduler<Int>(dispatcher)

            scheduler.startSingleTimer(0, 1000) { fail() }

            scheduler.startSingleTimer(0, 1000) {
                assertEquals(1000, timeSource.millis())
            }
        }
    }

    @Test
    fun testNegativeDelay() {
        runSimulation {
            val scheduler = TimerScheduler<Int>(dispatcher)

            assertThrows<IllegalArgumentException> {
                scheduler.startSingleTimer(1, -1) {
                    fail()
                }
            }
        }
    }
}
