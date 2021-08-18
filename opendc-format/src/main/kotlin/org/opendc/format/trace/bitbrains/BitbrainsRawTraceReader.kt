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

package org.opendc.format.trace.bitbrains

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import java.io.InputStream

/**
 * A trace reader that enables the user to read Bitbrains specific trace data.
 */
public class BitbrainsRawTraceReader(input: InputStream) : Iterator<BitbrainsRawTraceReader.Entry>, AutoCloseable {
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

    /**
     * The mapping iterator to use.
     */
    private val iterator: MappingIterator<Entry> = CsvMapper().readerFor(Entry::class.java).with(schema)
        .readValues(input)

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }

    override fun next(): Entry {
        return iterator.next()
    }

    override fun close() {
        iterator.close()
    }

    /**
     * A single entry in the trace.
     */
    public data class Entry(
        @JsonProperty("Timestamp [ms]")
        val timestamp: Long,
        @JsonProperty("CPU cores")
        val cpuCores: Int,
        @JsonProperty("CPU capacity provisioned [MHZ]")
        val cpuCapacity: Double,
        @JsonProperty("CPU usage [MHZ]")
        val cpuUsage: Double,
        @JsonProperty("CPU usage [%]")
        val cpuUsagePct: Double,
        @JsonProperty("Memory capacity provisioned [KB]")
        val memCapacity: Double,
        @JsonProperty("Memory usage [KB]")
        val memUsage: Double,
        @JsonProperty("Disk read throughput [KB/s]")
        val diskRead: Double,
        @JsonProperty("Disk write throughput [KB/s]")
        val diskWrite: Double,
        @JsonProperty("Network received throughput [KB/s]")
        val netReceived: Double,
        @JsonProperty("Network transmitted throughput [KB/s]")
        val netTransmitted: Double
    )
}
