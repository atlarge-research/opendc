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
 * A [PostgresMetricsWriter] for persisting [HostMetrics].
 */
public class PostgresHostMetricsWriter(ds: DataSource, batchSize: Int) :
    PostgresMetricsWriter<HostMetrics>(ds, batchSize) {
    override fun createStatement(conn: Connection): PreparedStatement {
        return conn.prepareStatement("INSERT INTO host_metrics (scenario_id, run_id, host_id, state, timestamp, duration, vm_count, requested_burst, granted_burst, overcommissioned_burst, interfered_burst, cpu_usage, cpu_demand, power_draw) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
    }

    override fun persist(action: Action.Write<HostMetrics>, stmt: PreparedStatement) {
        stmt.setLong(1, action.scenario)
        stmt.setInt(2, action.run)
        stmt.setString(3, action.metrics.host.name)
        stmt.setString(4, action.metrics.host.state.name)
        stmt.setTimestamp(5, Timestamp(action.metrics.time))
        stmt.setLong(6, action.metrics.duration)
        stmt.setInt(7, action.metrics.vmCount)
        stmt.setLong(8, action.metrics.requestedBurst)
        stmt.setLong(9, action.metrics.grantedBurst)
        stmt.setLong(10, action.metrics.overcommissionedBurst)
        stmt.setLong(11, action.metrics.interferedBurst)
        stmt.setDouble(12, action.metrics.cpuUsage)
        stmt.setDouble(13, action.metrics.cpuDemand)
        stmt.setDouble(14, action.metrics.powerDraw)
    }
}
