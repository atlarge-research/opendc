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

package org.opendc.trace.gwf

import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.dataformat.csv.CsvParser
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import org.opendc.trace.TableColumnType
import org.opendc.trace.TableReader
import org.opendc.trace.conv.TASK_ALLOC_NCPUS
import org.opendc.trace.conv.TASK_ID
import org.opendc.trace.conv.TASK_PARENTS
import org.opendc.trace.conv.TASK_REQ_NCPUS
import org.opendc.trace.conv.TASK_RUNTIME
import org.opendc.trace.conv.TASK_SUBMIT_TIME
import org.opendc.trace.conv.TASK_WORKFLOW_ID
import org.opendc.trace.util.convertTo
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.regex.Pattern

/**
 * A [TableReader] implementation for the GWF format.
 */
internal class GwfTaskTableReader(private val parser: CsvParser) : TableReader {
    /**
     * A flag to indicate whether a single row has been read already.
     */
    private var isStarted = false

    init {
        parser.schema = schema
    }

    override fun nextRow(): Boolean {
        if (!isStarted) {
            isStarted = true
        }

        // Reset the row state
        reset()

        if (parser.isClosed || !nextStart()) {
            return false
        }

        while (true) {
            val token = parser.nextValue()

            if (token == null || token == JsonToken.END_OBJECT) {
                break
            }

            when (parser.currentName) {
                "WorkflowID" -> workflowId = parser.text
                "JobID" -> jobId = parser.text
                "SubmitTime" -> submitTime = Instant.ofEpochSecond(parser.longValue)
                "RunTime" -> runtime = Duration.ofSeconds(parser.longValue)
                "NProcs" -> nProcs = parser.intValue
                "ReqNProcs" -> reqNProcs = parser.intValue
                "Dependencies" -> dependencies = parseParents(parser.valueAsString)
            }
        }

        return true
    }

    override fun resolve(name: String): Int {
        return when (name) {
            TASK_ID -> COL_JOB_ID
            TASK_WORKFLOW_ID -> COL_WORKFLOW_ID
            TASK_SUBMIT_TIME -> COL_SUBMIT_TIME
            TASK_RUNTIME -> COL_RUNTIME
            TASK_ALLOC_NCPUS -> COL_NPROC
            TASK_REQ_NCPUS -> COL_REQ_NPROC
            TASK_PARENTS -> COL_DEPS
            else -> -1
        }
    }

    override fun isNull(index: Int): Boolean {
        require(index in 0..COL_DEPS) { "Invalid column" }
        return false
    }

    override fun getBoolean(index: Int): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(index: Int): Int {
        checkActive()
        return when (index) {
            COL_REQ_NPROC -> reqNProcs
            COL_NPROC -> nProcs
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

    override fun getString(index: Int): String? {
        checkActive()
        return when (index) {
            COL_JOB_ID -> jobId
            COL_WORKFLOW_ID -> workflowId
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getUUID(index: Int): UUID? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInstant(index: Int): Instant? {
        checkActive()
        return when (index) {
            COL_SUBMIT_TIME -> submitTime
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getDuration(index: Int): Duration? {
        checkActive()
        return when (index) {
            COL_RUNTIME -> runtime
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun <T> getList(index: Int, elementType: Class<T>): List<T>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun <K, V> getMap(index: Int, keyType: Class<K>, valueType: Class<V>): Map<K, V>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun <T> getSet(index: Int, elementType: Class<T>): Set<T>? {
        checkActive()
        return when (index) {
            COL_DEPS -> TYPE_DEPS.convertTo(dependencies, elementType)
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun close() {
        parser.close()
    }

    /**
     * Helper method to check if the reader is active.
     */
    private fun checkActive() {
        check(isStarted && !parser.isClosed) { "No active row. Did you call nextRow()?" }
    }

    /**
     * The pattern used to parse the parents.
     */
    private val pattern = Pattern.compile("\\s+")

    /**
     * Parse the parents into a set of longs.
     */
    private fun parseParents(value: String): Set<String> {
        val result = mutableSetOf<String>()
        val deps = value.split(pattern)

        for (dep in deps) {
            if (dep.isBlank()) {
                continue
            }

            result.add(dep)
        }

        return result
    }

    /**
     * Advance the parser until the next object start.
     */
    private fun nextStart(): Boolean {
        var token = parser.nextValue()

        while (token != null && token != JsonToken.START_OBJECT) {
            token = parser.nextValue()
        }

        return token != null
    }

    /**
     * Reader state fields.
     */
    private var workflowId: String? = null
    private var jobId: String? = null
    private var submitTime: Instant? = null
    private var runtime: Duration? = null
    private var nProcs = -1
    private var reqNProcs = -1
    private var dependencies = emptySet<String>()

    /**
     * Reset the state.
     */
    private fun reset() {
        workflowId = null
        jobId = null
        submitTime = null
        runtime = null
        nProcs = -1
        reqNProcs = -1
        dependencies = emptySet()
    }

    private val COL_WORKFLOW_ID = 0
    private val COL_JOB_ID = 1
    private val COL_SUBMIT_TIME = 2
    private val COL_RUNTIME = 3
    private val COL_NPROC = 4
    private val COL_REQ_NPROC = 5
    private val COL_DEPS = 6

    private val TYPE_DEPS = TableColumnType.Set(TableColumnType.String)

    companion object {
        /**
         * The [CsvSchema] that is used to parse the trace.
         */
        private val schema = CsvSchema.builder()
            .addColumn("WorkflowID", CsvSchema.ColumnType.NUMBER)
            .addColumn("JobID", CsvSchema.ColumnType.NUMBER)
            .addColumn("SubmitTime", CsvSchema.ColumnType.NUMBER)
            .addColumn("RunTime", CsvSchema.ColumnType.NUMBER)
            .addColumn("NProcs", CsvSchema.ColumnType.NUMBER)
            .addColumn("ReqNProcs", CsvSchema.ColumnType.NUMBER)
            .addColumn("Dependencies", CsvSchema.ColumnType.STRING)
            .setAllowComments(true)
            .setUseHeader(true)
            .setColumnSeparator(',')
            .build()
    }
}
