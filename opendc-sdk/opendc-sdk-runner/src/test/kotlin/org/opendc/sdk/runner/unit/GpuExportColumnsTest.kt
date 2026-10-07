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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.sdk.model.telemetry.ExportColumnsSpec
import org.opendc.sdk.model.telemetry.ExportSpec
import org.opendc.sdk.model.telemetry.OnlyColumns
import org.opendc.sdk.runner.factory.toExportSettings
import org.opendc.sdk.runner.telemetry.parquet.DfltHostExportColumns
import org.opendc.sdk.runner.telemetry.parquet.DfltTaskExportColumns

/**
 * Test suite for the GPU columns, which are derived per simulation from its topology and column selection.
 */
class GpuExportColumnsTest {
    private val taskGpuColumns = DfltTaskExportColumns.GPU_COLUMNS.map { it.name }

    @Test
    fun `GPU columns follow the topology of each simulation`() {
        // Built one after another in the same JVM, as when running several scenarios
        val withGpu = gpuColumns(ExportSpec(), gpuCount = 1)
        val withGpuAgain = gpuColumns(ExportSpec(), gpuCount = 1)
        val withoutGpu = gpuColumns(ExportSpec(), gpuCount = 0)

        val expected = hostGpuColumns(count = 1) to taskGpuColumns
        assertAll(
            { assertEquals(expected, withGpu) },
            { assertEquals(expected, withGpuAgain) { "the GPU columns of an earlier simulation should not be repeated" } },
            { assertEquals(emptyList<String>() to emptyList<String>(), withoutGpu) },
        )
    }

    @Test
    fun `hosts get the GPU columns of the host with the most GPUs`() {
        assertEquals(hostGpuColumns(count = 2), gpuColumns(ExportSpec(), gpuCount = 2).first)
    }

    @Test
    fun `a selection without GPU columns has none, also if the topology has GPUs`() {
        val spec = ExportSpec(columns = ExportColumnsSpec(host = OnlyColumns(setOf("cpu_usage")), task = OnlyColumns(setOf("cpu_usage"))))

        val config = spec.toExportSettings(gpuCount = 1).config

        assertAll(
            { assertEquals(setOf("cpu_usage", "host_id", "timestamp"), config.hostExportColumns.map { it.name }.toSet()) },
            { assertEquals(setOf("cpu_usage", "task_id", "timestamp"), config.taskExportColumns.map { it.name }.toSet()) },
        )
    }

    @Test
    fun `a selected GPU column is exported, also if the topology has no GPUs`() {
        val spec = ExportSpec(columns = ExportColumnsSpec(host = OnlyColumns(setOf("gpu_usage")), task = OnlyColumns(setOf("gpu_usage"))))

        val config = spec.toExportSettings(gpuCount = 0).config

        assertAll(
            { assertEquals(setOf("gpu_usage_0", "host_id", "timestamp"), config.hostExportColumns.map { it.name }.toSet()) },
            { assertEquals(setOf("gpu_usage", "task_id", "timestamp"), config.taskExportColumns.map { it.name }.toSet()) },
        )
    }

    /**
     * The names of the host and task GPU columns that [spec] exports for a topology with at most [gpuCount] GPUs per host,
     * as lists so that a repeated column shows.
     */
    private fun gpuColumns(
        spec: ExportSpec,
        gpuCount: Int,
    ): Pair<List<String>, List<String>> {
        val config = spec.toExportSettings(gpuCount).config
        val host = config.hostExportColumns.map { it.name }.filter { it.startsWith("gpu_") }
        val task = config.taskExportColumns.map { it.name }.filter { it in taskGpuColumns }
        return host to task
    }

    private fun hostGpuColumns(count: Int): List<String> =
        (0 until count).flatMap { i -> DfltHostExportColumns.GPU_METRICS.map { "${it}_$i" } }
}
