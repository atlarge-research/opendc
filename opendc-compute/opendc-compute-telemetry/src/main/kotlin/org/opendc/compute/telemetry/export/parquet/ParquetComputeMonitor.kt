/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.compute.telemetry.export.parquet

import org.opendc.compute.telemetry.ComputeMonitor
import org.opendc.compute.telemetry.table.HostTableReader
import org.opendc.compute.telemetry.table.ServiceTableReader
import org.opendc.compute.telemetry.table.TaskTableReader
import org.opendc.trace.util.parquet.exporter.ExportColumn
import org.opendc.trace.util.parquet.exporter.Exportable
import org.opendc.trace.util.parquet.exporter.Exporter
import java.io.File

/**
 * A [ComputeMonitor] that logs the events to a Parquet file.
 */
public class ParquetComputeMonitor(
    private val hostExporter: Exporter<HostTableReader>,
    private val taskExporter: Exporter<TaskTableReader>,
    private val serviceExporter: Exporter<ServiceTableReader>,
) : ComputeMonitor, AutoCloseable {
    override fun record(reader: HostTableReader) {
        hostExporter.write(reader)
    }

    override fun record(reader: TaskTableReader) {
        taskExporter.write(reader)
    }

    override fun record(reader: ServiceTableReader) {
        serviceExporter.write(reader)
    }

    override fun close() {
        hostExporter.close()
        taskExporter.close()
        serviceExporter.close()
    }

    public companion object {
        /**
         * Overloaded constructor with [ComputeExportConfig] as parameter.
         *
         * @param[base]         parent pathname for output file.
         * @param[partition]    child pathname for output file.
         * @param[bufferSize]   size of the buffer used by the writer thread.
         */
        public operator fun invoke(
            base: File,
            partition: String,
            bufferSize: Int,
            computeExportConfig: ComputeExportConfig,
        ): ParquetComputeMonitor =
            invoke(
                base = base,
                partition = partition,
                bufferSize = bufferSize,
                hostExportColumns = computeExportConfig.hostExportColumns,
                taskExportColumns = computeExportConfig.taskExportColumns,
                serviceExportColumns = computeExportConfig.serviceExportColumns,
            )

        /**
         * Constructor that loads default [ExportColumn]s defined in
         * [DfltHostExportColumns], [DfltTaskExportColumns], [DfltServiceExportColumns]
         * in case optional parameters are omitted and all fields need to be retrieved.
         *
         * @param[base]         parent pathname for output file.
         * @param[partition]    child pathname for output file.
         * @param[bufferSize]   size of the buffer used by the writer thread.
         */
        public operator fun invoke(
            base: File,
            partition: String,
            bufferSize: Int,
            hostExportColumns: Collection<ExportColumn<HostTableReader>>? = null,
            taskExportColumns: Collection<ExportColumn<TaskTableReader>>? = null,
            serviceExportColumns: Collection<ExportColumn<ServiceTableReader>>? = null,
        ): ParquetComputeMonitor {
            // Loads the fields in case they need to be retrieved if optional params are omitted.
            ComputeExportConfig.loadDfltColumns()

            return ParquetComputeMonitor(
                hostExporter =
                    Exporter(
                        outputFile = File(base, "$partition/host.parquet").also { it.parentFile.mkdirs() },
                        columns = hostExportColumns ?: Exportable.getAllLoadedColumns(),
                        bufferSize = bufferSize,
                    ),
                taskExporter =
                    Exporter(
                        outputFile = File(base, "$partition/task.parquet").also { it.parentFile.mkdirs() },
                        columns = taskExportColumns ?: Exportable.getAllLoadedColumns(),
                        bufferSize = bufferSize,
                    ),
                serviceExporter =
                    Exporter(
                        outputFile = File(base, "$partition/service.parquet").also { it.parentFile.mkdirs() },
                        columns = serviceExportColumns ?: Exportable.getAllLoadedColumns(),
                        bufferSize = bufferSize,
                    ),
            )
        }
    }
}
