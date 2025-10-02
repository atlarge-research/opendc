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

package org.opendc.trace.formats.workload.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.api.InitContext
import org.apache.parquet.hadoop.api.ReadSupport
import org.apache.parquet.io.api.RecordMaterializer
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.Types
import org.opendc.trace.TableColumn
import org.opendc.trace.conv.TASK_CHILDREN
import org.opendc.trace.conv.TASK_CPU_CAPACITY
import org.opendc.trace.conv.TASK_CPU_COUNT
import org.opendc.trace.conv.TASK_DEADLINE
import org.opendc.trace.conv.TASK_DEFERRABLE
import org.opendc.trace.conv.TASK_DURATION
import org.opendc.trace.conv.TASK_GPU_CAPACITY
import org.opendc.trace.conv.TASK_GPU_COUNT
import org.opendc.trace.conv.TASK_ID
import org.opendc.trace.conv.TASK_MEM_CAPACITY
import org.opendc.trace.conv.TASK_NAME
import org.opendc.trace.conv.TASK_PARENTS
import org.opendc.trace.conv.TASK_SUBMISSION_TIME

/**
 * A [ReadSupport] instance for [Task] objects.
 */
internal class TaskReadSupport(private val projection: List<String>?) : ReadSupport<Task>() {
    /**
     * Mapping from field names to [TableColumn]s.
     */
    private val fieldMap =
        mapOf(
            "id" to TASK_ID,
            "name" to TASK_NAME,
            "submissionTime" to TASK_SUBMISSION_TIME,
            "submission_time" to TASK_SUBMISSION_TIME,
            "duration" to TASK_DURATION,
            "maxCores" to TASK_CPU_COUNT,
            "cpu_count" to TASK_CPU_COUNT,
            "cpu_capacity" to TASK_CPU_CAPACITY,
            "requiredMemory" to TASK_MEM_CAPACITY,
            "mem_capacity" to TASK_MEM_CAPACITY,
            "gpu_count" to TASK_GPU_COUNT,
            "gpu_capacity" to TASK_GPU_CAPACITY,
            "parents" to TASK_PARENTS,
            "children" to TASK_CHILDREN,
            "deferrable" to TASK_DEFERRABLE,
            "deadline" to TASK_DEADLINE,
        )

    override fun init(context: InitContext): ReadContext {
        val projectedSchema =
            if (projection != null) {
                Types.buildMessage()
                    .apply {
                        val projectionSet = projection.toSet()

                        for (field in TASK_SCHEMA.fields) {
                            val col = fieldMap[field.name] ?: continue
                            if (col in projectionSet) {
                                addField(field)
                            }
                        }
                    }
                    .named(TASK_SCHEMA.name)
            } else {
                TASK_SCHEMA
            }

        return ReadContext(projectedSchema)
    }

    override fun prepareForRead(
        configuration: Configuration,
        keyValueMetaData: Map<String, String>,
        fileSchema: MessageType,
        readContext: ReadContext,
    ): RecordMaterializer<Task> = TaskRecordMaterializer(readContext.requestedSchema)
}
