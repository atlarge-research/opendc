/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.experiments.compute.export.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.api.WriteSupport
import org.apache.parquet.io.api.RecordConsumer
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.Types
import org.opendc.experiments.compute.telemetry.table.HostTableReader
import org.opendc.trace.util.parquet.LocalParquetWriter
import java.io.File
import java.util.UUID

/**
 * A Parquet event writer for [HostTableReader]s.
 */
public class ParquetHostDataWriter(path: File, bufferSize: Int) :
    ParquetDataWriter<HostTableReader>(path, HostDataWriteSupport(), bufferSize) {

    override fun buildWriter(builder: LocalParquetWriter.Builder<HostTableReader>): ParquetWriter<HostTableReader> {
        return builder
            .withDictionaryEncoding("host_id", true)
            .build()
    }

    override fun toString(): String = "host-writer"

    /**
     * A [WriteSupport] implementation for a [HostTableReader].
     */
    private class HostDataWriteSupport : WriteSupport<HostTableReader>() {
        lateinit var recordConsumer: RecordConsumer

        override fun init(configuration: Configuration): WriteContext {
            return WriteContext(SCHEMA, emptyMap())
        }

        override fun prepareForWrite(recordConsumer: RecordConsumer) {
            this.recordConsumer = recordConsumer
        }

        override fun write(record: HostTableReader) {
            write(recordConsumer, record)
        }

        private fun write(consumer: RecordConsumer, data: HostTableReader) {
            consumer.startMessage()

            consumer.startField("timestamp", 0)
            consumer.addLong(data.timestamp.toEpochMilli())
            consumer.endField("timestamp", 0)

            consumer.startField("host_id", 1)
            consumer.addBinary(UUID.fromString(data.host.id).toBinary())
            consumer.endField("host_id", 1)

            consumer.startField("uptime", 2)
            consumer.addLong(data.uptime)
            consumer.endField("uptime", 2)

            consumer.startField("downtime", 3)
            consumer.addLong(data.downtime)
            consumer.endField("downtime", 3)

            val bootTime = data.bootTime
            if (bootTime != null) {
                consumer.startField("boot_time", 4)
                consumer.addLong(bootTime.toEpochMilli())
                consumer.endField("boot_time", 4)
            }

            consumer.startField("cpu_count", 5)
            consumer.addInteger(data.host.cpuCount)
            consumer.endField("cpu_count", 5)

            consumer.startField("cpu_limit", 6)
            consumer.addDouble(data.cpuLimit)
            consumer.endField("cpu_limit", 6)

            consumer.startField("cpu_time_active", 7)
            consumer.addLong(data.cpuActiveTime)
            consumer.endField("cpu_time_active", 7)

            consumer.startField("cpu_time_idle", 8)
            consumer.addLong(data.cpuIdleTime)
            consumer.endField("cpu_time_idle", 8)

            consumer.startField("cpu_time_steal", 9)
            consumer.addLong(data.cpuStealTime)
            consumer.endField("cpu_time_steal", 9)

            consumer.startField("cpu_time_lost", 10)
            consumer.addLong(data.cpuLostTime)
            consumer.endField("cpu_time_lost", 10)

            consumer.startField("mem_limit", 11)
            consumer.addLong(data.host.memCapacity)
            consumer.endField("mem_limit", 11)

            consumer.startField("power_total", 12)
            consumer.addDouble(data.powerTotal)
            consumer.endField("power_total", 12)

            consumer.startField("guests_terminated", 13)
            consumer.addInteger(data.guestsTerminated)
            consumer.endField("guests_terminated", 13)

            consumer.startField("guests_running", 14)
            consumer.addInteger(data.guestsRunning)
            consumer.endField("guests_running", 14)

            consumer.startField("guests_error", 15)
            consumer.addInteger(data.guestsError)
            consumer.endField("guests_error", 15)

            consumer.startField("guests_invalid", 16)
            consumer.addInteger(data.guestsInvalid)
            consumer.endField("guests_invalid", 16)

            consumer.endMessage()
        }
    }

    private companion object {
        /**
         * The schema of the host data.
         */
        val SCHEMA: MessageType = Types
            .buildMessage()
            .addFields(
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT64)
                    .`as`(LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS))
                    .named("timestamp"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY)
                    .length(16)
                    .`as`(LogicalTypeAnnotation.uuidType())
                    .named("host_id"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT64)
                    .named("uptime"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT64)
                    .named("downtime"),
                Types
                    .optional(PrimitiveType.PrimitiveTypeName.INT64)
                    .`as`(LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS))
                    .named("boot_time"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT32)
                    .named("cpu_count"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.DOUBLE)
                    .named("cpu_limit"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT64)
                    .named("cpu_time_active"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT64)
                    .named("cpu_time_idle"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT64)
                    .named("cpu_time_steal"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT64)
                    .named("cpu_time_lost"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT64)
                    .named("mem_limit"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.DOUBLE)
                    .named("power_total"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT32)
                    .named("guests_terminated"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT32)
                    .named("guests_running"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT32)
                    .named("guests_error"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT32)
                    .named("guests_invalid")
            )
            .named("host")
    }
}
