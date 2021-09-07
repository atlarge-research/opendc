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
        builder["timestamp"] = data.timestamp.toEpochMilli()

        builder["host_id"] = data.host.id
        builder["num_cpus"] = data.host.cpuCount
        builder["mem_capacity"] = data.host.memCapacity

        builder["uptime"] = data.uptime
        builder["downtime"] = data.downtime
        val bootTime = data.bootTime
        if (bootTime != null) {
            builder["boot_time"] = bootTime.toEpochMilli()
        }

        builder["cpu_limit"] = data.cpuLimit
        builder["cpu_time_active"] = data.cpuActiveTime
        builder["cpu_time_idle"] = data.cpuIdleTime
        builder["cpu_time_steal"] = data.cpuStealTime
        builder["cpu_time_lost"] = data.cpuLostTime

        builder["power_total"] = data.powerTotal

        builder["guests_terminated"] = data.guestsTerminated
        builder["guests_running"] = data.guestsRunning
        builder["guests_error"] = data.guestsError
        builder["guests_invalid"] = data.guestsInvalid
    }

    override fun toString(): String = "host-writer"

    companion object {
        private val SCHEMA: Schema = SchemaBuilder
            .record("host")
            .namespace("org.opendc.telemetry.compute")
            .fields()
            .requiredLong("timestamp")
            .requiredString("host_id")
            .requiredInt("num_cpus")
            .requiredLong("mem_capacity")
            .requiredLong("uptime")
            .requiredLong("downtime")
            .optionalLong("boot_time")
            .requiredDouble("cpu_limit")
            .requiredLong("cpu_time_active")
            .requiredLong("cpu_time_idle")
            .requiredLong("cpu_time_steal")
            .requiredLong("cpu_time_lost")
            .requiredDouble("power_total")
            .requiredInt("guests_terminated")
            .requiredInt("guests_running")
            .requiredInt("guests_error")
            .requiredInt("guests_invalid")
            .endRecord()
    }
}
