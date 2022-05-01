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

import org.apache.parquet.hadoop.ParquetWriter
import org.opendc.trace.*
import org.opendc.trace.conv.*
import org.opendc.trace.opendc.parquet.ResourceState
import java.time.Duration
import java.time.Instant

/**
 * A [TableWriter] implementation for the OpenDC virtual machine trace format.
 */
internal class OdcVmResourceStateTableWriter(private val writer: ParquetWriter<ResourceState>) : TableWriter {
    /**
     * The current state for the record that is being written.
     */
    private var _isActive = false
    private var _id: String = ""
    private var _timestamp: Instant = Instant.MIN
    private var _duration: Duration = Duration.ZERO
    private var _cpuCount: Int = 0
    private var _cpuUsage: Double = Double.NaN

    override fun startRow() {
        _isActive = true
        _id = ""
        _timestamp = Instant.MIN
        _duration = Duration.ZERO
        _cpuCount = 0
        _cpuUsage = Double.NaN
    }

    override fun endRow() {
        check(_isActive) { "No active row" }
        _isActive = false

        check(lastId != _id || _timestamp >= lastTimestamp) { "Records need to be ordered by (id, timestamp)" }

        writer.write(ResourceState(_id, _timestamp, _duration, _cpuCount, _cpuUsage))

        lastId = _id
        lastTimestamp = _timestamp
    }

    override fun resolve(column: TableColumn<*>): Int = columns[column] ?: -1

    override fun set(index: Int, value: Any) {
        check(_isActive) { "No active row" }

        when (index) {
            COL_ID -> _id = value as String
            COL_TIMESTAMP -> _timestamp = value as Instant
            COL_DURATION -> _duration = value as Duration
            COL_CPU_COUNT -> _cpuCount = value as Int
            COL_CPU_USAGE -> _cpuUsage = value as Double
        }
    }

    override fun setBoolean(index: Int, value: Boolean) {
        throw IllegalArgumentException("Invalid column or type [index $index]")
    }

    override fun setInt(index: Int, value: Int) {
        check(_isActive) { "No active row" }
        when (index) {
            COL_CPU_COUNT -> _cpuCount = value
            else -> throw IllegalArgumentException("Invalid column or type [index $index]")
        }
    }

    override fun setLong(index: Int, value: Long) {
        throw IllegalArgumentException("Invalid column or type [index $index]")
    }

    override fun setDouble(index: Int, value: Double) {
        check(_isActive) { "No active row" }
        when (index) {
            COL_CPU_USAGE -> _cpuUsage = value
            else -> throw IllegalArgumentException("Invalid column or type [index $index]")
        }
    }

    override fun flush() {
        // Not available
    }

    override fun close() {
        writer.close()
    }

    /**
     * Last column values that are used to check for correct partitioning.
     */
    private var lastId: String? = null
    private var lastTimestamp: Instant = Instant.MAX

    private val COL_ID = 0
    private val COL_TIMESTAMP = 1
    private val COL_DURATION = 2
    private val COL_CPU_COUNT = 3
    private val COL_CPU_USAGE = 4

    private val columns = mapOf(
        RESOURCE_ID to COL_ID,
        RESOURCE_STATE_TIMESTAMP to COL_TIMESTAMP,
        RESOURCE_STATE_DURATION to COL_DURATION,
        RESOURCE_CPU_COUNT to COL_CPU_COUNT,
        RESOURCE_STATE_CPU_USAGE to COL_CPU_USAGE,
    )
}
