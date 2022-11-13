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

import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.simulator.kotlin.runSimulation

/**
 * Test suite for the [Pacer] class.
 */
class PacerTest {
    @Test
    fun testSingleEnqueue() {
        var count = 0

        runSimulation {
            val pacer = Pacer(dispatcher, /*quantum*/ 100) {
                count++
            }

            pacer.enqueue()
        }

        assertEquals(1, count) { "Process should execute once" }
    }

    @Test
    fun testCascade() {
        var count = 0

        runSimulation {
            val pacer = Pacer(dispatcher, /*quantum*/ 100) {
                count++
            }

            pacer.enqueue()
            pacer.enqueue()

            assertTrue(pacer.isPending)
        }

        assertEquals(1, count) { "Process should execute once" }
    }

    @Test
    fun testCancel() {
        var count = 0

        runSimulation {
            val pacer = Pacer(dispatcher, /*quantum*/ 100) {
                count++
            }

            pacer.enqueue()
            pacer.cancel()

            assertFalse(pacer.isPending)
        }

        assertEquals(0, count) { "Process should never execute " }
    }

    @Test
    fun testCancelWithoutPending() {
        var count = 0

        runSimulation {
            val pacer = Pacer(dispatcher, /*quantum*/ 100) {
                count++
            }

            assertFalse(pacer.isPending)
            assertDoesNotThrow { pacer.cancel() }

            pacer.enqueue()
        }

        assertEquals(1, count) { "Process should execute once" }
    }

    @Test
    fun testSubsequent() {
        var count = 0

        runSimulation {
            val pacer = Pacer(dispatcher, /*quantum*/ 100) {
                count++
            }

            pacer.enqueue()
            delay(100)
            pacer.enqueue()
        }

        assertEquals(2, count) { "Process should execute twice" }
    }
}
