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

import org.opendc.trace.*
import org.opendc.trace.util.CompositeTableReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.bufferedReader
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

/**
 * The resource state [Table] in the extended Bitbrains format.
 */
internal class BitbrainsExResourceStateTable(path: Path) : Table {
    /**
     * The partitions that belong to the table.
     */
    private val partitions = Files.walk(path, 1)
        .filter { !Files.isDirectory(it) && it.extension == "txt" }
        .collect(Collectors.toMap({ it.nameWithoutExtension }, { it }))
        .toSortedMap()

    override val name: String = TABLE_RESOURCE_STATES

    override val isSynthetic: Boolean = false

    override val columns: List<TableColumn<*>> = listOf(
        RESOURCE_ID,
        RESOURCE_CLUSTER_ID,
        RESOURCE_STATE_TIMESTAMP,
        RESOURCE_CPU_COUNT,
        RESOURCE_CPU_CAPACITY,
        RESOURCE_STATE_CPU_USAGE,
        RESOURCE_STATE_CPU_USAGE_PCT,
        RESOURCE_STATE_CPU_DEMAND,
        RESOURCE_STATE_CPU_READY_PCT,
        RESOURCE_MEM_CAPACITY,
        RESOURCE_STATE_DISK_READ,
        RESOURCE_STATE_DISK_WRITE,
    )

    override fun newReader(): TableReader {
        val it = partitions.iterator()

        return object : CompositeTableReader() {
            override fun nextReader(): TableReader? {
                return if (it.hasNext()) {
                    val (_, path) = it.next()
                    val reader = path.bufferedReader()
                    return BitbrainsExResourceStateTableReader(reader)
                } else {
                    null
                }
            }

            override fun toString(): String = "SvCompositeTableReader"
        }
    }

    override fun newReader(partition: String): TableReader {
        val path = requireNotNull(partitions[partition]) { "Invalid partition $partition" }
        val reader = path.bufferedReader()
        return BitbrainsExResourceStateTableReader(reader)
    }

    override fun toString(): String = "BitbrainsExResourceStateTable"
}
