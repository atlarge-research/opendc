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

import org.apache.parquet.hadoop.ParquetWriter
import org.opendc.trace.TableWriter
import org.opendc.trace.conv.TASK_CHILDREN
import org.opendc.trace.conv.TASK_CPU_CAPACITY
import org.opendc.trace.conv.TASK_CPU_COUNT
import org.opendc.trace.conv.TASK_DEADLINE
import org.opendc.trace.conv.TASK_DURATION
import org.opendc.trace.conv.TASK_GPU_CAPACITY
import org.opendc.trace.conv.TASK_GPU_COUNT
import org.opendc.trace.conv.TASK_ID
import org.opendc.trace.conv.TASK_MEM_CAPACITY
import org.opendc.trace.conv.TASK_NAME
import org.opendc.trace.conv.TASK_NATURE
import org.opendc.trace.conv.TASK_PARENTS
import org.opendc.trace.conv.TASK_SUBMISSION_TIME
import org.opendc.trace.formats.workload.parquet.Task
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * A [TableWriter] implementation for the OpenDC virtual machine trace format.
 */
internal class TaskTableWriter(private val writer: ParquetWriter<Task>) : TableWriter {
    /**
     * The current state for the record that is being written.
     */
    private var localIsActive = false
    private var localId: Int = -99
    private var localName: String = ""
    private var localSubmissionTime: Instant = Instant.MIN
    private var localDuration: Long = 0L
    private var localCpuCount: Int = 0
    private var localCpuCapacity: Double = Double.NaN
    private var localMemCapacity: Double = Double.NaN
    private var localGpuCount: Int = 0
    private var localGpuCapacity: Double = Double.NaN
    private var localParents = mutableSetOf<Int>()
    private var localChildren = mutableSetOf<Int>()
    private var localNature: String? = null
    private var localDeadline: Long = -1

    override fun startRow() {
        localIsActive = true
        localId = -99
        localName = ""
        localSubmissionTime = Instant.MIN
        localDuration = 0L
        localCpuCount = 0
        localCpuCapacity = Double.NaN
        localMemCapacity = Double.NaN
        localGpuCount = 0
        localGpuCapacity = Double.NaN
        localParents.clear()
        localChildren.clear()
        localNature = null
        localDeadline = -1L
    }

    override fun endRow() {
        check(localIsActive) { "No active row" }
        localIsActive = false
        writer.write(
            Task(
                localId,
                localName,
                localSubmissionTime,
                localDuration,
                localCpuCount,
                localCpuCapacity,
                localMemCapacity,
                localGpuCount,
                localGpuCapacity,
                localParents,
                localChildren,
                localNature,
                localDeadline,
            ),
        )
    }

    override fun resolve(name: String): Int {
        return when (name) {
            TASK_ID -> colID
            TASK_NAME -> colID
            TASK_SUBMISSION_TIME -> colSubmissionTime
            TASK_DURATION -> colDuration
            TASK_CPU_COUNT -> colCpuCount
            TASK_CPU_CAPACITY -> colCpuCapacity
            TASK_MEM_CAPACITY -> colMemCapacity
            TASK_GPU_COUNT -> colGpuCount
            TASK_GPU_CAPACITY -> colGpuCapacity
            TASK_PARENTS -> colParents
            TASK_CHILDREN -> colChildren
            TASK_NATURE -> colNature
            TASK_DEADLINE -> colDeadline
            else -> -1
        }
    }

    override fun setBoolean(
        index: Int,
        value: Boolean,
    ) {
        throw IllegalArgumentException("Invalid column or type [index $index]")
    }

    override fun setInt(
        index: Int,
        value: Int,
    ) {
        check(localIsActive) { "No active row" }
        when (index) {
            colID -> localId = value
            colCpuCount -> localCpuCount = value
            colGpuCount -> localGpuCount = value
            else -> throw IllegalArgumentException("Invalid column or type [index $index]")
        }
    }

    override fun setLong(
        index: Int,
        value: Long,
    ) {
        check(localIsActive) { "No active row" }
        when (index) {
            colDuration -> localDuration = value
            colDeadline -> localDeadline = value
            else -> throw IllegalArgumentException("Invalid column index $index")
        }
    }

    override fun setFloat(
        index: Int,
        value: Float,
    ) {
        throw IllegalArgumentException("Invalid column or type [index $index]")
    }

    override fun setDouble(
        index: Int,
        value: Double,
    ) {
        check(localIsActive) { "No active row" }
        when (index) {
            colCpuCapacity -> localCpuCapacity = value
            colMemCapacity -> localMemCapacity = value
            colGpuCapacity -> localGpuCapacity = value
            else -> throw IllegalArgumentException("Invalid column or type [index $index]")
        }
    }

    override fun setString(
        index: Int,
        value: String,
    ) {
        check(localIsActive) { "No active row" }
        when (index) {
            colName -> localName = value
            colNature -> localNature = value
            else -> throw IllegalArgumentException("Invalid column index $index")
        }
    }

    override fun setUUID(
        index: Int,
        value: UUID,
    ) {
        throw IllegalArgumentException("Invalid column or type [index $index]")
    }

    override fun setInstant(
        index: Int,
        value: Instant,
    ) {
        check(localIsActive) { "No active row" }
        when (index) {
            colSubmissionTime -> localSubmissionTime = value
            else -> throw IllegalArgumentException("Invalid column index $index")
        }
    }

    override fun setDuration(
        index: Int,
        value: Duration,
    ) {
        throw IllegalArgumentException("Invalid column or type [index $index]")
    }

    override fun <T> setList(
        index: Int,
        value: List<T>,
    ) {
        throw IllegalArgumentException("Invalid column or type [index $index]")
    }

    override fun <T> setSet(
        index: Int,
        value: Set<T>,
    ) {
        throw IllegalArgumentException("Invalid column or type [index $index]")
    }

    override fun <K, V> setMap(
        index: Int,
        value: Map<K, V>,
    ) {
        throw IllegalArgumentException("Invalid column or type [index $index]")
    }

    override fun flush() {
        // Not available
    }

    override fun close() {
        writer.close()
    }

    private val colID = 0
    private val colName = 1
    private val colSubmissionTime = 2
    private val colDuration = 3
    private val colCpuCount = 4
    private val colCpuCapacity = 5
    private val colMemCapacity = 6
    private val colGpuCount = 7
    private val colGpuCapacity = 8
    private val colParents = 9
    private val colChildren = 10
    private val colNature = 11
    private val colDeadline = 12
}
