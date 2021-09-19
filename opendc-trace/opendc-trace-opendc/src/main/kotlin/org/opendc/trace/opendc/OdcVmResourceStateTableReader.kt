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

    override fun nextRow(): Boolean {
        record = reader.read()
        return record != null
    }

    override fun hasColumn(column: TableColumn<*>): Boolean {
        return when (column) {
            RESOURCE_STATE_ID -> true
            RESOURCE_STATE_TIMESTAMP -> true
            RESOURCE_STATE_DURATION -> true
            RESOURCE_STATE_NCPUS -> true
            RESOURCE_STATE_CPU_USAGE -> true
            else -> false
        }
    }

    override fun <T> get(column: TableColumn<T>): T {
        val record = checkNotNull(record) { "Reader in invalid state" }

        @Suppress("UNCHECKED_CAST")
        val res: Any = when (column) {
            RESOURCE_STATE_ID -> record["id"].toString()
            RESOURCE_STATE_TIMESTAMP -> Instant.ofEpochMilli(record["time"] as Long)
            RESOURCE_STATE_DURATION -> Duration.ofMillis(record["duration"] as Long)
            RESOURCE_STATE_NCPUS -> record["cores"]
            RESOURCE_STATE_CPU_USAGE -> (record["cpuUsage"] as Number).toDouble()
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
            RESOURCE_STATE_NCPUS -> record["cores"] as Int
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getLong(column: TableColumn<Long>): Long {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getDouble(column: TableColumn<Double>): Double {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (column) {
            RESOURCE_STATE_CPU_USAGE -> (record["cpuUsage"] as Number).toDouble()
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun close() {
        reader.close()
    }

    override fun toString(): String = "OdcVmResourceStateTableReader"
}
