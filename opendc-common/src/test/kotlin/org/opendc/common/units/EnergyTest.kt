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
import org.opendc.common.units.Energy.Companion.times
import kotlin.test.assertEquals

class EnergyTest {
    @Test
    fun `every scale converts to every other scale`() {
        // Reference unit: joules. A watt-hour is 3600 joules.
        assertScales(
            listOf(
                Scale("J", 1.0, { Energy.ofJoule(it) }, { it.toJoule() }),
                Scale("kJ", 1e3, { Energy.ofKJoule(it) }, { it.toKJoule() }),
                Scale("MJ", 1e6, { Energy.ofMJoule(it) }, { it.toMJoule() }),
                Scale("GJ", 1e9, { Energy.ofGJoule(it) }, { it.toGJoule() }),
                Scale("Wh", 3_600.0, { Energy.ofWh(it) }, { it.toWh() }),
                Scale("kWh", 3.6e6, { Energy.ofKWh(it) }, { it.toKWh() }),
                Scale("MWh", 3.6e9, { Energy.ofMWh(it) }, { it.toMWh() }),
            ),
        )
    }

    @Test
    fun `a number times an energy is an energy`() {
        val energy: Energy = 3 * Energy.ofKWh(2)

        assertEquals(Energy.ofKWh(6), energy)
    }

    @Test
    fun `formats in joules up to a kilowatt-hour, and in watt-hours from there`() {
        assertFormats(
            Energy.ofJoule(500) to "500 J",
            Energy.ofJoule(-500) to "-500 J",
            Energy.ofKJoule(1) to "1 kJ",
            Energy.ofKJoule(3_000) to "3000 kJ",
            Energy.ofKWh(1) to "1 kWh",
            // Megajoules are shown as kilowatt-hours, and gigajoules as megawatt-hours.
            Energy.ofMJoule(36) to "10 kWh",
            Energy.ofKWh(250) to "250 kWh",
            Energy.ofMWh(1) to "1 MWh",
            Energy.ofGJoule(36) to "10 MWh",
            Energy.ofMWh(87_600) to "87600 MWh",
        )
    }

    @Test
    fun `parses every supported spelling`() {
        assertParses(
            "1 J" to Energy.ofJoule(1),
            "10.5 Joules" to Energy.ofJoule(10.5),
            "2 kJ" to Energy.ofKJoule(2),
            "3 MJ" to Energy.ofMJoule(3),
            "4 megajoules" to Energy.ofMJoule(4),
            "2 GJ" to Energy.ofGJoule(2),
            "1 Wh" to Energy.ofWh(1),
            "  3    watts-hour  " to Energy.ofWh(3),
            "1 KWh" to Energy.ofKWh(1),
            "1 kWh" to Energy.ofKWh(1),
            "1 kW/h" to Energy.ofKWh(1),
            "2 MWh" to Energy.ofMWh(2),
            "2 megawatt-hour" to Energy.ofMWh(2),
            "2 megawatt-hours" to Energy.ofMWh(2),
            "5 kilowatt-hours" to Energy.ofKWh(5),
            "3 watt-hours" to Energy.ofWh(3),
        )
    }

    @Test
    fun `a lower-case m is milli, which is rejected instead of read as mega`() {
        assertRejects<Energy>("e.g. \"1 MWh\"", "1 mWh", "1 mwh", "1 milliwatt-hour", "2 milliwatt-hours")
        assertRejects<Energy>("e.g. \"1 MJ\"", "1 mJ", "3 millijoules")
    }

    @Test
    fun `a bare number is read as joules`() {
        assertEquals(Energy.ofJoule(42), parse<Energy>("42"))
    }

    @Test
    fun `writes values that read back exactly`() {
        assertEquals("3600000 J", write(Energy.ofKWh(1)))
        assertRoundTrips(
            Energy.zero,
            Energy.ofJoule(0.1),
            Energy.ofWh(1.0 / 3),
            Energy.ofMWh(12),
            Energy.ofGJoule(0.1),
            Energy.ofKJoule(-2.5),
        )
    }
}
