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

package org.opendc.web.launcher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.sdk.model.telemetry.OutputFileSpec
import org.opendc.sdk.runner.telemetry.parquet.DfltBatteryExportColumns
import org.opendc.sdk.runner.telemetry.parquet.DfltHostExportColumns
import org.opendc.sdk.runner.telemetry.parquet.DfltPowerSourceExportColumns
import org.opendc.sdk.runner.telemetry.parquet.DfltServiceExportColumns
import org.opendc.sdk.runner.telemetry.table.battery.BatterySample
import org.opendc.sdk.runner.telemetry.table.host.HostSample
import org.opendc.sdk.runner.telemetry.table.powerSource.PowerSourceSample
import org.opendc.sdk.runner.telemetry.table.service.ServiceSample
import org.opendc.trace.parquet.exporter.ExportColumn

/**
 * The catalog names columns the simulator writes and tables the frontend reads. Both are spelled out
 * somewhere else too, and a metric whose column has been renamed under it does not fail loudly: it
 * reports an empty line where a chart expects one.
 */
class ResultMetricTest {
    @Test
    fun `every metric names a column the simulator actually exports`() {
        // Loading the objects is what registers their columns; nothing else in this process has.
        DfltHostExportColumns
        DfltServiceExportColumns
        DfltPowerSourceExportColumns
        DfltBatteryExportColumns
        val exported =
            mapOf(
                OutputFileSpec.HOST to ExportColumn.getAllLoadedColumns<HostSample>().map { it.name },
                OutputFileSpec.SERVICE to ExportColumn.getAllLoadedColumns<ServiceSample>().map { it.name },
                OutputFileSpec.POWER_SOURCE to ExportColumn.getAllLoadedColumns<PowerSourceSample>().map { it.name },
                OutputFileSpec.BATTERY to ExportColumn.getAllLoadedColumns<BatterySample>().map { it.name },
            )

        for (metric in ResultMetric.entries) {
            val columns = exported.getValue(metric.table)
            for (read in metric.columns) {
                assertTrue(read in columns) { "${metric.id} reads $read, which ${metric.table} does not export" }
            }
        }
    }

    // Two fleets getting through identical work must report identical utilization. Averaging each
    // host's own share instead weights a machine holding a tenth of the capacity as heavily as one
    // holding all of it, so the same work reads differently for having been carved up differently.
    @Test
    fun `folds a share by weight rather than by averaging the entities' own shares`() {
        val big = Reading(part = 100.0, whole = 1000.0)
        val small = Reading(part = 10.0, whole = 10.0)

        val folded = ResultMetric.HOST_CPU_UTILIZATION.fold(listOf(big, small))

        assertEquals(110.0 / 1010.0, folded, 1e-9) { "the fleet used 110 of 1010" }
        assertNotEquals(0.55, folded, "an unweighted mean of 10% and 100% is not what the fleet did")
    }

    @Test
    fun `reports nothing supplied as nothing used rather than as a share of nothing`() {
        assertEquals(0.0, ResultMetric.HOST_CPU_UTILIZATION.fold(listOf(Reading(0.0, 0.0))))
    }

    @Test
    fun `no two metrics answer to the same identity, so a chart cannot read the wrong column`() {
        val ids = ResultMetric.entries.map { it.id }

        assertEquals(ids.size, ids.toSet().size) { "duplicate metric ids: $ids" }
    }
}
