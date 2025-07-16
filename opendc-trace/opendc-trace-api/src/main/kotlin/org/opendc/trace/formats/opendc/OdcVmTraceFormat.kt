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

package org.opendc.trace.formats.opendc

import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.core.JsonFactory
import org.apache.parquet.column.ParquetProperties
import org.apache.parquet.hadoop.ParquetFileWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.opendc.trace.TableColumn
import org.opendc.trace.TableColumnType
import org.opendc.trace.TableReader
import org.opendc.trace.TableWriter
import org.opendc.trace.conv.INTERFERENCE_GROUP_MEMBERS
import org.opendc.trace.conv.INTERFERENCE_GROUP_SCORE
import org.opendc.trace.conv.INTERFERENCE_GROUP_TARGET
import org.opendc.trace.conv.TABLE_INTERFERENCE_GROUPS
import org.opendc.trace.conv.TABLE_RESOURCES
import org.opendc.trace.conv.TABLE_RESOURCE_STATES
import org.opendc.trace.conv.resourceChildren
import org.opendc.trace.conv.resourceCpuCapacity
import org.opendc.trace.conv.resourceCpuCount
import org.opendc.trace.conv.resourceDeadline
import org.opendc.trace.conv.resourceDuration
import org.opendc.trace.conv.resourceGpuCapacity
import org.opendc.trace.conv.resourceGpuCount
import org.opendc.trace.conv.resourceID
import org.opendc.trace.conv.resourceMemCapacity
import org.opendc.trace.conv.resourceNature
import org.opendc.trace.conv.resourceParents
import org.opendc.trace.conv.resourceStateCpuUsage
import org.opendc.trace.conv.resourceStateDuration
import org.opendc.trace.conv.resourceStateTimestamp
import org.opendc.trace.conv.resourceSubmissionTime
import org.opendc.trace.formats.opendc.parquet.ResourceReadSupport
import org.opendc.trace.formats.opendc.parquet.ResourceStateReadSupport
import org.opendc.trace.formats.opendc.parquet.ResourceStateWriteSupport
import org.opendc.trace.formats.opendc.parquet.ResourceWriteSupport
import org.opendc.trace.spi.TableDetails
import org.opendc.trace.spi.TraceFormat
import org.opendc.trace.util.parquet.LocalParquetReader
import org.opendc.trace.util.parquet.LocalParquetWriter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * A [TraceFormat] implementation of the OpenDC virtual machine trace format.
 */
public class OdcVmTraceFormat : TraceFormat {
    /**
     * A [JsonFactory] that is used to parse the JSON-based interference model.
     */
    private val jsonFactory = JsonFactory()

    /**
     * The name of this trace format.
     */
    override val name: String = "opendc-vm"

    override fun create(path: Path) {
        // Construct directory containing the trace files
        Files.createDirectories(path)

        val tables = getTables(path)

        for (table in tables) {
            val writer = newWriter(path, table)
            writer.close()
        }
    }

    override fun getTables(path: Path): List<String> = listOf(TABLE_RESOURCES, TABLE_RESOURCE_STATES, TABLE_INTERFERENCE_GROUPS)

    override fun getDetails(
        path: Path,
        table: String,
    ): TableDetails {
        return when (table) {
            TABLE_RESOURCES ->
                TableDetails(
                    listOf(
                        TableColumn(resourceID, TableColumnType.String),
                        TableColumn(resourceSubmissionTime, TableColumnType.Instant),
                        TableColumn(resourceDuration, TableColumnType.Long),
                        TableColumn(resourceCpuCount, TableColumnType.Int),
                        TableColumn(resourceCpuCapacity, TableColumnType.Double),
                        TableColumn(resourceMemCapacity, TableColumnType.Double),
                        TableColumn(resourceGpuCount, TableColumnType.Int),
                        TableColumn(resourceGpuCapacity, TableColumnType.Double),
                        TableColumn(resourceParents, TableColumnType.Set(TableColumnType.String)),
                        TableColumn(resourceChildren, TableColumnType.Set(TableColumnType.String)),
                        TableColumn(resourceNature, TableColumnType.String),
                        TableColumn(resourceDeadline, TableColumnType.Long),
                    ),
                )
            TABLE_RESOURCE_STATES ->
                TableDetails(
                    listOf(
                        TableColumn(resourceID, TableColumnType.String),
                        TableColumn(resourceStateTimestamp, TableColumnType.Instant),
                        TableColumn(resourceStateDuration, TableColumnType.Duration),
                        TableColumn(resourceCpuCount, TableColumnType.Int),
                        TableColumn(resourceStateCpuUsage, TableColumnType.Double),
                    ),
                )
            TABLE_INTERFERENCE_GROUPS ->
                TableDetails(
                    listOf(
                        TableColumn(INTERFERENCE_GROUP_MEMBERS, TableColumnType.Set(TableColumnType.String)),
                        TableColumn(INTERFERENCE_GROUP_TARGET, TableColumnType.Double),
                        TableColumn(INTERFERENCE_GROUP_SCORE, TableColumnType.Double),
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
                val reader = LocalParquetReader(path.resolve("tasks.parquet"), ResourceReadSupport(projection))
                OdcVmResourceTableReader(reader)
            }
            TABLE_RESOURCE_STATES -> {
                val reader = LocalParquetReader(path.resolve("fragments.parquet"), ResourceStateReadSupport(projection))
                OdcVmResourceStateTableReader(reader)
            }
            TABLE_INTERFERENCE_GROUPS -> {
                val modelPath = path.resolve("interference-model.json")
                val parser =
                    if (modelPath.exists()) {
                        jsonFactory.createParser(modelPath.toFile())
                    } else {
                        jsonFactory.createParser("[]") // If model does not exist, return empty model
                    }

                OdcVmInterferenceJsonTableReader(parser)
            }
            else -> throw IllegalArgumentException("Table $table not supported")
        }
    }

    override fun newWriter(
        path: Path,
        table: String,
    ): TableWriter {
        return when (table) {
            TABLE_RESOURCES -> {
                val writer =
                    LocalParquetWriter.builder(path.resolve("tasks.parquet"), ResourceWriteSupport())
                        .withCompressionCodec(CompressionCodecName.ZSTD)
                        .withPageWriteChecksumEnabled(true)
                        .withWriterVersion(ParquetProperties.WriterVersion.PARQUET_2_0)
                        .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
                        .build()
                OdcVmResourceTableWriter(writer)
            }
            TABLE_RESOURCE_STATES -> {
                val writer =
                    LocalParquetWriter.builder(path.resolve("fragments.parquet"), ResourceStateWriteSupport())
                        .withCompressionCodec(CompressionCodecName.ZSTD)
                        .withDictionaryEncoding("id", true)
                        .withBloomFilterEnabled("id", true)
                        .withPageWriteChecksumEnabled(true)
                        .withWriterVersion(ParquetProperties.WriterVersion.PARQUET_2_0)
                        .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
                        .build()
                OdcVmResourceStateTableWriter(writer)
            }
            TABLE_INTERFERENCE_GROUPS -> {
                val generator = jsonFactory.createGenerator(path.resolve("interference-model.json").toFile(), JsonEncoding.UTF8)
                OdcVmInterferenceJsonTableWriter(generator)
            }
            else -> throw IllegalArgumentException("Table $table not supported")
        }
    }
}
