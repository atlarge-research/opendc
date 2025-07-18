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

package org.opendc.trace.formats.workload

import org.apache.parquet.column.ParquetProperties
import org.apache.parquet.hadoop.ParquetFileWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.opendc.trace.TableColumn
import org.opendc.trace.TableColumnType
import org.opendc.trace.TableReader
import org.opendc.trace.TableWriter
import org.opendc.trace.conv.FRAGMENT_CPU_USAGE
import org.opendc.trace.conv.FRAGMENT_DURATION
import org.opendc.trace.conv.TABLE_FRAGMENTS
import org.opendc.trace.conv.TABLE_TASKS
import org.opendc.trace.conv.TASK_CHILDREN
import org.opendc.trace.conv.TASK_CPU_CAPACITY
import org.opendc.trace.conv.TASK_CPU_COUNT
import org.opendc.trace.conv.TASK_DEADLINE
import org.opendc.trace.conv.TASK_DURATION
import org.opendc.trace.conv.TASK_GPU_CAPACITY
import org.opendc.trace.conv.TASK_GPU_COUNT
import org.opendc.trace.conv.TASK_ID
import org.opendc.trace.conv.TASK_MEM_CAPACITY
import org.opendc.trace.conv.TASK_NATURE
import org.opendc.trace.conv.TASK_PARENTS
import org.opendc.trace.conv.TASK_SUBMISSION_TIME
import org.opendc.trace.formats.workload.parquet.FragmentReadSupport
import org.opendc.trace.formats.workload.parquet.FragmentWriteSupport
import org.opendc.trace.formats.workload.parquet.TaskReadSupport
import org.opendc.trace.formats.workload.parquet.TaskWriteSupport
import org.opendc.trace.spi.TableDetails
import org.opendc.trace.spi.TraceFormat
import org.opendc.trace.util.parquet.LocalParquetReader
import org.opendc.trace.util.parquet.LocalParquetWriter
import java.nio.file.Files
import java.nio.file.Path

/**
 * A [TraceFormat] implementation of the OpenDC virtual machine trace format.
 */
public class WorkloadTraceFormat : TraceFormat {
    /**
     * The name of this trace format.
     */
    override val name: String = "workload"

    override fun create(path: Path) {
        // Construct directory containing the trace files
        Files.createDirectories(path)

        val tables = getTables(path)

        for (table in tables) {
            val writer = newWriter(path, table)
            writer.close()
        }
    }

    override fun getTables(path: Path): List<String> = listOf(TABLE_TASKS, TABLE_FRAGMENTS)

    override fun getDetails(
        path: Path,
        table: String,
    ): TableDetails {
        return when (table) {
            TABLE_TASKS ->
                TableDetails(
                    listOf(
                        TableColumn(TASK_ID, TableColumnType.String),
                        TableColumn(TASK_SUBMISSION_TIME, TableColumnType.Instant),
                        TableColumn(TASK_DURATION, TableColumnType.Long),
                        TableColumn(TASK_CPU_COUNT, TableColumnType.Int),
                        TableColumn(TASK_CPU_CAPACITY, TableColumnType.Double),
                        TableColumn(TASK_MEM_CAPACITY, TableColumnType.Double),
                        TableColumn(TASK_GPU_COUNT, TableColumnType.Int),
                        TableColumn(TASK_GPU_CAPACITY, TableColumnType.Double),
                        TableColumn(TASK_PARENTS, TableColumnType.Set(TableColumnType.String)),
                        TableColumn(TASK_CHILDREN, TableColumnType.Set(TableColumnType.String)),
                        TableColumn(TASK_NATURE, TableColumnType.String),
                        TableColumn(TASK_DEADLINE, TableColumnType.Long),
                    ),
                )
            TABLE_FRAGMENTS ->
                TableDetails(
                    listOf(
                        TableColumn(TASK_ID, TableColumnType.String),
                        TableColumn(FRAGMENT_DURATION, TableColumnType.Duration),
                        TableColumn(TASK_CPU_COUNT, TableColumnType.Int),
                        TableColumn(FRAGMENT_CPU_USAGE, TableColumnType.Double),
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
            TABLE_TASKS -> {
                val reader = LocalParquetReader(path.resolve("tasks.parquet"), TaskReadSupport(projection))
                TaskTableReader(reader)
            }
            TABLE_FRAGMENTS -> {
                val reader = LocalParquetReader(path.resolve("fragments.parquet"), FragmentReadSupport(projection))
                FragmentTableReader(reader)
            }
            else -> throw IllegalArgumentException("Table $table not supported")
        }
    }

    override fun newWriter(
        path: Path,
        table: String,
    ): TableWriter {
        return when (table) {
            TABLE_TASKS -> {
                val writer =
                    LocalParquetWriter.builder(path.resolve("tasks.parquet"), TaskWriteSupport())
                        .withCompressionCodec(CompressionCodecName.ZSTD)
                        .withPageWriteChecksumEnabled(true)
                        .withWriterVersion(ParquetProperties.WriterVersion.PARQUET_2_0)
                        .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
                        .build()
                TaskTableWriter(writer)
            }
            TABLE_FRAGMENTS -> {
                val writer =
                    LocalParquetWriter.builder(path.resolve("fragments.parquet"), FragmentWriteSupport())
                        .withCompressionCodec(CompressionCodecName.ZSTD)
                        .withDictionaryEncoding("id", true)
                        .withBloomFilterEnabled("id", true)
                        .withPageWriteChecksumEnabled(true)
                        .withWriterVersion(ParquetProperties.WriterVersion.PARQUET_2_0)
                        .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
                        .build()
                FragmentTableWriter(writer)
            }
            else -> throw IllegalArgumentException("Table $table not supported")
        }
    }
}
