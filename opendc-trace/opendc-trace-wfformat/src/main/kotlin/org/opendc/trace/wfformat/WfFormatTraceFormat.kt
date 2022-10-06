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

import com.fasterxml.jackson.core.JsonFactory
import org.opendc.trace.TableColumn
import org.opendc.trace.TableColumnType
import org.opendc.trace.TableReader
import org.opendc.trace.TableWriter
import org.opendc.trace.conv.TABLE_TASKS
import org.opendc.trace.conv.TASK_CHILDREN
import org.opendc.trace.conv.TASK_ID
import org.opendc.trace.conv.TASK_PARENTS
import org.opendc.trace.conv.TASK_REQ_NCPUS
import org.opendc.trace.conv.TASK_RUNTIME
import org.opendc.trace.conv.TASK_WORKFLOW_ID
import org.opendc.trace.spi.TableDetails
import org.opendc.trace.spi.TraceFormat
import java.nio.file.Path

/**
 * A [TraceFormat] implementation for the WfCommons workload trace format.
 */
public class WfFormatTraceFormat : TraceFormat {
    /**
     * The [JsonFactory] that is used to created JSON parsers.
     */
    private val factory = JsonFactory()

    override val name: String = "wfformat"

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
                    TableColumn(TASK_RUNTIME, TableColumnType.Duration),
                    TableColumn(TASK_REQ_NCPUS, TableColumnType.Int),
                    TableColumn(TASK_PARENTS, TableColumnType.Set(TableColumnType.String)),
                    TableColumn(TASK_CHILDREN, TableColumnType.Set(TableColumnType.String))
                )
            )
            else -> throw IllegalArgumentException("Table $table not supported")
        }
    }

    override fun newReader(path: Path, table: String, projection: List<String>?): TableReader {
        return when (table) {
            TABLE_TASKS -> WfFormatTaskTableReader(factory.createParser(path.toFile()))
            else -> throw IllegalArgumentException("Table $table not supported")
        }
    }

    override fun newWriter(path: Path, table: String): TableWriter {
        throw UnsupportedOperationException("Writing not supported for this format")
    }
}
