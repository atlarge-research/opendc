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
    private var _id = ""
    private var _workflowId = ""
    private var _submitTime = Instant.MIN
    private var _waitTime = Duration.ZERO
    private var _runtime = Duration.ZERO
    private var _requestedCpus = 0
    private var _groupId = 0
    private var _userId = 0
    private var _parents = mutableSetOf<String>()
    private var _children = mutableSetOf<String>()

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
                    override fun addLong(value: Long) {
                        _id = value.toString()
                    }
                }
                "workflow_id" -> object : PrimitiveConverter() {
                    override fun addLong(value: Long) {
                        _workflowId = value.toString()
                    }
                }
                "ts_submit" -> object : PrimitiveConverter() {
                    override fun addLong(value: Long) {
                        _submitTime = Instant.ofEpochMilli(value)
                    }
                }
                "wait_time" -> object : PrimitiveConverter() {
                    override fun addLong(value: Long) {
                        _waitTime = Duration.ofMillis(value)
                    }
                }
                "runtime" -> object : PrimitiveConverter() {
                    override fun addLong(value: Long) {
                        _runtime = Duration.ofMillis(value)
                    }
                }
                "resource_amount_requested" -> object : PrimitiveConverter() {
                    override fun addDouble(value: Double) {
                        _requestedCpus = value.roundToInt()
                    }
                }
                "group_id" -> object : PrimitiveConverter() {
                    override fun addInt(value: Int) {
                        _groupId = value
                    }
                }
                "user_id" -> object : PrimitiveConverter() {
                    override fun addInt(value: Int) {
                        _userId = value
                    }
                }
                "children" -> RelationConverter(_children)
                "parents" -> RelationConverter(_parents)
                else -> error("Unknown column $type")
            }
        }

        override fun start() {
            _id = ""
            _workflowId = ""
            _submitTime = Instant.MIN
            _waitTime = Duration.ZERO
            _runtime = Duration.ZERO
            _requestedCpus = 0
            _groupId = 0
            _userId = 0
            _parents.clear()
            _children.clear()
        }

        override fun end() {}

        override fun getConverter(fieldIndex: Int): Converter = converters[fieldIndex]
    }

    override fun getCurrentRecord(): Task = Task(
        _id,
        _workflowId,
        _submitTime,
        _waitTime,
        _runtime,
        _requestedCpus,
        _groupId,
        _userId,
        _parents.toSet(),
        _children.toSet()
    )

    override fun getRootConverter(): GroupConverter = root

    /**
     * Helper class to convert parent and child relations and add them to [relations].
     */
    private class RelationConverter(private val relations: MutableSet<String>) : GroupConverter() {
        private val entryConverter = object : PrimitiveConverter() {
            override fun addLong(value: Long) {
                relations.add(value.toString())
            }

            override fun addDouble(value: Double) {
                relations.add(value.roundToLong().toString())
            }
        }

        private val listConverter = object : GroupConverter() {
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
