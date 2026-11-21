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

package org.opendc.web.server.traces

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.opendc.trace.spi.TraceFormat
import org.opendc.web.server.ApiTest
import org.opendc.web.server.model.TraceKind
import org.opendc.web.server.rest.InvalidDocumentException
import org.opendc.web.server.storage.LocalObjectStore
import org.opendc.web.server.storage.ObjectStore
import org.opendc.web.server.storage.traceKey
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class TraceIngestTest {
    @TempDir
    lateinit var root: Path

    private val store: ObjectStore by lazy { LocalObjectStore(root) }
    private val ingest: TraceIngest by lazy { TraceIngest(store) }

    // A table is checked against the format named by its kind in lower case, so every kind needs a reader.
    @Test
    fun `every kind names a format opendc-trace can open`() {
        for (kind in TraceKind.entries) {
            assertNotNull(TraceFormat.byName(kind.name.lowercase()), "no reader answers to ${kind.name.lowercase()}")
        }
    }

    @Test
    fun `a real carbon table is accepted and counted`() {
        val key = store("fixtures/traces/carbon.parquet")

        val size = ingest.inspect(TraceKind.CARBON, "carbon", key)

        assertEquals(Files.size(ApiTest.fixturePath("traces/carbon.parquet")), size.sizeBytes)
        assertTrue(size.rowCount > 0, "a real trace has rows")
    }

    @Test
    fun `a real workload's tables are accepted and counted`() {
        val tasks = ingest.inspect(TraceKind.WORKLOAD, "tasks", store("fixtures/traces/workload/tasks.parquet"))
        val fragments = ingest.inspect(TraceKind.WORKLOAD, "fragments", store("fixtures/traces/workload/fragments.parquet"))

        assertTrue(tasks.rowCount > 0)
        assertTrue(fragments.rowCount > tasks.rowCount, "a workload has more fragments than tasks")
    }

    @Test
    fun `content that is not parquet at all is refused, naming the table`() {
        val key = traceKey(UUID.randomUUID(), "carbon")
        store.put(key, "not a trace".byteInputStream())

        val problem = assertThrows<InvalidDocumentException> { ingest.inspect(TraceKind.CARBON, "carbon", key) }

        assertEquals("carbon", problem.problem.issues.single().path)
    }

    // A real parquet file of the wrong sort opens fine; only its columns give it away.
    @Test
    fun `a parquet file that is not the table it was filed as is refused`() {
        val key = store("fixtures/traces/carbon.parquet")

        val problem = assertThrows<InvalidDocumentException> { ingest.inspect(TraceKind.WORKLOAD, "tasks", key) }

        assertEquals("tasks", problem.problem.issues.single().path)
        assertTrue("column" in problem.problem.issues.single().message, problem.problem.issues.single().message)
    }

    private fun store(fixture: String): String {
        val key = traceKey(UUID.randomUUID(), fixture.substringAfterLast('/').substringBefore('.'))
        Files.newInputStream(ApiTest.fixturePath(fixture.removePrefix("fixtures/"))).use { store.put(key, it) }
        return key
    }
}
