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

package org.opendc.trace.bitbrains

import com.fasterxml.jackson.dataformat.csv.CsvFactory
import org.opendc.trace.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

/**
 * The resources [Table] in the Bitbrains format.
 */
internal class BitbrainsResourceTable(private val factory: CsvFactory, path: Path) : Table {
    /**
     * The VMs that belong to the table.
     */
    private val vms =
        Files.walk(path, 1)
            .filter { !Files.isDirectory(it) && it.extension == "csv" }
            .collect(Collectors.toMap({ it.nameWithoutExtension }, { it }))
            .toSortedMap()

    override val name: String = TABLE_RESOURCES

    override val isSynthetic: Boolean = true

    override val columns: List<TableColumn<*>> = listOf(RESOURCE_ID)

    override fun newReader(): TableReader {
        return BitbrainsResourceTableReader(factory, vms)
    }

    override fun newReader(partition: String): TableReader {
        throw IllegalArgumentException("Unknown partition $partition")
    }

    override fun toString(): String = "BitbrainsResourceTable"
}
