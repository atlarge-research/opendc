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

import org.opendc.trace.TableReader
import org.opendc.trace.conv.RESOURCE_CPU_CAPACITY
import org.opendc.trace.conv.RESOURCE_CPU_COUNT
import org.opendc.trace.conv.RESOURCE_ID
import org.opendc.trace.conv.RESOURCE_MEM_CAPACITY
import org.opendc.trace.conv.RESOURCE_START_TIME
import org.opendc.trace.conv.RESOURCE_STOP_TIME
import org.opendc.trace.opendc.parquet.Resource
import org.opendc.trace.util.parquet.LocalParquetReader
import java.time.Duration
import java.time.Instant
import java.util.UUID

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

    private val COL_ID = 0
    private val COL_START_TIME = 1
    private val COL_STOP_TIME = 2
    private val COL_CPU_COUNT = 3
    private val COL_CPU_CAPACITY = 4
    private val COL_MEM_CAPACITY = 5

    override fun resolve(name: String): Int {
        return when (name) {
            RESOURCE_ID -> COL_ID
            RESOURCE_START_TIME -> COL_START_TIME
            RESOURCE_STOP_TIME -> COL_STOP_TIME
            RESOURCE_CPU_COUNT -> COL_CPU_COUNT
            RESOURCE_CPU_CAPACITY -> COL_CPU_CAPACITY
            RESOURCE_MEM_CAPACITY -> COL_MEM_CAPACITY
            else -> -1
        }
    }

    override fun isNull(index: Int): Boolean {
        require(index in 0..COL_MEM_CAPACITY) { "Invalid column index" }
        return false
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

    override fun getFloat(index: Int): Float {
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

    override fun getString(index: Int): String {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (index) {
            COL_ID -> record.id
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getUUID(index: Int): UUID? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInstant(index: Int): Instant {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (index) {
            COL_START_TIME -> record.startTime
            COL_STOP_TIME -> record.stopTime
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getDuration(index: Int): Duration? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun <T> getList(index: Int, elementType: Class<T>): List<T>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun <T> getSet(index: Int, elementType: Class<T>): Set<T>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun <K, V> getMap(index: Int, keyType: Class<K>, valueType: Class<V>): Map<K, V>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun close() {
        reader.close()
    }

    override fun toString(): String = "OdcVmResourceTableReader"
}
