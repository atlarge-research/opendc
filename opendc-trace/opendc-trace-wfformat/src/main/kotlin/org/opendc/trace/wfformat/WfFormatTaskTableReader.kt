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
import org.opendc.trace.*
import java.time.Duration
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

    override fun resolve(column: TableColumn<*>): Int = columns[column] ?: -1

    override fun isNull(index: Int): Boolean {
        check(index in 0..columns.size) { "Invalid column value" }
        return false
    }

    override fun get(index: Int): Any? {
        return when (index) {
            COL_ID -> id
            COL_WORKFLOW_ID -> workflowId
            COL_RUNTIME -> runtime
            COL_PARENTS -> parents
            COL_CHILDREN -> children
            COL_NPROC -> getInt(index)
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getBoolean(index: Int): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(index: Int): Int {
        return when (index) {
            COL_NPROC -> cores
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
     * Parse the trace and seek until the workflow description.
     */
    private fun seekWorkflow(): Boolean {
        while (parser.nextValue() != JsonToken.END_OBJECT) {
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

    private val columns = mapOf(
        TASK_ID to COL_ID,
        TASK_WORKFLOW_ID to COL_WORKFLOW_ID,
        TASK_RUNTIME to COL_RUNTIME,
        TASK_REQ_NCPUS to COL_NPROC,
        TASK_PARENTS to COL_PARENTS,
        TASK_CHILDREN to COL_CHILDREN,
    )
}
