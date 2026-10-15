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
import org.opendc.common.units.TimeDelta.Companion.toTimeDelta
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals

class TimeDeltaTest {
    @Test
    fun `every scale converts to every other scale`() {
        // Reference unit: milliseconds.
        assertScales(
            listOf(
                Scale("ns", 1e-6, { TimeDelta.ofNs(it) }, { it.toNs() }),
                Scale("micros", 1e-3, { TimeDelta.ofMicros(it) }, { it.toMicros() }),
                Scale("ms", 1.0, { TimeDelta.ofMs(it) }, { it.toMs() }),
                Scale("sec", 1_000.0, { TimeDelta.ofSec(it) }, { it.toSec() }),
                Scale("min", 60_000.0, { TimeDelta.ofMin(it) }, { it.toMin() }),
                Scale("hours", 3_600_000.0, { TimeDelta.ofHours(it) }, { it.toHours() }),
                Scale("days", 86_400_000.0, { TimeDelta.ofDays(it) }, { it.toDays() }),
                Scale("weeks", 604_800_000.0, { TimeDelta.ofWeeks(it) }, { it.toWeeks() }),
            ),
        )
    }

    @Test
    fun `whole units convert exactly`() {
        assertEquals(TimeDelta.ofDays(7), TimeDelta.ofWeeks(1))
        assertEquals(TimeDelta.ofHours(24), TimeDelta.ofDays(1))
        assertEquals(TimeDelta.ofMin(90), TimeDelta.ofHours(1.5))
        assertEquals(14.0, TimeDelta.ofWeeks(2).toDays())
        assertEquals(1_500L, TimeDelta.ofSec(1.5).toMsLong())
    }

    @Test
    fun `converts from and to java time`() {
        assertEquals(TimeDelta.ofHours(3), Duration.ofHours(3).toTimeDelta())
        assertEquals(TimeDelta.ofNs(1_500), Duration.ofNanos(1_500).toTimeDelta())
        assertEquals(TimeDelta.ofDays(1), TimeDelta.ofDuration(Duration.ofDays(1)))
        assertEquals(Instant.parse("1970-01-02T00:00:00Z"), TimeDelta.ofDays(1).toInstantFromEpoch())
    }

    @Test
    fun `formats as an ISO-8601 duration`() {
        assertEquals("PT1H30M", TimeDelta.ofMin(90).toString())
        assertEquals("PT0.25S", TimeDelta.ofMs(250).toString())
    }

    @Test
    fun `parses every supported spelling`() {
        assertParses(
            "1 ns" to TimeDelta.ofNs(1),
            "2 nanoseconds" to TimeDelta.ofNs(2),
            "3 microseconds" to TimeDelta.ofMicros(3),
            "4 ms" to TimeDelta.ofMs(4),
            "5 milliseconds" to TimeDelta.ofMs(5),
            "6 s" to TimeDelta.ofSec(6),
            "7 sec" to TimeDelta.ofSec(7),
            "8 seconds" to TimeDelta.ofSec(8),
            "9 m" to TimeDelta.ofMin(9),
            "  30    minutes   " to TimeDelta.ofMin(30),
            "1h" to TimeDelta.ofHours(1),
            "10 hours" to TimeDelta.ofHours(10),
            "2 d" to TimeDelta.ofDays(2),
            "3 days" to TimeDelta.ofDays(3),
            "1 w" to TimeDelta.ofWeeks(1),
            "2 weeks" to TimeDelta.ofWeeks(2),
            "PT13H" to TimeDelta.ofHours(13),
            "0.5 s" to TimeDelta.ofMs(500),
        )
    }

    @Test
    fun `a bare number is read as milliseconds`() {
        assertEquals(TimeDelta.ofMs(1500), parse<TimeDelta>("1500"))
    }

    @Test
    fun `writes values that read back exactly`() {
        assertEquals("1500 ms", write(TimeDelta.ofSec(1.5)))
        assertRoundTrips(
            TimeDelta.zero,
            TimeDelta.ofNs(1),
            TimeDelta.ofSec(0.1),
            TimeDelta.ofSec(1.0 / 3),
            TimeDelta.ofWeeks(52),
            TimeDelta.ofMs(-2.5),
        )
    }
}
