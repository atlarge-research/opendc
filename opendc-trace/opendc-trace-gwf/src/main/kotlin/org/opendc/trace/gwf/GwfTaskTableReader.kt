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
import org.opendc.trace.*
import java.util.regex.Pattern

/**
 * A [TableReader] implementation for the GWF format.
 */
internal class GwfTaskTableReader(private val parser: CsvParser) : TableReader {
    init {
        parser.schema = schema
    }

    override fun nextRow(): Boolean {
        // Reset the row state
        reset()

        if (!nextStart()) {
            return false
        }

        while (true) {
            val token = parser.nextValue()

            if (token == null || token == JsonToken.END_OBJECT) {
                break
            }

            when (parser.currentName) {
                "WorkflowID" -> workflowId = parser.longValue
                "JobID" -> jobId = parser.longValue
                "SubmitTime" -> submitTime = parser.longValue
                "RunTime" -> runtime = parser.longValue
                "NProcs" -> nProcs = parser.intValue
                "ReqNProcs" -> reqNProcs = parser.intValue
                "Dependencies" -> parseParents(parser.valueAsString)
            }
        }

        return true
    }

    override fun hasColumn(column: TableColumn<*>): Boolean {
        return when (column) {
            TASK_WORKFLOW_ID -> true
            TASK_ID -> true
            TASK_SUBMIT_TIME -> true
            TASK_RUNTIME -> true
            TASK_REQ_NCPUS -> true
            TASK_ALLOC_NCPUS -> true
            TASK_PARENTS -> true
            else -> false
        }
    }

    override fun <T> get(column: TableColumn<T>): T {
        val res: Any = when (column) {
            TASK_WORKFLOW_ID -> workflowId
            TASK_ID -> jobId
            TASK_SUBMIT_TIME -> submitTime
            TASK_RUNTIME -> runtime
            TASK_REQ_NCPUS -> nProcs
            TASK_ALLOC_NCPUS -> reqNProcs
            TASK_PARENTS -> dependencies
            else -> throw IllegalArgumentException("Invalid column")
        }

        @Suppress("UNCHECKED_CAST")
        return res as T
    }

    override fun getBoolean(column: TableColumn<Boolean>): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(column: TableColumn<Int>): Int {
        return when (column) {
            TASK_REQ_NCPUS -> nProcs
            TASK_ALLOC_NCPUS -> reqNProcs
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getLong(column: TableColumn<Long>): Long {
        return when (column) {
            TASK_WORKFLOW_ID -> workflowId
            TASK_ID -> jobId
            TASK_SUBMIT_TIME -> submitTime
            TASK_RUNTIME -> runtime
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getDouble(column: TableColumn<Double>): Double {
        throw IllegalArgumentException("Invalid column")
    }

    override fun close() {
        parser.close()
    }

    /**
     * The pattern used to parse the parents.
     */
    private val pattern = Pattern.compile("\\s+")

    /**
     * Parse the parents into a set of longs.
     */
    private fun parseParents(value: String): Set<Long> {
        val result = mutableSetOf<Long>()
        val deps = value.split(pattern)

        for (dep in deps) {
            if (dep.isBlank()) {
                continue
            }

            result.add(dep.toLong(10))
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
    private var workflowId = -1L
    private var jobId = -1L
    private var submitTime = -1L
    private var runtime = -1L
    private var nProcs = -1
    private var reqNProcs = -1
    private var dependencies = emptySet<Long>()

    /**
     * Reset the state.
     */
    private fun reset() {
        workflowId = -1
        jobId = -1
        submitTime = -1
        runtime = -1
        nProcs = -1
        reqNProcs = -1
        dependencies = emptySet()
    }

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
