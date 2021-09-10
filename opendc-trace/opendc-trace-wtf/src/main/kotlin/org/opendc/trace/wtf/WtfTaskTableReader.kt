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

import org.apache.avro.generic.GenericRecord
import org.opendc.trace.*
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

    override fun nextRow(): Boolean {
        record = reader.read()
        return record != null
    }

    override fun hasColumn(column: TableColumn<*>): Boolean {
        return when (column) {
            TASK_ID -> true
            TASK_WORKFLOW_ID -> true
            TASK_SUBMIT_TIME -> true
            TASK_WAIT_TIME -> true
            TASK_RUNTIME -> true
            TASK_REQ_NCPUS -> true
            TASK_PARENTS -> true
            TASK_CHILDREN -> true
            TASK_GROUP_ID -> true
            TASK_USER_ID -> true
            else -> false
        }
    }

    override fun <T> get(column: TableColumn<T>): T {
        val record = checkNotNull(record) { "Reader in invalid state" }

        @Suppress("UNCHECKED_CAST")
        val res: Any = when (column) {
            TASK_ID -> (record["id"] as Long).toString()
            TASK_WORKFLOW_ID -> (record["workflow_id"] as Long).toString()
            TASK_SUBMIT_TIME -> Instant.ofEpochMilli(record["ts_submit"] as Long)
            TASK_WAIT_TIME -> Duration.ofMillis(record["wait_time"] as Long)
            TASK_RUNTIME -> Duration.ofMillis(record["runtime"] as Long)
            TASK_REQ_NCPUS -> (record["resource_amount_requested"] as Double).toInt()
            TASK_PARENTS -> (record["parents"] as ArrayList<GenericRecord>).map { it["item"].toString() }.toSet()
            TASK_CHILDREN -> (record["children"] as ArrayList<GenericRecord>).map { it["item"].toString() }.toSet()
            TASK_GROUP_ID -> record["group_id"]
            TASK_USER_ID -> record["user_id"]
            else -> throw IllegalArgumentException("Invalid column")
        }

        @Suppress("UNCHECKED_CAST")
        return res as T
    }

    override fun getBoolean(column: TableColumn<Boolean>): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(column: TableColumn<Int>): Int {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (column) {
            TASK_REQ_NCPUS -> (record["resource_amount_requested"] as Double).toInt()
            TASK_GROUP_ID -> record["group_id"] as Int
            TASK_USER_ID -> record["user_id"] as Int
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getLong(column: TableColumn<Long>): Long {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getDouble(column: TableColumn<Double>): Double {
        throw IllegalArgumentException("Invalid column")
    }

    override fun close() {
        reader.close()
    }
}
