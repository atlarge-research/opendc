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

import com.fasterxml.jackson.dataformat.csv.CsvFactory
import org.opendc.trace.*
import java.net.URL

/**
 * A [Table] containing the tasks in a GWF trace.
 */
internal class GwfTaskTable(private val factory: CsvFactory, private val url: URL) : Table {
    override val name: String = TABLE_TASKS

    override val isSynthetic: Boolean = false

    override val columns: List<TableColumn<*>> = listOf(
        TASK_WORKFLOW_ID,
        TASK_ID,
        TASK_SUBMIT_TIME,
        TASK_RUNTIME,
        TASK_REQ_NCPUS,
        TASK_ALLOC_NCPUS,
        TASK_PARENTS
    )

    override fun newReader(): TableReader {
        return GwfTaskTableReader(factory.createParser(url))
    }

    override fun newReader(partition: String): TableReader {
        throw IllegalArgumentException("Invalid partition $partition")
    }

    override fun toString(): String = "GwfTaskTable"
}
