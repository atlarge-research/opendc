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

package org.opendc.sdk.model.failure.loader

import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.api.WriteSupport
import org.apache.parquet.io.api.RecordConsumer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.opendc.simulator.failure.models.Failure
import org.opendc.trace.formats.failure.parquet.FAILURE_SCHEMA
import org.opendc.trace.parquet.LocalParquetWriter
import java.io.File
import java.nio.file.Path
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Verifies that [FailureTraceLoader] turns a failure trace into [Failure]s in trace order, rotated to the
 * requested start point.
 */
class FailureTraceLoaderTest {
    @TempDir
    lateinit var tempDir: Path

    private val failures =
        listOf(
            Failure(1_000, 100, 0.1),
            Failure(2_000, 200, 0.2),
            Failure(3_000, 300, 0.3),
            Failure(4_000, 400, 0.4),
        )

    @Test
    fun `failures are loaded in trace order`() {
        val loader = FailureTraceLoader(writeTrace(failures))

        assertEquals(failures, loader.load())
    }

    @Test
    fun `the start point rotates the failures`() {
        val loader = FailureTraceLoader(writeTrace(failures))

        assertEquals(failures.drop(2) + failures.take(2), loader.load(startPoint = 0.5))
    }

    @Test
    fun `the start index is rounded down`() {
        val loader = FailureTraceLoader(writeTrace(failures))

        assertEquals(failures.drop(1) + failures.take(1), loader.load(startPoint = 0.49))
    }

    @Test
    fun `an empty trace loads no failures`() {
        val loader = FailureTraceLoader(writeTrace(emptyList()))

        assertEquals(emptyList(), loader.load(startPoint = 0.5))
    }

    @Test
    fun `a start point outside the trace is rejected`() {
        val loader = FailureTraceLoader(writeTrace(failures))

        val error = assertFailsWith<IllegalArgumentException> { loader.load(startPoint = 1.0) }

        assertContains(error.message.orEmpty(), "start point")
    }

    @Test
    fun `an invalid failure is rejected with its row and trace`() {
        val trace = writeEntries(listOf(Entry(1_000, 100, 0.1), Entry(2_000, 0, 0.2)))

        val error = assertFailsWith<IllegalArgumentException> { FailureTraceLoader(trace).load() }

        assertContains(error.message.orEmpty(), "row 1 of $trace")
        assertContains(error.message.orEmpty(), "duration greater than 0")
    }

    @Test
    fun `a missing trace is rejected`() {
        val loader = FailureTraceLoader(tempDir.resolve("missing.parquet").toFile())

        val error = assertFailsWith<IllegalArgumentException> { loader.load() }

        assertContains(error.message.orEmpty(), "cannot be found")
    }

    /**
     * Write a failure trace with the given [failures], in the given order.
     */
    private fun writeTrace(failures: List<Failure>): File =
        writeEntries(failures.map { Entry(it.failureInterval, it.failureDuration, it.failureIntensity) })

    /**
     * Write a failure trace with the given raw [entries], in the given order, without validating them.
     */
    private fun writeEntries(entries: List<Entry>): File {
        val path = tempDir.resolve("failures.parquet")

        LocalParquetWriter.builder(path, FailureWriteSupport()).build().use { writer ->
            entries.forEach { writer.write(it) }
        }

        return path.toFile()
    }

    /**
     * A row of a failure trace.
     */
    private data class Entry(val interval: Long, val duration: Long, val intensity: Double)

    /**
     * Writes [Entry]s using the schema the failure trace reader expects.
     */
    private class FailureWriteSupport : WriteSupport<Entry>() {
        private lateinit var recordConsumer: RecordConsumer

        override fun init(configuration: Configuration): WriteContext = WriteContext(FAILURE_SCHEMA, emptyMap())

        override fun prepareForWrite(recordConsumer: RecordConsumer) {
            this.recordConsumer = recordConsumer
        }

        override fun write(record: Entry) {
            recordConsumer.startMessage()
            recordConsumer.startField("failure_interval", 0)
            recordConsumer.addLong(record.interval)
            recordConsumer.endField("failure_interval", 0)
            recordConsumer.startField("failure_duration", 1)
            recordConsumer.addLong(record.duration)
            recordConsumer.endField("failure_duration", 1)
            recordConsumer.startField("failure_intensity", 2)
            recordConsumer.addDouble(record.intensity)
            recordConsumer.endField("failure_intensity", 2)
            recordConsumer.endMessage()
        }
    }
}
