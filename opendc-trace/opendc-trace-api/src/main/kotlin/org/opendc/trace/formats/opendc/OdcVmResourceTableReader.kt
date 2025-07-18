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

package org.opendc.trace.formats.opendc

import org.opendc.trace.TableColumnType
import org.opendc.trace.TableReader
import org.opendc.trace.conv.resourceChildren
import org.opendc.trace.conv.resourceCpuCapacity
import org.opendc.trace.conv.resourceCpuCount
import org.opendc.trace.conv.resourceDeadline
import org.opendc.trace.conv.resourceDuration
import org.opendc.trace.conv.resourceGpuCapacity
import org.opendc.trace.conv.resourceGpuCount
import org.opendc.trace.conv.resourceID
import org.opendc.trace.conv.resourceMemCapacity
import org.opendc.trace.conv.resourceNature
import org.opendc.trace.conv.resourceParents
import org.opendc.trace.conv.resourceSubmissionTime
import org.opendc.trace.formats.opendc.parquet.Resource
import org.opendc.trace.util.convertTo
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

    private val colID = 0
    private val colSubmissionTime = 1
    private val colDurationTime = 2
    private val colCpuCount = 3
    private val colCpuCapacity = 4
    private val colMemCapacity = 5
    private val colGpuCapacity = 6
    private val colGpuCount = 7
    private val colParents = 8
    private val colChildren = 9
    private val colNature = 10
    private val colDeadline = 11

    private val typeParents = TableColumnType.Set(TableColumnType.String)
    private val typeChildren = TableColumnType.Set(TableColumnType.String)

    override fun resolve(name: String): Int {
        return when (name) {
            resourceID -> colID
            resourceSubmissionTime -> colSubmissionTime
            resourceDuration -> colDurationTime
            resourceCpuCount -> colCpuCount
            resourceCpuCapacity -> colCpuCapacity
            resourceMemCapacity -> colMemCapacity
            resourceGpuCount -> colGpuCount
            resourceGpuCapacity -> colGpuCapacity
            resourceParents -> colParents
            resourceChildren -> colChildren
            resourceNature -> colNature
            resourceDeadline -> colDeadline
            else -> -1
        }
    }

    override fun isNull(index: Int): Boolean {
        require(index in 0..colDeadline) { "Invalid column index" }
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (index) {
            colNature -> record.nature == null
            colDeadline -> record.deadline == -1L
            else -> false
        }
    }

    override fun getBoolean(index: Int): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(index: Int): Int {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (index) {
            colCpuCount -> record.cpuCount
            colGpuCount -> record.gpuCount
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getLong(index: Int): Long {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (index) {
            colDurationTime -> record.durationTime
            colDeadline -> record.deadline
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getFloat(index: Int): Float {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getDouble(index: Int): Double {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (index) {
            colCpuCapacity -> record.cpuCapacity
            colMemCapacity -> record.memCapacity
            colGpuCapacity -> record.gpuCapacity
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getString(index: Int): String? {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (index) {
            colID -> record.id
            colNature -> record.nature
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getUUID(index: Int): UUID? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInstant(index: Int): Instant {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (index) {
            colSubmissionTime -> record.submissionTime
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getDuration(index: Int): Duration? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun <T> getList(
        index: Int,
        elementType: Class<T>,
    ): List<T>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun <T> getSet(
        index: Int,
        elementType: Class<T>,
    ): Set<T>? {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (index) {
            colParents -> typeParents.convertTo(record.parents, elementType)
            colChildren -> typeChildren.convertTo(record.children, elementType)
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun <K, V> getMap(
        index: Int,
        keyType: Class<K>,
        valueType: Class<V>,
    ): Map<K, V>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun close() {
        reader.close()
    }

    override fun toString(): String = "OdcVmResourceTableReader"
}
