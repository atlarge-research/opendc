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

package org.opendc.trace.wfformat

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import org.opendc.trace.TableColumnType
import org.opendc.trace.TableReader
import org.opendc.trace.conv.TASK_CHILDREN
import org.opendc.trace.conv.TASK_ID
import org.opendc.trace.conv.TASK_PARENTS
import org.opendc.trace.conv.TASK_REQ_NCPUS
import org.opendc.trace.conv.TASK_RUNTIME
import org.opendc.trace.conv.TASK_WORKFLOW_ID
import org.opendc.trace.util.convertTo
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.math.roundToInt

/**
 * A [TableReader] implementation for the WfCommons workload trace format.
 */
internal class WfFormatTaskTableReader(private val parser: JsonParser) : TableReader {
    /**
     * The current nesting of the parser.
     */
    private var level: ParserLevel = ParserLevel.TOP

    override fun nextRow(): Boolean {
        reset()

        var hasJob = false

        while (!hasJob) {
            when (level) {
                ParserLevel.TOP -> {
                    val token = parser.nextToken()

                    // Check whether the document is not empty and starts with an object
                    if (token == null) {
                        parser.close()
                        break
                    } else if (token != JsonToken.START_OBJECT) {
                        throw JsonParseException(parser, "Expected object", parser.currentLocation)
                    } else {
                        level = ParserLevel.TRACE
                    }
                }
                ParserLevel.TRACE -> {
                    // Seek for the workflow object in the file
                    if (!seekWorkflow()) {
                        parser.close()
                        break
                    } else if (!parser.isExpectedStartObjectToken) {
                        throw JsonParseException(parser, "Expected object", parser.currentLocation)
                    } else {
                        level = ParserLevel.WORKFLOW
                    }
                }
                ParserLevel.WORKFLOW -> {
                    // Seek for the jobs object in the file
                    level = if (!seekJobs()) {
                        ParserLevel.TRACE
                    } else if (!parser.isExpectedStartArrayToken) {
                        throw JsonParseException(parser, "Expected array", parser.currentLocation)
                    } else {
                        ParserLevel.JOB
                    }
                }
                ParserLevel.JOB -> {
                    when (parser.nextToken()) {
                        JsonToken.END_ARRAY -> level = ParserLevel.WORKFLOW
                        JsonToken.START_OBJECT -> {
                            parseJob()
                            hasJob = true
                            break
                        }
                        else -> throw JsonParseException(parser, "Unexpected token", parser.currentLocation)
                    }
                }
            }
        }

        return hasJob
    }

    override fun resolve(name: String): Int {
        return when (name) {
            TASK_ID -> COL_ID
            TASK_WORKFLOW_ID -> COL_WORKFLOW_ID
            TASK_RUNTIME -> COL_RUNTIME
            TASK_REQ_NCPUS -> COL_NPROC
            TASK_PARENTS -> COL_PARENTS
            TASK_CHILDREN -> COL_CHILDREN
            else -> -1
        }
    }

    override fun isNull(index: Int): Boolean {
        require(index in 0..COL_CHILDREN) { "Invalid column value" }
        return false
    }

    override fun getBoolean(index: Int): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(index: Int): Int {
        checkActive()
        return when (index) {
            COL_NPROC -> cores
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
            COL_ID -> id
            COL_WORKFLOW_ID -> workflowId
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getUUID(index: Int): UUID? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInstant(index: Int): Instant? {
        throw IllegalArgumentException("Invalid column")
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

    override fun <T> getSet(index: Int, elementType: Class<T>): Set<T>? {
        checkActive()
        return when (index) {
            COL_PARENTS -> TYPE_PARENTS.convertTo(parents, elementType)
            COL_CHILDREN -> TYPE_CHILDREN.convertTo(children, elementType)
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun <K, V> getMap(index: Int, keyType: Class<K>, valueType: Class<V>): Map<K, V>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun close() {
        parser.close()
    }

    /**
     * Helper method to check if the reader is active.
     */
    private fun checkActive() {
        check(level != ParserLevel.TOP && !parser.isClosed) { "No active row. Did you call nextRow()?" }
    }

    /**
     * Parse the trace and seek until the workflow description.
     */
    private fun seekWorkflow(): Boolean {
        while (parser.nextValue() != JsonToken.END_OBJECT && !parser.isClosed) {
            when (parser.currentName) {
                "name" -> workflowId = parser.text
                "workflow" -> return true
                else -> parser.skipChildren()
            }
        }

        return false
    }

    /**
     * Parse the workflow description in the file and seek until the first job.
     */
    private fun seekJobs(): Boolean {
        while (parser.nextValue() != JsonToken.END_OBJECT) {
            when (parser.currentName) {
                "jobs" -> return true
                else -> parser.skipChildren()
            }
        }

        return false
    }

    /**
     * Parse a single job in the file.
     */
    private fun parseJob() {
        while (parser.nextValue() != JsonToken.END_OBJECT) {
            when (parser.currentName) {
                "name" -> id = parser.text
                "parents" -> parents = parseIds()
                "children" -> children = parseIds()
                "runtime" -> runtime = Duration.ofSeconds(parser.numberValue.toLong())
                "cores" -> cores = parser.floatValue.roundToInt()
                else -> parser.skipChildren()
            }
        }
    }

    /**
     * Parse the parents/children of a job.
     */
    private fun parseIds(): Set<String> {
        if (!parser.isExpectedStartArrayToken) {
            throw JsonParseException(parser, "Expected array", parser.currentLocation)
        }

        val ids = mutableSetOf<String>()

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.currentToken != JsonToken.VALUE_STRING) {
                throw JsonParseException(parser, "Expected token", parser.currentLocation)
            }

            ids.add(parser.valueAsString)
        }

        return ids
    }

    private enum class ParserLevel {
        TOP, TRACE, WORKFLOW, JOB
    }

    /**
     * State fields for the parser.
     */
    private var id: String? = null
    private var workflowId: String? = null
    private var runtime: Duration? = null
    private var parents: Set<String>? = null
    private var children: Set<String>? = null
    private var cores = -1

    private fun reset() {
        id = null
        runtime = null
        parents = null
        children = null
        cores = -1
    }

    private val COL_ID = 0
    private val COL_WORKFLOW_ID = 1
    private val COL_RUNTIME = 3
    private val COL_NPROC = 4
    private val COL_PARENTS = 5
    private val COL_CHILDREN = 6

    private val TYPE_PARENTS = TableColumnType.Set(TableColumnType.String)
    private val TYPE_CHILDREN = TableColumnType.Set(TableColumnType.String)
}
