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

import org.opendc.trace.TableColumnType
import org.opendc.trace.TableReader
import org.opendc.trace.conv.TASK_CHILDREN
import org.opendc.trace.conv.TASK_CPU_CAPACITY
import org.opendc.trace.conv.TASK_CPU_COUNT
import org.opendc.trace.conv.TASK_DEADLINE
import org.opendc.trace.conv.TASK_DEFERRABLE
import org.opendc.trace.conv.TASK_DURATION
import org.opendc.trace.conv.TASK_GPU_CAPACITY
import org.opendc.trace.conv.TASK_GPU_COUNT
import org.opendc.trace.conv.TASK_ID
import org.opendc.trace.conv.TASK_MEM_CAPACITY
import org.opendc.trace.conv.TASK_NAME
import org.opendc.trace.conv.TASK_PARENTS
import org.opendc.trace.conv.TASK_SUBMISSION_TIME
import org.opendc.trace.formats.workload.parquet.TaskParquetSchema
import org.opendc.trace.util.convertTo
import org.opendc.trace.util.parquet.LocalParquetReader
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * A [TableReader] implementation for the "resources table" in the OpenDC virtual machine trace format.
 */
internal class TaskTableReader(private val reader: LocalParquetReader<TaskParquetSchema>) : TableReader {
    /**
     * The current record.
     */
    private var record: TaskParquetSchema? = null

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
    private val colName = 1
    private val colSubmissionTime = 2
    private val colDurationTime = 3
    private val colCpuCount = 4
    private val colCpuCapacity = 5
    private val colMemCapacity = 6
    private val colGpuCapacity = 7
    private val colGpuCount = 8
    private val colParents = 9
    private val colChildren = 10
    private val colDeferrable = 11
    private val colDeadline = 12

    private val typeParents = TableColumnType.Set(TableColumnType.Int)
    private val typeChildren = TableColumnType.Set(TableColumnType.Int)

    override fun resolve(name: String): Int {
        return when (name) {
            TASK_ID -> colID
            TASK_NAME -> colName
            TASK_SUBMISSION_TIME -> colSubmissionTime
            TASK_DURATION -> colDurationTime
            TASK_CPU_COUNT -> colCpuCount
            TASK_CPU_CAPACITY -> colCpuCapacity
            TASK_MEM_CAPACITY -> colMemCapacity
            TASK_GPU_COUNT -> colGpuCount
            TASK_GPU_CAPACITY -> colGpuCapacity
            TASK_PARENTS -> colParents
            TASK_CHILDREN -> colChildren
            TASK_DEFERRABLE -> colDeferrable
            TASK_DEADLINE -> colDeadline
            else -> -1
        }
    }

    override fun isNull(index: Int): Boolean {
        require(index in 0..colDeadline) { "Invalid column index" }
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (index) {
            colDeadline -> record.deadline == -1L
            else -> false
        }
    }

    override fun getBoolean(index: Int): Boolean {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (index) {
            colDeferrable -> record.deferrable
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getInt(index: Int): Int {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (index) {
            colID -> record.id
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
            colName -> record.name
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getUUID(index: Int): UUID {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInstant(index: Int): Instant {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (index) {
            colSubmissionTime -> record.submissionTime
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getDuration(index: Int): Duration {
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
