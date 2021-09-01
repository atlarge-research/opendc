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

package org.opendc.experiments.capelin.trace.bp

import org.apache.avro.generic.GenericRecord
import org.opendc.trace.*
import org.opendc.trace.util.parquet.LocalParquetReader
import java.nio.file.Path

/**
 * The resource state [Table] in the Bitbrains Parquet format.
 */
internal class BPResourceStateTable(private val path: Path) : Table {
    override val name: String = TABLE_RESOURCE_STATES
    override val isSynthetic: Boolean = false

    override fun isSupported(column: TableColumn<*>): Boolean {
        return when (column) {
            RESOURCE_STATE_ID -> true
            RESOURCE_STATE_TIMESTAMP -> true
            RESOURCE_STATE_DURATION -> true
            RESOURCE_STATE_NCPUS -> true
            RESOURCE_STATE_CPU_USAGE -> true
            else -> false
        }
    }

    override fun newReader(): TableReader {
        val reader = LocalParquetReader<GenericRecord>(path.resolve("trace.parquet"))
        return BPResourceStateTableReader(reader)
    }

    override fun newReader(partition: String): TableReader {
        throw IllegalArgumentException("Unknown partition $partition")
    }
}
