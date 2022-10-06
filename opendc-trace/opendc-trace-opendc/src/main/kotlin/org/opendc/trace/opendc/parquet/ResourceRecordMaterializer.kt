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

package org.opendc.trace.opendc.parquet

import org.apache.parquet.io.api.Binary
import org.apache.parquet.io.api.Converter
import org.apache.parquet.io.api.GroupConverter
import org.apache.parquet.io.api.PrimitiveConverter
import org.apache.parquet.io.api.RecordMaterializer
import org.apache.parquet.schema.MessageType
import java.time.Instant

/**
 * A [RecordMaterializer] for [Resource] records.
 */
internal class ResourceRecordMaterializer(schema: MessageType) : RecordMaterializer<Resource>() {
    /**
     * State of current record being read.
     */
    private var _id = ""
    private var _startTime = Instant.MIN
    private var _stopTime = Instant.MIN
    private var _cpuCount = 0
    private var _cpuCapacity = 0.0
    private var _memCapacity = 0.0

    /**
     * Root converter for the record.
     */
    private val root = object : GroupConverter() {
        /**
         * The converters for the columns of the schema.
         */
        private val converters = schema.fields.map { type ->
            when (type.name) {
                "id" -> object : PrimitiveConverter() {
                    override fun addBinary(value: Binary) {
                        _id = value.toStringUsingUTF8()
                    }
                }
                "start_time", "submissionTime" -> object : PrimitiveConverter() {
                    override fun addLong(value: Long) {
                        _startTime = Instant.ofEpochMilli(value)
                    }
                }
                "stop_time", "endTime" -> object : PrimitiveConverter() {
                    override fun addLong(value: Long) {
                        _stopTime = Instant.ofEpochMilli(value)
                    }
                }
                "cpu_count", "maxCores" -> object : PrimitiveConverter() {
                    override fun addInt(value: Int) {
                        _cpuCount = value
                    }
                }
                "cpu_capacity" -> object : PrimitiveConverter() {
                    override fun addDouble(value: Double) {
                        _cpuCapacity = value
                    }
                }
                "mem_capacity", "requiredMemory" -> object : PrimitiveConverter() {
                    override fun addDouble(value: Double) {
                        _memCapacity = value
                    }

                    override fun addLong(value: Long) {
                        _memCapacity = value.toDouble()
                    }
                }
                else -> error("Unknown column $type")
            }
        }

        override fun start() {
            _id = ""
            _startTime = Instant.MIN
            _stopTime = Instant.MIN
            _cpuCount = 0
            _cpuCapacity = 0.0
            _memCapacity = 0.0
        }

        override fun end() {}

        override fun getConverter(fieldIndex: Int): Converter = converters[fieldIndex]
    }

    override fun getCurrentRecord(): Resource = Resource(_id, _startTime, _stopTime, _cpuCount, _cpuCapacity, _memCapacity)

    override fun getRootConverter(): GroupConverter = root
}
