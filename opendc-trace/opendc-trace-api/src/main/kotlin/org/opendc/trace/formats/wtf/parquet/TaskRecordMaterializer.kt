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

package org.opendc.trace.wtf.parquet

import org.apache.parquet.io.api.Converter
import org.apache.parquet.io.api.GroupConverter
import org.apache.parquet.io.api.PrimitiveConverter
import org.apache.parquet.io.api.RecordMaterializer
import org.apache.parquet.schema.MessageType
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * A [RecordMaterializer] for [Task] records.
 */
internal class TaskRecordMaterializer(schema: MessageType) : RecordMaterializer<Task>() {
    /**
     * State of current record being read.
     */
    private var localID = ""
    private var localWorkflowID = ""
    private var localSubmitTime = Instant.MIN
    private var localWaitTime = Duration.ZERO
    private var localRuntime = Duration.ZERO
    private var localRequestedCpus = 0
    private var localGroupId = 0
    private var localUserId = 0
    private var localParents = mutableSetOf<String>()
    private var localChildren = mutableSetOf<String>()

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
                                override fun addLong(value: Long) {
                                    localID = value.toString()
                                }
                            }
                        "workflow_id" ->
                            object : PrimitiveConverter() {
                                override fun addLong(value: Long) {
                                    localWorkflowID = value.toString()
                                }
                            }
                        "ts_submit" ->
                            object : PrimitiveConverter() {
                                override fun addLong(value: Long) {
                                    localSubmitTime = Instant.ofEpochMilli(value)
                                }
                            }
                        "wait_time" ->
                            object : PrimitiveConverter() {
                                override fun addLong(value: Long) {
                                    localWaitTime = Duration.ofMillis(value)
                                }
                            }
                        "runtime" ->
                            object : PrimitiveConverter() {
                                override fun addLong(value: Long) {
                                    localRuntime = Duration.ofMillis(value)
                                }
                            }
                        "resource_amount_requested" ->
                            object : PrimitiveConverter() {
                                override fun addDouble(value: Double) {
                                    localRequestedCpus = value.roundToInt()
                                }
                            }
                        "group_id" ->
                            object : PrimitiveConverter() {
                                override fun addInt(value: Int) {
                                    localGroupId = value
                                }
                            }
                        "user_id" ->
                            object : PrimitiveConverter() {
                                override fun addInt(value: Int) {
                                    localUserId = value
                                }
                            }
                        "children" -> RelationConverter(localChildren)
                        "parents" -> RelationConverter(localParents)
                        else -> error("Unknown column $type")
                    }
                }

            override fun start() {
                localID = ""
                localWorkflowID = ""
                localSubmitTime = Instant.MIN
                localWaitTime = Duration.ZERO
                localRuntime = Duration.ZERO
                localRequestedCpus = 0
                localGroupId = 0
                localUserId = 0
                localParents.clear()
                localChildren.clear()
            }

            override fun end() {}

            override fun getConverter(fieldIndex: Int): Converter = converters[fieldIndex]
        }

    override fun getCurrentRecord(): Task =
        Task(
            localID,
            localWorkflowID,
            localSubmitTime,
            localWaitTime,
            localRuntime,
            localRequestedCpus,
            localGroupId,
            localUserId,
            localParents.toSet(),
            localChildren.toSet(),
        )

    override fun getRootConverter(): GroupConverter = root

    /**
     * Helper class to convert parent and child relations and add them to [relations].
     */
    private class RelationConverter(private val relations: MutableSet<String>) : GroupConverter() {
        private val entryConverter =
            object : PrimitiveConverter() {
                override fun addLong(value: Long) {
                    relations.add(value.toString())
                }

                override fun addDouble(value: Double) {
                    relations.add(value.roundToLong().toString())
                }
            }

        private val listConverter =
            object : GroupConverter() {
                override fun getConverter(fieldIndex: Int): Converter {
                    require(fieldIndex == 0)
                    return entryConverter
                }

                override fun start() {}

                override fun end() {}
            }

        override fun getConverter(fieldIndex: Int): Converter {
            require(fieldIndex == 0)
            return listConverter
        }

        override fun start() {}

        override fun end() {}
    }
}
