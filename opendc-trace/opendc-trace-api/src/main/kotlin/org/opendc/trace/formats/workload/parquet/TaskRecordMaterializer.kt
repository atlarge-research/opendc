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

import org.apache.parquet.io.api.Binary
import org.apache.parquet.io.api.Converter
import org.apache.parquet.io.api.GroupConverter
import org.apache.parquet.io.api.PrimitiveConverter
import org.apache.parquet.io.api.RecordMaterializer
import org.apache.parquet.schema.MessageType
import java.time.Instant

/**
 * A [RecordMaterializer] for [Task] records.
 */
internal class TaskRecordMaterializer(schema: MessageType) : RecordMaterializer<Task>() {
    /**
     * State of current record being read.
     */
    private var localId = -99
    private var localName = ""
    private var localSubmissionTime = Instant.MIN
    private var localDuration = 0L
    private var localCpuCount = 0
    private var localCpuCapacity = 0.0
    private var localMemCapacity = 0.0
    private var localGpuCount = 0
    private var localGpuCapacity = 0.0
    private var localParents = mutableSetOf<Int>()
    private var localChildren = mutableSetOf<Int>()
    private var localDeferrable: Boolean = false
    private var localDeadline = -1L

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
                        "name" ->
                            object : PrimitiveConverter() {
                                override fun addBinary(value: Binary) {
                                    localName = value.toStringUsingUTF8()
                                }
                            }
                        "submission_time", "submissionTime" ->
                            object : PrimitiveConverter() {
                                override fun addLong(value: Long) {
                                    localSubmissionTime = Instant.ofEpochMilli(value)
                                }
                            }
                        "duration" ->
                            object : PrimitiveConverter() {
                                override fun addLong(value: Long) {
                                    localDuration = value
                                }
                            }
                        "cpu_count", "maxCores" ->
                            object : PrimitiveConverter() {
                                override fun addInt(value: Int) {
                                    localCpuCount = value
                                }
                            }
                        "cpu_capacity" ->
                            object : PrimitiveConverter() {
                                override fun addDouble(value: Double) {
                                    localCpuCapacity = value
                                }
                            }
                        "mem_capacity", "requiredMemory" ->
                            object : PrimitiveConverter() {
                                override fun addDouble(value: Double) {
                                    localMemCapacity = value
                                }

                                override fun addLong(value: Long) {
                                    localMemCapacity = value.toDouble()
                                }
                            }
                        "gpu_count", "gpuMaxCores" ->
                            object : PrimitiveConverter() {
                                override fun addInt(value: Int) {
                                    localGpuCount = value
                                }
                            }
                        "gpu_capacity" ->
                            object : PrimitiveConverter() {
                                override fun addDouble(value: Double) {
                                    localGpuCapacity = value
                                }
                            }
                        "parents" -> RelationConverter(localParents)
                        "children" -> RelationConverter(localChildren)
                        "deferrable" ->
                            object : PrimitiveConverter() {
                                override fun addBoolean(value: Boolean) {
                                    localDeferrable = value
                                }
                            }
                        "deadline" ->
                            object : PrimitiveConverter() {
                                override fun addLong(value: Long) {
                                    localDeadline = value
                                }
                            }
                        else -> error("Unknown column $type")
                    }
                }

            override fun start() {
                localId = -99
                localName = ""
                localSubmissionTime = Instant.MIN
                localDuration = 0L
                localCpuCount = 0
                localCpuCapacity = 0.0
                localMemCapacity = 0.0
                localGpuCount = 0
                localGpuCapacity = 0.0
                localParents.clear()
                localChildren.clear()
                localDeferrable = false
                localDeadline = -1
            }

            override fun end() {}

            override fun getConverter(fieldIndex: Int): Converter = converters[fieldIndex]
        }

    override fun getCurrentRecord(): Task =
        Task(
            localId,
            localName,
            localSubmissionTime,
            localDuration,
            localCpuCount,
            localCpuCapacity,
            localMemCapacity,
            localGpuCount,
            localGpuCapacity,
            localParents.toMutableSet(),
            localChildren.toSet(),
            localDeferrable,
            localDeadline,
        )

    override fun getRootConverter(): GroupConverter = root

    /**
     * Helper class to convert parent and child relations and add them to [relations].
     */
    private class RelationConverter(private val relations: MutableSet<Int>) : GroupConverter() {
        private val entryConverter =
            object : PrimitiveConverter() {
                override fun addInt(value: Int) {
                    relations.add(value)
                }
            }

        private val listGroupConverter =
            object : GroupConverter() {
                override fun getConverter(fieldIndex: Int): Converter {
                    // fieldIndex = 0 corresponds to "element"
                    require(fieldIndex == 0)
                    return entryConverter
                }

                override fun start() {}

                override fun end() {}
            }

        override fun getConverter(fieldIndex: Int): Converter {
            // fieldIndex = 0 corresponds to "list"
            require(fieldIndex == 0)
            return listGroupConverter
        }

        override fun start() {}

        override fun end() {}
    }
}
