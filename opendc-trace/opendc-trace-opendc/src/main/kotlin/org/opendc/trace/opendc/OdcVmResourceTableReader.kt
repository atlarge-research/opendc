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
import java.time.Instant

/**
 * A [TableReader] implementation for the resources table in the OpenDC virtual machine trace format.
 */
internal class OdcVmResourceTableReader(private val reader: LocalParquetReader<GenericRecord>) : TableReader {
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
            RESOURCE_ID -> true
            RESOURCE_START_TIME -> true
            RESOURCE_STOP_TIME -> true
            RESOURCE_CPU_COUNT -> true
            RESOURCE_MEM_CAPACITY -> true
            else -> false
        }
    }

    override fun <T> get(column: TableColumn<T>): T {
        val record = checkNotNull(record) { "Reader in invalid state" }

        @Suppress("UNCHECKED_CAST")
        val res: Any = when (column) {
            RESOURCE_ID -> record[COL_ID].toString()
            RESOURCE_START_TIME -> Instant.ofEpochMilli(record[COL_START_TIME] as Long)
            RESOURCE_STOP_TIME -> Instant.ofEpochMilli(record[COL_STOP_TIME] as Long)
            RESOURCE_CPU_COUNT -> getInt(RESOURCE_CPU_COUNT)
            RESOURCE_MEM_CAPACITY -> getDouble(RESOURCE_MEM_CAPACITY)
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
            RESOURCE_CPU_COUNT -> record[COL_CPU_COUNT] as Int
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getLong(column: TableColumn<Long>): Long {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getDouble(column: TableColumn<Double>): Double {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (column) {
            RESOURCE_MEM_CAPACITY -> (record[COL_MEM_CAPACITY] as Number).toDouble()
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun close() {
        reader.close()
    }

    override fun toString(): String = "OdcVmResourceTableReader"

    /**
     * Initialize the columns for the reader based on [schema].
     */
    private fun initColumns(schema: Schema) {
        try {
            COL_ID = schema.getField("id").pos()
            COL_START_TIME = (schema.getField("start_time") ?: schema.getField("submissionTime")).pos()
            COL_STOP_TIME = (schema.getField("stop_time") ?: schema.getField("endTime")).pos()
            COL_CPU_COUNT = (schema.getField("cpu_count") ?: schema.getField("maxCores")).pos()
            COL_MEM_CAPACITY = (schema.getField("mem_capacity") ?: schema.getField("requiredMemory")).pos()
        } catch (e: NullPointerException) {
            // This happens when the field we are trying to access does not exist
            throw IllegalArgumentException("Invalid schema")
        }
    }

    private var COL_ID = -1
    private var COL_START_TIME = -1
    private var COL_STOP_TIME = -1
    private var COL_CPU_COUNT = -1
    private var COL_MEM_CAPACITY = -1
}
