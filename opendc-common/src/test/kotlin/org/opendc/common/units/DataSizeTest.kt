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

class DataSizeTest {
    @Test
    fun `every scale converts to every other scale`() {
        // Reference unit: bytes. Metric prefixes are powers of 1000, binary prefixes powers of 1024, a byte is 8 bits.
        assertScales(
            listOf(
                Scale("bits", 1.0 / 8, { DataSize.ofBits(it) }, { it.toBits() }),
                Scale("bytes", 1.0, { DataSize.ofBytes(it) }, { it.toBytes() }),
                Scale("Kb", 125.0, { DataSize.ofKb(it) }, { it.toKb() }),
                Scale("KB", 1e3, { DataSize.ofKB(it) }, { it.toKB() }),
                Scale("Mb", 125e3, { DataSize.ofMb(it) }, { it.toMb() }),
                Scale("MB", 1e6, { DataSize.ofMB(it) }, { it.toMB() }),
                Scale("Gb", 125e6, { DataSize.ofGb(it) }, { it.toGb() }),
                Scale("GB", 1e9, { DataSize.ofGB(it) }, { it.toGB() }),
                Scale("Tb", 125e9, { DataSize.ofTb(it) }, { it.toTb() }),
                Scale("TB", 1e12, { DataSize.ofTB(it) }, { it.toTB() }),
                Scale("Kib", 128.0, { DataSize.ofKib(it) }, { it.toKib() }),
                Scale("KiB", 1_024.0, { DataSize.ofKiB(it) }, { it.toKiB() }),
                Scale("Mib", 131_072.0, { DataSize.ofMib(it) }, { it.toMib() }),
                Scale("MiB", 1_048_576.0, { DataSize.ofMiB(it) }, { it.toMiB() }),
                Scale("Gib", 134_217_728.0, { DataSize.ofGib(it) }, { it.toGib() }),
                Scale("GiB", 1_073_741_824.0, { DataSize.ofGiB(it) }, { it.toGiB() }),
                Scale("Tib", 137_438_953_472.0, { DataSize.ofTib(it) }, { it.toTib() }),
                Scale("TiB", 1_099_511_627_776.0, { DataSize.ofTiB(it) }, { it.toTiB() }),
            ),
        )
    }

    @Test
    fun `binary prefixes are exact powers of two`() {
        assertEquals(DataSize.ofKiB(1), DataSize.ofBytes(1_024))
        assertEquals(DataSize.ofMiB(1), DataSize.ofKiB(1_024))
        assertEquals(DataSize.ofGiB(1), DataSize.ofMiB(1_024))
        assertEquals(DataSize.ofTiB(1), DataSize.ofGiB(1_024))
        assertEquals(DataSize.ofBytes(1), DataSize.ofBits(8))
    }

    @Test
    fun `formats with the largest prefix that keeps the value readable`() {
        assertFormats(
            DataSize.ofBytes(50) to "50 B",
            DataSize.ofKiB(50) to "50 KiB",
            DataSize.ofMiB(50) to "50 MiB",
            DataSize.ofGiB(2) to "2 GiB",
            DataSize.ofGiB(-2) to "-2 GiB",
        )
    }

    @Test
    fun `parses every supported spelling`() {
        assertParses(
            "8 b" to DataSize.ofBits(8),
            "16 bits" to DataSize.ofBits(16),
            "1 B" to DataSize.ofBytes(1),
            "2 bytes" to DataSize.ofBytes(2),
            "1 Kb" to DataSize.ofKb(1),
            "1 KB" to DataSize.ofKB(1),
            "1 kB" to DataSize.ofKB(1),
            "1 Kib" to DataSize.ofKib(1),
            "1 KiB" to DataSize.ofKiB(1),
            "1 Mb" to DataSize.ofMb(1),
            "1 MB" to DataSize.ofMB(1),
            "1 Mib" to DataSize.ofMib(1),
            "1 MiB" to DataSize.ofMiB(1),
            "100GB" to DataSize.ofGB(100),
            "1 Gb" to DataSize.ofGb(1),
            "1 Gib" to DataSize.ofGib(1),
            "1 GiB" to DataSize.ofGiB(1),
            "1 TB" to DataSize.ofTB(1),
            "1 Tb" to DataSize.ofTb(1),
            "1 Tib" to DataSize.ofTib(1),
            "1 TiB" to DataSize.ofTiB(1),
            "1.5 GiB" to DataSize.ofMiB(1_536),
            // Data sizes have no milli scale, so a lower-case "m" can only mean mega.
            "1 mB" to DataSize.ofMB(1),
            "  1    MB   " to DataSize.ofMB(1),
        )
    }

    @Test
    fun `a bare number is read as MiB`() {
        assertEquals(DataSize.ofMiB(512), parse<DataSize>("512"))
    }

    @Test
    fun `writes values that read back exactly`() {
        assertEquals("1536 MiB", write(DataSize.ofGiB(1.5)))
        assertRoundTrips(
            DataSize.zero,
            DataSize.ofBits(1),
            DataSize.ofKB(0.1),
            DataSize.ofGB(1.0 / 3),
            DataSize.ofTiB(512),
            DataSize.ofMiB(-2.5),
        )
    }
}
