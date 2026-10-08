/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.runner.factory

import mu.KotlinLogging
import org.opendc.sdk.model.telemetry.AllColumns
import org.opendc.sdk.model.telemetry.ColumnSelection
import org.opendc.sdk.model.telemetry.ExportSpec
import org.opendc.sdk.model.telemetry.OnlyColumns
import org.opendc.sdk.model.telemetry.OutputFileSpec
import org.opendc.sdk.runner.telemetry.parquet.ComputeExportConfig
import org.opendc.sdk.runner.telemetry.parquet.DfltHostExportColumns
import org.opendc.sdk.runner.telemetry.parquet.DfltTaskExportColumns
import org.opendc.sdk.runner.telemetry.table.battery.BatterySample
import org.opendc.sdk.runner.telemetry.table.cluster.ClusterSample
import org.opendc.sdk.runner.telemetry.table.datacenter.DataCenterSample
import org.opendc.sdk.runner.telemetry.table.host.HostSample
import org.opendc.sdk.runner.telemetry.table.powerSource.PowerSourceSample
import org.opendc.sdk.runner.telemetry.table.service.ServiceSample
import org.opendc.sdk.runner.telemetry.table.task.TaskSample
import org.opendc.trace.parquet.exporter.ExportColumn
import org.opendc.trace.parquet.exporter.Exportable
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

public data class ExportSettings(
    val config: ComputeExportConfig,
    val filesToExport: Map<OutputFileSpec, Boolean>,
    val exportInterval: Duration,
    val printFrequency: Int?,
)

/**
 * Derives the engine export settings from this [ExportSpec] for a topology whose hosts have at most [gpuCount] GPUs. The
 * columns are derived per simulation, as the GPU columns depend on its topology.
 */
internal fun ExportSpec.toExportSettings(gpuCount: Int): ExportSettings =
    ExportSettings(
        config = toComputeExportConfig(gpuCount),
        filesToExport = toFilesToExport(),
        exportInterval = Duration.ofMillis(exportInterval.toMsLong()),
        printFrequency = printFrequency,
    )

private fun ExportSpec.toComputeExportConfig(gpuCount: Int): ComputeExportConfig {
    ComputeExportConfig.loadDfltColumns()
    return ComputeExportConfig(
        columns.battery.resolve<BatterySample>("battery"),
        columns.cluster.resolve<ClusterSample>("cluster"),
        columns.dataCenter.resolve<DataCenterSample>("dataCenter"),
        hostColumns(columns.host, gpuCount),
        columns.powerSource.resolve<PowerSourceSample>("powerSource"),
        columns.service.resolve<ServiceSample>("service"),
        taskColumns(columns.task, gpuCount),
    )
}

/**
 * The host columns. The per-GPU columns are generated for [gpuCount] GPUs, the most of any host, so in a topology that
 * mixes hosts with and without GPUs every host has them. A selection that names a GPU metric, such as `gpu_usage`, gets
 * its columns even if the topology has no GPUs, for one GPU.
 */
private fun hostColumns(
    selection: ColumnSelection,
    gpuCount: Int,
): List<ExportColumn<HostSample>> {
    val gpuMetrics =
        when (selection) {
            AllColumns -> DfltHostExportColumns.GPU_METRICS
            is OnlyColumns -> DfltHostExportColumns.GPU_METRICS.filter { it in selection.columns }
        }

    var count = gpuCount
    if (selection is OnlyColumns && gpuMetrics.isNotEmpty() && gpuCount == 0) {
        warnNoGpus("host", gpuMetrics)
        count = 1
    }

    return selection.resolve<HostSample>("host", DfltHostExportColumns.GPU_METRICS) + DfltHostExportColumns.gpuColumns(count, gpuMetrics)
}

/**
 * The task columns. The GPU columns are left out by default if the topology has no GPUs, but are kept if selected.
 */
private fun taskColumns(
    selection: ColumnSelection,
    gpuCount: Int,
): List<ExportColumn<TaskSample>> {
    val columns = selection.resolve<TaskSample>("task")
    val gpuColumns = columns.filter { it in DfltTaskExportColumns.GPU_COLUMNS }
    if (gpuCount > 0 || gpuColumns.isEmpty()) {
        return columns
    }

    if (selection == AllColumns) {
        return columns - gpuColumns.toSet()
    }

    warnNoGpus("task", gpuColumns.map { it.name })
    return columns
}

private fun warnNoGpus(
    table: String,
    columns: List<String>,
) {
    logger.warn { "The topology has no GPUs, but the selected $table columns $columns are exported anyway" }
}

/**
 * The columns of this selection for the [table], whose columns can also be selected by the names in [otherNames]. A
 * selected name that is not a column of the table is ignored with a warning.
 */
private inline fun <reified T : Exportable> ColumnSelection.resolve(
    table: String,
    otherNames: Collection<String> = emptyList(),
): List<ExportColumn<T>> {
    val all = ExportColumn.getAllLoadedColumns<T>()
    return when (this) {
        AllColumns -> all
        is OnlyColumns -> {
            warnUnknownColumns(table, columns, all.map { it.name } + otherNames)
            all.filter { it.name in columns }
        }
    }
}

/**
 * The warnings about unknown columns that have been logged, so that each is logged once rather than for every run.
 */
private val loggedUnknownColumns: MutableSet<String> = ConcurrentHashMap.newKeySet()

private fun warnUnknownColumns(
    table: String,
    selected: Set<String>,
    known: Collection<String>,
) {
    val unknown = selected - known.toSet()
    if (unknown.isEmpty()) {
        return
    }

    val message =
        "The export model selects unknown $table columns ${unknown.sorted()}, which are not exported. " +
            "The $table columns are ${known.distinct().sorted()}"
    if (loggedUnknownColumns.add(message)) {
        logger.warn { message }
    }
}

private fun ExportSpec.toFilesToExport(): Map<OutputFileSpec, Boolean> {
    val enabled = filesToExport.toSet()
    return OutputFileSpec.entries.associateWith { it in enabled }
}
