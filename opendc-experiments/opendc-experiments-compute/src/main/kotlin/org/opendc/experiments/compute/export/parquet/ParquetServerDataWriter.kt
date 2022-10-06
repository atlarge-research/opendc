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
import org.opendc.experiments.compute.telemetry.table.ServerTableReader
import org.opendc.trace.util.parquet.LocalParquetWriter
import java.io.File
import java.util.UUID

/**
 * A Parquet event writer for [ServerTableReader]s.
 */
public class ParquetServerDataWriter(path: File, bufferSize: Int) :
    ParquetDataWriter<ServerTableReader>(path, ServerDataWriteSupport(), bufferSize) {

    override fun buildWriter(builder: LocalParquetWriter.Builder<ServerTableReader>): ParquetWriter<ServerTableReader> {
        return builder
            .withDictionaryEncoding("server_id", true)
            .withDictionaryEncoding("host_id", true)
            .build()
    }

    override fun toString(): String = "server-writer"

    /**
     * A [WriteSupport] implementation for a [ServerTableReader].
     */
    private class ServerDataWriteSupport : WriteSupport<ServerTableReader>() {
        lateinit var recordConsumer: RecordConsumer

        override fun init(configuration: Configuration): WriteContext {
            return WriteContext(SCHEMA, emptyMap())
        }

        override fun prepareForWrite(recordConsumer: RecordConsumer) {
            this.recordConsumer = recordConsumer
        }

        override fun write(record: ServerTableReader) {
            write(recordConsumer, record)
        }

        private fun write(consumer: RecordConsumer, data: ServerTableReader) {
            consumer.startMessage()

            consumer.startField("timestamp", 0)
            consumer.addLong(data.timestamp.toEpochMilli())
            consumer.endField("timestamp", 0)

            consumer.startField("server_id", 1)
            consumer.addBinary(UUID.fromString(data.server.id).toBinary())
            consumer.endField("server_id", 1)

            val hostId = data.host?.id
            if (hostId != null) {
                consumer.startField("host_id", 2)
                consumer.addBinary(UUID.fromString(hostId).toBinary())
                consumer.endField("host_id", 2)
            }

            consumer.startField("uptime", 3)
            consumer.addLong(data.uptime)
            consumer.endField("uptime", 3)

            consumer.startField("downtime", 4)
            consumer.addLong(data.downtime)
            consumer.endField("downtime", 4)

            val bootTime = data.bootTime
            if (bootTime != null) {
                consumer.startField("boot_time", 5)
                consumer.addLong(bootTime.toEpochMilli())
                consumer.endField("boot_time", 5)
            }

            val provisionTime = data.provisionTime
            if (provisionTime != null) {
                consumer.startField("provision_time", 6)
                consumer.addLong(provisionTime.toEpochMilli())
                consumer.endField("provision_time", 6)
            }

            consumer.startField("cpu_count", 7)
            consumer.addInteger(data.server.cpuCount)
            consumer.endField("cpu_count", 7)

            consumer.startField("cpu_limit", 8)
            consumer.addDouble(data.cpuLimit)
            consumer.endField("cpu_limit", 8)

            consumer.startField("cpu_time_active", 9)
            consumer.addLong(data.cpuActiveTime)
            consumer.endField("cpu_time_active", 9)

            consumer.startField("cpu_time_idle", 10)
            consumer.addLong(data.cpuIdleTime)
            consumer.endField("cpu_time_idle", 10)

            consumer.startField("cpu_time_steal", 11)
            consumer.addLong(data.cpuStealTime)
            consumer.endField("cpu_time_steal", 11)

            consumer.startField("cpu_time_lost", 12)
            consumer.addLong(data.cpuLostTime)
            consumer.endField("cpu_time_lost", 12)

            consumer.startField("mem_limit", 13)
            consumer.addLong(data.server.memCapacity)
            consumer.endField("mem_limit", 13)

            consumer.endMessage()
        }
    }

    private companion object {
        /**
         * The schema of the server data.
         */
        val SCHEMA: MessageType = Types.buildMessage()
            .addFields(
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT64)
                    .`as`(LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS))
                    .named("timestamp"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY)
                    .length(16)
                    .`as`(LogicalTypeAnnotation.uuidType())
                    .named("server_id"),
                Types
                    .optional(PrimitiveType.PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY)
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
                    .named("provision_time"),
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
                    .named("mem_limit")
            )
            .named("server")
    }
}
