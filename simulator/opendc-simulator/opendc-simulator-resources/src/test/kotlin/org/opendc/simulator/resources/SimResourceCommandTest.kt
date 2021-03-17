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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

/**
 * Test suite for [SimResourceCommand].
 */
class SimResourceCommandTest {
    @Test
    fun testZeroWork() {
        assertThrows<IllegalArgumentException> {
            SimResourceCommand.Consume(0.0, 1.0)
        }
    }

    @Test
    fun testNegativeWork() {
        assertThrows<IllegalArgumentException> {
            SimResourceCommand.Consume(-1.0, 1.0)
        }
    }

    @Test
    fun testZeroLimit() {
        assertThrows<IllegalArgumentException> {
            SimResourceCommand.Consume(1.0, 0.0)
        }
    }

    @Test
    fun testNegativeLimit() {
        assertThrows<IllegalArgumentException> {
            SimResourceCommand.Consume(1.0, -1.0, 1)
        }
    }

    @Test
    fun testConsumeCorrect() {
        assertDoesNotThrow {
            SimResourceCommand.Consume(1.0, 1.0)
        }
    }

    @Test
    fun testIdleCorrect() {
        assertDoesNotThrow {
            SimResourceCommand.Idle(1)
        }
    }
}
