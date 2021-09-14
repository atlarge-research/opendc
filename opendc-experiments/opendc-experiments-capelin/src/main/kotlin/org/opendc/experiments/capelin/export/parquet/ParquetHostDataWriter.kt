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
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.ParquetWriter
import org.opendc.telemetry.compute.table.HostData
import java.io.File

/**
 * A Parquet event writer for [HostData]s.
 */
public class ParquetHostDataWriter(path: File, bufferSize: Int) :
    ParquetDataWriter<HostData>(path, SCHEMA, bufferSize) {

    override fun buildWriter(builder: AvroParquetWriter.Builder<GenericData.Record>): ParquetWriter<GenericData.Record> {
        return builder
            .withDictionaryEncoding("host_id", true)
            .build()
    }

    override fun convert(builder: GenericRecordBuilder, data: HostData) {
        builder["timestamp"] = data.timestamp
        builder["host_id"] = data.host.id
        builder["powered_on"] = true
        builder["uptime"] = data.uptime
        builder["downtime"] = data.downtime
        builder["total_work"] = data.totalWork
        builder["granted_work"] = data.grantedWork
        builder["overcommitted_work"] = data.overcommittedWork
        builder["interfered_work"] = data.interferedWork
        builder["cpu_usage"] = data.cpuUsage
        builder["cpu_demand"] = data.cpuDemand
        builder["power_draw"] = data.powerDraw
        builder["num_instances"] = data.instanceCount
        builder["num_cpus"] = data.host.cpuCount
    }

    override fun toString(): String = "host-writer"

    companion object {
        private val SCHEMA: Schema = SchemaBuilder
            .record("host")
            .namespace("org.opendc.telemetry.compute")
            .fields()
            .requiredLong("timestamp")
            .requiredString("host_id")
            .requiredBoolean("powered_on")
            .requiredLong("uptime")
            .requiredLong("downtime")
            .requiredDouble("total_work")
            .requiredDouble("granted_work")
            .requiredDouble("overcommitted_work")
            .requiredDouble("interfered_work")
            .requiredDouble("cpu_usage")
            .requiredDouble("cpu_demand")
            .requiredDouble("power_draw")
            .requiredInt("num_instances")
            .requiredInt("num_cpus")
            .endRecord()
    }
}
