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

package org.opendc.experiments.capelin.trace

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

/**
 * Test suite for the [PerformanceInterferenceReader] class.
 */
class PerformanceInterferenceReaderTest {
    @Test
    fun testSmoke() {
        val input = checkNotNull(PerformanceInterferenceReader::class.java.getResourceAsStream("/perf-interference.json"))
        val reader = PerformanceInterferenceReader(input)

        val result = reader.use { reader.read() }

        assertAll(
            { assertEquals(2, result.size) },
            { assertEquals(setOf("vm_a", "vm_c", "vm_x", "vm_y"), result[0].members) },
            { assertEquals(0.0, result[0].targetLoad, 0.001) },
            { assertEquals(0.8830158730158756, result[0].score, 0.001) }
        )
    }
}
