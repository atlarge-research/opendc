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

package org.opendc.trace.formats.workload

import org.opendc.trace.TableReader
import org.opendc.trace.conv.FRAGMENT_CPU_USAGE
import org.opendc.trace.conv.FRAGMENT_DURATION
import org.opendc.trace.conv.FRAGMENT_GPU_USAGE
import org.opendc.trace.conv.TASK_ID
import org.opendc.trace.formats.workload.parquet.FragmentParquetSchema
import org.opendc.trace.util.parquet.LocalParquetReader
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * A [TableReader] implementation for the OpenDC virtual machine trace format.
 */
internal class FragmentTableReader(private val reader: LocalParquetReader<FragmentParquetSchema>) : TableReader {
    /**
     * The current record.
     */
    private var record: FragmentParquetSchema? = null

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

    private val colID = 0
    private val colDuration = 1
    private val colCpuUsage = 2
    private val colGpuUsage = 3

    override fun resolve(name: String): Int {
        return when (name) {
            TASK_ID -> colID
            FRAGMENT_DURATION -> colDuration
            FRAGMENT_CPU_USAGE -> colCpuUsage
            FRAGMENT_GPU_USAGE -> colGpuUsage
            else -> -1
        }
    }

    override fun isNull(index: Int): Boolean {
        require(index in 0..colCpuUsage) { "Invalid column index" }
        return false
    }

    override fun getBoolean(index: Int): Boolean {
        throw IllegalArgumentException("Invalid column or type [index $index]")
    }

    override fun getInt(index: Int): Int {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (index) {
            colID -> record.id
            else -> throw IllegalArgumentException("Invalid column or type [index $index]")
        }
    }

    override fun getLong(index: Int): Long {
        throw IllegalArgumentException("Invalid column or type [index $index]")
    }

    override fun getFloat(index: Int): Float {
        throw IllegalArgumentException("Invalid column or type [index $index]")
    }

    override fun getDouble(index: Int): Double {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (index) {
            colCpuUsage -> record.cpuUsage
            colGpuUsage -> record.gpuUsage
            else -> throw IllegalArgumentException("Invalid column or type [index $index]")
        }
    }

    override fun getString(index: Int): String {
        throw IllegalArgumentException("Invalid column index $index")
    }

    override fun getUUID(index: Int): UUID? {
        throw IllegalArgumentException("Invalid column or type [index $index]")
    }

    override fun getInstant(index: Int): Instant {
        throw IllegalArgumentException("Invalid column index $index")
    }

    override fun getDuration(index: Int): Duration {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (index) {
            colDuration -> record.duration
            else -> throw IllegalArgumentException("Invalid column index $index")
        }
    }

    override fun <T> getList(
        index: Int,
        elementType: Class<T>,
    ): List<T>? {
        throw IllegalArgumentException("Invalid column or type [index $index]")
    }

    override fun <T> getSet(
        index: Int,
        elementType: Class<T>,
    ): Set<T>? {
        throw IllegalArgumentException("Invalid column or type [index $index]")
    }

    override fun <K, V> getMap(
        index: Int,
        keyType: Class<K>,
        valueType: Class<V>,
    ): Map<K, V>? {
        throw IllegalArgumentException("Invalid column or type [index $index]")
    }

    override fun close() {
        reader.close()
    }

    override fun toString(): String = "OdcVmResourceStateTableReader"
}
