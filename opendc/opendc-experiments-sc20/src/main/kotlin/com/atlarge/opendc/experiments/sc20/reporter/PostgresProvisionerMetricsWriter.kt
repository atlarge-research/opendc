/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.experiments.sc20.reporter

import de.bytefish.pgbulkinsert.row.SimpleRow
import de.bytefish.pgbulkinsert.row.SimpleRowWriter
import javax.sql.DataSource

/**
 * A [PostgresMetricsWriter] for persisting [ProvisionerMetrics].
 */
public class PostgresProvisionerMetricsWriter(ds: DataSource, parallelism: Int, batchSize: Int) :
    PostgresMetricsWriter<ProvisionerMetrics>(ds, parallelism, batchSize) {

    override val table: SimpleRowWriter.Table = SimpleRowWriter.Table(
        "provisioner_metrics",
        *arrayOf(
            "scenario_id",
            "run_id",
            "timestamp",
            "host_total_count",
            "host_available_count",
            "vm_total_count",
            "vm_active_count",
            "vm_inactive_count",
            "vm_waiting_count",
            "vm_failed_count"
        )
    )

    override fun persist(action: Action.Write<ProvisionerMetrics>, row: SimpleRow) {
        row.setLong("scenario_id", action.scenario)
        row.setInteger("run_id", action.run)
        row.setLong("timestamp", action.metrics.time)
        row.setInteger("host_total_count", action.metrics.totalHostCount)
        row.setInteger("host_available_count", action.metrics.availableHostCount)
        row.setInteger("vm_total_count", action.metrics.totalVmCount)
        row.setInteger("vm_active_count", action.metrics.activeVmCount)
        row.setInteger("vm_inactive_count", action.metrics.inactiveVmCount)
        row.setInteger("vm_waiting_count", action.metrics.waitingVmCount)
        row.setInteger("vm_failed_count", action.metrics.failedVmCount)
    }

    override fun toString(): String = "provisioner-writer"
}
