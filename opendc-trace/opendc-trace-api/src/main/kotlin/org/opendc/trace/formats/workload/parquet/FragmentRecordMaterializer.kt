/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.trace.formats.workload.parquet

import org.apache.parquet.io.api.Converter
import org.apache.parquet.io.api.GroupConverter
import org.apache.parquet.io.api.PrimitiveConverter
import org.apache.parquet.io.api.RecordMaterializer
import org.apache.parquet.schema.MessageType
import java.time.Duration
import java.time.Instant

/**
 * A [RecordMaterializer] for [FragmentParquetSchema] records.
 */
internal class FragmentRecordMaterializer(schema: MessageType) : RecordMaterializer<FragmentParquetSchema>() {
    /**
     * State of current record being read.
     */
    private var localId = -99
    private var localTimestamp = Instant.MIN
    private var localDuration = Duration.ZERO
    private var localCpuCount = 0
    private var localCpuUsage = 0.0
    private var localGpuCount = 0
    private var localGpuUsage = 0.0

    /**
     * Root converter for the record.
     */
    private val root =
        object : GroupConverter() {
            /**
             * The converters for the columns of the schema.
             */
            private val converters =
                schema.fields.map { type ->
                    when (type.name) {
                        "id" ->
                            object : PrimitiveConverter() {
                                override fun addInt(value: Int) {
                                    localId = value
                                }
                            }
                        "timestamp", "time" ->
                            object : PrimitiveConverter() {
                                override fun addLong(value: Long) {
                                    localTimestamp = Instant.ofEpochMilli(value)
                                }
                            }
                        "duration" ->
                            object : PrimitiveConverter() {
                                override fun addLong(value: Long) {
                                    localDuration = Duration.ofMillis(value)
                                }
                            }
                        "cpu_count", "cores" ->
                            object : PrimitiveConverter() {
                                override fun addInt(value: Int) {
                                    localCpuCount = value
                                }
                            }
                        "cpu_usage", "cpuUsage" ->
                            object : PrimitiveConverter() {
                                override fun addDouble(value: Double) {
                                    localCpuUsage = value
                                }
                            }
                        "gpu_count", "gpuCount", "gpu_cores", "gpuCores" ->
                            object : PrimitiveConverter() {
                                override fun addInt(value: Int) {
                                    localGpuCount = value
                                }
                            }
                        "gpu_usage", "gpuUsage" ->
                            object : PrimitiveConverter() {
                                override fun addDouble(value: Double) {
                                    localGpuUsage = value
                                }
                            }
                        else -> error("Unknown column $type")
                    }
                }

            override fun start() {
                localId = -99
                localDuration = Duration.ZERO
                localCpuCount = 0
                localCpuUsage = 0.0
                localGpuCount = 0
                localGpuUsage = 0.0
            }

            override fun end() {}

            override fun getConverter(fieldIndex: Int): Converter = converters[fieldIndex]
        }

    override fun getCurrentRecord(): FragmentParquetSchema =
        FragmentParquetSchema(
            localId,
            localDuration,
            localCpuUsage,
            localGpuUsage,
        )

    override fun getRootConverter(): GroupConverter = root
}
