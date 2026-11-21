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

import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.runtime.StartupEvent
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.web.server.model.Trace
import org.opendc.web.server.model.TracePart
import org.opendc.web.server.storage.ObjectStore
import org.opendc.web.server.storage.traceKey

/**
 * The seeder runs once while Quarkus starts, so these read what it left behind. Placeholder tables
 * that are not parquet are bundled for `bitbrains-small` under `src/test/resources/traces`; the other
 * built-ins ship none.
 */
@QuarkusTest
class BuiltInTracesTest {
    @Inject
    lateinit var store: ObjectStore

    @Inject
    lateinit var seeder: BuiltInTraces

    @Test
    fun `a bundled trace is stored under the key derived from its identity`() {
        val trace = traceOf("bitbrains-small")
        val parts = partsOf("bitbrains-small")

        assertEquals(setOf("tasks", "fragments"), parts.map { it.tableName }.toSet())
        for (part in parts) {
            val key = traceKey(trace.publicId, part.tableName)
            assertTrue(store.exists(key), "${part.tableName} was recorded but is not in the store")
            assertEquals(part.sizeBytes, store.open(key).use { it.readBytes().size.toLong() })
        }
    }

    @Test
    fun `a bundled file that cannot be read as parquet is stored without a count`() {
        assertTrue(partsOf("bitbrains-small").all { it.rowCount == null })
    }

    @Test
    fun `a built-in that ships no files is left unresolved`() {
        assertEquals(emptyList<String>(), partsOf("surf-week").map { it.tableName })
        assertEquals(emptyList<String>(), partsOf("surf-month").map { it.tableName })
    }

    @Test
    fun `seeding again changes nothing`() {
        val before = partsOf("bitbrains-small").map { it.tableName to it.sizeBytes }.toSet()

        seeder.seed(StartupEvent())

        assertEquals(before, partsOf("bitbrains-small").map { it.tableName to it.sizeBytes }.toSet())
        assertEquals(2, partsOf("bitbrains-small").size)
    }

    private fun traceOf(slug: String): Trace =
        QuarkusTransaction.requiringNew().call { checkNotNull(Trace.findBySlug(slug)) { "missing built-in $slug" } }

    private fun partsOf(slug: String): List<TracePart> = QuarkusTransaction.requiringNew().call { TracePart.findByTrace(traceOf(slug).id) }
}
