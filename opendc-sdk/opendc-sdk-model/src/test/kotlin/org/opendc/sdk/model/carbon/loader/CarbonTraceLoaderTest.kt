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

package org.opendc.sdk.model.carbon.loader

import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.api.WriteSupport
import org.apache.parquet.io.api.RecordConsumer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.opendc.simulator.compute.carbon.CarbonFragment
import org.opendc.trace.formats.carbon.parquet.CARBON_SCHEMA
import org.opendc.trace.parquet.LocalParquetWriter
import java.io.File
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Verifies that [CarbonTraceLoader] turns a carbon intensity trace into [CarbonFragment]s that are
 * ordered by start time and together cover the whole timeline.
 */
class CarbonTraceLoaderTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `fragments are sorted by start time and cover the whole timeline`() {
        val trace = writeTrace(hour(2) to 120.0, hour(0) to 80.0, hour(1) to 100.0)

        val fragments = CarbonTraceLoader(trace).load()

        assertEquals(
            listOf(
                Triple(Long.MIN_VALUE, hour(1), 80.0),
                Triple(hour(1), hour(2), 100.0),
                Triple(hour(2), Long.MAX_VALUE, 120.0),
            ),
            fragments.summary(),
        )
    }

    @Test
    fun `a single entry covers the whole timeline`() {
        val trace = writeTrace(hour(0) to 100.0)

        val fragments = CarbonTraceLoader(trace).load()

        assertEquals(listOf(Triple(Long.MIN_VALUE, Long.MAX_VALUE, 100.0)), fragments.summary())
    }

    @Test
    fun `loading the same trace twice returns the same fragments`() {
        val loader = CarbonTraceLoader(writeTrace(hour(0) to 80.0, hour(1) to 120.0))

        val first = loader.load().summary()
        val second = loader.load().summary()

        assertEquals(first, second)
    }

    @Test
    fun `a missing trace is rejected`() {
        val loader = CarbonTraceLoader(tempDir.resolve("missing.parquet").toFile())

        val error = assertFailsWith<IllegalArgumentException> { loader.load() }

        assertContains(error.message.orEmpty(), "cannot be found")
    }

    @Test
    fun `an empty trace is rejected`() {
        val loader = CarbonTraceLoader(writeTrace())

        val error = assertFailsWith<IllegalStateException> { loader.load() }

        assertContains(error.message.orEmpty(), "contains no entries")
    }

    /**
     * Write a carbon trace with the given (timestamp in epoch millis, carbon intensity) entries, in the given order.
     */
    private fun writeTrace(vararg entries: Pair<Long, Double>): File {
        val path = tempDir.resolve("carbon.parquet")

        LocalParquetWriter.builder(path, CarbonWriteSupport()).build().use { writer ->
            entries.forEach { writer.write(it) }
        }

        return path.toFile()
    }

    private fun hour(n: Long): Long = START.plus(Duration.ofHours(n)).toEpochMilli()

    private fun List<CarbonFragment>.summary(): List<Triple<Long, Long, Double>> =
        map { Triple(it.startTime, it.endTime, it.carbonIntensity) }

    /**
     * Writes (timestamp, carbon intensity) entries using the schema the carbon trace reader expects.
     */
    private class CarbonWriteSupport : WriteSupport<Pair<Long, Double>>() {
        private lateinit var recordConsumer: RecordConsumer

        override fun init(configuration: Configuration): WriteContext = WriteContext(CARBON_SCHEMA, emptyMap())

        override fun prepareForWrite(recordConsumer: RecordConsumer) {
            this.recordConsumer = recordConsumer
        }

        override fun write(record: Pair<Long, Double>) {
            val (timestamp, carbonIntensity) = record

            recordConsumer.startMessage()
            recordConsumer.startField("timestamp", 0)
            recordConsumer.addLong(timestamp)
            recordConsumer.endField("timestamp", 0)
            recordConsumer.startField("carbon_intensity", 1)
            recordConsumer.addDouble(carbonIntensity)
            recordConsumer.endField("carbon_intensity", 1)
            recordConsumer.endMessage()
        }
    }

    private companion object {
        val START: Instant = Instant.parse("2022-01-01T00:00:00Z")
    }
}
