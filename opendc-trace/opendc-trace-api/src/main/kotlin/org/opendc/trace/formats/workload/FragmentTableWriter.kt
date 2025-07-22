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
import org.opendc.trace.conv.FRAGMENT_CPU_USAGE
import org.opendc.trace.conv.FRAGMENT_DURATION
import org.opendc.trace.conv.FRAGMENT_GPU_USAGE
import org.opendc.trace.conv.TASK_ID
import org.opendc.trace.formats.workload.parquet.Fragment
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * A [TableWriter] implementation for the OpenDC virtual machine trace format.
 */
internal class FragmentTableWriter(private val writer: ParquetWriter<Fragment>) : TableWriter {
    /**
     * The current state for the record that is being written.
     */
    private var localIsActive = false
    private var localID: Int = -99
    private var localDuration: Duration = Duration.ZERO
    private var localCpuUsage: Double = Double.NaN
    private var localGpuUsage: Double = Double.NaN

    override fun startRow() {
        localIsActive = true
        localID = -99
        localDuration = Duration.ZERO
        localCpuUsage = Double.NaN
        localGpuUsage = Double.NaN
    }

    override fun endRow() {
        check(localIsActive) { "No active row" }
        localIsActive = false

        check(lastId != localID) { "Records need to be ordered by (id, timestamp)" }

        writer.write(Fragment(localID, localDuration, localCpuUsage, localGpuUsage))

        lastId = localID
    }

    override fun resolve(name: String): Int {
        return when (name) {
            TASK_ID -> colID
            FRAGMENT_DURATION -> colDuration
            FRAGMENT_CPU_USAGE -> colCpuUsage
            FRAGMENT_GPU_USAGE -> colGpuUsage
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
            colID -> localID = value
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
            colGpuUsage -> localGpuUsage = value
            else -> throw IllegalArgumentException("Invalid column or type [index $index]")
        }
    }

    override fun setString(
        index: Int,
        value: String,
    ) {
        throw IllegalArgumentException("Invalid column or type [index $index]")
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
        throw IllegalArgumentException("Invalid column or type [index $index]")
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
    private var lastId: Int? = null

    private val colID = 0
    private val colDuration = 1
    private val colCpuUsage = 2
    private val colGpuUsage = 3
}
