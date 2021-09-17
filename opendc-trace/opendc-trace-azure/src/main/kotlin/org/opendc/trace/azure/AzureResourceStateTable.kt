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

package org.opendc.trace.azure

import com.fasterxml.jackson.dataformat.csv.CsvFactory
import org.opendc.trace.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

/**
 * The resource state [Table] for the Azure v1 VM traces.
 */
internal class AzureResourceStateTable(private val factory: CsvFactory, path: Path) : Table {
    /**
     * The partitions that belong to the table.
     */
    private val partitions = Files.walk(path.resolve("vm_cpu_readings"), 1)
        .filter { !Files.isDirectory(it) && it.extension == "csv" }
        .collect(Collectors.toMap({ it.nameWithoutExtension }, { it }))
        .toSortedMap()

    override val name: String = TABLE_RESOURCE_STATES

    override val isSynthetic: Boolean = false

    override val columns: List<TableColumn<*>> = listOf(
        RESOURCE_STATE_ID,
        RESOURCE_STATE_TIMESTAMP,
        RESOURCE_STATE_CPU_USAGE_PCT
    )

    override fun newReader(): TableReader {
        val it = partitions.iterator()

        return object : TableReader {
            var delegate: TableReader? = nextDelegate()

            override fun nextRow(): Boolean {
                var delegate = delegate

                while (delegate != null) {
                    if (delegate.nextRow()) {
                        break
                    }

                    delegate.close()
                    delegate = nextDelegate()
                }

                this.delegate = delegate
                return delegate != null
            }

            override fun hasColumn(column: TableColumn<*>): Boolean = delegate?.hasColumn(column) ?: false

            override fun <T> get(column: TableColumn<T>): T {
                val delegate = checkNotNull(delegate) { "Invalid reader state" }
                return delegate.get(column)
            }

            override fun getBoolean(column: TableColumn<Boolean>): Boolean {
                val delegate = checkNotNull(delegate) { "Invalid reader state" }
                return delegate.getBoolean(column)
            }

            override fun getInt(column: TableColumn<Int>): Int {
                val delegate = checkNotNull(delegate) { "Invalid reader state" }
                return delegate.getInt(column)
            }

            override fun getLong(column: TableColumn<Long>): Long {
                val delegate = checkNotNull(delegate) { "Invalid reader state" }
                return delegate.getLong(column)
            }

            override fun getDouble(column: TableColumn<Double>): Double {
                val delegate = checkNotNull(delegate) { "Invalid reader state" }
                return delegate.getDouble(column)
            }

            override fun close() {
                delegate?.close()
            }

            private fun nextDelegate(): TableReader? {
                return if (it.hasNext()) {
                    val (_, path) = it.next()
                    return AzureResourceStateTableReader(factory.createParser(path.toFile()))
                } else {
                    null
                }
            }

            override fun toString(): String = "AzureCompositeTableReader"
        }
    }

    override fun newReader(partition: String): TableReader {
        val path = requireNotNull(partitions[partition]) { "Invalid partition $partition" }
        return AzureResourceStateTableReader(factory.createParser(path.toFile()))
    }

    override fun toString(): String = "AzureResourceStateTable"
}
