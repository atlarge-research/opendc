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

class DataRateTest {
    @Test
    fun `every scale converts to every other scale`() {
        // Reference unit: bits per second. Lower-case b is bits, upper-case B is bytes.
        assertScales(
            listOf(
                Scale("bps", 1.0, { DataRate.ofbps(it) }, { it.tobps() }),
                Scale("Bps", 8.0, { DataRate.ofBps(it) }, { it.toBps() }),
                Scale("Kbps", 1e3, { DataRate.ofKbps(it) }, { it.toKbps() }),
                Scale("KBps", 8e3, { DataRate.ofKBps(it) }, { it.toKBps() }),
                Scale("Kibps", 1_024.0, { DataRate.ofKibps(it) }, { it.toKibps() }),
                Scale("KiBps", 8_192.0, { DataRate.ofKiBps(it) }, { it.toKiBps() }),
                Scale("Mbps", 1e6, { DataRate.ofMbps(it) }, { it.toMbps() }),
                Scale("MBps", 8e6, { DataRate.ofMBps(it) }, { it.toMBps() }),
                Scale("Mibps", 1_048_576.0, { DataRate.ofMibps(it) }, { it.toMibps() }),
                Scale("MiBps", 8_388_608.0, { DataRate.ofMiBps(it) }, { it.toMiBps() }),
                Scale("Gbps", 1e9, { DataRate.ofGbps(it) }, { it.toGbps() }),
                Scale("GBps", 8e9, { DataRate.ofGBps(it) }, { it.toGBps() }),
                Scale("Gibps", 1_073_741_824.0, { DataRate.ofGibps(it) }, { it.toGibps() }),
                Scale("GiBps", 8_589_934_592.0, { DataRate.ofGiBps(it) }, { it.toGiBps() }),
            ),
        )
    }

    @Test
    fun `a byte per second is eight bits per second`() {
        assertEquals(DataRate.ofbps(8), DataRate.ofBps(1))
        assertEquals(1.0, DataRate.ofbps(8).toBps())
        assertEquals(8.0, DataRate.ofBps(1).tobps())
    }

    @Test
    fun `formats with the largest prefix that keeps the value readable`() {
        assertFormats(
            DataRate.ofbps(50) to "50 bps",
            DataRate.ofKbps(50) to "50 kbps",
            DataRate.ofMbps(50) to "50 Mbps",
            DataRate.ofGbps(2) to "2 Gbps",
        )
    }

    @Test
    fun `parses every supported spelling`() {
        assertParses(
            "1 bps" to DataRate.ofbps(1),
            "1 bit/s" to DataRate.ofbps(1),
            "1 Bps" to DataRate.ofBps(1),
            "1 Kbps" to DataRate.ofKbps(1),
            "1 kbps" to DataRate.ofKbps(1),
            "10KBps" to DataRate.ofKBps(10),
            "1 Kibps" to DataRate.ofKibps(1),
            "1 KiBps" to DataRate.ofKiBps(1),
            "1 Mb/s" to DataRate.ofMbps(1),
            "1 MB/s" to DataRate.ofMBps(1),
            "1 Mibps" to DataRate.ofMibps(1),
            "1 MiBps" to DataRate.ofMiBps(1),
            "1 Gbps" to DataRate.ofGbps(1),
            "10 Gbit/s" to DataRate.ofGbps(10),
            "1 Gb per s" to DataRate.ofGbps(1),
            "   0.3    GBps  " to DataRate.ofGBps(0.3),
            "1 Gibps" to DataRate.ofGibps(1),
            "1 GiBps" to DataRate.ofGiBps(1),
        )
    }

    @Test
    fun `a bare number is read as Kibps`() {
        assertEquals(DataRate.ofKibps(100), parse<DataRate>("100"))
    }

    @Test
    fun `writes values that read back exactly`() {
        assertEquals("1500 bps", write(DataRate.ofKbps(1.5)))
        assertRoundTrips(
            DataRate.zero,
            DataRate.ofbps(1),
            DataRate.ofMBps(0.1),
            DataRate.ofGbps(1.0 / 3),
            DataRate.ofGiBps(400),
            DataRate.ofKbps(-2.5),
        )
    }
}
