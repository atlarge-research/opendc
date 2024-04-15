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
import com.fasterxml.jackson.dataformat.csv.CsvParser
import org.opendc.trace.TableColumn
import org.opendc.trace.TableColumnType
import org.opendc.trace.TableReader
import org.opendc.trace.TableWriter
import org.opendc.trace.conv.TABLE_RESOURCES
import org.opendc.trace.conv.TABLE_RESOURCE_STATES
import org.opendc.trace.conv.resourceCpuCapacity
import org.opendc.trace.conv.resourceCpuCount
import org.opendc.trace.conv.resourceID
import org.opendc.trace.conv.resourceMemCapacity
import org.opendc.trace.conv.resourceStateCpuUsage
import org.opendc.trace.conv.resourceStateCpuUsagePct
import org.opendc.trace.conv.resourceStateDiskRead
import org.opendc.trace.conv.resourceStateDiskWrite
import org.opendc.trace.conv.resourceStateMemUsage
import org.opendc.trace.conv.resourceStateNetRx
import org.opendc.trace.conv.resourceStateNetTx
import org.opendc.trace.conv.resourceStateTimestamp
import org.opendc.trace.spi.TableDetails
import org.opendc.trace.spi.TraceFormat
import org.opendc.trace.util.CompositeTableReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

/**
 * A format implementation for the GWF trace format.
 */
public class BitbrainsTraceFormat : TraceFormat {
    /**
     * The name of this trace format.
     */
    override val name: String = "bitbrains"

    /**
     * The [CsvFactory] used to create the parser.
     */
    private val factory =
        CsvFactory()
            .enable(CsvParser.Feature.ALLOW_COMMENTS)
            .enable(CsvParser.Feature.TRIM_SPACES)

    override fun create(path: Path) {
        throw UnsupportedOperationException("Writing not supported for this format")
    }

    override fun getTables(path: Path): List<String> = listOf(TABLE_RESOURCES, TABLE_RESOURCE_STATES)

    override fun getDetails(
        path: Path,
        table: String,
    ): TableDetails {
        return when (table) {
            TABLE_RESOURCES ->
                TableDetails(
                    listOf(
                        TableColumn(resourceID, TableColumnType.String),
                    ),
                )
            TABLE_RESOURCE_STATES ->
                TableDetails(
                    listOf(
                        TableColumn(resourceID, TableColumnType.String),
                        TableColumn(resourceStateTimestamp, TableColumnType.Instant),
                        TableColumn(resourceCpuCount, TableColumnType.Int),
                        TableColumn(resourceCpuCapacity, TableColumnType.Double),
                        TableColumn(resourceStateCpuUsage, TableColumnType.Double),
                        TableColumn(resourceStateCpuUsagePct, TableColumnType.Double),
                        TableColumn(resourceMemCapacity, TableColumnType.Double),
                        TableColumn(resourceStateMemUsage, TableColumnType.Double),
                        TableColumn(resourceStateDiskRead, TableColumnType.Double),
                        TableColumn(resourceStateDiskWrite, TableColumnType.Double),
                        TableColumn(resourceStateNetRx, TableColumnType.Double),
                        TableColumn(resourceStateNetTx, TableColumnType.Double),
                    ),
                )
            else -> throw IllegalArgumentException("Table $table not supported")
        }
    }

    override fun newReader(
        path: Path,
        table: String,
        projection: List<String>?,
    ): TableReader {
        return when (table) {
            TABLE_RESOURCES -> {
                val vms =
                    Files.walk(path, 1)
                        .filter { !Files.isDirectory(it) && it.extension == "csv" }
                        .collect(Collectors.toMap({ it.nameWithoutExtension }, { it }))
                        .toSortedMap()
                BitbrainsResourceTableReader(factory, vms)
            }
            TABLE_RESOURCE_STATES -> newResourceStateReader(path)
            else -> throw IllegalArgumentException("Table $table not supported")
        }
    }

    override fun newWriter(
        path: Path,
        table: String,
    ): TableWriter {
        throw UnsupportedOperationException("Writing not supported for this format")
    }

    /**
     * Construct a [TableReader] for reading over all resource state partitions.
     */
    private fun newResourceStateReader(path: Path): TableReader {
        val partitions =
            Files.walk(path, 1)
                .filter { !Files.isDirectory(it) && it.extension == "csv" }
                .collect(Collectors.toMap({ it.nameWithoutExtension }, { it }))
                .toSortedMap()
        val it = partitions.iterator()

        return object : CompositeTableReader() {
            override fun nextReader(): TableReader? {
                return if (it.hasNext()) {
                    val (partition, partPath) = it.next()
                    return BitbrainsResourceStateTableReader(partition, factory.createParser(partPath.toFile()))
                } else {
                    null
                }
            }

            override fun toString(): String = "BitbrainsCompositeTableReader"
        }
    }
}
