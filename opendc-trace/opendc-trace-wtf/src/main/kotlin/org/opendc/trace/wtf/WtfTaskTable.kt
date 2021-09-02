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

import org.apache.avro.generic.GenericRecord
import org.opendc.trace.*
import org.opendc.trace.util.parquet.LocalParquetReader
import java.nio.file.Path

/**
 * A [Table] containing the tasks in a GWF trace.
 */
internal class WtfTaskTable(private val path: Path) : Table {
    override val name: String = TABLE_TASKS

    override val isSynthetic: Boolean = false

    override fun isSupported(column: TableColumn<*>): Boolean {
        return when (column) {
            TASK_ID -> true
            TASK_WORKFLOW_ID -> true
            TASK_SUBMIT_TIME -> true
            TASK_WAIT_TIME -> true
            TASK_RUNTIME -> true
            TASK_REQ_NCPUS -> true
            TASK_PARENTS -> true
            TASK_CHILDREN -> true
            TASK_GROUP_ID -> true
            TASK_USER_ID -> true
            else -> false
        }
    }

    override fun newReader(): TableReader {
        val reader = LocalParquetReader<GenericRecord>(path.resolve("tasks/schema-1.0"))
        return WtfTaskTableReader(reader)
    }

    override fun newReader(partition: String): TableReader {
        throw IllegalArgumentException("Invalid partition $partition")
    }

    override fun toString(): String = "WtfTaskTable"
}
