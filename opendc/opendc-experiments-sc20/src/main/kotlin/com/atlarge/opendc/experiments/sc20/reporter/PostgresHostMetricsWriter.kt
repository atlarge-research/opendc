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
 * A [PostgresMetricsWriter] for persisting [HostMetrics].
 */
public class PostgresHostMetricsWriter(ds: DataSource, parallelism: Int, batchSize: Int) :
    PostgresMetricsWriter<HostMetrics>(ds, parallelism, batchSize) {

    override val table: SimpleRowWriter.Table = SimpleRowWriter.Table(
        "host_metrics",
        *arrayOf(
            "scenario_id",
            "run_id",
            "host_id",
            "state",
            "timestamp",
            "duration",
            "vm_count",
            "requested_burst",
            "granted_burst",
            "overcommissioned_burst",
            "interfered_burst",
            "cpu_usage",
            "cpu_demand",
            "power_draw"
        )
    )

    override fun persist(action: Action.Write<HostMetrics>, row: SimpleRow) {
        row.setLong("scenario_id", action.scenario)
        row.setInteger("run_id", action.run)
        row.setText("host_id", action.metrics.host.name)
        row.setText("state", action.metrics.host.state.name)
        row.setLong("timestamp", action.metrics.time)
        row.setLong("duration", action.metrics.duration)
        row.setInteger("vm_count", action.metrics.vmCount)
        row.setLong("requested_burst", action.metrics.requestedBurst)
        row.setLong("granted_burst", action.metrics.grantedBurst)
        row.setLong("overcommissioned_burst", action.metrics.overcommissionedBurst)
        row.setLong("interfered_burst", action.metrics.interferedBurst)
        row.setDouble("cpu_usage", action.metrics.cpuUsage)
        row.setDouble("cpu_demand", action.metrics.cpuDemand)
        row.setDouble("power_draw", action.metrics.powerDraw)
    }

    override fun toString(): String = "host-writer"
}
