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

package org.opendc.common

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.kotlin.runSimulation

/**
 * Test suite for [DispatcherCoroutineDispatcher].
 */
class DispatcherCoroutineDispatcherTest {
    @Test
    fun testYield() = runSimulation {
        withContext(dispatcher.asCoroutineDispatcher()) {
            val startTime = dispatcher.currentTime
            yield()
            assertEquals(startTime, dispatcher.currentTime)
        }
    }

    @Test
    fun testDelay() = runSimulation {
        withContext(dispatcher.asCoroutineDispatcher()) {
            val startTime = dispatcher.currentTime
            delay(10)
            assertEquals(startTime + 10, dispatcher.currentTime)
        }
    }

    @Test
    fun testTimeout() = runSimulation {
        withContext(dispatcher.asCoroutineDispatcher()) {
            assertThrows<TimeoutCancellationException> {
                withTimeout(10) {
                    delay(1000)
                }
            }

            assertEquals(10, dispatcher.currentTime)
        }
    }
}
