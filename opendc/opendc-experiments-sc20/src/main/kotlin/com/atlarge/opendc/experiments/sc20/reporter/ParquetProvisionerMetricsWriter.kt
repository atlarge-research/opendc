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
    .name("host_total_count").type().intType().noDefault()
    .name("host_available_count").type().intType().noDefault()
    .name("vm_total_count").type().intType().noDefault()
    .name("vm_active_count").type().intType().noDefault()
    .name("vm_inactive_count").type().intType().noDefault()
    .name("vm_waiting_count").type().intType().noDefault()
    .name("vm_failed_count").type().intType().noDefault()
    .endRecord()

public class ParquetProvisionerMetricsWriter(path: File, batchSize: Int) :
    ParquetMetricsWriter<ProvisionerMetrics>(path, schema, batchSize) {

    override fun persist(action: Action.Write<ProvisionerMetrics>, row: GenericData.Record) {
        row.put("scenario_id", action.scenario)
        row.put("run_id", action.run)
        row.put("timestamp", action.metrics.time)
        row.put("host_total_count", action.metrics.totalHostCount)
        row.put("host_available_count", action.metrics.availableHostCount)
        row.put("vm_total_count", action.metrics.totalVmCount)
        row.put("vm_active_count", action.metrics.activeVmCount)
        row.put("vm_inactive_count", action.metrics.inactiveVmCount)
        row.put("vm_waiting_count", action.metrics.waitingVmCount)
        row.put("vm_failed_count", action.metrics.failedVmCount)
    }

    override fun toString(): String = "host-writer"
}
