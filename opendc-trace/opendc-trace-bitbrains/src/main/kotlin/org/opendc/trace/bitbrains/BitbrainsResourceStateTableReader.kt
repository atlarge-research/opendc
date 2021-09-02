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

import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.dataformat.csv.CsvParser
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import org.opendc.trace.*
import java.time.Instant

/**
 * A [TableReader] for the Bitbrains resource state table.
 */
internal class BitbrainsResourceStateTableReader(private val partition: String, private val parser: CsvParser) : TableReader {
    /**
     * The current parser state.
     */
    private val state = RowState()

    init {
        parser.schema = schema
    }

    override fun nextRow(): Boolean {
        // Reset the row state
        state.reset()

        if (!nextStart()) {
            return false
        }

        while (true) {
            val token = parser.nextValue()

            if (token == null || token == JsonToken.END_OBJECT) {
                break
            }

            when (parser.currentName) {
                "Timestamp [ms]" -> state.timestamp = Instant.ofEpochSecond(parser.longValue)
                "CPU cores" -> state.cpuCores = parser.intValue
                "CPU capacity provisioned [MHZ]" -> state.cpuCapacity = parser.doubleValue
                "CPU usage [MHZ]" -> state.cpuUsage = parser.doubleValue
                "CPU usage [%]" -> state.cpuUsagePct = parser.doubleValue
                "Memory capacity provisioned [KB]" -> state.memCapacity = parser.doubleValue
                "Memory usage [KB]" -> state.memUsage = parser.doubleValue
                "Disk read throughput [KB/s]" -> state.diskRead = parser.doubleValue
                "Disk write throughput [KB/s]" -> state.diskWrite = parser.doubleValue
                "Network received throughput [KB/s]" -> state.netReceived = parser.doubleValue
                "Network transmitted throughput [KB/s]" -> state.netTransmitted = parser.doubleValue
            }
        }

        return true
    }

    override fun hasColumn(column: TableColumn<*>): Boolean {
        return when (column) {
            RESOURCE_STATE_ID -> true
            RESOURCE_STATE_TIMESTAMP -> true
            RESOURCE_STATE_NCPUS -> true
            RESOURCE_STATE_CPU_CAPACITY -> true
            RESOURCE_STATE_CPU_USAGE -> true
            RESOURCE_STATE_CPU_USAGE_PCT -> true
            RESOURCE_STATE_MEM_CAPACITY -> true
            RESOURCE_STATE_MEM_USAGE -> true
            RESOURCE_STATE_DISK_READ -> true
            RESOURCE_STATE_DISK_WRITE -> true
            RESOURCE_STATE_NET_RX -> true
            RESOURCE_STATE_NET_TX -> true
            else -> false
        }
    }

    override fun <T> get(column: TableColumn<T>): T {
        val res: Any? = when (column) {
            RESOURCE_STATE_ID -> partition
            RESOURCE_STATE_TIMESTAMP -> state.timestamp
            RESOURCE_STATE_NCPUS -> state.cpuCores
            RESOURCE_STATE_CPU_CAPACITY -> state.cpuCapacity
            RESOURCE_STATE_CPU_USAGE -> state.cpuUsage
            RESOURCE_STATE_CPU_USAGE_PCT -> state.cpuUsagePct
            RESOURCE_STATE_MEM_CAPACITY -> state.memCapacity
            RESOURCE_STATE_MEM_USAGE -> state.memUsage
            RESOURCE_STATE_DISK_READ -> state.diskRead
            RESOURCE_STATE_DISK_WRITE -> state.diskWrite
            RESOURCE_STATE_NET_RX -> state.netReceived
            RESOURCE_STATE_NET_TX -> state.netTransmitted
            else -> throw IllegalArgumentException("Invalid column")
        }

        @Suppress("UNCHECKED_CAST")
        return res as T
    }

    override fun getBoolean(column: TableColumn<Boolean>): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(column: TableColumn<Int>): Int {
        return when (column) {
            RESOURCE_STATE_NCPUS -> state.cpuCores
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getLong(column: TableColumn<Long>): Long {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getDouble(column: TableColumn<Double>): Double {
        return when (column) {
            RESOURCE_STATE_CPU_CAPACITY -> state.cpuCapacity
            RESOURCE_STATE_CPU_USAGE -> state.cpuUsage
            RESOURCE_STATE_CPU_USAGE_PCT -> state.cpuUsagePct
            RESOURCE_STATE_MEM_CAPACITY -> state.memCapacity
            RESOURCE_STATE_MEM_USAGE -> state.memUsage
            RESOURCE_STATE_DISK_READ -> state.diskRead
            RESOURCE_STATE_DISK_WRITE -> state.diskWrite
            RESOURCE_STATE_NET_RX -> state.netReceived
            RESOURCE_STATE_NET_TX -> state.netTransmitted
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun close() {
        parser.close()
    }

    /**
     * Advance the parser until the next object start.
     */
    private fun nextStart(): Boolean {
        var token = parser.nextValue()

        while (token != null && token != JsonToken.START_OBJECT) {
            token = parser.nextValue()
        }

        return token != null
    }

    /**
     * The current row state.
     */
    private class RowState {
        var timestamp: Instant? = null
        var cpuCores = -1
        var cpuCapacity = Double.NaN
        var cpuUsage = Double.NaN
        var cpuUsagePct = Double.NaN
        var memCapacity = Double.NaN
        var memUsage = Double.NaN
        var diskRead = Double.NaN
        var diskWrite = Double.NaN
        var netReceived = Double.NaN
        var netTransmitted = Double.NaN

        /**
         * Reset the state.
         */
        fun reset() {
            timestamp = null
            cpuCores = -1
            cpuCapacity = Double.NaN
            cpuUsage = Double.NaN
            cpuUsagePct = Double.NaN
            memCapacity = Double.NaN
            memUsage = Double.NaN
            diskRead = Double.NaN
            diskWrite = Double.NaN
            netReceived = Double.NaN
            netTransmitted = Double.NaN
        }
    }

    companion object {
        /**
         * The [CsvSchema] that is used to parse the trace.
         */
        private val schema = CsvSchema.builder()
            .addColumn("Timestamp [ms]", CsvSchema.ColumnType.NUMBER)
            .addColumn("CPU cores", CsvSchema.ColumnType.NUMBER)
            .addColumn("CPU capacity provisioned [MHZ]", CsvSchema.ColumnType.NUMBER)
            .addColumn("CPU usage [MHZ]", CsvSchema.ColumnType.NUMBER)
            .addColumn("CPU usage [%]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Memory capacity provisioned [KB]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Memory usage [KB]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Disk read throughput [KB/s]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Disk write throughput [KB/s]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Network received throughput [KB/s]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Network transmitted throughput [KB/s]", CsvSchema.ColumnType.NUMBER)
            .setAllowComments(true)
            .setUseHeader(true)
            .setColumnSeparator(';')
            .build()
    }
}
