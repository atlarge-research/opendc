/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.web.runner.internal

import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Test suite for [ReportCollector].
 */
class ReportCollectorTest {
    private lateinit var collector: ReportCollector

    @BeforeEach
    fun setUp() {
        collector = ReportCollector()
    }

    @Test
    fun testCollectEmpty() {
        val report = collector.collect()
        val logs = report["logs"] as List<*>
        val summary = report["summary"] as Map<*, *>

        assertTrue(logs.isEmpty())
        assertEquals(0, summary["totalWarnings"])
        assertEquals(0, summary["totalErrors"])
    }

    @Test
    fun testCollectWithRuntimeAndWaitTime() {
        val report = collector.collect(runtimeSeconds = 120, waitTimeSeconds = 30)
        val summary = report["summary"] as Map<*, *>

        assertEquals(120, summary["runtimeSeconds"])
        assertEquals(30, summary["waitTimeSeconds"])
    }

    @Test
    fun testCollectWithTimestamps() {
        val createdAt = Instant.parse("2024-01-01T00:00:00Z")
        val startedAt = Instant.parse("2024-01-01T00:01:00Z")

        val report = collector.collect(createdAt = createdAt, startedAt = startedAt)

        assertEquals(createdAt.toString(), report["createdAt"])
        assertEquals(startedAt.toString(), report["startedAt"])
    }

    @Test
    fun testCollectWithoutOptionalParams() {
        val report = collector.collect()

        assertTrue(!report.containsKey("createdAt"))
        assertTrue(!report.containsKey("startedAt"))
        val summary = report["summary"] as Map<*, *>
        assertTrue(!summary.containsKey("runtimeSeconds"))
        assertTrue(!summary.containsKey("waitTimeSeconds"))
    }

    @Test
    fun testAttachAndDetect() {
        // Attach collector to root logger
        collector.attach()

        val logger = LogManager.getLogger(ReportCollectorTest::class.java)
        logger.warn("test warning message")
        logger.error("test error message")

        collector.detach()

        // Log after detach should not be captured
        logger.warn("this should not be captured")

        val report = collector.collect()
        val logs = report["logs"] as List<*>
        val summary = report["summary"] as Map<*, *>

        assertEquals(2, logs.size)
        assertEquals(1, summary["totalWarnings"])
        assertEquals(1, summary["totalErrors"])
    }

    @Test
    fun testOnlyWarnAndErrorCaptured() {
        collector.attach()

        val logger = LogManager.getLogger(ReportCollectorTest::class.java)
        logger.info("info message - should not be captured")
        logger.debug("debug message - should not be captured")
        logger.warn("warn message - should be captured")
        logger.error("error message - should be captured")

        collector.detach()

        val report = collector.collect()
        val logs = report["logs"] as List<*>

        assertEquals(2, logs.size)
    }

    @Test
    fun testLogEntryStructure() {
        collector.attach()

        val logger = LogManager.getLogger(ReportCollectorTest::class.java)
        logger.warn("test warning")

        collector.detach()

        val report = collector.collect()
        val logs = report["logs"] as List<*>
        val entry = logs[0] as Map<*, *>

        assertNotNull(entry["timestamp"])
        assertEquals("WARN", entry["level"])
        assertEquals(ReportCollectorTest::class.java.name, entry["logger"])
        assertEquals("test warning", entry["message"])
    }

    @Test
    fun testClear() {
        collector.attach()

        val logger = LogManager.getLogger(ReportCollectorTest::class.java)
        logger.warn("test warning")

        collector.detach()

        collector.clear()

        val report = collector.collect()
        val logs = report["logs"] as List<*>
        assertTrue(logs.isEmpty())
    }

    @Test
    fun testSummaryCountsPerLevel() {
        collector.attach()

        val logger = LogManager.getLogger(ReportCollectorTest::class.java)
        logger.warn("warn 1")
        logger.warn("warn 2")
        logger.error("error 1")

        collector.detach()

        val report = collector.collect()
        val summary = report["summary"] as Map<*, *>

        assertEquals(2, summary["totalWarnings"])
        assertEquals(1, summary["totalErrors"])
    }
}
