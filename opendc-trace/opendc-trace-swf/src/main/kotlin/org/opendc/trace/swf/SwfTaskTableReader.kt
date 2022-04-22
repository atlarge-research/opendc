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

import org.opendc.trace.*
import org.opendc.trace.conv.*
import java.io.BufferedReader
import java.time.Duration
import java.time.Instant

/**
 * A [TableReader] implementation for the SWF format.
 */
internal class SwfTaskTableReader(private val reader: BufferedReader) : TableReader {
    /**
     * The current row.
     */
    private var fields = emptyList<String>()

    /**
     * A [Regex] object to match whitespace.
     */
    private val whitespace = "\\s+".toRegex()

    override fun nextRow(): Boolean {
        var line: String
        var num = 0

        while (true) {
            line = reader.readLine() ?: return false
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

        fields = line.trim().split(whitespace)

        if (fields.size < 18) {
            throw IllegalArgumentException("Invalid format at line $line")
        }

        return true
    }

    override fun resolve(column: TableColumn<*>): Int = columns[column] ?: -1

    override fun isNull(index: Int): Boolean {
        require(index in columns.values) { "Invalid column index" }
        return false
    }

    override fun get(index: Int): Any? {
        return when (index) {
            COL_JOB_ID -> fields[index]
            COL_SUBMIT_TIME -> Instant.ofEpochSecond(fields[index].toLong(10))
            COL_WAIT_TIME, COL_RUN_TIME -> Duration.ofSeconds(fields[index].toLong(10))
            COL_REQ_NCPUS, COL_ALLOC_NCPUS, COL_STATUS, COL_GROUP_ID, COL_USER_ID -> getInt(index)
            COL_PARENT_JOB -> {
                val parent = fields[index].toLong(10)
                if (parent < 0) emptySet() else setOf(parent)
            }
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getBoolean(index: Int): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(index: Int): Int {
        return when (index) {
            COL_REQ_NCPUS, COL_ALLOC_NCPUS, COL_STATUS, COL_GROUP_ID, COL_USER_ID -> fields[index].toInt(10)
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

    /**
     * Default column indices for the SWF format.
     */
    private val COL_JOB_ID = 0
    private val COL_SUBMIT_TIME = 1
    private val COL_WAIT_TIME = 2
    private val COL_RUN_TIME = 3
    private val COL_ALLOC_NCPUS = 4
    private val COL_AVG_CPU_TIME = 5
    private val COL_USED_MEM = 6
    private val COL_REQ_NCPUS = 7
    private val COL_REQ_TIME = 8
    private val COL_REQ_MEM = 9
    private val COL_STATUS = 10
    private val COL_USER_ID = 11
    private val COL_GROUP_ID = 12
    private val COL_EXEC_NUM = 13
    private val COL_QUEUE_NUM = 14
    private val COL_PART_NUM = 15
    private val COL_PARENT_JOB = 16
    private val COL_PARENT_THINK_TIME = 17

    private val columns = mapOf(
        TASK_ID to COL_JOB_ID,
        TASK_SUBMIT_TIME to COL_SUBMIT_TIME,
        TASK_WAIT_TIME to COL_WAIT_TIME,
        TASK_RUNTIME to COL_RUN_TIME,
        TASK_ALLOC_NCPUS to COL_ALLOC_NCPUS,
        TASK_REQ_NCPUS to COL_REQ_NCPUS,
        TASK_STATUS to COL_STATUS,
        TASK_USER_ID to COL_USER_ID,
        TASK_GROUP_ID to COL_GROUP_ID,
        TASK_PARENTS to COL_PARENT_JOB
    )
}
