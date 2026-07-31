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

import org.opendc.sdk.model.telemetry.AllColumns
import org.opendc.sdk.model.telemetry.ColumnSelection
import org.opendc.sdk.model.telemetry.ExportSpec
import org.opendc.sdk.model.telemetry.OnlyColumns
import org.opendc.sdk.model.telemetry.OutputFileSpec
import org.opendc.sdk.runner.telemetry.parquet.ComputeExportConfig
import org.opendc.sdk.runner.telemetry.parquet.withGpuColumns
import org.opendc.sdk.runner.telemetry.table.battery.BatterySample
import org.opendc.sdk.runner.telemetry.table.host.HostSample
import org.opendc.sdk.runner.telemetry.table.powerSource.PowerSourceSample
import org.opendc.sdk.runner.telemetry.table.service.ServiceSample
import org.opendc.sdk.runner.telemetry.table.task.TaskSample
import org.opendc.trace.util.parquet.exporter.ExportColumn
import org.opendc.trace.util.parquet.exporter.Exportable
import java.time.Duration

public data class ExportSettings(
    val config: ComputeExportConfig,
    val filesToExport: Map<OutputFileSpec, Boolean>,
    val exportInterval: Duration,
    val printFrequency: Int?,
)

/** Derives the engine export settings from this [ExportSpec], adding GPU columns for [gpuCount] GPUs. */
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
        columns.battery.resolve<BatterySample>(),
        columns.host.resolve<HostSample>(),
        columns.powerSource.resolve<PowerSourceSample>(),
        columns.service.resolve<ServiceSample>(),
        columns.task.resolve<TaskSample>(),
    ).withGpuColumns(gpuCount)
}

private inline fun <reified T : Exportable> ColumnSelection.resolve(): List<ExportColumn<T>> {
    val all = ExportColumn.getAllLoadedColumns<T>()
    return when (this) {
        AllColumns -> all
        is OnlyColumns -> all.filter { it.name in columns }
    }
}

private fun ExportSpec.toFilesToExport(): Map<OutputFileSpec, Boolean> {
    val enabled = filesToExport.toSet()
    return OutputFileSpec.entries.associateWith { it in enabled }
}
