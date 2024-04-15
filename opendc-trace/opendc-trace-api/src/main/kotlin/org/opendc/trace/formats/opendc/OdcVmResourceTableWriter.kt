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
import org.opendc.trace.conv.resourceID
import org.opendc.trace.conv.resourceMemCapacity
import org.opendc.trace.conv.resourceStartTime
import org.opendc.trace.conv.resourceStopTime
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
    private var localStartTime: Instant = Instant.MIN
    private var localStopTime: Instant = Instant.MIN
    private var localCpuCount: Int = 0
    private var localCpuCapacity: Double = Double.NaN
    private var localMemCapacity: Double = Double.NaN

    override fun startRow() {
        localIsActive = true
        localId = ""
        localStartTime = Instant.MIN
        localStopTime = Instant.MIN
        localCpuCount = 0
        localCpuCapacity = Double.NaN
        localMemCapacity = Double.NaN
    }

    override fun endRow() {
        check(localIsActive) { "No active row" }
        localIsActive = false
        writer.write(Resource(localId, localStartTime, localStopTime, localCpuCount, localCpuCapacity, localMemCapacity))
    }

    override fun resolve(name: String): Int {
        return when (name) {
            resourceID -> colID
            resourceStartTime -> colStartTime
            resourceStopTime -> colStopTime
            resourceCpuCount -> colCpuCount
            resourceCpuCapacity -> colCpuCapacity
            resourceMemCapacity -> colMemCapacity
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
            colStartTime -> localStartTime = value
            colStopTime -> localStopTime = value
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
    private val colStartTime = 1
    private val colStopTime = 2
    private val colCpuCount = 3
    private val colCpuCapacity = 4
    private val colMemCapacity = 5
}
