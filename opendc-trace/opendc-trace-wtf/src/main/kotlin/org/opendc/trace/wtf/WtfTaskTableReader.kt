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

import org.opendc.trace.*
import org.opendc.trace.conv.*
import org.opendc.trace.util.parquet.LocalParquetReader
import org.opendc.trace.wtf.parquet.Task

/**
 * A [TableReader] implementation for the WTF format.
 */
internal class WtfTaskTableReader(private val reader: LocalParquetReader<Task>) : TableReader {
    /**
     * The current record.
     */
    private var record: Task? = null

    override fun nextRow(): Boolean {
        try {
            val record = reader.read()
            this.record = record

            return record != null
        } catch (e: Throwable) {
            this.record = null
            throw e
        }
    }

    override fun resolve(column: TableColumn<*>): Int = columns[column] ?: -1

    override fun isNull(index: Int): Boolean {
        check(index in 0..columns.size) { "Invalid column index" }
        return get(index) == null
    }

    override fun get(index: Int): Any? {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (index) {
            COL_ID -> record.id
            COL_WORKFLOW_ID -> record.workflowId
            COL_SUBMIT_TIME -> record.submitTime
            COL_WAIT_TIME -> record.waitTime
            COL_RUNTIME -> record.runtime
            COL_REQ_NCPUS, COL_GROUP_ID, COL_USER_ID -> getInt(index)
            COL_PARENTS -> record.parents
            COL_CHILDREN -> record.children
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getBoolean(index: Int): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(index: Int): Int {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (index) {
            COL_REQ_NCPUS -> record.requestedCpus
            COL_GROUP_ID -> record.groupId
            COL_USER_ID -> record.userId
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
