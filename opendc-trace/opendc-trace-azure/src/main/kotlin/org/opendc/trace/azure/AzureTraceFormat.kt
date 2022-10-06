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
import com.fasterxml.jackson.dataformat.csv.CsvParser
import org.opendc.trace.TableColumn
import org.opendc.trace.TableColumnType
import org.opendc.trace.TableReader
import org.opendc.trace.TableWriter
import org.opendc.trace.conv.RESOURCE_CPU_COUNT
import org.opendc.trace.conv.RESOURCE_ID
import org.opendc.trace.conv.RESOURCE_MEM_CAPACITY
import org.opendc.trace.conv.RESOURCE_START_TIME
import org.opendc.trace.conv.RESOURCE_STATE_CPU_USAGE_PCT
import org.opendc.trace.conv.RESOURCE_STATE_TIMESTAMP
import org.opendc.trace.conv.RESOURCE_STOP_TIME
import org.opendc.trace.conv.TABLE_RESOURCES
import org.opendc.trace.conv.TABLE_RESOURCE_STATES
import org.opendc.trace.spi.TableDetails
import org.opendc.trace.spi.TraceFormat
import org.opendc.trace.util.CompositeTableReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import java.util.zip.GZIPInputStream
import kotlin.io.path.inputStream
import kotlin.io.path.name

/**
 * A format implementation for the Azure v1 format.
 */
public class AzureTraceFormat : TraceFormat {
    /**
     * The name of this trace format.
     */
    override val name: String = "azure"

    /**
     * The [CsvFactory] used to create the parser.
     */
    private val factory = CsvFactory()
        .enable(CsvParser.Feature.ALLOW_COMMENTS)
        .enable(CsvParser.Feature.TRIM_SPACES)

    override fun create(path: Path) {
        throw UnsupportedOperationException("Writing not supported for this format")
    }

    override fun getTables(path: Path): List<String> = listOf(TABLE_RESOURCES, TABLE_RESOURCE_STATES)

    override fun getDetails(path: Path, table: String): TableDetails {
        return when (table) {
            TABLE_RESOURCES -> TableDetails(
                listOf(
                    TableColumn(RESOURCE_ID, TableColumnType.String),
                    TableColumn(RESOURCE_START_TIME, TableColumnType.Instant),
                    TableColumn(RESOURCE_STOP_TIME, TableColumnType.Instant),
                    TableColumn(RESOURCE_CPU_COUNT, TableColumnType.Int),
                    TableColumn(RESOURCE_MEM_CAPACITY, TableColumnType.Double)
                )
            )
            TABLE_RESOURCE_STATES -> TableDetails(
                listOf(
                    TableColumn(RESOURCE_ID, TableColumnType.String),
                    TableColumn(RESOURCE_STATE_TIMESTAMP, TableColumnType.Instant),
                    TableColumn(RESOURCE_STATE_CPU_USAGE_PCT, TableColumnType.Double)
                )
            )
            else -> throw IllegalArgumentException("Table $table not supported")
        }
    }

    override fun newReader(path: Path, table: String, projection: List<String>?): TableReader {
        return when (table) {
            TABLE_RESOURCES -> {
                val stream = GZIPInputStream(path.resolve("vmtable/vmtable.csv.gz").inputStream())
                AzureResourceTableReader(factory.createParser(stream))
            }
            TABLE_RESOURCE_STATES -> newResourceStateReader(path)
            else -> throw IllegalArgumentException("Table $table not supported")
        }
    }

    override fun newWriter(path: Path, table: String): TableWriter {
        throw UnsupportedOperationException("Writing not supported for this format")
    }

    /**
     * Construct a [TableReader] for reading over all VM CPU readings.
     */
    private fun newResourceStateReader(path: Path): TableReader {
        val partitions = Files.walk(path.resolve("vm_cpu_readings"), 1)
            .filter { !Files.isDirectory(it) && it.name.endsWith(".csv.gz") }
            .collect(Collectors.toMap({ it.name.removeSuffix(".csv.gz") }, { it }))
            .toSortedMap()
        val it = partitions.iterator()

        return object : CompositeTableReader() {
            override fun nextReader(): TableReader? {
                return if (it.hasNext()) {
                    val (_, partPath) = it.next()
                    val stream = GZIPInputStream(partPath.inputStream())
                    return AzureResourceStateTableReader(factory.createParser(stream))
                } else {
                    null
                }
            }

            override fun toString(): String = "AzureCompositeTableReader"
        }
    }
}
