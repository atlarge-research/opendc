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
import java.time.Duration
import java.time.Instant
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

    override fun resolve(column: TableColumn<*>): Int = columns[column] ?: -1

    override fun isNull(index: Int): Boolean {
        check(index in 0..columns.size) { "Invalid column" }
        return false
    }

    override fun get(index: Int): Any? {
        return when (index) {
            COL_JOB_ID -> jobId
            COL_WORKFLOW_ID -> workflowId
            COL_SUBMIT_TIME -> submitTime
            COL_RUNTIME -> runtime
            COL_REQ_NPROC -> getInt(index)
            COL_NPROC -> getInt(index)
            COL_DEPS -> dependencies
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getBoolean(index: Int): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(index: Int): Int {
        return when (index) {
            COL_REQ_NPROC -> reqNProcs
            COL_NPROC -> nProcs
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
        parser.close()
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

    private val columns = mapOf(
        TASK_ID to COL_JOB_ID,
        TASK_WORKFLOW_ID to COL_WORKFLOW_ID,
        TASK_SUBMIT_TIME to COL_SUBMIT_TIME,
        TASK_RUNTIME to COL_RUNTIME,
        TASK_ALLOC_NCPUS to COL_NPROC,
        TASK_REQ_NCPUS to COL_REQ_NPROC,
        TASK_PARENTS to COL_DEPS
    )

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
