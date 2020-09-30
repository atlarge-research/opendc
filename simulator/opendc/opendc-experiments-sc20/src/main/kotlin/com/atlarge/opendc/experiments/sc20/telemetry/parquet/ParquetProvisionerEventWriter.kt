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

package com.atlarge.opendc.experiments.sc20.telemetry.parquet

import com.atlarge.opendc.experiments.sc20.telemetry.ProvisionerEvent
import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import java.io.File

/**
 * A Parquet event writer for [ProvisionerEvent]s.
 */
public class ParquetProvisionerEventWriter(path: File, bufferSize: Int) :
    ParquetEventWriter<ProvisionerEvent>(path, schema, convert, bufferSize) {

    override fun toString(): String = "provisioner-writer"

    companion object {
        val convert: (ProvisionerEvent, GenericData.Record) -> Unit = { event, record ->
            record.put("timestamp", event.timestamp)
            record.put("host_total_count", event.totalHostCount)
            record.put("host_available_count", event.availableHostCount)
            record.put("vm_total_count", event.totalVmCount)
            record.put("vm_active_count", event.activeVmCount)
            record.put("vm_inactive_count", event.inactiveVmCount)
            record.put("vm_waiting_count", event.waitingVmCount)
            record.put("vm_failed_count", event.failedVmCount)
        }

        val schema: Schema = SchemaBuilder
            .record("provisioner_metrics")
            .namespace("com.atlarge.opendc.experiments.sc20")
            .fields()
            .name("timestamp").type().longType().noDefault()
            .name("host_total_count").type().intType().noDefault()
            .name("host_available_count").type().intType().noDefault()
            .name("vm_total_count").type().intType().noDefault()
            .name("vm_active_count").type().intType().noDefault()
            .name("vm_inactive_count").type().intType().noDefault()
            .name("vm_waiting_count").type().intType().noDefault()
            .name("vm_failed_count").type().intType().noDefault()
            .endRecord()
    }
}
