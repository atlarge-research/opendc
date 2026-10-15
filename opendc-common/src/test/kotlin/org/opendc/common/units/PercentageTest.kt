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
import org.opendc.common.units.Percentage.Companion.percentageOf
import kotlin.test.assertEquals

class PercentageTest {
    @Test
    fun `ratio and percentage value describe the same amount`() {
        assertEquals(Percentage.ofRatio(0.5), Percentage.ofPercentage(50))
        assertEquals(0.25, Percentage.ofPercentage(25).toRatio())
        assertEquals(150.0, Percentage.ofRatio(1.5).toPercentage())
    }

    @Test
    fun `ofRatio accepts any number`() {
        assertEquals(Percentage.ofPercentage(100), Percentage.ofRatio(1))
        assertEquals(Percentage.ofPercentage(25), Percentage.ofRatio(0.25f))
    }

    @Test
    fun `ratios above 1 are allowed`() {
        // ofRatio logs a warning here, as ofRatio(2) is usually a mistake for ofPercentage(2).
        assertEquals(200.0, Percentage.ofRatio(2).toPercentage())
        // Ratios computed between two values are often above 1.
        assertEquals(Percentage.ofPercentage(300), Power.ofWatts(300) / Power.ofWatts(100))
    }

    @Test
    fun `is the ratio between two values`() {
        assertEquals(Percentage.ofPercentage(25), 1 percentageOf 4)
        assertEquals(Percentage.ofPercentage(400), DataSize.ofGiB(1) percentageOf DataSize.ofMiB(256))
        assertEquals(Percentage.ofPercentage(50), Power.ofWatts(100) / Power.ofWatts(200))
    }

    @Test
    fun `formats as a percentage`() {
        assertEquals("12%", Percentage.ofRatio(0.12).fmtValue("%.0f"))
    }

    @Test
    fun `parses percentages and is not bounded at 100`() {
        assertParses(
            "  30%   " to Percentage.ofPercentage(30),
            "120%" to Percentage.ofPercentage(120),
            "50 percentage" to Percentage.ofPercentage(50),
        )
    }

    @Test
    fun `a bare number is read as a ratio`() {
        assertEquals(Percentage.ofPercentage(50), parse<Percentage>("0.5"))
    }

    @Test
    fun `writes values that read back exactly`() {
        assertEquals("50%", write(Percentage.ofRatio(0.5)))
        assertEquals("7%", write(Percentage.ofRatio(0.07)))
        assertRoundTrips(
            Percentage.zero,
            Percentage.ofRatio(0.07),
            Percentage.ofRatio(1.0 / 3),
            Percentage.ofPercentage(33.3),
            Percentage.ofRatio(1e-12),
            Percentage.ofPercentage(-2.5),
        )
    }
}
