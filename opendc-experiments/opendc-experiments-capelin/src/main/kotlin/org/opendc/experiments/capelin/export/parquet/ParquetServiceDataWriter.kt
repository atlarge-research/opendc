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
import org.apache.avro.generic.GenericRecordBuilder
import org.opendc.telemetry.compute.table.ServiceData
import java.io.File

/**
 * A Parquet event writer for [ServiceData]s.
 */
public class ParquetServiceDataWriter(path: File, bufferSize: Int) :
    ParquetDataWriter<ServiceData>(path, SCHEMA, bufferSize) {

    override fun convert(builder: GenericRecordBuilder, data: ServiceData) {
        builder["timestamp"] = data.timestamp
        builder["host_total_count"] = data.hostCount
        builder["host_available_count"] = data.activeHostCount
        builder["instance_total_count"] = data.instanceCount
        builder["instance_active_count"] = data.runningInstanceCount
        builder["instance_inactive_count"] = data.finishedInstanceCount
        builder["instance_waiting_count"] = data.queuedInstanceCount
        builder["instance_failed_count"] = data.failedInstanceCount
    }

    override fun toString(): String = "service-writer"

    companion object {
        private val SCHEMA: Schema = SchemaBuilder
            .record("service")
            .namespace("org.opendc.telemetry.compute")
            .fields()
            .requiredLong("timestamp")
            .requiredInt("host_total_count")
            .requiredInt("host_available_count")
            .requiredInt("instance_total_count")
            .requiredInt("instance_active_count")
            .requiredInt("instance_inactive_count")
            .requiredInt("instance_waiting_count")
            .requiredInt("instance_failed_count")
            .endRecord()
    }
}
