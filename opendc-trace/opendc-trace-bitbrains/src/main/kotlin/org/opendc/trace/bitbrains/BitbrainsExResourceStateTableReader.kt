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

package org.opendc.trace.bitbrains

import org.opendc.trace.TableReader
import org.opendc.trace.conv.resourceClusterID
import org.opendc.trace.conv.resourceCpuCapacity
import org.opendc.trace.conv.resourceCpuCount
import org.opendc.trace.conv.resourceID
import org.opendc.trace.conv.resourceMemCapacity
import org.opendc.trace.conv.resourceStateCpuDemand
import org.opendc.trace.conv.resourceStateCpuReadyPct
import org.opendc.trace.conv.resourceStateCpuUsage
import org.opendc.trace.conv.resourceStateCpuUsagePct
import org.opendc.trace.conv.resourceStateDiskRead
import org.opendc.trace.conv.resourceStateDiskWrite
import org.opendc.trace.conv.resourceStateTimestamp
import java.io.BufferedReader
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * A [TableReader] for the Bitbrains resource state table.
 */
internal class BitbrainsExResourceStateTableReader(private val reader: BufferedReader) : TableReader {
    private var state = State.Pending

    override fun nextRow(): Boolean {
        val state = state
        if (state == State.Closed) {
            return false
        } else if (state == State.Pending) {
            this.state = State.Active
        }

        reset()

        var line: String?
        var num = 0

        while (true) {
            line = reader.readLine()

            if (line == null) {
                this.state = State.Closed
                return false
            }

            num++

            if (line[0] == '#' || line.isBlank()) {
                // Ignore empty lines or comments
                continue
            }

            break
        }

        line = line!!.trim()

        val length = line.length
        var col = 0
        var start: Int
        var end = 0

        while (end < length) {
            // Trim all whitespace before the field
            start = end
            while (start < length && line[start].isWhitespace()) {
                start++
            }

            end = line.indexOf(' ', start)

            if (end < 0) {
                end = length
            }

            val field = line.subSequence(start, end) as String
            when (col++) {
                colTimestamp -> timestamp = Instant.ofEpochSecond(field.toLong(10))
                colCpuUsage -> cpuUsage = field.toDouble()
                colCpuDemand -> cpuDemand = field.toDouble()
                colDiskRead -> diskRead = field.toDouble()
                colDiskWrite -> diskWrite = field.toDouble()
                colClusterID -> cluster = field.trim()
                colNcpus -> cpuCores = field.toInt(10)
                colCpuReadyPct -> cpuReadyPct = field.toDouble()
                colPoweredOn -> poweredOn = field.toInt(10) == 1
                colCpuCapacity -> cpuCapacity = field.toDouble()
                colID -> id = field.trim()
                colMemCapacity -> memCapacity = field.toDouble() * 1000 // Convert from MB to KB
            }
        }

        return true
    }

    override fun resolve(name: String): Int {
        return when (name) {
            resourceID -> colID
            resourceClusterID -> colClusterID
            resourceStateTimestamp -> colTimestamp
            resourceCpuCount -> colNcpus
            resourceCpuCapacity -> colCpuCapacity
            resourceStateCpuUsage -> colCpuUsage
            resourceStateCpuUsagePct -> colCpuUsagePct
            resourceStateCpuDemand -> colCpuDemand
            resourceStateCpuReadyPct -> colCpuReadyPct
            resourceMemCapacity -> colMemCapacity
            resourceStateDiskRead -> colDiskRead
            resourceStateDiskWrite -> colDiskWrite
            else -> -1
        }
    }

    override fun isNull(index: Int): Boolean {
        require(index in 0 until colMax) { "Invalid column index" }
        return false
    }

    override fun getBoolean(index: Int): Boolean {
        check(state == State.Active) { "No active row" }
        return when (index) {
            colPoweredOn -> poweredOn
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getInt(index: Int): Int {
        check(state == State.Active) { "No active row" }
        return when (index) {
            colNcpus -> cpuCores
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
        check(state == State.Active) { "No active row" }
        return when (index) {
            colCpuCapacity -> cpuCapacity
            colCpuUsage -> cpuUsage
            colCpuUsagePct -> cpuUsage / cpuCapacity
            colCpuReadyPct -> cpuReadyPct
            colCpuDemand -> cpuDemand
            colMemCapacity -> memCapacity
            colDiskRead -> diskRead
            colDiskWrite -> diskWrite
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getString(index: Int): String? {
        check(state == State.Active) { "No active row" }
        return when (index) {
            colID -> id
            colClusterID -> cluster
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getUUID(index: Int): UUID? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInstant(index: Int): Instant? {
        check(state == State.Active) { "No active row" }
        return when (index) {
            colTimestamp -> timestamp
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
        throw IllegalArgumentException("Invalid column")
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
        reset()
        state = State.Closed
    }

    /**
     * State fields of the reader.
     */
    private var id: String? = null
    private var cluster: String? = null
    private var timestamp: Instant? = null
    private var cpuCores = -1
    private var cpuCapacity = Double.NaN
    private var cpuUsage = Double.NaN
    private var cpuDemand = Double.NaN
    private var cpuReadyPct = Double.NaN
    private var memCapacity = Double.NaN
    private var diskRead = Double.NaN
    private var diskWrite = Double.NaN
    private var poweredOn: Boolean = false

    /**
     * Reset the state of the reader.
     */
    private fun reset() {
        id = null
        timestamp = null
        cluster = null
        cpuCores = -1
        cpuCapacity = Double.NaN
        cpuUsage = Double.NaN
        cpuDemand = Double.NaN
        cpuReadyPct = Double.NaN
        memCapacity = Double.NaN
        diskRead = Double.NaN
        diskWrite = Double.NaN
        poweredOn = false
    }

    /**
     * Default column indices for the extended Bitbrains format.
     */
    private val colTimestamp = 0
    private val colCpuUsage = 1
    private val colCpuDemand = 2
    private val colDiskRead = 4
    private val colDiskWrite = 6
    private val colClusterID = 10
    private val colNcpus = 12
    private val colCpuReadyPct = 13
    private val colPoweredOn = 14
    private val colCpuCapacity = 18
    private val colID = 19
    private val colMemCapacity = 20
    private val colCpuUsagePct = 21
    private val colMax = colCpuUsagePct + 1

    private enum class State {
        Pending,
        Active,
        Closed,
    }
}
