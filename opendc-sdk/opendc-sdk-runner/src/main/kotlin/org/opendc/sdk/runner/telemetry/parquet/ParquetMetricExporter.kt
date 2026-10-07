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
import org.opendc.sdk.runner.telemetry.table.cluster.ClusterSample
import org.opendc.sdk.runner.telemetry.table.datacenter.DataCenterSample
import org.opendc.sdk.runner.telemetry.table.host.HostSample
import org.opendc.sdk.runner.telemetry.table.powerSource.PowerSourceSample
import org.opendc.sdk.runner.telemetry.table.service.ServiceSample
import org.opendc.sdk.runner.telemetry.table.simulation.SimulationMeta
import org.opendc.sdk.runner.telemetry.table.task.TaskMeta
import org.opendc.sdk.runner.telemetry.table.task.TaskSample
import org.opendc.sdk.runner.telemetry.table.topology.TopologyMeta
import org.opendc.trace.parquet.exporter.ExportColumn
import org.opendc.trace.parquet.exporter.Exportable
import org.opendc.trace.parquet.exporter.Exporter
import java.io.File

/**
 * A [MetricExporter] that logs the events to a Parquet file.
 *
 * @param taskMetaExporter The exporter of the static attributes of the tasks, or `null` to not write them.
 * @param metaDirectory The directory to write the simulation and topology meta files to, or `null` to not write them.
 */
public class ParquetMetricExporter(
    private val batteryExporter: Exporter<BatterySample>?,
    private val clusterExporter: Exporter<ClusterSample>?,
    private val dataCenterExporter: Exporter<DataCenterSample>?,
    private val hostExporter: Exporter<HostSample>?,
    private val powerSourceExporter: Exporter<PowerSourceSample>?,
    private val serviceExporter: Exporter<ServiceSample>?,
    private val taskExporter: Exporter<TaskSample>?,
    private val taskMetaExporter: Exporter<TaskMeta>? = null,
    private val metaDirectory: File? = null,
) : MetricExporter, AutoCloseable {
    override fun export(reader: BatterySample) {
        batteryExporter?.write(reader)
    }

    override fun export(reader: ClusterSample) {
        clusterExporter?.write(reader)
    }

    override fun export(reader: DataCenterSample) {
        dataCenterExporter?.write(reader)
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

    override fun export(meta: SimulationMeta) {
        val directory = metaDirectory ?: return
        directory.mkdirs()

        writeMeta(File(directory, "simulation.parquet"), MetaColumns.SIMULATION, listOf(meta))
    }

    override fun export(meta: TopologyMeta) {
        val directory = metaDirectory ?: return
        directory.mkdirs()

        val gpuCount = meta.hosts.maxOfOrNull { it.gpuCapacities.size } ?: 0

        writeMeta(File(directory, "dataCenter.parquet"), MetaColumns.DATA_CENTER, meta.dataCenters)
        writeMeta(File(directory, "cluster.parquet"), MetaColumns.CLUSTER, meta.clusters)
        writeMeta(File(directory, "host.parquet"), MetaColumns.host(gpuCount), meta.hosts)
        writeMeta(File(directory, "powerSource.parquet"), MetaColumns.POWER_SOURCE, meta.powerSources)
        writeMeta(File(directory, "battery.parquet"), MetaColumns.BATTERY, meta.batteries)
    }

    override fun export(meta: TaskMeta) {
        taskMetaExporter?.write(meta)
    }

    private inline fun <reified T : Exportable> writeMeta(
        file: File,
        columns: List<ExportColumn<T>>,
        rows: List<T>,
    ) {
        Exporter(outputFile = file, columns = columns).use { exporter -> rows.forEach(exporter::write) }
    }

    /**
     * Close the exporters, and throw the first failure of one of them. Every exporter is closed, also after one fails, so
     * that no writer threads or part files are left behind.
     */
    override fun close() {
        val exporters =
            listOfNotNull(
                batteryExporter,
                clusterExporter,
                dataCenterExporter,
                hostExporter,
                powerSourceExporter,
                serviceExporter,
                taskExporter,
                taskMetaExporter,
            )

        var failure: Throwable? = null
        for (exporter in exporters) {
            try {
                exporter.close()
            } catch (cause: Throwable) {
                val first = failure
                if (first == null) {
                    failure = cause
                } else {
                    first.addSuppressed(cause)
                }
            }
        }

        failure?.let { throw it }
    }

    public companion object {
        /**
         * Overloaded constructor with [ComputeExportConfig] as parameter.
         *
         * @param[base]         parent pathname for output file.
         * @param[partition]    child pathname for output file.
         * @param[bufferSize]   size of the buffer used by the writer thread.
         * @param[writerThreads] the number of threads that write each of the task and host files.
         */
        public operator fun invoke(
            base: File,
            partition: String,
            bufferSize: Int,
            filesToExport: Map<OutputFileSpec, Boolean>,
            computeExportConfig: ComputeExportConfig,
            writerThreads: Int = 1,
        ): ParquetMetricExporter =
            invoke(
                base = base,
                partition = partition,
                bufferSize = bufferSize,
                filesToExport = filesToExport,
                batteryExportColumns = computeExportConfig.batteryExportColumns,
                clusterExportColumns = computeExportConfig.clusterExportColumns,
                dataCenterExportColumns = computeExportConfig.dataCenterExportColumns,
                hostExportColumns = computeExportConfig.hostExportColumns,
                powerSourceExportColumns = computeExportConfig.powerSourceExportColumns,
                serviceExportColumns = computeExportConfig.serviceExportColumns,
                taskExportColumns = computeExportConfig.taskExportColumns,
                writerThreads = writerThreads,
            )

        /**
         * Constructor that loads default [ExportColumn]s defined in
         * [DfltHostExportColumns], [DfltTaskExportColumns], [DfltPowerSourceExportColumns], [DfltServiceExportColumns]
         * in case optional parameters are omitted and all fields need to be retrieved.
         *
         * @param[base]         parent pathname for output file.
         * @param[partition]    child pathname for output file.
         * @param[bufferSize]   size of the buffer used by the writer thread.
         * @param[writerThreads] the number of threads that write each of the task and host files, which grow with the
         * size of the simulation. The other files are written by a single thread.
         */
        public operator fun invoke(
            base: File,
            partition: String,
            bufferSize: Int,
            filesToExport: Map<OutputFileSpec, Boolean>,
            batteryExportColumns: Collection<ExportColumn<BatterySample>>? = null,
            clusterExportColumns: Collection<ExportColumn<ClusterSample>>? = null,
            dataCenterExportColumns: Collection<ExportColumn<DataCenterSample>>? = null,
            hostExportColumns: Collection<ExportColumn<HostSample>>? = null,
            powerSourceExportColumns: Collection<ExportColumn<PowerSourceSample>>? = null,
            serviceExportColumns: Collection<ExportColumn<ServiceSample>>? = null,
            taskExportColumns: Collection<ExportColumn<TaskSample>>? = null,
            writerThreads: Int = 1,
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

            val clusterExporter =
                if (filesToExport[OutputFileSpec.CLUSTER] == true) {
                    Exporter(
                        outputFile = File(base, "$partition/cluster.parquet").also { it.parentFile.mkdirs() },
                        columns = clusterExportColumns ?: Exportable.getAllLoadedColumns(),
                        bufferSize = bufferSize,
                    )
                } else {
                    null
                }

            val dataCenterExporter =
                if (filesToExport[OutputFileSpec.DATA_CENTER] == true) {
                    Exporter(
                        outputFile = File(base, "$partition/dataCenter.parquet").also { it.parentFile.mkdirs() },
                        columns = dataCenterExportColumns ?: Exportable.getAllLoadedColumns(),
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
                        writerThreads = writerThreads,
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
                        writerThreads = writerThreads,
                    )
                } else {
                    null
                }

            val taskMetaExporter =
                if (filesToExport[OutputFileSpec.TASK] == true) {
                    Exporter(
                        outputFile = File(base, "$partition/meta/task.parquet").also { it.parentFile.mkdirs() },
                        columns = MetaColumns.TASK,
                        bufferSize = bufferSize,
                    )
                } else {
                    null
                }

            return ParquetMetricExporter(
                batteryExporter = batteryExporter,
                clusterExporter = clusterExporter,
                dataCenterExporter = dataCenterExporter,
                hostExporter = hostExporter,
                powerSourceExporter = powerSourceExporter,
                serviceExporter = serviceExporter,
                taskExporter = taskExporter,
                taskMetaExporter = taskMetaExporter,
                metaDirectory = File(base, "$partition/meta"),
            )
        }
    }
}
