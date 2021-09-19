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
import java.time.Instant

/**
 * A [TableReader] implementation for the resources table in the OpenDC virtual machine trace format.
 */
internal class OdcVmResourceTableReader(private val reader: LocalParquetReader<GenericRecord>) : TableReader {
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
            RESOURCE_ID -> true
            RESOURCE_START_TIME -> true
            RESOURCE_STOP_TIME -> true
            RESOURCE_NCPUS -> true
            RESOURCE_MEM_CAPACITY -> true
            else -> false
        }
    }

    override fun <T> get(column: TableColumn<T>): T {
        val record = checkNotNull(record) { "Reader in invalid state" }

        @Suppress("UNCHECKED_CAST")
        val res: Any = when (column) {
            RESOURCE_ID -> record["id"].toString()
            RESOURCE_START_TIME -> Instant.ofEpochMilli(record["submissionTime"] as Long)
            RESOURCE_STOP_TIME -> Instant.ofEpochMilli(record["endTime"] as Long)
            RESOURCE_NCPUS -> getInt(RESOURCE_NCPUS)
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
            RESOURCE_NCPUS -> record["maxCores"] as Int
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getLong(column: TableColumn<Long>): Long {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getDouble(column: TableColumn<Double>): Double {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (column) {
            RESOURCE_MEM_CAPACITY -> (record["requiredMemory"] as Number).toDouble() * 1000.0 // MB to KB
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun close() {
        reader.close()
    }

    override fun toString(): String = "OdcVmResourceTableReader"
}
