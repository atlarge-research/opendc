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

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.dataformat.csv.CsvParser
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import org.opendc.trace.*
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

/**
 * A [TableReader] for the Bitbrains resource state table.
 */
internal class BitbrainsResourceStateTableReader(private val partition: String, private val parser: CsvParser) : TableReader {
    /**
     * The [DateTimeFormatter] used to parse the timestamps in case of the Materna trace.
     */
    private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    /**
     * The type of timestamps in the trace.
     */
    private var timestampType: TimestampType = TimestampType.UNDECIDED

    /**
     * The [NumberFormat] used to parse doubles containing a comma.
     */
    private val nf = NumberFormat.getInstance(Locale.GERMAN)

    /**
     * A flag to indicate that the trace contains decimals with a comma separator.
     */
    private var usesCommaDecimalSeparator = false

    init {
        parser.schema = schema
    }

    override fun nextRow(): Boolean {
        // Reset the row state
        reset()

        if (!nextStart()) {
            return false
        }

        while (true) {
            val token = parser.nextValue()

            if (token == null || token == JsonToken.END_OBJECT) {
                break
            }

            when (parser.currentName) {
                "Timestamp [ms]" -> {
                    timestamp = when (timestampType) {
                        TimestampType.UNDECIDED -> {
                            try {
                                val res = LocalDateTime.parse(parser.text, formatter).toInstant(ZoneOffset.UTC)
                                timestampType = TimestampType.DATE_TIME
                                res
                            } catch (e: DateTimeParseException) {
                                timestampType = TimestampType.EPOCH_MILLIS
                                Instant.ofEpochSecond(parser.longValue)
                            }
                        }
                        TimestampType.DATE_TIME -> LocalDateTime.parse(parser.text, formatter).toInstant(ZoneOffset.UTC)
                        TimestampType.EPOCH_MILLIS -> Instant.ofEpochSecond(parser.longValue)
                    }
                }
                "CPU cores" -> cpuCores = parser.intValue
                "CPU capacity provisioned [MHZ]" -> cpuCapacity = parseSafeDouble()
                "CPU usage [MHZ]" -> cpuUsage = parseSafeDouble()
                "CPU usage [%]" -> cpuUsagePct = parseSafeDouble() / 100.0 // Convert to range [0, 1]
                "Memory capacity provisioned [KB]" -> memCapacity = parseSafeDouble()
                "Memory usage [KB]" -> memUsage = parseSafeDouble()
                "Disk read throughput [KB/s]" -> diskRead = parseSafeDouble()
                "Disk write throughput [KB/s]" -> diskWrite = parseSafeDouble()
                "Network received throughput [KB/s]" -> netReceived = parseSafeDouble()
                "Network transmitted throughput [KB/s]" -> netTransmitted = parseSafeDouble()
            }
        }

        return true
    }

    override fun hasColumn(column: TableColumn<*>): Boolean {
        return when (column) {
            RESOURCE_STATE_ID -> true
            RESOURCE_STATE_TIMESTAMP -> true
            RESOURCE_STATE_CPU_COUNT -> true
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
            RESOURCE_STATE_TIMESTAMP -> timestamp
            RESOURCE_STATE_CPU_COUNT -> cpuCores
            RESOURCE_STATE_CPU_CAPACITY -> cpuCapacity
            RESOURCE_STATE_CPU_USAGE -> cpuUsage
            RESOURCE_STATE_CPU_USAGE_PCT -> cpuUsagePct
            RESOURCE_STATE_MEM_CAPACITY -> memCapacity
            RESOURCE_STATE_MEM_USAGE -> memUsage
            RESOURCE_STATE_DISK_READ -> diskRead
            RESOURCE_STATE_DISK_WRITE -> diskWrite
            RESOURCE_STATE_NET_RX -> netReceived
            RESOURCE_STATE_NET_TX -> netTransmitted
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
            RESOURCE_STATE_CPU_COUNT -> cpuCores
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getLong(column: TableColumn<Long>): Long {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getDouble(column: TableColumn<Double>): Double {
        return when (column) {
            RESOURCE_STATE_CPU_CAPACITY -> cpuCapacity
            RESOURCE_STATE_CPU_USAGE -> cpuUsage
            RESOURCE_STATE_CPU_USAGE_PCT -> cpuUsagePct
            RESOURCE_STATE_MEM_CAPACITY -> memCapacity
            RESOURCE_STATE_MEM_USAGE -> memUsage
            RESOURCE_STATE_DISK_READ -> diskRead
            RESOURCE_STATE_DISK_WRITE -> diskWrite
            RESOURCE_STATE_NET_RX -> netReceived
            RESOURCE_STATE_NET_TX -> netTransmitted
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
     * Try to parse the current value safely as double.
     */
    private fun parseSafeDouble(): Double {
        if (!usesCommaDecimalSeparator) {
            try {
                return parser.doubleValue
            } catch (e: JsonParseException) {
                usesCommaDecimalSeparator = true
            }
        }

        val text = parser.text
        if (text.isBlank()) {
            return 0.0
        }

        return nf.parse(text).toDouble()
    }

    /**
     * State fields of the reader.
     */
    private var timestamp: Instant? = null
    private var cpuCores = -1
    private var cpuCapacity = Double.NaN
    private var cpuUsage = Double.NaN
    private var cpuUsagePct = Double.NaN
    private var memCapacity = Double.NaN
    private var memUsage = Double.NaN
    private var diskRead = Double.NaN
    private var diskWrite = Double.NaN
    private var netReceived = Double.NaN
    private var netTransmitted = Double.NaN

    /**
     * Reset the state.
     */
    private fun reset() {
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

    /**
     * The type of the timestamp in the trace.
     */
    private enum class TimestampType {
        UNDECIDED, DATE_TIME, EPOCH_MILLIS
    }

    companion object {
        /**
         * The [CsvSchema] that is used to parse the trace.
         */
        private val schema = CsvSchema.builder()
            .addColumn("Timestamp [ms]", CsvSchema.ColumnType.NUMBER_OR_STRING)
            .addColumn("CPU cores", CsvSchema.ColumnType.NUMBER)
            .addColumn("CPU capacity provisioned [MHZ]", CsvSchema.ColumnType.NUMBER)
            .addColumn("CPU usage [MHZ]", CsvSchema.ColumnType.NUMBER)
            .addColumn("CPU usage [%]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Memory capacity provisioned [KB]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Memory usage [KB]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Memory usage [%]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Disk read throughput [KB/s]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Disk write throughput [KB/s]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Disk size [GB]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Network received throughput [KB/s]", CsvSchema.ColumnType.NUMBER)
            .addColumn("Network transmitted throughput [KB/s]", CsvSchema.ColumnType.NUMBER)
            .setAllowComments(true)
            .setUseHeader(true)
            .setColumnSeparator(';')
            .build()
    }
}
