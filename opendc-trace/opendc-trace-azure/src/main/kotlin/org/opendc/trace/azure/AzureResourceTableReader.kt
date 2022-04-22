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
import org.opendc.trace.conv.*
import java.time.Instant

/**
 * A [TableReader] for the Azure v1 VM resources table.
 */
internal class AzureResourceTableReader(private val parser: CsvParser) : TableReader {
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
                "vm id" -> id = parser.text
                "timestamp vm created" -> startTime = Instant.ofEpochSecond(parser.longValue)
                "timestamp vm deleted" -> stopTime = Instant.ofEpochSecond(parser.longValue)
                "vm virtual core count" -> cpuCores = parser.intValue
                "vm memory" -> memCapacity = parser.doubleValue * 1e6 // GB to KB
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
            COL_START_TIME -> startTime
            COL_STOP_TIME -> stopTime
            COL_CPU_COUNT -> getInt(index)
            COL_MEM_CAPACITY -> getDouble(index)
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getBoolean(index: Int): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(index: Int): Int {
        return when (index) {
            COL_CPU_COUNT -> cpuCores
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getLong(index: Int): Long {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getDouble(index: Int): Double {
        return when (index) {
            COL_MEM_CAPACITY -> memCapacity
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
    private var startTime: Instant? = null
    private var stopTime: Instant? = null
    private var cpuCores = -1
    private var memCapacity = Double.NaN

    /**
     * Reset the state.
     */
    private fun reset() {
        id = null
        startTime = null
        stopTime = null
        cpuCores = -1
        memCapacity = Double.NaN
    }

    private val COL_ID = 0
    private val COL_START_TIME = 1
    private val COL_STOP_TIME = 2
    private val COL_CPU_COUNT = 3
    private val COL_MEM_CAPACITY = 4
    private val columns = mapOf(
        RESOURCE_ID to COL_ID,
        RESOURCE_START_TIME to COL_START_TIME,
        RESOURCE_STOP_TIME to COL_STOP_TIME,
        RESOURCE_CPU_COUNT to COL_CPU_COUNT,
        RESOURCE_MEM_CAPACITY to COL_MEM_CAPACITY
    )

    companion object {
        /**
         * The [CsvSchema] that is used to parse the trace.
         */
        private val schema = CsvSchema.builder()
            .addColumn("vm id", CsvSchema.ColumnType.NUMBER)
            .addColumn("subscription id", CsvSchema.ColumnType.STRING)
            .addColumn("deployment id", CsvSchema.ColumnType.NUMBER)
            .addColumn("timestamp vm created", CsvSchema.ColumnType.NUMBER)
            .addColumn("timestamp vm deleted", CsvSchema.ColumnType.NUMBER)
            .addColumn("max cpu", CsvSchema.ColumnType.NUMBER)
            .addColumn("avg cpu", CsvSchema.ColumnType.NUMBER)
            .addColumn("p95 cpu", CsvSchema.ColumnType.NUMBER)
            .addColumn("vm category", CsvSchema.ColumnType.NUMBER)
            .addColumn("vm virtual core count", CsvSchema.ColumnType.NUMBER)
            .addColumn("vm memory", CsvSchema.ColumnType.NUMBER)
            .setAllowComments(true)
            .build()
    }
}
