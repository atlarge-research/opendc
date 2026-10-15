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

package org.opendc.common.units

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FrequencyTest {
    @Test
    fun `every scale converts to every other scale`() {
        // Reference unit: hertz.
        assertScales(
            listOf(
                Scale("Hz", 1.0, { Frequency.ofHz(it) }, { it.toHz() }),
                Scale("KHz", 1e3, { Frequency.ofKHz(it) }, { it.toKHz() }),
                Scale("MHz", 1e6, { Frequency.ofMHz(it) }, { it.toMHz() }),
                Scale("GHz", 1e9, { Frequency.ofGHz(it) }, { it.toGHz() }),
            ),
        )
    }

    @Test
    fun `formats with the largest prefix that keeps the value readable`() {
        assertFormats(
            Frequency.ofHz(50) to "50 Hz",
            Frequency.ofKHz(50) to "50 kHz",
            Frequency.ofMHz(50) to "50 MHz",
            Frequency.ofGHz(3) to "3 GHz",
        )
    }

    @Test
    fun `parses every supported spelling`() {
        assertParses(
            "1000 Hz" to Frequency.ofHz(1_000),
            "1 hertz" to Frequency.ofHz(1),
            "2 kHz" to Frequency.ofKHz(2),
            "2400 MHz" to Frequency.ofMHz(2_400),
            "2megahertz" to Frequency.ofMHz(2),
            "  10    GHz   " to Frequency.ofGHz(10),
            "2400 MHZ" to Frequency.ofMHz(2_400),
        )
    }

    @Test
    fun `a lower-case m is milli, which is rejected instead of read as mega`() {
        assertRejects<Frequency>("e.g. \"1 MHz\"", "2400 mhz", "2400 mHz", "1 millihertz")
    }

    @Test
    fun `a bare number is read as MHz`() {
        assertEquals(Frequency.ofMHz(2_400), parse<Frequency>("2400"))
    }

    @Test
    fun `writes values that read back exactly`() {
        assertEquals("2500 MHz", write(Frequency.ofGHz(2.5)))
        assertRoundTrips(
            Frequency.zero,
            Frequency.ofHz(1),
            Frequency.ofGHz(0.1),
            Frequency.ofMHz(1.0 / 3),
            Frequency.ofKHz(-2.5),
        )
    }
}
