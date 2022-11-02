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

package org.opendc.compute.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.opendc.compute.api.Flavor
import java.util.UUID

/**
 * Test suite for the [ServiceFlavor] implementation.
 */
class ServiceFlavorTest {
    @Test
    fun testEquality() {
        val service = mockk<ComputeService>()
        val uid = UUID.randomUUID()
        val a = ServiceFlavor(service, uid, "test", 1, 1024, mutableMapOf(), mutableMapOf<String, Any>())
        val b = ServiceFlavor(service, uid, "test", 1, 1024, mutableMapOf(), mutableMapOf<String, Any>())

        assertEquals(a, b)
    }

    @Test
    fun testInequalityWithDifferentType() {
        val service = mockk<ComputeService>()
        val uid = UUID.randomUUID()
        val a = ServiceFlavor(service, uid, "test", 1, 1024, mutableMapOf(), mutableMapOf<String, Any>())

        val b = mockk<Flavor>(relaxUnitFun = true)
        every { b.uid } returns UUID.randomUUID()

        assertNotEquals(a, b)
    }

    @Test
    fun testInequalityWithIncorrectType() {
        val service = mockk<ComputeService>()
        val uid = UUID.randomUUID()
        val a = ServiceFlavor(service, uid, "test", 1, 1024, mutableMapOf(), mutableMapOf<String, Any>())

        assertNotEquals(a, Unit)
    }
}
