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
import org.opendc.trace.conv.resourceCpuCount
import org.opendc.trace.conv.resourceID
import org.opendc.trace.conv.resourceStateCpuUsage
import org.opendc.trace.conv.resourceStateDuration
import org.opendc.trace.conv.resourceStateTimestamp
import org.opendc.trace.formats.opendc.parquet.ResourceState
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * A [TableWriter] implementation for the OpenDC virtual machine trace format.
 */
internal class OdcVmResourceStateTableWriter(private val writer: ParquetWriter<ResourceState>) : TableWriter {
    /**
     * The current state for the record that is being written.
     */
    private var localIsActive = false
    private var localID: String = ""
    private var localTimestamp: Instant = Instant.MIN
    private var localDuration: Duration = Duration.ZERO
    private var localCpuCount: Int = 0
    private var localCpuUsage: Double = Double.NaN

    override fun startRow() {
        localIsActive = true
        localID = ""
        localTimestamp = Instant.MIN
        localDuration = Duration.ZERO
        localCpuCount = 0
        localCpuUsage = Double.NaN
    }

    override fun endRow() {
        check(localIsActive) { "No active row" }
        localIsActive = false

        check(lastId != localID || localTimestamp >= lastTimestamp) { "Records need to be ordered by (id, timestamp)" }

        writer.write(ResourceState(localID, localTimestamp, localDuration, localCpuCount, localCpuUsage))

        lastId = localID
        lastTimestamp = localTimestamp
    }

    override fun resolve(name: String): Int {
        return when (name) {
            resourceID -> colID
            resourceStateTimestamp -> colTimestamp
            resourceStateDuration -> colDuration
            resourceCpuCount -> colCpuCount
            resourceStateCpuUsage -> colCpuUsage
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
        throw IllegalArgumentException("Invalid column or type [index $index]")
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
            colCpuUsage -> localCpuUsage = value
            else -> throw IllegalArgumentException("Invalid column or type [index $index]")
        }
    }

    override fun setString(
        index: Int,
        value: String,
    ) {
        check(localIsActive) { "No active row" }

        when (index) {
            colID -> localID = value
            else -> throw IllegalArgumentException("Invalid column or type [index $index]")
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
            colTimestamp -> localTimestamp = value
            else -> throw IllegalArgumentException("Invalid column or type [index $index]")
        }
    }

    override fun setDuration(
        index: Int,
        value: Duration,
    ) {
        check(localIsActive) { "No active row" }

        when (index) {
            colDuration -> localDuration = value
            else -> throw IllegalArgumentException("Invalid column or type [index $index]")
        }
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

    /**
     * Last column values that are used to check for correct partitioning.
     */
    private var lastId: String? = null
    private var lastTimestamp: Instant = Instant.MAX

    private val colID = 0
    private val colTimestamp = 1
    private val colDuration = 2
    private val colCpuCount = 3
    private val colCpuUsage = 4
}
