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

import org.apache.parquet.hadoop.ParquetWriter
import org.opendc.trace.TableWriter
import org.opendc.trace.conv.resourceCpuCapacity
import org.opendc.trace.conv.resourceCpuCount
import org.opendc.trace.conv.resourceDeadline
import org.opendc.trace.conv.resourceDuration
import org.opendc.trace.conv.resourceID
import org.opendc.trace.conv.resourceMemCapacity
import org.opendc.trace.conv.resourceNature
import org.opendc.trace.conv.resourceSubmissionTime
import org.opendc.trace.formats.opendc.parquet.Resource
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * A [TableWriter] implementation for the OpenDC virtual machine trace format.
 */
internal class OdcVmResourceTableWriter(private val writer: ParquetWriter<Resource>) : TableWriter {
    /**
     * The current state for the record that is being written.
     */
    private var localIsActive = false
    private var localId: String = ""
    private var localSubmissionTime: Instant = Instant.MIN
    private var localDuration: Long = 0L
    private var localCpuCount: Int = 0
    private var localCpuCapacity: Double = Double.NaN
    private var localMemCapacity: Double = Double.NaN
    private var localNature: String? = null
    private var localDeadline: Long = -1

    override fun startRow() {
        localIsActive = true
        localId = ""
        localSubmissionTime = Instant.MIN
        localDuration = 0L
        localCpuCount = 0
        localCpuCapacity = Double.NaN
        localMemCapacity = Double.NaN
        localNature = null
        localDeadline = -1L
    }

    override fun endRow() {
        check(localIsActive) { "No active row" }
        localIsActive = false
        writer.write(Resource(localId, localSubmissionTime, localDuration, localCpuCount, localCpuCapacity,
            localMemCapacity, localNature, localDeadline))
    }

    override fun resolve(name: String): Int {
        return when (name) {
            resourceID -> colID
            resourceSubmissionTime -> colSubmissionTime
            resourceDuration -> colDuration
            resourceCpuCount -> colCpuCount
            resourceCpuCapacity -> colCpuCapacity
            resourceMemCapacity -> colMemCapacity
            resourceNature -> colNature
            resourceDeadline -> colDeadline
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
            colCpuCount -> localCpuCount = value
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
            else -> throw IllegalArgumentException("Invalid column or type [index $index]")
        }
    }

    override fun setString(
        index: Int,
        value: String,
    ) {
        check(localIsActive) { "No active row" }
        when (index) {
            colID -> localId = value
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
    private val colSubmissionTime = 1
    private val colDuration = 2
    private val colCpuCount = 3
    private val colCpuCapacity = 4
    private val colMemCapacity = 5
    private val colNature = 6
    private val colDeadline = 7
}
