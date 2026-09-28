/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.simulator.failure.models

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

/**
 * Verifies that a [Failure] only accepts values that let it advance the simulation clock.
 */
class FailureTest {
    @Test
    fun `a failure may start immediately`() {
        assertDoesNotThrow { Failure(failureInterval = 0, failureDuration = 1, failureIntensity = 1.0) }
    }

    @Test
    fun `a failure cannot start at a negative time`() {
        assertThrows<IllegalArgumentException> { Failure(failureInterval = -1, failureDuration = 1, failureIntensity = 1.0) }
    }

    @Test
    fun `a failure must have a positive duration`() {
        assertAll(
            { assertThrows<IllegalArgumentException> { Failure(failureInterval = 1, failureDuration = 0, failureIntensity = 1.0) } },
            { assertThrows<IllegalArgumentException> { Failure(failureInterval = 1, failureDuration = -1, failureIntensity = 1.0) } },
        )
    }

    @Test
    fun `a failure must have an intensity above 0 and at most 1`() {
        assertAll(
            { assertThrows<IllegalArgumentException> { Failure(failureInterval = 1, failureDuration = 1, failureIntensity = 0.0) } },
            { assertThrows<IllegalArgumentException> { Failure(failureInterval = 1, failureDuration = 1, failureIntensity = 1.5) } },
            { assertDoesNotThrow { Failure(failureInterval = 1, failureDuration = 1, failureIntensity = 1.0) } },
        )
    }
}
