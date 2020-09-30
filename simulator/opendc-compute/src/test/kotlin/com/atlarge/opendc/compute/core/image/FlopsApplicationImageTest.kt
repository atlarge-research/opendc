/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.compute.core.image

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

/**
 * Test suite for [FlopsApplicationImage]
 */
@DisplayName("FlopsApplicationImage")
internal class FlopsApplicationImageTest {
    @Test
    fun `flops must be non-negative`() {
        assertThrows<IllegalArgumentException>("FLOPs must be non-negative") {
            FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", emptyMap(), -1, 1)
        }
    }

    @Test
    fun `cores cannot be zero`() {
        assertThrows<IllegalArgumentException>("Cores cannot be zero") {
            FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", emptyMap(), 1, 0)
        }
    }

    @Test
    fun `cores cannot be negative`() {
        assertThrows<IllegalArgumentException>("Cores cannot be negative") {
            FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", emptyMap(), 1, -1)
        }
    }

    @Test
    fun `utilization cannot be zero`() {
        assertThrows<IllegalArgumentException>("Utilization cannot be zero") {
            FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", emptyMap(), 1, 1, 0.0)
        }
    }

    @Test
    fun `utilization cannot be negative`() {
        assertThrows<IllegalArgumentException>("Utilization cannot be negative") {
            FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", emptyMap(), 1, 1, -1.0)
        }
    }

    @Test
    fun `utilization cannot be larger than one`() {
        assertThrows<IllegalArgumentException>("Utilization cannot be larger than one") {
            FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", emptyMap(), 1, 1, 2.0)
        }
    }
}
