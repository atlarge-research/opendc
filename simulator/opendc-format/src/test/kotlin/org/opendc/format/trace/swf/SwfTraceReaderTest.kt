/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.format.trace.swf

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class SwfTraceReaderTest {
    @Test
    internal fun testParseSwf() {
        val reader = SwfTraceReader(File(SwfTraceReaderTest::class.java.getResource("/swf_trace.txt").toURI()))
        var entry = reader.next()
        assertEquals(0, entry.submissionTime)
        // 1961 slices for waiting, 3 full and 1 partial running slices
        assertEquals(1965, entry.workload.image.flopsHistory.toList().size)

        entry = reader.next()
        assertEquals(164472, entry.submissionTime)
        // 1188 slices for waiting, 0 full and 1 partial running slices
        assertEquals(1189, entry.workload.image.flopsHistory.toList().size)
        assertEquals(5_100_000L, entry.workload.image.flopsHistory.toList().last().flops)
        assertEquals(0.25, entry.workload.image.flopsHistory.toList().last().usage)
    }
}
