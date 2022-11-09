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

package org.opendc.simulator.kotlin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Test suite for the Kotlin simulation builders.
 */
class SimulationBuildersTest {
    @Test
    fun testDelay() = runSimulation {
        assertEquals(0, currentTime)
        delay(100)
        assertEquals(100, currentTime)
    }

    @Test
    fun testController() = runSimulation {
        var completed = false

        launch {
            delay(20)
            completed = true
        }

        advanceBy(10)
        assertFalse(completed)
        advanceBy(11)
        assertTrue(completed)

        completed = false
        launch { completed = true }
        runCurrent()
        assertTrue(completed)
    }

    @Test
    fun testFailOnActiveJobs() {
        assertThrows<IllegalStateException> {
            runSimulation {
                launch { suspendCancellableCoroutine {} }
            }
        }
    }

    @Test
    fun testPropagateException() {
        assertThrows<IllegalStateException> {
            runSimulation {
                throw IllegalStateException("Test")
            }
        }
    }

    @Test
    fun testInvalidDispatcher() {
        assertThrows<IllegalArgumentException> {
            runSimulation(Dispatchers.Default) { }
        }
    }

    @Test
    fun testExistingJob() {
        runSimulation(Job()) {
            delay(10)
        }
    }
}
