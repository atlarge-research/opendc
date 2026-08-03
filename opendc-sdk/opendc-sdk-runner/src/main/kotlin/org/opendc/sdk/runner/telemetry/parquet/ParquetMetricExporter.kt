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

package org.opendc.sdk.runner.telemetry.parquet

import org.opendc.sdk.model.telemetry.OutputFileSpec
import org.opendc.sdk.runner.telemetry.MetricExporter
import org.opendc.sdk.runner.telemetry.table.battery.BatterySample
import org.opendc.sdk.runner.telemetry.table.host.HostSample
import org.opendc.sdk.runner.telemetry.table.powerSource.PowerSourceSample
import org.opendc.sdk.runner.telemetry.table.service.ServiceSample
import org.opendc.sdk.runner.telemetry.table.task.TaskSample
import org.opendc.trace.util.parquet.exporter.ExportColumn
import org.opendc.trace.util.parquet.exporter.Exportable
import org.opendc.trace.util.parquet.exporter.Exporter
import java.io.File

/**
 * A [MetricExporter] that logs the events to a Parquet file.
 */
public class ParquetMetricExporter(
    private val batteryExporter: Exporter<BatterySample>?,
    private val hostExporter: Exporter<HostSample>?,
    private val powerSourceExporter: Exporter<PowerSourceSample>?,
    private val serviceExporter: Exporter<ServiceSample>?,
    private val taskExporter: Exporter<TaskSample>?,
) : MetricExporter, AutoCloseable {
    override fun export(reader: BatterySample) {
        batteryExporter?.write(reader)
    }

    override fun export(reader: HostSample) {
        hostExporter?.write(reader)
    }

    override fun export(reader: PowerSourceSample) {
        powerSourceExporter?.write(reader)
    }

    override fun export(reader: ServiceSample) {
        serviceExporter?.write(reader)
    }

    override fun export(reader: TaskSample) {
        taskExporter?.write(reader)
    }

    override fun close() {
        batteryExporter?.close()
        hostExporter?.close()
        powerSourceExporter?.close()
        serviceExporter?.close()
        taskExporter?.close()
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
            filesToExport: Map<OutputFileSpec, Boolean>,
            computeExportConfig: ComputeExportConfig,
        ): ParquetMetricExporter =
            invoke(
                base = base,
                partition = partition,
                bufferSize = bufferSize,
                filesToExport = filesToExport,
                batteryExportColumns = computeExportConfig.batteryExportColumns,
                hostExportColumns = computeExportConfig.hostExportColumns,
                powerSourceExportColumns = computeExportConfig.powerSourceExportColumns,
                serviceExportColumns = computeExportConfig.serviceExportColumns,
                taskExportColumns = computeExportConfig.taskExportColumns,
            )

        /**
         * Constructor that loads default [ExportColumn]s defined in
         * [DfltHostExportColumns], [DfltTaskExportColumns], [DfltPowerSourceExportColumns], [DfltServiceExportColumns]
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
            filesToExport: Map<OutputFileSpec, Boolean>,
            batteryExportColumns: Collection<ExportColumn<BatterySample>>? = null,
            hostExportColumns: Collection<ExportColumn<HostSample>>? = null,
            powerSourceExportColumns: Collection<ExportColumn<PowerSourceSample>>? = null,
            serviceExportColumns: Collection<ExportColumn<ServiceSample>>? = null,
            taskExportColumns: Collection<ExportColumn<TaskSample>>? = null,
        ): ParquetMetricExporter {
            // Loads the fields in case they need to be retrieved if optional params are omitted.
            ComputeExportConfig.loadDfltColumns()

            val batteryExporter =
                if (filesToExport[OutputFileSpec.BATTERY] == true) {
                    Exporter(
                        outputFile = File(base, "$partition/battery.parquet").also { it.parentFile.mkdirs() },
                        columns = batteryExportColumns ?: Exportable.getAllLoadedColumns(),
                        bufferSize = bufferSize,
                    )
                } else {
                    null
                }

            val hostExporter =
                if (filesToExport[OutputFileSpec.HOST] == true) {
                    Exporter(
                        outputFile = File(base, "$partition/host.parquet").also { it.parentFile.mkdirs() },
                        columns = hostExportColumns ?: Exportable.getAllLoadedColumns(),
                        bufferSize = bufferSize,
                    )
                } else {
                    null
                }

            val powerSourceExporter =
                if (filesToExport[OutputFileSpec.POWER_SOURCE] == true) {
                    Exporter(
                        outputFile = File(base, "$partition/powerSource.parquet").also { it.parentFile.mkdirs() },
                        columns = powerSourceExportColumns ?: Exportable.getAllLoadedColumns(),
                        bufferSize = bufferSize,
                    )
                } else {
                    null
                }

            val serviceExporter =
                if (filesToExport[OutputFileSpec.SERVICE] == true) {
                    Exporter(
                        outputFile = File(base, "$partition/service.parquet").also { it.parentFile.mkdirs() },
                        columns = serviceExportColumns ?: Exportable.getAllLoadedColumns(),
                        bufferSize = bufferSize,
                    )
                } else {
                    null
                }

            val taskExporter =
                if (filesToExport[OutputFileSpec.TASK] == true) {
                    Exporter(
                        outputFile = File(base, "$partition/task.parquet").also { it.parentFile.mkdirs() },
                        columns = taskExportColumns ?: Exportable.getAllLoadedColumns(),
                        bufferSize = bufferSize,
                    )
                } else {
                    null
                }

            return ParquetMetricExporter(
                batteryExporter = batteryExporter,
                hostExporter = hostExporter,
                powerSourceExporter = powerSourceExporter,
                serviceExporter = serviceExporter,
                taskExporter = taskExporter,
            )
        }
    }
}
