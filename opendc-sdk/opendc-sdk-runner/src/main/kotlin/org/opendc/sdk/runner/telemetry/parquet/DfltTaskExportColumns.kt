/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.sdk.runner.telemetry.parquet

import org.apache.parquet.io.api.Binary
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BINARY
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.FLOAT
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT32
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT64
import org.apache.parquet.schema.Types
import org.opendc.sdk.runner.telemetry.table.task.TaskSample
import org.opendc.simulator.compute.task.TaskState
import org.opendc.trace.parquet.exporter.ExportColumn

/**
 * This object wraps the [ExportColumn]s to solves ambiguity for field
 * names that are included in more than 1 exportable
 *
 * Additionally, it allows to load all the fields at once by just its symbol,
 * so that these columns can be deserialized. Additional fields can be added
 * from anywhere, and they are deserializable as long as they are loaded by the jvm.
 *
 * ```kotlin
 * ...
 * // Loads the column
 * DfltTaskExportColumns
 * ...
 * ```
 */
public object DfltTaskExportColumns {
    public val TIMESTAMP: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(INT64).named("timestamp"),
        ) { it.timestamp.toEpochMilli() }

    public val TASK_ID: ExportColumn<TaskSample> =
        ExportColumn(
            field =
                Types.required(INT32)
                    .named("task_id"),
        ) { it.taskId }

    public val HOST_ID: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.optional(INT32).named("host_id"),
        ) { it.hostId }

    public val CPU_USAGE: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(FLOAT).named("cpu_usage"),
        ) { it.cpuUsage }

    public val CPU_DEMAND: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(FLOAT).named("cpu_demand"),
        ) { it.cpuDemand }

    public val CPU_TIME_ACTIVE: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(INT64).named("cpu_time_active"),
        ) { it.cpuActiveTime }

    public val CPU_TIME_IDLE: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(INT64).named("cpu_time_idle"),
        ) { it.cpuIdleTime }

    public val CPU_TIME_STEAL: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(INT64).named("cpu_time_steal"),
        ) { it.cpuStealTime }

    public val CPU_TIME_LOST: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(INT64).named("cpu_time_lost"),
        ) { it.cpuLostTime }

    public val GPU_USAGE: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(FLOAT).named("gpu_usage"),
        ) { it.gpuUsage }

    public val GPU_DEMAND: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(FLOAT).named("gpu_demand"),
        ) { it.gpuDemand }

    public val GPU_TIME_ACTIVE: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(INT64).named("gpu_time_active"),
        ) { it.gpuActiveTime }

    public val GPU_TIME_IDLE: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(INT64).named("gpu_time_idle"),
        ) { it.gpuIdleTime }

    public val GPU_TIME_STEAL: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(INT64).named("gpu_time_steal"),
        ) { it.gpuStealTime }

    public val GPU_TIME_LOST: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(INT64).named("gpu_time_lost"),
        ) { it.gpuLostTime }

    public val NUM_FAILURES: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(INT64).named("num_failures"),
        ) { it.numFailures }

    public val NUM_PAUSES: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(INT64).named("num_pauses"),
        ) { it.numPauses }

    public val SCHEDULE_TIME: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.optional(INT64).named("schedule_time"),
        ) { it.scheduleTime }

    public val FINISH_TIME: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.optional(INT64).named("finish_time"),
        ) { it.finishTime }

    /**
     * The names of the task states, indexed by ordinal, so that exporting a state does not allocate.
     */
    private val TASK_STATE_NAMES = TaskState.entries.map { Binary.fromString(it.name) }

    public val TASK_STATE: ExportColumn<TaskSample> =
        ExportColumn(
            field =
                Types.required(BINARY)
                    .`as`(LogicalTypeAnnotation.stringType())
                    .named("task_state"),
        ) { TASK_STATE_NAMES[it.taskState.ordinal] }

    public val schedulingDelay: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(INT64).named("scheduling_delay"),
        ) { it.schedulingDelay }

    public val failureDelay: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(INT64).named("failure_delay"),
        ) { it.failureDelay }

    public val checkpointDelay: ExportColumn<TaskSample> =
        ExportColumn(
            field = Types.required(INT64).named("checkpoint_delay"),
        ) { it.checkpointDelay }

    /**
     * The GPU columns, which are only exported by default if the topology has GPUs.
     */
    internal val GPU_COLUMNS =
        setOf(
            GPU_USAGE,
            GPU_DEMAND,
            GPU_TIME_ACTIVE,
            GPU_TIME_IDLE,
            GPU_TIME_STEAL,
            GPU_TIME_LOST,
        )

    /**
     * The columns that are always included in the output file.
     */
    internal val BASE_EXPORT_COLUMNS =
        setOf(
            TASK_ID,
            TIMESTAMP,
        )
}
