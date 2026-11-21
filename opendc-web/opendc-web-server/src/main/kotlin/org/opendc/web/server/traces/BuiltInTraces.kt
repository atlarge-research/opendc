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

import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.transaction.Transactional
import org.apache.parquet.hadoop.ParquetFileReader
import org.opendc.web.server.model.Trace
import org.opendc.web.server.model.TracePart
import org.opendc.web.server.storage.ObjectStore
import org.opendc.web.server.storage.StoredObjectFile
import org.opendc.web.server.storage.traceKey
import org.slf4j.LoggerFactory

/**
 * Puts the traces the deployment ships with into the store, so a built-in resolves exactly the way an
 * upload does. Files are bundled as resources under `traces/<slug>/<table>.parquet`; a trace whose
 * files are not bundled is left unresolved.
 */
@ApplicationScoped
class BuiltInTraces(private val store: ObjectStore) {
    // Failures are not swallowed: a library that cannot be filled is a misconfiguration to catch at boot.
    @Transactional
    internal fun seed(
        @Suppress("UNUSED_PARAMETER") @Observes event: StartupEvent,
    ) {
        for (trace in Trace.findBuiltIns()) {
            for (table in trace.kind.tables) {
                seedTable(trace, table)
            }
        }
    }

    // A shipped file is trusted to be the table it claims, unlike an upload, but its footer is still
    // read: the row count decides the memory an experiment over it is given.
    private fun seedTable(
        trace: Trace,
        table: String,
    ) {
        val key = traceKey(trace.publicId, table)
        val existing = TracePart.find(trace.id, table)
        // A row without its object means the bucket was replaced, so the resource is stored again.
        if (existing != null && store.exists(key)) {
            return
        }

        val bundled = javaClass.getResourceAsStream("/traces/${trace.slug}/$table.parquet")
        if (bundled == null) {
            LOG.info("Built-in trace {} ships no {} table; leaving it unresolved", trace.slug, table)
            return
        }

        val size = bundled.use { store.put(key, it) }
        val part =
            existing ?: TracePart().also {
                it.trace = trace
                it.tableName = table
            }
        part.sizeBytes = size
        part.rowCount = rowCount(key)
        if (existing == null) {
            part.persist()
        }
        LOG.info("Stored built-in trace {} table {} ({} bytes, {} rows)", trace.slug, table, size, part.rowCount)
    }

    /**
     * How many rows the stored file holds, or null when it is not parquet; the trace still resolves
     * and dispatch estimates from the scenario alone.
     */
    private fun rowCount(key: String): Long? =
        try {
            ParquetFileReader.open(StoredObjectFile(store, key)).use { it.recordCount }
        } catch (e: Exception) {
            LOG.warn("Built-in {} could not be read as parquet, so it is stored uncounted: {}", key, e.message)
            null
        }

    private companion object {
        val LOG = LoggerFactory.getLogger(BuiltInTraces::class.java)
    }
}
