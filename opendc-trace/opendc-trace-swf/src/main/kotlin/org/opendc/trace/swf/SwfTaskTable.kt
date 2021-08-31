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
import java.nio.file.Path
import kotlin.io.path.bufferedReader

/**
 * A [Table] containing the tasks in a SWF trace.
 */
internal class SwfTaskTable(private val path: Path) : Table {
    override val name: String = TABLE_TASKS

    override val isSynthetic: Boolean = false

    override fun isSupported(column: TableColumn<*>): Boolean {
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

    override fun newReader(): TableReader {
        val reader = path.bufferedReader()
        return SwfTaskTableReader(reader)
    }

    override fun newReader(partition: String): TableReader {
        throw IllegalArgumentException("Invalid partition $partition")
    }

    override fun toString(): String = "SwfTaskTable"
}
