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

import org.opendc.trace.*
import org.opendc.trace.conv.*
import org.opendc.trace.opendc.parquet.Resource
import org.opendc.trace.util.parquet.LocalParquetReader

/**
 * A [TableReader] implementation for the "resources table" in the OpenDC virtual machine trace format.
 */
internal class OdcVmResourceTableReader(private val reader: LocalParquetReader<Resource>) : TableReader {
    /**
     * The current record.
     */
    private var record: Resource? = null

    override fun nextRow(): Boolean {
        try {
            val record = reader.read()
            this.record = record

            return record != null
        } catch (e: Throwable) {
            this.record = null
            throw e
        }
    }

    override fun resolve(column: TableColumn<*>): Int = columns[column] ?: -1

    override fun isNull(index: Int): Boolean {
        check(index in 0..columns.size) { "Invalid column index" }
        return get(index) == null
    }

    override fun get(index: Int): Any? {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (index) {
            COL_ID -> record.id
            COL_START_TIME -> record.startTime
            COL_STOP_TIME -> record.stopTime
            COL_CPU_COUNT -> getInt(index)
            COL_CPU_CAPACITY -> getDouble(index)
            COL_MEM_CAPACITY -> getDouble(index)
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getBoolean(index: Int): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(index: Int): Int {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (index) {
            COL_CPU_COUNT -> record.cpuCount
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getLong(index: Int): Long {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getDouble(index: Int): Double {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (index) {
            COL_CPU_CAPACITY -> record.cpuCapacity
            COL_MEM_CAPACITY -> record.memCapacity
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun close() {
        reader.close()
    }

    override fun toString(): String = "OdcVmResourceTableReader"

    private val COL_ID = 0
    private val COL_START_TIME = 1
    private val COL_STOP_TIME = 2
    private val COL_CPU_COUNT = 3
    private val COL_CPU_CAPACITY = 4
    private val COL_MEM_CAPACITY = 5

    private val columns = mapOf(
        RESOURCE_ID to COL_ID,
        RESOURCE_START_TIME to COL_START_TIME,
        RESOURCE_STOP_TIME to COL_STOP_TIME,
        RESOURCE_CPU_COUNT to COL_CPU_COUNT,
        RESOURCE_CPU_CAPACITY to COL_CPU_CAPACITY,
        RESOURCE_MEM_CAPACITY to COL_MEM_CAPACITY,
    )
}
