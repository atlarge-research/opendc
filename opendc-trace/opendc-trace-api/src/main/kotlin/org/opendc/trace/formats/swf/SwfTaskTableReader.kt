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

package org.opendc.trace.swf

import org.opendc.trace.TableReader
import org.opendc.trace.conv.TASK_ALLOC_NCPUS
import org.opendc.trace.conv.TASK_GROUP_ID
import org.opendc.trace.conv.TASK_ID
import org.opendc.trace.conv.TASK_PARENTS
import org.opendc.trace.conv.TASK_REQ_NCPUS
import org.opendc.trace.conv.TASK_RUNTIME
import org.opendc.trace.conv.TASK_STATUS
import org.opendc.trace.conv.TASK_SUBMIT_TIME
import org.opendc.trace.conv.TASK_USER_ID
import org.opendc.trace.conv.TASK_WAIT_TIME
import java.io.BufferedReader
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * A [TableReader] implementation for the SWF format.
 */
internal class SwfTaskTableReader(private val reader: BufferedReader) : TableReader {
    /**
     * A flag to indicate the state of the reader
     */
    private var state = State.Pending

    /**
     * The current row.
     */
    private var fields = emptyList<String>()

    /**
     * A [Regex] object to match whitespace.
     */
    private val whitespace = "\\s+".toRegex()

    override fun nextRow(): Boolean {
        var line: String?
        var num = 0

        val state = state
        if (state == State.Closed) {
            return false
        } else if (state == State.Pending) {
            this.state = State.Active
        }

        while (true) {
            line = reader.readLine()

            if (line == null) {
                this.state = State.Closed
                return false
            }
            num++

            if (line.isBlank()) {
                // Ignore empty lines
                continue
            } else if (line.startsWith(";")) {
                // Ignore comments for now
                continue
            }

            break
        }

        fields = line!!.trim().split(whitespace)

        if (fields.size < 18) {
            throw IllegalArgumentException("Invalid format at line $line")
        }

        return true
    }

    override fun resolve(name: String): Int {
        return when (name) {
            TASK_ID -> colJobID
            TASK_SUBMIT_TIME -> colSubmitTime
            TASK_WAIT_TIME -> colWaitTime
            TASK_RUNTIME -> colRunTime
            TASK_ALLOC_NCPUS -> colAllocNcpus
            TASK_REQ_NCPUS -> colReqNcpus
            TASK_STATUS -> colStatus
            TASK_USER_ID -> colUserID
            TASK_GROUP_ID -> colGroupID
            TASK_PARENTS -> colParentJob
            else -> -1
        }
    }

    override fun isNull(index: Int): Boolean {
        require(index in colJobID..colParentThinkTime) { "Invalid column index" }
        return false
    }

    override fun getBoolean(index: Int): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(index: Int): Int {
        check(state == State.Active) { "No active row" }
        return when (index) {
            colReqNcpus, colAllocNcpus, colStatus, colGroupID, colUserID -> fields[index].toInt(10)
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
        check(state == State.Active) { "No active row" }
        return when (index) {
            colJobID -> fields[index]
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getUUID(index: Int): UUID? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInstant(index: Int): Instant? {
        check(state == State.Active) { "No active row" }
        return when (index) {
            colSubmitTime -> Instant.ofEpochSecond(fields[index].toLong(10))
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getDuration(index: Int): Duration? {
        check(state == State.Active) { "No active row" }
        return when (index) {
            colWaitTime, colRunTime -> Duration.ofSeconds(fields[index].toLong(10))
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
        check(state == State.Active) { "No active row" }
        @Suppress("UNCHECKED_CAST")
        return when (index) {
            colParentJob -> {
                require(elementType.isAssignableFrom(String::class.java))
                val parent = fields[index].toLong(10)
                if (parent < 0) emptySet() else setOf(parent)
            }
            else -> throw IllegalArgumentException("Invalid column")
        } as Set<T>?
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
        state = State.Closed
    }

    /**
     * Default column indices for the SWF format.
     */
    private val colJobID = 0
    private val colSubmitTime = 1
    private val colWaitTime = 2
    private val colRunTime = 3
    private val colAllocNcpus = 4
    private val colAvgCpuTime = 5
    private val colUsedMem = 6
    private val colReqNcpus = 7
    private val colReqTime = 8
    private val colReqMem = 9
    private val colStatus = 10
    private val colUserID = 11
    private val colGroupID = 12
    private val colExecNum = 13
    private val colQueueNum = 14
    private val colPartNum = 15
    private val colParentJob = 16
    private val colParentThinkTime = 17

    private enum class State {
        Pending,
        Active,
        Closed,
    }
}
