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
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.Type
import org.apache.parquet.schema.Types
import org.opendc.trace.conv.TASK_CHILDREN
import org.opendc.trace.conv.TASK_GROUP_ID
import org.opendc.trace.conv.TASK_ID
import org.opendc.trace.conv.TASK_PARENTS
import org.opendc.trace.conv.TASK_REQ_NCPUS
import org.opendc.trace.conv.TASK_RUNTIME
import org.opendc.trace.conv.TASK_SUBMIT_TIME
import org.opendc.trace.conv.TASK_USER_ID
import org.opendc.trace.conv.TASK_WAIT_TIME
import org.opendc.trace.conv.TASK_WORKFLOW_ID

/**
 * A [ReadSupport] instance for [Task] objects.
 *
 * @param projection The projection of the table to read.
 */
internal class TaskReadSupport(private val projection: List<String>?) : ReadSupport<Task>() {
    /**
     * Mapping of table columns to their Parquet column names.
     */
    private val colMap = mapOf(
        TASK_ID to "id",
        TASK_WORKFLOW_ID to "workflow_id",
        TASK_SUBMIT_TIME to "ts_submit",
        TASK_WAIT_TIME to "wait_time",
        TASK_RUNTIME to "runtime",
        TASK_REQ_NCPUS to "resource_amount_requested",
        TASK_PARENTS to "parents",
        TASK_CHILDREN to "children",
        TASK_GROUP_ID to "group_id",
        TASK_USER_ID to "user_id"
    )

    override fun init(context: InitContext): ReadContext {
        val projectedSchema =
            if (projection != null) {
                Types.buildMessage()
                    .apply {
                        val fieldByName = READ_SCHEMA.fields.associateBy { it.name }

                        for (col in projection) {
                            val fieldName = colMap[col] ?: continue
                            addField(fieldByName.getValue(fieldName))
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
                    .named("parents")
            )
            .named("task")
    }
}
