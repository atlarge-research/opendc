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

import org.opendc.trace.TableColumnType
import org.opendc.trace.TableReader
import org.opendc.trace.conv.TASK_CHILDREN
import org.opendc.trace.conv.TASK_GROUP_ID
import org.opendc.trace.conv.TASK_ID
import org.opendc.trace.conv.TASK_PARENTS
import org.opendc.trace.conv.TASK_REQ_NCPUS
import org.opendc.trace.conv.TASK_RUNTIME
import org.opendc.trace.conv.TASK_SUBMIT_TIME
import org.opendc.trace.conv.TASK_USER_ID
import org.opendc.trace.conv.TASK_WAIT_TIME
import org.opendc.trace.conv.TASK_WORKFLOW_ID
import org.opendc.trace.util.convertTo
import org.opendc.trace.util.parquet.LocalParquetReader
import org.opendc.trace.wtf.parquet.Task
import java.time.Duration
import java.time.Instant
import java.util.UUID

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

    private val TYPE_PARENTS = TableColumnType.Set(TableColumnType.String)
    private val TYPE_CHILDREN = TableColumnType.Set(TableColumnType.String)

    override fun resolve(name: String): Int {
        return when (name) {
            TASK_ID -> COL_ID
            TASK_WORKFLOW_ID -> COL_WORKFLOW_ID
            TASK_SUBMIT_TIME -> COL_SUBMIT_TIME
            TASK_WAIT_TIME -> COL_WAIT_TIME
            TASK_RUNTIME -> COL_RUNTIME
            TASK_REQ_NCPUS -> COL_REQ_NCPUS
            TASK_PARENTS -> COL_PARENTS
            TASK_CHILDREN -> COL_CHILDREN
            TASK_GROUP_ID -> COL_GROUP_ID
            TASK_USER_ID -> COL_USER_ID
            else -> -1
        }
    }

    override fun isNull(index: Int): Boolean {
        require(index in COL_ID..COL_USER_ID) { "Invalid column index" }
        return false
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

    override fun getFloat(index: Int): Float {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getDouble(index: Int): Double {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getString(index: Int): String {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (index) {
            COL_ID -> record.id
            COL_WORKFLOW_ID -> record.workflowId
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getUUID(index: Int): UUID? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInstant(index: Int): Instant {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (index) {
            COL_SUBMIT_TIME -> record.submitTime
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getDuration(index: Int): Duration {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (index) {
            COL_WAIT_TIME -> record.waitTime
            COL_RUNTIME -> record.runtime
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun <T> getList(index: Int, elementType: Class<T>): List<T>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun <T> getSet(index: Int, elementType: Class<T>): Set<T>? {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (index) {
            COL_PARENTS -> TYPE_PARENTS.convertTo(record.parents, elementType)
            COL_CHILDREN -> TYPE_CHILDREN.convertTo(record.children, elementType)
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun <K, V> getMap(index: Int, keyType: Class<K>, valueType: Class<V>): Map<K, V>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun close() {
        reader.close()
    }
}
