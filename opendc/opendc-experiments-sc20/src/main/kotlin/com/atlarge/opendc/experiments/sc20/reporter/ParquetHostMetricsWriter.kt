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

import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import java.io.File

private val schema: Schema = SchemaBuilder
    .record("host_metrics")
    .namespace("com.atlarge.opendc.experiments.sc20")
    .fields()
    .name("scenario_id").type().longType().noDefault()
    .name("run_id").type().intType().noDefault()
    .name("timestamp").type().longType().noDefault()
    .name("duration").type().longType().noDefault()
    .name("host_id").type().stringType().noDefault()
    .name("state").type().stringType().noDefault()
    .name("vm_count").type().intType().noDefault()
    .name("requested_burst").type().longType().noDefault()
    .name("granted_burst").type().longType().noDefault()
    .name("overcommissioned_burst").type().longType().noDefault()
    .name("interfered_burst").type().longType().noDefault()
    .name("cpu_usage").type().doubleType().noDefault()
    .name("cpu_demand").type().doubleType().noDefault()
    .name("power_draw").type().doubleType().noDefault()
    .endRecord()

public class ParquetHostMetricsWriter(path: File, batchSize: Int) :
    ParquetMetricsWriter<HostMetrics>(path, schema, batchSize) {

    override fun persist(action: Action.Write<HostMetrics>, row: GenericData.Record) {
        row.put("scenario_id", action.scenario)
        row.put("run_id", action.run)
        row.put("host_id", action.metrics.host.name)
        row.put("state", action.metrics.host.state.name)
        row.put("timestamp", action.metrics.time)
        row.put("duration", action.metrics.duration)
        row.put("vm_count", action.metrics.vmCount)
        row.put("requested_burst", action.metrics.requestedBurst)
        row.put("granted_burst", action.metrics.grantedBurst)
        row.put("overcommissioned_burst", action.metrics.overcommissionedBurst)
        row.put("interfered_burst", action.metrics.interferedBurst)
        row.put("cpu_usage", action.metrics.cpuUsage)
        row.put("cpu_demand", action.metrics.cpuDemand)
        row.put("power_draw", action.metrics.powerDraw)
    }

    override fun toString(): String = "host-writer"
}
