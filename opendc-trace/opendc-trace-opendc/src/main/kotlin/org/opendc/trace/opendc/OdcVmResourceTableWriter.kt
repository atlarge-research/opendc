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
import java.time.Instant
import kotlin.math.roundToLong

/**
 * A [TableWriter] implementation for the OpenDC virtual machine trace format.
 */
internal class OdcVmResourceTableWriter(
    private val writer: ParquetWriter<GenericRecord>,
    private val schema: Schema
) : TableWriter {
    /**
     * The current builder for the record that is being written.
     */
    private var builder: GenericRecordBuilder? = null

    /**
     * The fields belonging to the resource schema.
     */
    private val fields = schema.fields

    override fun startRow() {
        builder = GenericRecordBuilder(schema)
    }

    override fun endRow() {
        val builder = checkNotNull(builder) { "No active row" }
        this.builder = null
        writer.write(builder.build())
    }

    override fun resolve(column: TableColumn<*>): Int {
        val schema = schema
        return when (column) {
            RESOURCE_ID -> schema.getField("id").pos()
            RESOURCE_START_TIME -> (schema.getField("start_time") ?: schema.getField("submissionTime")).pos()
            RESOURCE_STOP_TIME -> (schema.getField("stop_time") ?: schema.getField("endTime")).pos()
            RESOURCE_CPU_COUNT -> (schema.getField("cpu_count") ?: schema.getField("maxCores")).pos()
            RESOURCE_MEM_CAPACITY -> (schema.getField("mem_capacity") ?: schema.getField("requiredMemory")).pos()
            else -> -1
        }
    }

    override fun set(index: Int, value: Any) {
        val builder = checkNotNull(builder) { "No active row" }
        builder.set(
            fields[index],
            when (index) {
                COL_START_TIME, COL_STOP_TIME -> (value as Instant).toEpochMilli()
                COL_MEM_CAPACITY -> (value as Double).roundToLong()
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
     * Columns with special behavior.
     */
    private val COL_START_TIME = resolve(RESOURCE_START_TIME)
    private val COL_STOP_TIME = resolve(RESOURCE_STOP_TIME)
    private val COL_MEM_CAPACITY = resolve(RESOURCE_MEM_CAPACITY)
}
