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

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Timestamp
import javax.sql.DataSource

/**
 * A [PostgresMetricsWriter] for persisting [ProvisionerMetrics].
 */
public class PostgresProvisionerMetricsWriter(ds: DataSource, batchSize: Int) :
    PostgresMetricsWriter<ProvisionerMetrics>(ds, batchSize) {
    override fun createStatement(conn: Connection): PreparedStatement {
        return conn.prepareStatement("INSERT INTO provisioner_metrics (scenario_id, run_id, timestamp, host_total_count, host_available_count, vm_total_count, vm_active_count, vm_inactive_count, vm_waiting_count, vm_failed_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
    }

    override fun persist(action: Action.Write<ProvisionerMetrics>, stmt: PreparedStatement) {
        stmt.setLong(1, action.scenario)
        stmt.setInt(2, action.run)
        stmt.setTimestamp(3, Timestamp(action.metrics.time))
        stmt.setInt(4, action.metrics.totalHostCount)
        stmt.setInt(5, action.metrics.availableHostCount)
        stmt.setInt(6, action.metrics.totalVmCount)
        stmt.setInt(7, action.metrics.activeVmCount)
        stmt.setInt(8, action.metrics.inactiveVmCount)
        stmt.setInt(9, action.metrics.waitingVmCount)
        stmt.setInt(10, action.metrics.failedVmCount)
    }

    override fun toString(): String = "provisioner-writer"
}
