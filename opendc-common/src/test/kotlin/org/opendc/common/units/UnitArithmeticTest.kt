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
import org.opendc.common.units.TimeDelta.Companion.times
import org.opendc.common.units.Unit.Companion.averageOfUnitOrNull
import org.opendc.common.units.Unit.Companion.div
import org.opendc.common.units.Unit.Companion.sumOfUnit
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Operations that combine units, and the arithmetic every [Unit] shares.
 */
class UnitArithmeticTest {
    @Test
    fun `power over time is energy, and energy over time is power`() {
        assertEquals(Energy.ofWh(200), Power.ofWatts(100) * TimeDelta.ofHours(2))
        assertEquals(Energy.ofWh(200), TimeDelta.ofHours(2) * Power.ofWatts(100))
        assertEquals(Energy.ofWh(200), Power.ofWatts(100) * Duration.ofHours(2))
        assertEquals(Power.ofWatts(500), Energy.ofKWh(1) / TimeDelta.ofHours(2))
        assertEquals(Power.ofWatts(500), Energy.ofKWh(1) / Duration.ofHours(2))
        assertApprox(1.0, (Power.ofWatts(1) * TimeDelta.ofSec(1)).toJoule(), "1 W for 1 s")
    }

    @Test
    fun `data rate over time is a data size`() {
        assertApprox(30.0, (DataRate.ofMBps(10) * TimeDelta.ofSec(3)).toMB())
        assertApprox(30.0, (TimeDelta.ofSec(3) * DataRate.ofMBps(10)).toMB())
        assertApprox(1.0, (DataRate.ofbps(1) * TimeDelta.ofSec(1)).toBits(), "1 bps for 1 s")
    }

    @Test
    fun `data size over time is a data rate`() {
        assertApprox(10.0, (DataSize.ofMB(30) / TimeDelta.ofSec(3)).toMBps())
        assertApprox(1.0, (DataSize.ofKiB(1) / TimeDelta.ofSec(1)).toKiBps(), "1 KiB in 1 s")
        assertApprox(8.0, (DataSize.ofBytes(1) / TimeDelta.ofSec(1)).tobps(), "1 byte in 1 s")
    }

    @Test
    fun `data size over data rate is the transfer time`() {
        assertApprox(3.0, (DataSize.ofMB(30) / DataRate.ofMBps(10)).toSec())
        assertApprox(8.0, (DataSize.ofGiB(1) / DataRate.ofGibps(1)).toSec(), "1 GiB at 1 Gibps")
    }

    @Test
    fun `combined operations undo each other`() {
        val rate = DataRate.ofMibps(123.4)
        val size = DataSize.ofGB(5.5)
        val power = Power.ofKWatts(1.7)
        val time = TimeDelta.ofMin(42)

        assertApprox(rate.tobps(), ((rate * time) / time).tobps(), "(rate * time) / time")
        assertApprox(size.toBytes(), ((size / rate) * rate).toBytes(), "(size / rate) * rate")
        assertApprox(size.toBytes(), ((size / time) * time).toBytes(), "(size / time) * time")
        assertApprox(power.toWatts(), ((power * time) / time).toWatts(), "(power * time) / time")
    }

    @Test
    fun `frequency and time`() {
        assertEquals(1e9, Frequency.ofGHz(2) * TimeDelta.ofMs(500))
        assertEquals(Frequency.ofHz(5), 10 / TimeDelta.ofSec(2))
        assertEquals(Frequency.ofHz(5), 10 / Duration.ofSeconds(2))
    }

    @Test
    fun `scalar arithmetic`() {
        val twoSec = TimeDelta.ofSec(2)

        assertEquals(TimeDelta.ofSec(6), twoSec * 3)
        assertEquals(TimeDelta.ofSec(6), 3 * twoSec)
        assertEquals(TimeDelta.ofSec(0.5), twoSec / 4)
        assertEquals(TimeDelta.ofSec(3), twoSec + TimeDelta.ofSec(1))
        assertEquals(TimeDelta.ofSec(1), twoSec - TimeDelta.ofSec(1))
        assertEquals(TimeDelta.ofSec(-2), -twoSec)
        assertEquals(TimeDelta.ofSec(1), twoSec * Percentage.ofPercentage(50))
        assertEquals(Percentage.ofPercentage(200), twoSec / TimeDelta.ofSec(1))
    }

    @Test
    fun `values from different scales are equal when they describe the same amount`() {
        assertEquals(TimeDelta.ofSec(1), TimeDelta.ofMs(1_000))
        assertEquals(DataSize.ofKiB(1).hashCode(), DataSize.ofBytes(1_024).hashCode())
    }

    @Test
    fun `comparison, min, max and abs`() {
        val small = Power.ofWatts(10)
        val large = Power.ofKWatts(1)

        assertTrue(small < large)
        assertEquals(large, small max large)
        assertEquals(small, small min large)
        assertEquals(small, Unit.minOf(large, small, large))
        assertEquals(large, Unit.maxOf(small, large, small))
        assertEquals(small, (-small).abs())
        assertEquals(listOf(small, large), listOf(large, small).sorted())
    }

    @Test
    fun `zero checks`() {
        assertTrue(Energy.zero.isZero())
        assertTrue(Energy.ofJoule(1e-9).approxZero())
        assertFalse(Energy.ofJoule(1e-3).approxZero())
        // Units compare like boxed doubles, so -0.0 and +0.0 differ until normalized.
        assertNotEquals(TimeDelta.zero, -TimeDelta.zero)
        assertEquals(TimeDelta.zero, (-TimeDelta.zero).ifNeg0ThenPos0())
    }

    @Test
    fun `approx comparisons are relative to the size of the values`() {
        assertTrue(Power.ofMWatts(1) approx Power.ofWatts(1_000_001))
        assertFalse(Power.ofWatts(1) approx Power.ofWatts(1.001))
        assertTrue(Power.ofWatts(2) approxLarger Power.ofWatts(1))
        assertFalse(Power.ofWatts(1_000_001) approxLarger Power.ofMWatts(1))
        assertTrue(Power.ofWatts(1_000_001) approxLargerOrEq Power.ofMWatts(1))
        assertTrue(Power.ofWatts(1) approxSmaller Power.ofWatts(2))
        assertTrue(Power.ofMWatts(1) approxSmallerOrEq Power.ofWatts(999_999))
    }

    @Test
    fun `sum and average over collections`() {
        val draws = listOf(100, 200, 600)

        assertEquals(Power.ofWatts(900), draws.sumOfUnit { Power.ofWatts(it) })
        assertEquals(Power.ofWatts(300), draws.averageOfUnitOrNull { Power.ofWatts(it) })
        assertNull(emptyList<Int>().averageOfUnitOrNull { Power.ofWatts(it) })
        assertEquals(TimeDelta.ofMin(3), listOf(1, 2).sumOfUnit { TimeDelta.ofMin(it) })
    }
}
