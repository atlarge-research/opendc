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

package org.opendc.experiments.capelin.export.parquet

import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import org.opendc.telemetry.compute.table.HostData
import java.io.File

/**
 * A Parquet event writer for [HostData]s.
 */
public class ParquetHostDataWriter(path: File, bufferSize: Int) :
    ParquetDataWriter<HostData>(path, schema, convert, bufferSize) {

    override fun toString(): String = "host-writer"

    public companion object {
        private val convert: (HostData, GenericData.Record) -> Unit = { data, record ->
            record.put("host_id", data.host.name)
            record.put("state", data.host.state.name)
            record.put("timestamp", data.timestamp)
            record.put("total_work", data.totalWork)
            record.put("granted_work", data.grantedWork)
            record.put("overcommitted_work", data.overcommittedWork)
            record.put("interfered_work", data.interferedWork)
            record.put("cpu_usage", data.cpuUsage)
            record.put("cpu_demand", data.cpuDemand)
            record.put("power_draw", data.powerDraw)
            record.put("instance_count", data.instanceCount)
            record.put("cores", data.host.model.cpuCount)
        }

        private val schema: Schema = SchemaBuilder
            .record("host")
            .namespace("org.opendc.telemetry.compute")
            .fields()
            .name("timestamp").type().longType().noDefault()
            .name("host_id").type().stringType().noDefault()
            .name("state").type().stringType().noDefault()
            .name("requested_work").type().longType().noDefault()
            .name("granted_work").type().longType().noDefault()
            .name("overcommitted_work").type().longType().noDefault()
            .name("interfered_work").type().longType().noDefault()
            .name("cpu_usage").type().doubleType().noDefault()
            .name("cpu_demand").type().doubleType().noDefault()
            .name("power_draw").type().doubleType().noDefault()
            .name("instance_count").type().intType().noDefault()
            .name("cores").type().intType().noDefault()
            .endRecord()
    }
}
