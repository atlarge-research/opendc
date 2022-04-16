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

package org.opendc.trace.azure

import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.dataformat.csv.CsvParser
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import org.opendc.trace.*
import org.opendc.trace.conv.RESOURCE_ID
import org.opendc.trace.conv.RESOURCE_STATE_CPU_USAGE_PCT
import org.opendc.trace.conv.RESOURCE_STATE_TIMESTAMP
import java.time.Instant

/**
 * A [TableReader] for the Azure v1 VM resource state table.
 */
internal class AzureResourceStateTableReader(private val parser: CsvParser) : TableReader {
    init {
        parser.schema = schema
    }

    override fun nextRow(): Boolean {
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
                "timestamp" -> timestamp = Instant.ofEpochSecond(parser.longValue)
                "vm id" -> id = parser.text
                "CPU avg cpu" -> cpuUsagePct = (parser.doubleValue / 100.0) // Convert from % to [0, 1]
            }
        }

        return true
    }

    override fun resolve(column: TableColumn<*>): Int = columns[column] ?: -1

    override fun isNull(index: Int): Boolean {
        require(index in 0..columns.size) { "Invalid column index" }
        return false
    }

    override fun get(index: Int): Any? {
        return when (index) {
            COL_ID -> id
            COL_TIMESTAMP -> timestamp
            COL_CPU_USAGE_PCT -> cpuUsagePct
            else -> throw IllegalArgumentException("Invalid column index")
        }
    }

    override fun getBoolean(index: Int): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(index: Int): Int {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getLong(index: Int): Long {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getDouble(index: Int): Double {
        return when (index) {
            COL_CPU_USAGE_PCT -> cpuUsagePct
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
     * State fields of the reader.
     */
    private var id: String? = null
    private var timestamp: Instant? = null
    private var cpuUsagePct = Double.NaN

    /**
     * Reset the state.
     */
    private fun reset() {
        id = null
        timestamp = null
        cpuUsagePct = Double.NaN
    }

    private val COL_ID = 0
    private val COL_TIMESTAMP = 1
    private val COL_CPU_USAGE_PCT = 2
    private val columns = mapOf(
        RESOURCE_ID to COL_ID,
        RESOURCE_STATE_TIMESTAMP to COL_TIMESTAMP,
        RESOURCE_STATE_CPU_USAGE_PCT to COL_CPU_USAGE_PCT
    )

    companion object {
        /**
         * The [CsvSchema] that is used to parse the trace.
         */
        private val schema = CsvSchema.builder()
            .addColumn("timestamp", CsvSchema.ColumnType.NUMBER)
            .addColumn("vm id", CsvSchema.ColumnType.STRING)
            .addColumn("CPU min cpu", CsvSchema.ColumnType.NUMBER)
            .addColumn("CPU max cpu", CsvSchema.ColumnType.NUMBER)
            .addColumn("CPU avg cpu", CsvSchema.ColumnType.NUMBER)
            .setAllowComments(true)
            .build()
    }
}
