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

package org.opendc.compute.telemetry.export.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.api.WriteSupport
import org.apache.parquet.io.api.Binary
import org.apache.parquet.io.api.RecordConsumer
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.Types
import org.opendc.compute.telemetry.table.HostTableReader
import org.opendc.trace.util.parquet.LocalParquetWriter
import java.io.File

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
    private class HostDataWriteSupport() : WriteSupport<HostTableReader>() {
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

        private fun write(
            consumer: RecordConsumer,
            data: HostTableReader,
        ) {
            consumer.startMessage()

            consumer.startField("timestamp", 0)
            consumer.addLong(data.timestamp.toEpochMilli())
            consumer.endField("timestamp", 0)

            consumer.startField("absolute_timestamp", 1)
            consumer.addLong(data.absoluteTimestamp.toEpochMilli())
            consumer.endField("absolute_timestamp", 1)

            consumer.startField("host_id", 2)
            consumer.addBinary(Binary.fromString(data.host.id))
            consumer.endField("host_id", 2)

            consumer.startField("cpu_count", 3)
            consumer.addInteger(data.host.cpuCount)
            consumer.endField("cpu_count", 3)

            consumer.startField("mem_capacity", 4)
            consumer.addLong(data.host.memCapacity)
            consumer.endField("mem_capacity", 4)

            consumer.startField("guests_terminated", 5)
            consumer.addInteger(data.guestsTerminated)
            consumer.endField("guests_terminated", 5)

            consumer.startField("guests_running", 6)
            consumer.addInteger(data.guestsRunning)
            consumer.endField("guests_running", 6)

            consumer.startField("guests_error", 7)
            consumer.addInteger(data.guestsError)
            consumer.endField("guests_error", 7)

            consumer.startField("guests_invalid", 8)
            consumer.addInteger(data.guestsInvalid)
            consumer.endField("guests_invalid", 8)

            consumer.startField("cpu_limit", 9)
            consumer.addDouble(data.cpuLimit)
            consumer.endField("cpu_limit", 9)

            consumer.startField("cpu_usage", 10)
            consumer.addDouble(data.cpuUsage)
            consumer.endField("cpu_usage", 10)

            consumer.startField("cpu_demand", 11)
            consumer.addDouble(data.cpuUsage)
            consumer.endField("cpu_demand", 11)

            consumer.startField("cpu_utilization", 12)
            consumer.addDouble(data.cpuUtilization)
            consumer.endField("cpu_utilization", 12)

            consumer.startField("cpu_time_active", 13)
            consumer.addLong(data.cpuActiveTime)
            consumer.endField("cpu_time_active", 13)

            consumer.startField("cpu_time_idle", 14)
            consumer.addLong(data.cpuIdleTime)
            consumer.endField("cpu_time_idle", 14)

            consumer.startField("cpu_time_steal", 15)
            consumer.addLong(data.cpuStealTime)
            consumer.endField("cpu_time_steal", 15)

            consumer.startField("cpu_time_lost", 16)
            consumer.addLong(data.cpuLostTime)
            consumer.endField("cpu_time_lost", 16)

            consumer.startField("power_draw", 17)
            consumer.addDouble(data.powerDraw)
            consumer.endField("power_draw", 17)

            consumer.startField("energy_usage", 18)
            consumer.addDouble(data.energyUsage)
            consumer.endField("energy_usage", 18)

            consumer.startField("carbon_intensity", 19)
            consumer.addDouble(data.carbonIntensity)
            consumer.endField("carbon_intensity", 19)

            consumer.startField("carbon_emission", 20)
            consumer.addDouble(data.carbonEmission)
            consumer.endField("carbon_emission", 20)

            consumer.startField("uptime", 21)
            consumer.addLong(data.uptime)
            consumer.endField("uptime", 21)

            consumer.startField("downtime", 22)
            consumer.addLong(data.downtime)
            consumer.endField("downtime", 22)

            val bootTime = data.bootTime
            if (bootTime != null) {
                consumer.startField("boot_time", 23)
                consumer.addLong(bootTime.toEpochMilli())
                consumer.endField("boot_time", 23)
            }

            consumer.endMessage()
        }
    }

    private companion object {
        /**
         * The schema of the host data.
         */
        val SCHEMA: MessageType =
            Types
                .buildMessage()
                .addFields(
                    Types
                        .required(PrimitiveType.PrimitiveTypeName.INT64)
                        .named("timestamp"),
                    Types
                        .required(PrimitiveType.PrimitiveTypeName.INT64)
                        .named("absolute_timestamp"),
                    Types
                        .required(PrimitiveType.PrimitiveTypeName.BINARY)
                        .`as`(LogicalTypeAnnotation.stringType())
                        .named("host_id"),
                    Types
                        .required(PrimitiveType.PrimitiveTypeName.INT32)
                        .named("cpu_count"),
                    Types
                        .required(PrimitiveType.PrimitiveTypeName.INT64)
                        .named("mem_capacity"),
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
                        .named("guests_invalid"),
                    Types
                        .required(PrimitiveType.PrimitiveTypeName.DOUBLE)
                        .named("cpu_limit"),
                    Types
                        .required(PrimitiveType.PrimitiveTypeName.DOUBLE)
                        .named("cpu_usage"),
                    Types
                        .required(PrimitiveType.PrimitiveTypeName.DOUBLE)
                        .named("cpu_demand"),
                    Types
                        .required(PrimitiveType.PrimitiveTypeName.DOUBLE)
                        .named("cpu_utilization"),
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
                        .required(PrimitiveType.PrimitiveTypeName.DOUBLE)
                        .named("power_draw"),
                    Types
                        .required(PrimitiveType.PrimitiveTypeName.DOUBLE)
                        .named("energy_usage"),
                    Types
                        .required(PrimitiveType.PrimitiveTypeName.DOUBLE)
                        .named("carbon_intensity"),
                    Types
                        .required(PrimitiveType.PrimitiveTypeName.DOUBLE)
                        .named("carbon_emission"),
                    Types
                        .required(PrimitiveType.PrimitiveTypeName.INT64)
                        .named("uptime"),
                    Types
                        .required(PrimitiveType.PrimitiveTypeName.INT64)
                        .named("downtime"),
                    Types
                        .optional(PrimitiveType.PrimitiveTypeName.INT64)
                        .named("boot_time"),
                )
                .named("host")
    }
}
