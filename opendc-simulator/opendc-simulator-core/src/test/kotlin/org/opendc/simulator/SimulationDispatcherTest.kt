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

package org.opendc.simulator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

/**
 * Test suite for the [SimulationDispatcher] class.
 */
class SimulationDispatcherTest {
    /**
     * Test the basic functionality of [SimulationDispatcher.runCurrent].
     */
    @Test
    fun testRunCurrent() {
        val scheduler = SimulationDispatcher()
        var count = 0

        scheduler.schedule(1) { count += 1 }
        scheduler.schedule(2) { count += 1 }

        scheduler.advanceBy(1)
        assertEquals(0, count)
        scheduler.runCurrent()
        assertEquals(1, count)
        scheduler.advanceBy(1)
        assertEquals(1, count)
        scheduler.runCurrent()
        assertEquals(2, count)
        assertEquals(2, scheduler.currentTime)

        scheduler.advanceBy(Long.MAX_VALUE)
        scheduler.runCurrent()
        assertEquals(Long.MAX_VALUE, scheduler.currentTime)
    }

    /**
     * Test the clock of the [SimulationDispatcher].
     */
    @Test
    fun testClock() {
        val scheduler = SimulationDispatcher()
        var count = 0

        scheduler.schedule(1) { count += 1 }
        scheduler.schedule(2) { count += 1 }

        scheduler.advanceBy(2)
        assertEquals(2, scheduler.currentTime)
        assertEquals(2, scheduler.timeSource.millis())
        assertEquals(Instant.ofEpochMilli(2), scheduler.timeSource.instant())
    }

    /**
     * Test large delays.
     */
    @Test
    fun testAdvanceByLargeDelays() {
        val scheduler = SimulationDispatcher()
        var count = 0

        scheduler.schedule(1) { count += 1 }

        scheduler.advanceBy(10)

        scheduler.schedule(Long.MAX_VALUE) { count += 1 }
        scheduler.scheduleCancellable(Long.MAX_VALUE) { count += 1 }
        scheduler.schedule(100_000_000) { count += 1 }

        scheduler.advanceUntilIdle()
        assertEquals(4, count)
    }

    /**
     * Test negative delays.
     */
    @Test
    fun testNegativeDelays() {
        val scheduler = SimulationDispatcher()

        assertThrows<IllegalArgumentException> { scheduler.schedule(-100) { } }
        assertThrows<IllegalArgumentException> { scheduler.advanceBy(-100) }
    }
}
