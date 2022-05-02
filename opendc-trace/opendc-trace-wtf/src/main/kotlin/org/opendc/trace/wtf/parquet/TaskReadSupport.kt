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

package org.opendc.trace.wtf.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.api.InitContext
import org.apache.parquet.hadoop.api.ReadSupport
import org.apache.parquet.io.api.RecordMaterializer
import org.apache.parquet.schema.*

/**
 * A [ReadSupport] instance for [Task] objects.
 */
internal class TaskReadSupport : ReadSupport<Task>() {
    override fun init(context: InitContext): ReadContext {
        return ReadContext(READ_SCHEMA)
    }

    override fun prepareForRead(
        configuration: Configuration,
        keyValueMetaData: Map<String, String>,
        fileSchema: MessageType,
        readContext: ReadContext
    ): RecordMaterializer<Task> = TaskRecordMaterializer(readContext.requestedSchema)

    companion object {
        /**
         * Parquet read schema for the "tasks" table in the trace.
         */
        @JvmStatic
        val READ_SCHEMA: MessageType = Types.buildMessage()
            .addFields(
                Types
                    .optional(PrimitiveType.PrimitiveTypeName.INT64)
                    .named("id"),
                Types
                    .optional(PrimitiveType.PrimitiveTypeName.INT64)
                    .named("workflow_id"),
                Types
                    .optional(PrimitiveType.PrimitiveTypeName.INT64)
                    .`as`(LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS))
                    .named("ts_submit"),
                Types
                    .optional(PrimitiveType.PrimitiveTypeName.INT64)
                    .named("wait_time"),
                Types
                    .optional(PrimitiveType.PrimitiveTypeName.INT64)
                    .named("runtime"),
                Types
                    .optional(PrimitiveType.PrimitiveTypeName.DOUBLE)
                    .named("resource_amount_requested"),
                Types
                    .optional(PrimitiveType.PrimitiveTypeName.INT32)
                    .named("user_id"),
                Types
                    .optional(PrimitiveType.PrimitiveTypeName.INT32)
                    .named("group_id"),
                Types
                    .buildGroup(Type.Repetition.OPTIONAL)
                    .addField(
                        Types.repeatedGroup()
                            .addField(Types.optional(PrimitiveType.PrimitiveTypeName.INT64).named("item"))
                            .named("list")
                    )
                    .`as`(LogicalTypeAnnotation.listType())
                    .named("children"),
                Types
                    .buildGroup(Type.Repetition.OPTIONAL)
                    .addField(
                        Types.repeatedGroup()
                            .addField(Types.optional(PrimitiveType.PrimitiveTypeName.INT64).named("item"))
                            .named("list")
                    )
                    .`as`(LogicalTypeAnnotation.listType())
                    .named("parents"),
            )
            .named("task")
    }
}
