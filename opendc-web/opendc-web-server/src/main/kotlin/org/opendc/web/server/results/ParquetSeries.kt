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

package org.opendc.web.server.results

import jakarta.enterprise.context.ApplicationScoped
import org.apache.parquet.example.data.Group
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.hadoop.ParquetReader
import org.apache.parquet.hadoop.api.ReadSupport
import org.apache.parquet.hadoop.example.GroupReadSupport
import org.apache.parquet.io.InputFile
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.opendc.sdk.model.telemetry.OutputFileSpec
import org.opendc.web.launcher.EntityFold
import org.opendc.web.launcher.MetricPoint
import org.opendc.web.launcher.Reading
import org.opendc.web.launcher.ResultMetric
import org.opendc.web.launcher.RowIdentity
import org.opendc.web.launcher.rows
import org.opendc.web.server.storage.ObjectStore
import org.opendc.web.server.storage.StoredObjectFile
import java.util.Collections

/** The column every output table is stamped with, holding simulated milliseconds since the run began. */
private const val TIMESTAMP = "timestamp"

/** How many reduced runs are memoised at once; a reduction is small and its file never changes. */
private const val MEMOISED_RUNS = 512

/** Nothing has been read yet. Simulated time starts at zero, so it cannot stand in for this. */
private const val NO_INSTANT = Long.MIN_VALUE

/** What a table with one row per instant calls that row. Ids count up from zero, so none is this. */
private const val THE_ONLY_ROW = -1

/**
 * Reads the parquet a finished run left behind, the canonical account of a run, as the series a chart
 * is drawn from. Only the columns the charts name are read. A run is read once and remembered until a
 * retry calls [forget], since its output is otherwise never rewritten.
 */
@ApplicationScoped
class ParquetSeries(private val store: ObjectStore) {
    private val memoised =
        Collections.synchronizedMap(
            object : LinkedHashMap<String, Map<ResultMetric, List<MetricPoint>>>(MEMOISED_RUNS, LOAD_FACTOR, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Map<ResultMetric, List<MetricPoint>>>): Boolean =
                    size > MEMOISED_RUNS
            },
        )

    /**
     * What the run whose output lies under [prefix] measured, empty where nothing has landed. Only a
     * non-empty answer is remembered, so files that land later are still read.
     */
    fun read(prefix: String): Map<ResultMetric, List<MetricPoint>> {
        memoised[prefix]?.let { return it }
        val reduced =
            OutputFileSpec.entries.fold(
                emptyMap<ResultMetric, List<MetricPoint>>(),
            ) { all, table -> all + readTable(prefix, table) }
        if (reduced.isNotEmpty()) {
            memoised[prefix] = reduced
        }
        return reduced
    }

    /** Forgets every run under [prefix], because what it measured is about to be measured again. */
    fun forget(prefix: String) {
        synchronized(memoised) { memoised.keys.removeIf { it.startsWith(prefix) } }
    }

    private fun readTable(
        prefix: String,
        table: OutputFileSpec,
    ): Map<ResultMetric, List<MetricPoint>> {
        val metrics = ResultMetric.of(table)
        val key = "$prefix/${table.fileName}"
        if (metrics.isEmpty() || !store.exists(key)) {
            return emptyMap()
        }
        val file = StoredObjectFile(store, key)
        val schema = ParquetFileReader.open(file).use { it.fileMetaData.schema }
        val identity = (table.rows as? RowIdentity.PerEntity)?.takeIf { schema.containsField(it.column) }
        val capacities = capacities(prefix, identity, metrics)
        // The export spec decides which columns are written; a metric without its columns is left out, not zero.
        val present = metrics.filter { it.readable(schema, capacities) }
        if (present.isEmpty() || !schema.containsField(TIMESTAMP)) {
            return emptyMap()
        }

        val fold = InstantFold(present)
        rows(file, projection(schema, present, identity?.column)).use { rows ->
            var row = rows.read()
            while (row != null) {
                val current = row
                val entity = identity?.let { current.getInteger(it.column, 0) } ?: THE_ONLY_ROW
                fold.row(number(current, TIMESTAMP).toLong(), entity) { metric -> reading(current, entity, metric, capacities) }
                row = rows.read()
            }
        }
        return fold.finish()
    }

    /**
     * The capacities the share-of-capacity [metrics] divide by, per column and entity, read from the
     * meta file [identity] names. Empty where that file or a column has not landed.
     */
    private fun capacities(
        prefix: String,
        identity: RowIdentity.PerEntity?,
        metrics: List<ResultMetric>,
    ): Map<String, Map<Int, Double>> {
        val wanted = metrics.mapNotNull { (it.entities as? EntityFold.ShareOfCapacity)?.capacity }.distinct()
        if (identity == null || wanted.isEmpty() || !store.exists("$prefix/${identity.meta.path}")) {
            return emptyMap()
        }
        val file = StoredObjectFile(store, "$prefix/${identity.meta.path}")
        val schema = ParquetFileReader.open(file).use { it.fileMetaData.schema }
        val columns = wanted.filter { schema.containsField(it) }
        if (columns.isEmpty() || !schema.containsField(identity.column)) {
            return emptyMap()
        }
        val byColumn = columns.associateWith { mutableMapOf<Int, Double>() }
        val projection = MessageType(schema.name, (listOf(identity.column) + columns).map { schema.getType(it) })
        rows(file, projection).use { rows ->
            var row = rows.read()
            while (row != null) {
                val entity = row.getInteger(identity.column, 0)
                for (column in columns) {
                    byColumn.getValue(column)[entity] = number(row, column)
                }
                row = rows.read()
            }
        }
        return byColumn
    }

    private fun ResultMetric.readable(
        schema: MessageType,
        capacities: Map<String, Map<Int, Double>>,
    ): Boolean =
        columns.all { schema.containsField(it) } &&
            when (val fold = entities) {
                is EntityFold.ShareOfCapacity -> fold.capacity in capacities
                is EntityFold.Each, is EntityFold.Share -> true
            }

    private fun reading(
        row: Group,
        entity: Int,
        metric: ResultMetric,
        capacities: Map<String, Map<Int, Double>>,
    ): Reading =
        when (val fold = metric.entities) {
            is EntityFold.Each -> Reading(number(row, metric.column))
            is EntityFold.Share -> Reading(number(row, fold.part), number(row, fold.whole))
            is EntityFold.ShareOfCapacity -> Reading(number(row, fold.part), capacities.getValue(fold.capacity).getValue(entity))
        }

    private fun projection(
        schema: MessageType,
        metrics: List<ResultMetric>,
        identity: String?,
    ): MessageType {
        val read = (metrics.flatMap { it.columns } + listOfNotNull(identity)).distinct()
        return MessageType(schema.name, listOf(schema.getType(TIMESTAMP)) + read.map { schema.getType(it) })
    }

    private fun rows(
        file: InputFile,
        projection: MessageType,
    ): ParquetReader<Group> =
        object : ParquetReader.Builder<Group>(file) {
            override fun getReadSupport(): ReadSupport<Group> = GroupReadSupport()
        }.set(ReadSupport.PARQUET_READ_SCHEMA, projection.toString()).build()

    private fun number(
        row: Group,
        column: String,
    ): Double =
        when (val type = row.type.getType(column).asPrimitiveType().primitiveTypeName) {
            PrimitiveTypeName.INT32 -> row.getInteger(column, 0).toDouble()
            PrimitiveTypeName.INT64 -> row.getLong(column, 0).toDouble()
            PrimitiveTypeName.FLOAT -> row.getFloat(column, 0).toDouble()
            PrimitiveTypeName.DOUBLE -> row.getDouble(column, 0)
            else -> throw IllegalStateException("$column is a $type, which is not a measurement")
        }

    private companion object {
        const val LOAD_FACTOR = 0.75f
    }
}

/**
 * Folds the rows of one table into one value per metric per instant, across hosts.
 *
 * Rows are keyed by entity because the writer records the last instant of a run twice, and the
 * second row for a host replaces the first. Rows arrive in time order, so an instant is complete when
 * a later one begins, which bounds memory by the number of hosts.
 */
private class InstantFold(private val metrics: List<ResultMetric>) {
    private val reporting = metrics.associateWith { mutableMapOf<Int, Reading>() }
    private val series = metrics.associateWith { mutableListOf<MetricPoint>() }
    private var instant = NO_INSTANT

    fun row(
        timestamp: Long,
        entity: Int,
        reading: (ResultMetric) -> Reading,
    ) {
        if (timestamp != instant) {
            close()
            instant = timestamp
        }
        for (metric in metrics) {
            reporting.getValue(metric)[entity] = reading(metric)
        }
    }

    fun finish(): Map<ResultMetric, List<MetricPoint>> {
        close()
        return metrics.associateWith { series.getValue(it).toList() }.filterValues { it.isNotEmpty() }
    }

    private fun close() {
        if (instant == NO_INSTANT) {
            return
        }
        for (metric in metrics) {
            val byEntity = reporting.getValue(metric)
            if (byEntity.isNotEmpty()) {
                series.getValue(metric).add(MetricPoint(instant, metric.fold(byEntity.values)))
                byEntity.clear()
            }
        }
        instant = NO_INSTANT
    }
}
