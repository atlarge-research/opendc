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

package org.opendc.experiments.capelin.topology

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import java.io.File
import java.io.InputStream

/**
 * A helper class for reading a cluster specification file.
 */
class ClusterSpecReader {
    /**
     * The [CsvMapper] to map the environment file to an object.
     */
    private val mapper = CsvMapper()

    /**
     * The [ObjectReader] to convert the lines into objects.
     */
    private val reader: ObjectReader = mapper.readerFor(Entry::class.java).with(schema)

    /**
     * Read the specified [file].
     */
    fun read(file: File): List<ClusterSpec> {
        return reader.readValues<Entry>(file).use { read(it) }
    }

    /**
     * Read the specified [input].
     */
    fun read(input: InputStream): List<ClusterSpec> {
        return reader.readValues<Entry>(input).use { read(it) }
    }

    /**
     * Convert the specified [MappingIterator] into a list of [ClusterSpec]s.
     */
    private fun read(it: MappingIterator<Entry>): List<ClusterSpec> {
        val result = mutableListOf<ClusterSpec>()

        for (entry in it) {
            val def = ClusterSpec(
                entry.id,
                entry.name,
                entry.cpuCount,
                entry.cpuSpeed * 1000, // Convert to MHz
                entry.memCapacity * 1000, // Convert to MiB
                entry.hostCount,
                entry.memCapacityPerHost * 1000,
                entry.cpuCountPerHost
            )
            result.add(def)
        }

        return result
    }

    private open class Entry(
        @JsonProperty("ClusterID")
        val id: String,
        @JsonProperty("ClusterName")
        val name: String,
        @JsonProperty("Cores")
        val cpuCount: Int,
        @JsonProperty("Speed")
        val cpuSpeed: Double,
        @JsonProperty("Memory")
        val memCapacity: Double,
        @JsonProperty("numberOfHosts")
        val hostCount: Int,
        @JsonProperty("memoryCapacityPerHost")
        val memCapacityPerHost: Double,
        @JsonProperty("coreCountPerHost")
        val cpuCountPerHost: Int
    )

    companion object {
        /**
         * The [CsvSchema] that is used to parse the trace.
         */
        private val schema = CsvSchema.builder()
            .addColumn("ClusterID", CsvSchema.ColumnType.STRING)
            .addColumn("ClusterName", CsvSchema.ColumnType.STRING)
            .addColumn("Cores", CsvSchema.ColumnType.NUMBER)
            .addColumn("Speed", CsvSchema.ColumnType.NUMBER)
            .addColumn("Memory", CsvSchema.ColumnType.NUMBER)
            .addColumn("numberOfHosts", CsvSchema.ColumnType.NUMBER)
            .addColumn("memoryCapacityPerHost", CsvSchema.ColumnType.NUMBER)
            .addColumn("coreCountPerHost", CsvSchema.ColumnType.NUMBER)
            .setAllowComments(true)
            .setColumnSeparator(';')
            .setUseHeader(true)
            .build()
    }
}
