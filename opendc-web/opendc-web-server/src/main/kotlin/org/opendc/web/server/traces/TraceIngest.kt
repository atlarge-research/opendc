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

import jakarta.enterprise.context.ApplicationScoped
import org.apache.parquet.hadoop.ParquetFileReader
import org.opendc.trace.spi.TraceFormat
import org.opendc.web.server.model.Trace
import org.opendc.web.server.model.TraceKind
import org.opendc.web.server.model.TracePart
import org.opendc.web.server.rest.DocumentIssue
import org.opendc.web.server.rest.invalidDocument
import org.opendc.web.server.storage.ObjectStore
import org.opendc.web.server.storage.StoredObjectFile
import org.opendc.web.server.storage.traceKey
import java.nio.file.Path
import java.time.Instant

/** How large a stored table is. */
data class TableSize(
    val sizeBytes: Long,
    val rowCount: Long,
)

/**
 * Checks that an uploaded table really is the table it was filed as, and measures it. Only the
 * parquet footer is read: it carries the schema and row count at the same cost for any file size.
 */
@ApplicationScoped
class TraceIngest(private val store: ObjectStore) {
    /** Inspects every table of [trace] in the store and records its size, which makes the trace whole. */
    fun record(trace: Trace) {
        for (table in trace.kind.tables) {
            val size = inspect(trace.kind, table, traceKey(trace.publicId, table))
            val part =
                TracePart.find(trace.id, table) ?: TracePart().also {
                    it.trace = trace
                    it.tableName = table
                    it.persist()
                }
            part.sizeBytes = size.sizeBytes
            part.rowCount = size.rowCount
        }
        trace.updatedAt = Instant.now()
    }

    fun inspect(
        kind: TraceKind,
        table: String,
        key: String,
    ): TableSize {
        val footer =
            try {
                ParquetFileReader.open(StoredObjectFile(store, key)).use { reader ->
                    Footer(reader.fileMetaData.schema.fields.map { it.name }.toSet(), reader.recordCount)
                }
            } catch (e: Exception) {
                throw invalidDocument(
                    "The $table table could not be read as parquet",
                    listOf(DocumentIssue(table, e.message ?: "not a readable parquet file")),
                )
            }

        // Real traces omit columns, so the full set cannot be demanded; a column of a sibling table is
        // what gives away two swapped files.
        val own = columnsOf(kind, table)
        if (footer.columns.intersect(own).isEmpty()) {
            throw invalidDocument(
                "That file is not a $table table",
                listOf(DocumentIssue(table, "none of its columns belong to $table")),
            )
        }
        val elsewhere = footer.columns.intersect(siblingColumns(kind, table) - own)
        if (elsewhere.isNotEmpty()) {
            throw invalidDocument(
                "That file is not a $table table",
                listOf(DocumentIssue(table, "it carries the ${elsewhere.sorted().joinToString(", ")} column of another table")),
            )
        }
        return TableSize(sizeBytes = store.size(key), rowCount = footer.rowCount)
    }

    /** The columns opendc-trace knows for [table], asked of the format so this cannot drift. */
    private fun columnsOf(
        kind: TraceKind,
        table: String,
    ): Set<String> {
        val format = checkNotNull(TraceFormat.byName(kind.name.lowercase())) { "no reader answers to ${kind.name}" }
        return format.getDetails(Path.of("."), table).columns.map { it.name }.toSet()
    }

    private fun siblingColumns(
        kind: TraceKind,
        table: String,
    ): Set<String> = kind.tables.filter { it != table }.flatMap { columnsOf(kind, it) }.toSet()

    private data class Footer(
        val columns: Set<String>,
        val rowCount: Long,
    )
}
