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

import org.opendc.compute.simulator.telemetry.OutputFiles
import org.opendc.compute.simulator.telemetry.parquet.ComputeExportConfig
import org.opendc.compute.simulator.telemetry.parquet.withGpuColumns
import org.opendc.compute.simulator.telemetry.table.battery.BatteryTableReader
import org.opendc.compute.simulator.telemetry.table.host.HostTableReader
import org.opendc.compute.simulator.telemetry.table.powerSource.PowerSourceTableReader
import org.opendc.compute.simulator.telemetry.table.service.ServiceTableReader
import org.opendc.compute.simulator.telemetry.table.task.TaskTableReader
import org.opendc.sdk.model.export.AllColumns
import org.opendc.sdk.model.export.ColumnSelection
import org.opendc.sdk.model.export.ExportSpec
import org.opendc.sdk.model.export.OnlyColumns
import org.opendc.sdk.model.export.OutputFileSpec
import org.opendc.trace.util.parquet.exporter.ExportColumn
import org.opendc.trace.util.parquet.exporter.Exportable
import java.time.Duration

/** The engine export settings derived from an SDK [ExportSpec]. */
internal data class ExportSettings(
    val config: ComputeExportConfig,
    val filesToExport: Map<OutputFiles, Boolean>,
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
        columns.host.resolve<HostTableReader>(),
        columns.task.resolve<TaskTableReader>(),
        columns.powerSource.resolve<PowerSourceTableReader>(),
        columns.battery.resolve<BatteryTableReader>(),
        columns.service.resolve<ServiceTableReader>(),
    ).withGpuColumns(gpuCount)
}

private inline fun <reified T : Exportable> ColumnSelection.resolve(): List<ExportColumn<T>> {
    val all = ExportColumn.getAllLoadedColumns<T>()
    return when (this) {
        AllColumns -> all
        is OnlyColumns -> all.filter { it.name in columns }
    }
}

private fun ExportSpec.toFilesToExport(): Map<OutputFiles, Boolean> {
    val enabled = filesToExport.map { it.toEngineOutputFiles() }.toSet()
    return OutputFiles.entries.associateWith { it in enabled }
}

internal fun OutputFileSpec.toEngineOutputFiles(): OutputFiles =
    when (this) {
        OutputFileSpec.HOST -> OutputFiles.HOST
        OutputFileSpec.TASK -> OutputFiles.TASK
        OutputFileSpec.POWER_SOURCE -> OutputFiles.POWER_SOURCE
        OutputFileSpec.BATTERY -> OutputFiles.BATTERY
        OutputFileSpec.SERVICE -> OutputFiles.SERVICE
    }
