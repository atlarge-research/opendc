/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.sdk.runner.unit

import org.apache.parquet.example.data.Group
import org.apache.parquet.hadoop.example.GroupReadSupport
import org.apache.parquet.schema.Type
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.sdk.runner.telemetry.parquet.ComputeExportConfig
import org.opendc.sdk.runner.telemetry.parquet.DfltBatteryExportColumns
import org.opendc.sdk.runner.telemetry.parquet.DfltHostExportColumns
import org.opendc.sdk.runner.telemetry.parquet.DfltTaskExportColumns
import org.opendc.sdk.runner.telemetry.table.battery.BatterySample
import org.opendc.sdk.runner.telemetry.table.host.HostSample
import org.opendc.sdk.runner.telemetry.table.task.TaskSample
import org.opendc.simulator.compute.models.HostState
import org.opendc.simulator.compute.power.batteries.BatteryState
import org.opendc.simulator.compute.task.TaskState
import org.opendc.trace.parquet.LocalParquetReader
import org.opendc.trace.parquet.exporter.ExportColumn
import org.opendc.trace.parquet.exporter.Exportable
import org.opendc.trace.parquet.exporter.Exporter
import java.nio.file.Files
import java.time.Instant

/**
 * Test suite for the default export columns: the task, host and battery states, which are required string columns, and
 * which task columns may be null.
 */
class ExportColumnsTest {
    @Test
    fun `task states are exported by name`() {
        val samples = TaskState.entries.mapIndexed { i, state -> TaskSample(taskId = i, taskState = state) }

        val rows = writeAndRead(samples, listOf(DfltTaskExportColumns.TASK_ID, DfltTaskExportColumns.TASK_STATE))

        assertStates(TaskState.entries.map { it.name }, rows, "task_state")
    }

    @Test
    fun `host states are exported by name`() {
        val samples = HostState.entries.mapIndexed { i, state -> HostSample(hostId = i, hostState = state) }

        val rows = writeAndRead(samples, listOf(DfltHostExportColumns.HOST_ID, DfltHostExportColumns.HOST_STATE))

        assertStates(HostState.entries.map { it.name }, rows, "host_state")
    }

    @Test
    fun `battery states are exported by name`() {
        val samples = BatteryState.entries.mapIndexed { i, state -> BatterySample(batteryId = i, batteryState = state) }

        val rows = writeAndRead(samples, listOf(DfltBatteryExportColumns.BATTERY_ID, DfltBatteryExportColumns.BATTERY_STATE))

        assertStates(BatteryState.entries.map { it.name }, rows, "battery_state")
    }

    @Test
    fun `only the task columns that can be null are optional`() {
        val optional = taskColumns().filter { it.field.isRepetition(Type.Repetition.OPTIONAL) }.map { it.name }.toSet()

        assertEquals(setOf("host_id", "schedule_time", "finish_time"), optional)
    }

    @Test
    fun `a task that has not been placed is exported with nulls in the optional columns`() {
        val rows = writeAndRead(listOf(TaskSample(taskId = 1, timestamp = Instant.EPOCH)), taskColumns())

        val row = rows.single()
        assertAll(
            { assertEquals(1, row.getInteger("task_id", 0)) },
            { assertEquals(0, row.getFieldRepetitionCount("host_id")) { "host_id should be null" } },
            { assertEquals(0, row.getFieldRepetitionCount("schedule_time")) { "schedule_time should be null" } },
            { assertEquals(0, row.getFieldRepetitionCount("finish_time")) { "finish_time should be null" } },
        )
    }

    private fun taskColumns(): List<ExportColumn<TaskSample>> {
        ComputeExportConfig.loadDfltColumns()
        return ExportColumn.getAllLoadedColumns()
    }

    private fun assertStates(
        expected: List<String>,
        rows: List<Group>,
        field: String,
    ) {
        assertAll(
            { assertEquals(Type.Repetition.REQUIRED, rows.first().type.getType(field).repetition) { "$field should be required" } },
            { assertEquals(expected, rows.map { it.getString(field, 0) }) },
        )
    }

    private inline fun <reified T : Exportable> writeAndRead(
        samples: List<T>,
        columns: List<ExportColumn<T>>,
    ): List<Group> {
        val file = Files.createTempFile("opendc-states", ".parquet")
        try {
            Exporter(outputFile = file.toFile(), columns = columns).use { exporter -> samples.forEach(exporter::write) }
            return LocalParquetReader(file, GroupReadSupport()).use { reader -> generateSequence { reader.read() }.toList() }
        } finally {
            Files.deleteIfExists(file)
        }
    }
}
