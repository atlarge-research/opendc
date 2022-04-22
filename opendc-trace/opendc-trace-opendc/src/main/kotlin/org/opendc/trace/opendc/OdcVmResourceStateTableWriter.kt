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

import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.parquet.hadoop.ParquetWriter
import org.opendc.trace.*
import org.opendc.trace.conv.*
import java.time.Duration
import java.time.Instant

/**
 * A [TableWriter] implementation for the OpenDC virtual machine trace format.
 */
internal class OdcVmResourceStateTableWriter(
    private val writer: ParquetWriter<GenericRecord>,
    private val schema: Schema
) : TableWriter {
    /**
     * The current builder for the record that is being written.
     */
    private var builder: GenericRecordBuilder? = null

    /**
     * The fields belonging to the resource state schema.
     */
    private val fields = schema.fields

    override fun startRow() {
        builder = GenericRecordBuilder(schema)
    }

    override fun endRow() {
        val builder = checkNotNull(builder) { "No active row" }
        this.builder = null

        val record = builder.build()
        val id = record[COL_ID] as String
        val timestamp = record[COL_TIMESTAMP] as Long

        check(lastId != id || timestamp >= lastTimestamp) { "Records need to be ordered by (id, timestamp)" }

        writer.write(builder.build())

        lastId = id
        lastTimestamp = timestamp
    }

    override fun resolve(column: TableColumn<*>): Int {
        val schema = schema
        return when (column) {
            RESOURCE_ID -> schema.getField("id").pos()
            RESOURCE_STATE_TIMESTAMP -> (schema.getField("timestamp") ?: schema.getField("time")).pos()
            RESOURCE_STATE_DURATION -> schema.getField("duration").pos()
            RESOURCE_CPU_COUNT -> (schema.getField("cpu_count") ?: schema.getField("cores")).pos()
            RESOURCE_STATE_CPU_USAGE -> (schema.getField("cpu_usage") ?: schema.getField("cpuUsage")).pos()
            else -> -1
        }
    }

    override fun set(index: Int, value: Any) {
        val builder = checkNotNull(builder) { "No active row" }

        builder.set(
            fields[index],
            when (index) {
                COL_TIMESTAMP -> (value as Instant).toEpochMilli()
                COL_DURATION -> (value as Duration).toMillis()
                else -> value
            }
        )
    }

    override fun setBoolean(index: Int, value: Boolean) = set(index, value)

    override fun setInt(index: Int, value: Int) = set(index, value)

    override fun setLong(index: Int, value: Long) = set(index, value)

    override fun setDouble(index: Int, value: Double) = set(index, value)

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
    private var lastTimestamp: Long = Long.MIN_VALUE

    /**
     * Columns with special behavior.
     */
    private val COL_ID = resolve(RESOURCE_ID)
    private val COL_TIMESTAMP = resolve(RESOURCE_STATE_TIMESTAMP)
    private val COL_DURATION = resolve(RESOURCE_STATE_DURATION)
}
