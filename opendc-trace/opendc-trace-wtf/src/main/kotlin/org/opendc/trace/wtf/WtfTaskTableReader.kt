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

package org.opendc.trace.wtf

import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.opendc.trace.*
import org.opendc.trace.conv.*
import org.opendc.trace.util.parquet.LocalParquetReader
import java.time.Duration
import java.time.Instant

/**
 * A [TableReader] implementation for the WTF format.
 */
internal class WtfTaskTableReader(private val reader: LocalParquetReader<GenericRecord>) : TableReader {
    /**
     * The current record.
     */
    private var record: GenericRecord? = null

    /**
     * A flag to indicate that the columns have been initialized.
     */
    private var hasInitializedColumns = false

    override fun nextRow(): Boolean {
        val record = reader.read()
        this.record = record

        if (!hasInitializedColumns && record != null) {
            initColumns(record.schema)
            hasInitializedColumns = true
        }

        return record != null
    }

    override fun resolve(column: TableColumn<*>): Int = columns[column] ?: -1

    override fun isNull(index: Int): Boolean {
        check(index in 0..columns.size) { "Invalid column index" }
        return get(index) == null
    }

    override fun get(index: Int): Any? {
        val record = checkNotNull(record) { "Reader in invalid state" }
        @Suppress("UNCHECKED_CAST")
        return when (index) {
            COL_ID -> (record[AVRO_COL_ID] as Long).toString()
            COL_WORKFLOW_ID -> (record[AVRO_COL_WORKFLOW_ID] as Long).toString()
            COL_SUBMIT_TIME -> Instant.ofEpochMilli(record[AVRO_COL_SUBMIT_TIME] as Long)
            COL_WAIT_TIME -> Duration.ofMillis(record[AVRO_COL_WAIT_TIME] as Long)
            COL_RUNTIME -> Duration.ofMillis(record[AVRO_COL_RUNTIME] as Long)
            COL_REQ_NCPUS, COL_GROUP_ID, COL_USER_ID -> getInt(index)
            COL_PARENTS -> (record[AVRO_COL_PARENTS] as ArrayList<GenericRecord>).map { it["item"].toString() }.toSet()
            COL_CHILDREN -> (record[AVRO_COL_CHILDREN] as ArrayList<GenericRecord>).map { it["item"].toString() }.toSet()
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getBoolean(index: Int): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(index: Int): Int {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (index) {
            COL_REQ_NCPUS -> (record[AVRO_COL_REQ_NCPUS] as Double).toInt()
            COL_GROUP_ID -> record[AVRO_COL_GROUP_ID] as Int
            COL_USER_ID -> record[AVRO_COL_USER_ID] as Int
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getLong(index: Int): Long {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getDouble(index: Int): Double {
        throw IllegalArgumentException("Invalid column")
    }

    override fun close() {
        reader.close()
    }

    /**
     * Initialize the columns for the reader based on [schema].
     */
    private fun initColumns(schema: Schema) {
        try {
            AVRO_COL_ID = schema.getField("id").pos()
            AVRO_COL_WORKFLOW_ID = schema.getField("workflow_id").pos()
            AVRO_COL_SUBMIT_TIME = schema.getField("ts_submit").pos()
            AVRO_COL_WAIT_TIME = schema.getField("wait_time").pos()
            AVRO_COL_RUNTIME = schema.getField("runtime").pos()
            AVRO_COL_REQ_NCPUS = schema.getField("resource_amount_requested").pos()
            AVRO_COL_PARENTS = schema.getField("parents").pos()
            AVRO_COL_CHILDREN = schema.getField("children").pos()
            AVRO_COL_GROUP_ID = schema.getField("group_id").pos()
            AVRO_COL_USER_ID = schema.getField("user_id").pos()
        } catch (e: NullPointerException) {
            // This happens when the field we are trying to access does not exist
            throw IllegalArgumentException("Invalid schema", e)
        }
    }

    private var AVRO_COL_ID = -1
    private var AVRO_COL_WORKFLOW_ID = -1
    private var AVRO_COL_SUBMIT_TIME = -1
    private var AVRO_COL_WAIT_TIME = -1
    private var AVRO_COL_RUNTIME = -1
    private var AVRO_COL_REQ_NCPUS = -1
    private var AVRO_COL_PARENTS = -1
    private var AVRO_COL_CHILDREN = -1
    private var AVRO_COL_GROUP_ID = -1
    private var AVRO_COL_USER_ID = -1

    private val COL_ID = 0
    private val COL_WORKFLOW_ID = 1
    private val COL_SUBMIT_TIME = 2
    private val COL_WAIT_TIME = 3
    private val COL_RUNTIME = 4
    private val COL_REQ_NCPUS = 5
    private val COL_PARENTS = 6
    private val COL_CHILDREN = 7
    private val COL_GROUP_ID = 8
    private val COL_USER_ID = 9

    private val columns = mapOf(
        TASK_ID to COL_ID,
        TASK_WORKFLOW_ID to COL_WORKFLOW_ID,
        TASK_SUBMIT_TIME to COL_SUBMIT_TIME,
        TASK_WAIT_TIME to COL_WAIT_TIME,
        TASK_RUNTIME to COL_RUNTIME,
        TASK_REQ_NCPUS to COL_REQ_NCPUS,
        TASK_PARENTS to COL_PARENTS,
        TASK_CHILDREN to COL_CHILDREN,
        TASK_GROUP_ID to COL_GROUP_ID,
        TASK_USER_ID to COL_USER_ID,
    )
}
