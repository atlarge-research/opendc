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
import java.io.BufferedReader

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

    override fun hasColumn(column: TableColumn<*>): Boolean {
        return when (column) {
            TASK_ID -> true
            TASK_SUBMIT_TIME -> true
            TASK_WAIT_TIME -> true
            TASK_RUNTIME -> true
            TASK_REQ_NCPUS -> true
            TASK_ALLOC_NCPUS -> true
            TASK_PARENTS -> true
            TASK_STATUS -> true
            TASK_GROUP_ID -> true
            TASK_USER_ID -> true
            else -> false
        }
    }

    override fun <T> get(column: TableColumn<T>): T {
        val res: Any = when (column) {
            TASK_ID -> getLong(TASK_ID)
            TASK_SUBMIT_TIME -> getLong(TASK_SUBMIT_TIME)
            TASK_WAIT_TIME -> getLong(TASK_WAIT_TIME)
            TASK_RUNTIME -> getLong(TASK_RUNTIME)
            TASK_REQ_NCPUS -> getInt(TASK_REQ_NCPUS)
            TASK_ALLOC_NCPUS -> getInt(TASK_ALLOC_NCPUS)
            TASK_PARENTS -> {
                val parent = fields[COL_PARENT_JOB].toLong(10)
                if (parent < 0) emptySet() else setOf(parent)
            }
            TASK_STATUS -> getInt(TASK_STATUS)
            TASK_GROUP_ID -> getInt(TASK_GROUP_ID)
            TASK_USER_ID -> getInt(TASK_USER_ID)
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
            TASK_REQ_NCPUS -> fields[COL_REQ_NCPUS].toInt(10)
            TASK_ALLOC_NCPUS -> fields[COL_ALLOC_NCPUS].toInt(10)
            TASK_STATUS -> fields[COL_STATUS].toInt(10)
            TASK_GROUP_ID -> fields[COL_GROUP_ID].toInt(10)
            TASK_USER_ID -> fields[COL_USER_ID].toInt(10)
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getLong(column: TableColumn<Long>): Long {
        return when (column) {
            TASK_ID -> fields[COL_JOB_ID].toLong(10)
            TASK_SUBMIT_TIME -> fields[COL_SUBMIT_TIME].toLong(10)
            TASK_WAIT_TIME -> fields[COL_WAIT_TIME].toLong(10)
            TASK_RUNTIME -> fields[COL_RUN_TIME].toLong(10)
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getDouble(column: TableColumn<Double>): Double {
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
}
