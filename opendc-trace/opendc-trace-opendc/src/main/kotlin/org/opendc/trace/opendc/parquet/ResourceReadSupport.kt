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

package org.opendc.trace.opendc.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.api.InitContext
import org.apache.parquet.hadoop.api.ReadSupport
import org.apache.parquet.io.api.RecordMaterializer
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.Types
import org.opendc.trace.TableColumn
import org.opendc.trace.conv.RESOURCE_CPU_CAPACITY
import org.opendc.trace.conv.RESOURCE_CPU_COUNT
import org.opendc.trace.conv.RESOURCE_ID
import org.opendc.trace.conv.RESOURCE_MEM_CAPACITY
import org.opendc.trace.conv.RESOURCE_START_TIME
import org.opendc.trace.conv.RESOURCE_STOP_TIME

/**
 * A [ReadSupport] instance for [Resource] objects.
 */
internal class ResourceReadSupport(private val projection: List<String>?) : ReadSupport<Resource>() {
    /**
     * Mapping from field names to [TableColumn]s.
     */
    private val fieldMap = mapOf(
        "id" to RESOURCE_ID,
        "submissionTime" to RESOURCE_START_TIME,
        "start_time" to RESOURCE_START_TIME,
        "endTime" to RESOURCE_STOP_TIME,
        "stop_time" to RESOURCE_STOP_TIME,
        "maxCores" to RESOURCE_CPU_COUNT,
        "cpu_count" to RESOURCE_CPU_COUNT,
        "cpu_capacity" to RESOURCE_CPU_CAPACITY,
        "requiredMemory" to RESOURCE_MEM_CAPACITY,
        "mem_capacity" to RESOURCE_MEM_CAPACITY
    )

    override fun init(context: InitContext): ReadContext {
        val projectedSchema =
            if (projection != null) {
                Types.buildMessage()
                    .apply {
                        val projectionSet = projection.toSet()

                        for (field in READ_SCHEMA.fields) {
                            val col = fieldMap[field.name] ?: continue
                            if (col in projectionSet) {
                                addField(field)
                            }
                        }
                    }
                    .named(READ_SCHEMA.name)
            } else {
                READ_SCHEMA
            }

        return ReadContext(projectedSchema)
    }

    override fun prepareForRead(
        configuration: Configuration,
        keyValueMetaData: Map<String, String>,
        fileSchema: MessageType,
        readContext: ReadContext
    ): RecordMaterializer<Resource> = ResourceRecordMaterializer(readContext.requestedSchema)

    companion object {
        /**
         * Parquet read schema (version 2.0) for the "resources" table in the trace.
         */
        @JvmStatic
        val READ_SCHEMA_V2_0: MessageType = Types.buildMessage()
            .addFields(
                Types
                    .required(PrimitiveType.PrimitiveTypeName.BINARY)
                    .`as`(LogicalTypeAnnotation.stringType())
                    .named("id"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT64)
                    .`as`(LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS))
                    .named("submissionTime"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT64)
                    .`as`(LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS))
                    .named("endTime"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT32)
                    .named("maxCores"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT64)
                    .named("requiredMemory")
            )
            .named("resource")

        /**
         * Parquet read schema (version 2.1) for the "resources" table in the trace.
         */
        @JvmStatic
        val READ_SCHEMA_V2_1: MessageType = Types.buildMessage()
            .addFields(
                Types
                    .required(PrimitiveType.PrimitiveTypeName.BINARY)
                    .`as`(LogicalTypeAnnotation.stringType())
                    .named("id"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT64)
                    .`as`(LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS))
                    .named("start_time"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT64)
                    .`as`(LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS))
                    .named("stop_time"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT32)
                    .named("cpu_count"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.DOUBLE)
                    .named("cpu_capacity"),
                Types
                    .required(PrimitiveType.PrimitiveTypeName.INT64)
                    .named("mem_capacity")
            )
            .named("resource")

        /**
         * Parquet read schema for the "resources" table in the trace.
         */
        @JvmStatic
        val READ_SCHEMA: MessageType = READ_SCHEMA_V2_0
            .union(READ_SCHEMA_V2_1)
    }
}
