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

import org.opendc.trace.TableColumn
import org.opendc.trace.TableColumnType
import org.opendc.trace.TableReader
import org.opendc.trace.TableWriter
import org.opendc.trace.conv.TABLE_TASKS
import org.opendc.trace.conv.TASK_CHILDREN
import org.opendc.trace.conv.TASK_GROUP_ID
import org.opendc.trace.conv.TASK_ID
import org.opendc.trace.conv.TASK_PARENTS
import org.opendc.trace.conv.TASK_REQ_NCPUS
import org.opendc.trace.conv.TASK_RUNTIME
import org.opendc.trace.conv.TASK_SUBMIT_TIME
import org.opendc.trace.conv.TASK_USER_ID
import org.opendc.trace.conv.TASK_WAIT_TIME
import org.opendc.trace.conv.TASK_WORKFLOW_ID
import org.opendc.trace.spi.TableDetails
import org.opendc.trace.spi.TraceFormat
import org.opendc.trace.util.parquet.LocalParquetReader
import org.opendc.trace.wtf.parquet.TaskReadSupport
import java.nio.file.Path

/**
 * A [TraceFormat] implementation for the Workflow Trace Format (WTF).
 */
public class WtfTraceFormat : TraceFormat {
    override val name: String = "wtf"

    override fun create(path: Path) {
        throw UnsupportedOperationException("Writing not supported for this format")
    }

    override fun getTables(path: Path): List<String> = listOf(TABLE_TASKS)

    override fun getDetails(path: Path, table: String): TableDetails {
        return when (table) {
            TABLE_TASKS -> TableDetails(
                listOf(
                    TableColumn(TASK_ID, TableColumnType.String),
                    TableColumn(TASK_WORKFLOW_ID, TableColumnType.String),
                    TableColumn(TASK_SUBMIT_TIME, TableColumnType.Instant),
                    TableColumn(TASK_WAIT_TIME, TableColumnType.Duration),
                    TableColumn(TASK_RUNTIME, TableColumnType.Duration),
                    TableColumn(TASK_REQ_NCPUS, TableColumnType.Int),
                    TableColumn(TASK_PARENTS, TableColumnType.Set(TableColumnType.String)),
                    TableColumn(TASK_CHILDREN, TableColumnType.Set(TableColumnType.String)),
                    TableColumn(TASK_GROUP_ID, TableColumnType.Int),
                    TableColumn(TASK_USER_ID, TableColumnType.Int)
                )
            )
            else -> throw IllegalArgumentException("Table $table not supported")
        }
    }

    override fun newReader(path: Path, table: String, projection: List<String>?): TableReader {
        return when (table) {
            TABLE_TASKS -> {
                val reader = LocalParquetReader(path.resolve("tasks/schema-1.0"), TaskReadSupport(projection), strictTyping = false)
                WtfTaskTableReader(reader)
            }
            else -> throw IllegalArgumentException("Table $table not supported")
        }
    }

    override fun newWriter(path: Path, table: String): TableWriter {
        throw UnsupportedOperationException("Writing not supported for this format")
    }
}
