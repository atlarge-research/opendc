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
import org.junit.jupiter.api.assertThrows
import org.opendc.common.units.Timestamp.Companion.toTimestamp
import java.time.Instant
import kotlin.test.assertEquals

class TimestampTest {
    @Test
    fun `every scale converts to every other scale`() {
        // Reference unit: milliseconds since the Epoch.
        assertScales(
            listOf(
                Scale("ns", 1e-6, { Timestamp.ofEpochNs(it) }, { it.toEpochNs() }),
                Scale("micros", 1e-3, { Timestamp.ofEpochMicros(it) }, { it.toEpochMicros() }),
                Scale("ms", 1.0, { Timestamp.ofEpochMs(it) }, { it.toEpochMs() }),
                Scale("sec", 1_000.0, { Timestamp.ofEpochSec(it) }, { it.toEpochSec() }),
                Scale("min", 60_000.0, { Timestamp.ofEpochMin(it) }, { it.toEpochMin() }),
                Scale("hours", 3_600_000.0, { Timestamp.ofEpochHours(it) }, { it.toEpochHours() }),
                Scale("days", 86_400_000.0, { Timestamp.ofEpochDays(it) }, { it.toEpochDays() }),
                Scale("weeks", 604_800_000.0, { Timestamp.ofEpochWeeks(it) }, { it.toEpochWeeks() }),
            ),
        )
    }

    @Test
    fun `converts from and to instants`() {
        val instant = Instant.parse("2001-09-09T01:46:40Z")

        assertEquals(1e12, Timestamp.ofInstant(instant).toEpochMs())
        assertEquals(Timestamp.ofEpochSec(1e9), instant.toTimestamp())
        assertEquals(instant, Timestamp.ofEpochMs(1e12).toInstant())
        assertEquals(Instant.parse("1970-01-08T00:00:00Z"), Timestamp.ofEpochWeeks(1).toInstant())
        assertEquals("1970-01-02T00:00:00Z", Timestamp.ofEpochDays(1).toString())
    }

    @Test
    fun `moves by time deltas`() {
        val start = Timestamp.ofEpochHours(10)

        assertEquals(Timestamp.ofEpochMin(630), start + TimeDelta.ofMin(30))
        assertEquals(Timestamp.ofEpochHours(9), start - TimeDelta.ofHours(1))
        assertEquals(TimeDelta.ofDays(1), Timestamp.ofEpochDays(2) timeDelta Timestamp.ofEpochDays(1))
        // The difference keeps its sign.
        assertEquals(TimeDelta.ofDays(-1), Timestamp.ofEpochDays(1) timeDelta Timestamp.ofEpochDays(2))
        assertEquals(TimeDelta.ofHours(10), start.toTimeDeltaFromEpoch())
    }

    @Test
    @OptIn(UnintendedOperation::class)
    fun `adding or negating timestamps is refused`() {
        val a = Timestamp.ofEpochSec(1)
        val b = Timestamp.ofEpochSec(2)

        assertThrows<UnitOperationException> { a + b }
        assertThrows<UnitOperationException> { a - b }
        assertThrows<UnitOperationException> { -a }
        assertThrows<UnitOperationException> { a * Percentage.ofPercentage(50) }
    }

    @Test
    fun `parses instants, bare milliseconds and durations since the Epoch`() {
        assertParses(
            "2001-09-09T01:46:40Z" to Timestamp.ofEpochSec(1e9),
            "10000" to Timestamp.ofEpochMs(10_000),
            "PT1H" to Timestamp.ofEpochHours(1),
        )
    }

    @Test
    fun `writes whole milliseconds as an instant and other values as epoch milliseconds`() {
        assertEquals("2001-09-09T01:46:40Z", write(Timestamp.ofEpochSec(1e9)))
        assertEquals("1000.5", write(Timestamp.ofEpochMicros(1_000_500)))
        assertRoundTrips(
            Timestamp.zero,
            Timestamp.ofEpochMs(1),
            Timestamp.ofEpochMs(0.1),
            Timestamp.ofEpochSec(1.0 / 3),
            Timestamp.ofEpochWeeks(2_800),
            Timestamp.ofEpochNs(1),
        )
    }
}
