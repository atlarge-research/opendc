/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.trace.formats.workload.parquet

import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.Type
import org.apache.parquet.schema.Types

private val TASK_SCHEMA_V1: MessageType =
    Types.buildMessage()
        .addFields(
            Types
                .required(PrimitiveType.PrimitiveTypeName.BINARY)
                .`as`(LogicalTypeAnnotation.stringType())
                .named("id"),
            Types
                .required(PrimitiveType.PrimitiveTypeName.INT64)
                .`as`(LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS))
                .named("submission_time"),
            Types
                .required(PrimitiveType.PrimitiveTypeName.INT64)
                .named("duration"),
            Types
                .required(PrimitiveType.PrimitiveTypeName.INT32)
                .named("cpu_count"),
            Types
                .required(PrimitiveType.PrimitiveTypeName.DOUBLE)
                .named("cpu_capacity"),
            Types
                .required(PrimitiveType.PrimitiveTypeName.INT64)
                .named("mem_capacity"),
            Types
                .optional(PrimitiveType.PrimitiveTypeName.INT32)
                .named("gpu_count"),
            Types
                .optional(PrimitiveType.PrimitiveTypeName.DOUBLE)
                .named("gpu_capacity"),
            Types
                .buildGroup(Type.Repetition.OPTIONAL)
                .addField(
                    Types.repeatedGroup()
                        .addField(
                            Types.optional(PrimitiveType.PrimitiveTypeName.BINARY)
                                .`as`(LogicalTypeAnnotation.stringType())
                                .named("element"),
                        )
                        .named("list"),
                )
                .`as`(LogicalTypeAnnotation.listType())
                .named("parents"),
            Types
                .buildGroup(Type.Repetition.OPTIONAL)
                .addField(
                    Types.repeatedGroup()
                        .addField(
                            Types.optional(PrimitiveType.PrimitiveTypeName.BINARY)
                                .`as`(LogicalTypeAnnotation.stringType())
                                .named("element"),
                        )
                        .named("list"),
                )
                .`as`(LogicalTypeAnnotation.listType())
                .named("children"),
            Types
                .optional(PrimitiveType.PrimitiveTypeName.BINARY)
                .`as`(LogicalTypeAnnotation.stringType())
                .named("nature"),
            Types
                .optional(PrimitiveType.PrimitiveTypeName.INT64)
                .named("deadline"),
        )
        .named("resource")

private val TASK_SCHEMA_V2: MessageType =
    Types.buildMessage()
        .addFields(
            Types
                .required(PrimitiveType.PrimitiveTypeName.INT32)
                .named("id"),
            Types
                .optional(PrimitiveType.PrimitiveTypeName.BINARY)
                .`as`(LogicalTypeAnnotation.stringType())
                .named("name"),
            Types
                .required(PrimitiveType.PrimitiveTypeName.INT64)
                .`as`(LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS))
                .named("submission_time"),
            Types
                .required(PrimitiveType.PrimitiveTypeName.INT64)
                .named("duration"),
            Types
                .required(PrimitiveType.PrimitiveTypeName.INT32)
                .named("cpu_count"),
            Types
                .required(PrimitiveType.PrimitiveTypeName.DOUBLE)
                .named("cpu_capacity"),
            Types
                .required(PrimitiveType.PrimitiveTypeName.INT64)
                .named("mem_capacity"),
            Types
                .optional(PrimitiveType.PrimitiveTypeName.INT32)
                .named("gpu_count"),
            Types
                .optional(PrimitiveType.PrimitiveTypeName.DOUBLE)
                .named("gpu_capacity"),
            Types
                .buildGroup(Type.Repetition.OPTIONAL)
                .addField(
                    Types.repeatedGroup()
                        .addField(
                            Types.optional(
                                PrimitiveType.PrimitiveTypeName.INT32,
                            )
                                .named("element"),
                        )
                        .named("list"),
                )
                .`as`(LogicalTypeAnnotation.listType())
                .named("parents"),
            Types
                .buildGroup(Type.Repetition.OPTIONAL)
                .addField(
                    Types.repeatedGroup()
                        .addField(
                            Types.optional(
                                PrimitiveType.PrimitiveTypeName.INT32,
                            )
                                .named("element"),
                        )
                        .named("list"),
                )
                .`as`(LogicalTypeAnnotation.listType())
                .named("children"),
            Types
                .optional(PrimitiveType.PrimitiveTypeName.BINARY)
                .`as`(LogicalTypeAnnotation.stringType())
                .named("nature"),
            Types
                .optional(PrimitiveType.PrimitiveTypeName.INT64)
                .named("deadline"),
        )
        .named("resource")

public val TASK_SCHEMA: MessageType = TASK_SCHEMA_V2
