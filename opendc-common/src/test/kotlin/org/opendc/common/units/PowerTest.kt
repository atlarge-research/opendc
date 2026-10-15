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

class PowerTest {
    @Test
    fun `every scale converts to every other scale`() {
        // Reference unit: watts.
        assertScales(
            listOf(
                Scale("W", 1.0, { Power.ofWatts(it) }, { it.toWatts() }),
                Scale("kW", 1e3, { Power.ofKWatts(it) }, { it.toKWatts() }),
                Scale("MW", 1e6, { Power.ofMWatts(it) }, { it.toMWatts() }),
            ),
        )
    }

    @Test
    fun `formats watts below a kilowatt, kilowatts below a megawatt, and megawatts from there`() {
        assertFormats(
            Power.ofWatts(500) to "500 W",
            Power.ofWatts(-500) to "-500 W",
            Power.ofKWatts(1) to "1 kW",
            Power.ofKWatts(20) to "20 kW",
            Power.ofMWatts(1) to "1 MW",
            Power.ofMWatts(15) to "15 MW",
        )
    }

    @Test
    fun `parses every supported spelling`() {
        assertParses(
            "4 watts" to Power.ofWatts(4),
            "4 W" to Power.ofWatts(4),
            "1 Watt" to Power.ofWatts(1),
            "1 kW" to Power.ofKWatts(1),
            "2 KWatts" to Power.ofKWatts(2),
            "  1    KWatt   " to Power.ofKWatts(1),
            "3 MW" to Power.ofMWatts(3),
            "2 megawatts" to Power.ofMWatts(2),
            "1 MWATTS" to Power.ofMWatts(1),
        )
    }

    @Test
    fun `a lower-case m is milli, which is rejected instead of read as mega`() {
        assertRejects<Power>("Write \"M\" for mega, e.g. \"1 MW\"", "1 mW", "1 mw", "5 milliwatts")
    }

    @Test
    fun `a bare number is read as watts`() {
        assertEquals(Power.ofWatts(250), parse<Power>("250"))
    }

    @Test
    fun `writes values that read back exactly`() {
        assertEquals("1500 W", write(Power.ofKWatts(1.5)))
        assertRoundTrips(
            Power.zero,
            Power.ofWatts(0.1),
            Power.ofKWatts(1.0 / 3),
            Power.ofMWatts(20),
            Power.ofWatts(-2.5),
        )
    }
}
