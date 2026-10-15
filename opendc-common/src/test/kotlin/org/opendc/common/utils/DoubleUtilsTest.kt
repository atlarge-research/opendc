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

package org.opendc.common.utils

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DoubleUtilsTest {
    // Pairs that are clearly apart, equal, or within the default epsilon of each other.
    private val pairs = listOf(1.0 to 2.0, 2.0 to 1.0, 1.0 to 1.0, 1.0 to 1.0 + 1e-7, 1e6 to 1e6 - 1, 1e6 - 1 to 1e6)

    @Test
    fun `infix comparisons point the way their names say`() {
        assertTrue(1.0 approxSmaller 2.0)
        assertFalse(2.0 approxSmaller 1.0)
        assertTrue(2.0 approxLarger 1.0)
        assertFalse(1.0 approxLarger 2.0)
        assertTrue(1.0 approxSmallerOrEq 1.0)
        assertTrue(1.0 approxLargerOrEq 1.0)
    }

    @Test
    fun `infix comparisons are each other's complement`() {
        assertAll(
            pairs.flatMap { (a, b) ->
                listOf(
                    { assertEquals(!(a approxLargerOrEq b), a approxSmaller b, "$a approxSmaller $b") },
                    { assertEquals(!(a approxLarger b), a approxSmallerOrEq b, "$a approxSmallerOrEq $b") },
                )
            },
        )
    }

    @Test
    fun `values within epsilon are approximately equal`() {
        assertTrue(1.0 approx 1.0 + 1e-7)
        assertFalse(1.0 approx 1.1)
        assertTrue(1.0.approx(1.05, epsilon = 0.1))
        assertEquals(5.0, (5.0 + 1e-9).roundToIfWithinEpsilon(5.0))
    }

    @Test
    fun `replaces NaN only`() {
        assertEquals(0.0, Double.NaN ifNaN 0.0)
        assertEquals(3.0, 3.0 ifNaN 0.0)
    }
}
