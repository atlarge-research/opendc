/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.experiments.capelin.telemetry.parquet

import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import org.opendc.experiments.capelin.telemetry.HostEvent
import java.io.File

/**
 * A Parquet event writer for [HostEvent]s.
 */
public class ParquetHostEventWriter(path: File, bufferSize: Int) :
    ParquetEventWriter<HostEvent>(path, schema, convert, bufferSize) {

    override fun toString(): String = "host-writer"

    public companion object {
        private val convert: (HostEvent, GenericData.Record) -> Unit = { event, record ->
            // record.put("portfolio_id", event.run.parent.parent.id)
            // record.put("scenario_id", event.run.parent.id)
            // record.put("run_id", event.run.id)
            record.put("host_id", event.host.name)
            record.put("state", event.host.state.name)
            record.put("timestamp", event.timestamp)
            record.put("duration", event.duration)
            record.put("vm_count", event.vmCount)
            record.put("requested_burst", event.requestedBurst)
            record.put("granted_burst", event.grantedBurst)
            record.put("overcommissioned_burst", event.overcommissionedBurst)
            record.put("interfered_burst", event.interferedBurst)
            record.put("cpu_usage", event.cpuUsage)
            record.put("cpu_demand", event.cpuDemand)
            record.put("power_draw", event.powerDraw * (1.0 / 12))
            record.put("cores", event.cores)
        }

        private val schema: Schema = SchemaBuilder
            .record("host_metrics")
            .namespace("org.opendc.experiments.sc20")
            .fields()
            // .name("portfolio_id").type().intType().noDefault()
            // .name("scenario_id").type().intType().noDefault()
            // .name("run_id").type().intType().noDefault()
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
            .name("cores").type().intType().noDefault()
            .endRecord()
    }
}
