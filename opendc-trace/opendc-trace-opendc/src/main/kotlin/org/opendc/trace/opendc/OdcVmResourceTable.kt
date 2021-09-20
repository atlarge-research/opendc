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

package org.opendc.trace.opendc

import org.apache.avro.generic.GenericRecord
import org.opendc.trace.*
import org.opendc.trace.util.parquet.LocalParquetReader
import java.nio.file.Path

/**
 * The resource [Table] for the OpenDC virtual machine trace format.
 */
internal class OdcVmResourceTable(private val path: Path) : Table {
    override val name: String = TABLE_RESOURCES
    override val isSynthetic: Boolean = false

    override val columns: List<TableColumn<*>> = listOf(
        RESOURCE_ID,
        RESOURCE_START_TIME,
        RESOURCE_STOP_TIME,
        RESOURCE_CPU_COUNT,
        RESOURCE_MEM_CAPACITY,
    )

    override val partitionKeys: List<TableColumn<*>> = emptyList()

    override fun newReader(): TableReader {
        val reader = LocalParquetReader<GenericRecord>(path.resolve("meta.parquet"))
        return OdcVmResourceTableReader(reader)
    }

    override fun newReader(partition: String): TableReader {
        throw IllegalArgumentException("Unknown partition $partition")
    }
}
