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

    private val colID = 0
    private val colWorkflowID = 1
    private val colSubmitTime = 2
    private val colWaitTime = 3
    private val colRuntime = 4
    private val colReqNcpus = 5
    private val colParents = 6
    private val colChildren = 7
    private val colGroupID = 8
    private val colUserID = 9

    private val typeParents = TableColumnType.Set(TableColumnType.String)
    private val typeChildren = TableColumnType.Set(TableColumnType.String)

    override fun resolve(name: String): Int {
        return when (name) {
            TASK_ID -> colID
            TASK_WORKFLOW_ID -> colWorkflowID
            TASK_SUBMIT_TIME -> colSubmitTime
            TASK_WAIT_TIME -> colWaitTime
            TASK_RUNTIME -> colRuntime
            TASK_REQ_NCPUS -> colReqNcpus
            TASK_PARENTS -> colParents
            TASK_CHILDREN -> colChildren
            TASK_GROUP_ID -> colGroupID
            TASK_USER_ID -> colUserID
            else -> -1
        }
    }

    override fun isNull(index: Int): Boolean {
        require(index in colID..colUserID) { "Invalid column index" }
        return false
    }

    override fun getBoolean(index: Int): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(index: Int): Int {
        val record = checkNotNull(record) { "Reader in invalid state" }

        return when (index) {
            colReqNcpus -> record.requestedCpus
            colGroupID -> record.groupId
            colUserID -> record.userId
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
            colID -> record.id
            colWorkflowID -> record.workflowId
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getUUID(index: Int): UUID? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInstant(index: Int): Instant {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (index) {
            colSubmitTime -> record.submitTime
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getDuration(index: Int): Duration {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (index) {
            colWaitTime -> record.waitTime
            colRuntime -> record.runtime
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun <T> getList(
        index: Int,
        elementType: Class<T>,
    ): List<T>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun <T> getSet(
        index: Int,
        elementType: Class<T>,
    ): Set<T>? {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (index) {
            colParents -> typeParents.convertTo(record.parents, elementType)
            colChildren -> typeChildren.convertTo(record.children, elementType)
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun <K, V> getMap(
        index: Int,
        keyType: Class<K>,
        valueType: Class<V>,
    ): Map<K, V>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun close() {
        reader.close()
    }
}
