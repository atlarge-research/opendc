package com.atlarge.opendc.format.trace.swf

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class SwfTraceReaderTest {
    @Test
    internal fun testParseSwf() {
        val reader = SwfTraceReader(File(SwfTraceReaderTest::class.java.getResource("/swf_trace.txt").toURI()))
        val entry = reader.next()
        assertEquals(entry.submissionTime, 0)
    }
}
