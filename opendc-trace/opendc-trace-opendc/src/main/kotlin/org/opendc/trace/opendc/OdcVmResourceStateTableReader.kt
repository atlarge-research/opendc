/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.trace.opendc

import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.opendc.trace.*
import org.opendc.trace.util.parquet.LocalParquetReader
import java.time.Duration
import java.time.Instant

/**
 * A [TableReader] implementation for the OpenDC virtual machine trace format.
 */
internal class OdcVmResourceStateTableReader(private val reader: LocalParquetReader<GenericRecord>) : TableReader {
    /**
     * The current record.
     */
    private var record: GenericRecord? = null

    /**
     * A flag to indicate that the columns have been initialized.
     */
    private var hasInitializedColumns = false

    override fun nextRow(): Boolean {
        val record = reader.read()
        this.record = record

        if (!hasInitializedColumns && record != null) {
            initColumns(record.schema)
            hasInitializedColumns = true
        }

        return record != null
    }

    override fun hasColumn(column: TableColumn<*>): Boolean {
        return when (column) {
            RESOURCE_STATE_ID -> true
            RESOURCE_STATE_TIMESTAMP -> true
            RESOURCE_STATE_DURATION -> true
            RESOURCE_STATE_CPU_COUNT -> true
            RESOURCE_STATE_CPU_USAGE -> true
            else -> false
        }
    }

    override fun <T> get(column: TableColumn<T>): T {
        val record = checkNotNull(record) { "Reader in invalid state" }

        @Suppress("UNCHECKED_CAST")
        val res: Any = when (column) {
            RESOURCE_STATE_ID -> record[COL_ID].toString()
            RESOURCE_STATE_TIMESTAMP -> Instant.ofEpochMilli(record[COL_TIMESTAMP] as Long)
            RESOURCE_STATE_DURATION -> Duration.ofMillis(record[COL_DURATION] as Long)
            RESOURCE_STATE_CPU_COUNT -> getInt(RESOURCE_STATE_CPU_COUNT)
            RESOURCE_STATE_CPU_USAGE -> getDouble(RESOURCE_STATE_CPU_USAGE)
            else -> throw IllegalArgumentException("Invalid column")
        }

        @Suppress("UNCHECKED_CAST")
        return res as T
    }

    override fun getBoolean(column: TableColumn<Boolean>): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(column: TableColumn<Int>): Int {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (column) {
            RESOURCE_STATE_CPU_COUNT -> record[COL_CPU_COUNT] as Int
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getLong(column: TableColumn<Long>): Long {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getDouble(column: TableColumn<Double>): Double {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (column) {
            RESOURCE_STATE_CPU_USAGE -> (record[COL_CPU_USAGE] as Number).toDouble()
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun close() {
        reader.close()
    }

    override fun toString(): String = "OdcVmResourceStateTableReader"

    /**
     * Initialize the columns for the reader based on [schema].
     */
    private fun initColumns(schema: Schema) {
        try {
            COL_ID = schema.getField("id").pos()
            COL_TIMESTAMP = (schema.getField("timestamp") ?: schema.getField("time")).pos()
            COL_DURATION = schema.getField("duration").pos()
            COL_CPU_COUNT = (schema.getField("cpu_count") ?: schema.getField("cores")).pos()
            COL_CPU_USAGE = (schema.getField("cpu_usage") ?: schema.getField("cpuUsage")).pos()
        } catch (e: NullPointerException) {
            // This happens when the field we are trying to access does not exist
            throw IllegalArgumentException("Invalid schema", e)
        }
    }

    private var COL_ID = -1
    private var COL_TIMESTAMP = -1
    private var COL_DURATION = -1
    private var COL_CPU_COUNT = -1
    private var COL_CPU_USAGE = -1
}
