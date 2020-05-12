package com.atlarge.opendc.format.trace.swf

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
