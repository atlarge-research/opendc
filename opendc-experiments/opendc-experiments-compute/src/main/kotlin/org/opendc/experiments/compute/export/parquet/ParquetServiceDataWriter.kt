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
import org.apache.parquet.hadoop.api.WriteSupport
import org.apache.parquet.io.api.RecordConsumer
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.Types
import org.opendc.experiments.compute.telemetry.table.ServiceTableReader
import java.io.File

/**
 * A Parquet event writer for [ServiceTableReader]s.
 */
public class ParquetServiceDataWriter(path: File, bufferSize: Int) :
    ParquetDataWriter<ServiceTableReader>(path, ServiceDataWriteSupport(), bufferSize) {

    override fun toString(): String = "service-writer"

    /**
     * A [WriteSupport] implementation for a [ServiceTableReader].
     */
    private class ServiceDataWriteSupport : WriteSupport<ServiceTableReader>() {
        lateinit var recordConsumer: RecordConsumer

        override fun init(configuration: Configuration): WriteContext {
            return WriteContext(SCHEMA, emptyMap())
        }

        override fun prepareForWrite(recordConsumer: RecordConsumer) {
            this.recordConsumer = recordConsumer
        }

        override fun write(record: ServiceTableReader) {
            write(recordConsumer, record)
        }

        private fun write(consumer: RecordConsumer, data: ServiceTableReader) {
            consumer.startMessage()

            consumer.startField("timestamp", 0)
            consumer.addLong(data.timestamp.toEpochMilli())
            consumer.endField("timestamp", 0)

            consumer.startField("hosts_up", 1)
            consumer.addInteger(data.hostsUp)
            consumer.endField("hosts_up", 1)

            consumer.startField("hosts_down", 2)
            consumer.addInteger(data.hostsDown)
            consumer.endField("hosts_down", 2)

            consumer.startField("servers_pending", 3)
            consumer.addInteger(data.serversPending)
            consumer.endField("servers_pending", 3)

            consumer.startField("servers_active", 4)
            consumer.addInteger(data.serversActive)
            consumer.endField("servers_active", 4)

            consumer.startField("attempts_success", 5)
            consumer.addInteger(data.attemptsSuccess)
            consumer.endField("attempts_pending", 5)

            consumer.startField("attempts_failure", 6)
            consumer.addInteger(data.attemptsFailure)
            consumer.endField("attempts_failure", 6)

            consumer.startField("attempts_error", 7)
            consumer.addInteger(data.attemptsError)
            consumer.endField("attempts_error", 7)

            consumer.endMessage()
        }
    }

    private companion object {
        private val SCHEMA: MessageType = Types.buildMessage()
            .addFields(
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT64)
                    .`as`(LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS))
                    .named("timestamp"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT32)
                    .named("hosts_up"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT32)
                    .named("hosts_down"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT32)
                    .named("servers_pending"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT32)
                    .named("servers_active"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT32)
                    .named("attempts_success"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT32)
                    .named("attempts_failure"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT32)
                    .named("attempts_error")
            )
            .named("service")
    }
}
