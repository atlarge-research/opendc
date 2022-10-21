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

package org.opendc.simulator.flow2

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Test suite for the [InvocationStack] class.
 */
class InvocationStackTest {
    private val stack = InvocationStack(2)

    @Test
    fun testPollEmpty() {
        assertEquals(Long.MAX_VALUE, stack.poll())
    }

    @Test
    fun testAddSingle() {
        assertTrue(stack.tryAdd(10))
        assertEquals(10, stack.poll())
    }

    @Test
    fun testAddLater() {
        assertTrue(stack.tryAdd(10))
        assertFalse(stack.tryAdd(15))
        assertEquals(10, stack.poll())
    }

    @Test
    fun testAddEarlier() {
        assertTrue(stack.tryAdd(10))
        assertTrue(stack.tryAdd(5))
        assertEquals(5, stack.poll())
        assertEquals(10, stack.poll())
    }

    @Test
    fun testCapacityExceeded() {
        assertTrue(stack.tryAdd(10))
        assertTrue(stack.tryAdd(5))
        assertTrue(stack.tryAdd(2))
        assertEquals(2, stack.poll())
        assertEquals(5, stack.poll())
        assertEquals(10, stack.poll())
    }
}
