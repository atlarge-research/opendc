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

package org.opendc.compute.simulator.telemetry.parquet

import org.apache.parquet.io.api.Binary
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BINARY
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.FLOAT
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT32
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT64
import org.apache.parquet.schema.Types
import org.opendc.compute.simulator.telemetry.table.host.HostTableReader
import org.opendc.trace.util.parquet.exporter.ExportColumn

/**
 * This object wraps the [ExportColumn]s to solves ambiguity for field
 * names that are included in more than 1 exportable.
 *
 * Additionally, it allows to load all the fields at once by just its symbol,
 * so that these columns can be deserialized. Additional fields can be added
 * from anywhere, and they are deserializable as long as they are loaded by the jvm.
 *
 * ```kotlin
 * ...
 * // Loads the column
 * DfltHostExportColumns
 * ...
 * ```
 */
public object DfltHostExportColumns {
    public val TIMESTAMP: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("timestamp"),
        ) { it.timestamp.toEpochMilli() }

    public val TIMESTAMP_ABS: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("timestamp_absolute"),
        ) { it.timestampAbsolute.toEpochMilli() }

    public val HOST_NAME: ExportColumn<HostTableReader> =
        ExportColumn(
            field =
                Types.required(BINARY)
                    .`as`(LogicalTypeAnnotation.stringType())
                    .named("host_name"),
        ) { Binary.fromString(it.hostInfo.name) }

    public val CLUSTER_NAME: ExportColumn<HostTableReader> =
        ExportColumn(
            field =
                Types.required(BINARY)
                    .`as`(LogicalTypeAnnotation.stringType())
                    .named("cluster_name"),
        ) { Binary.fromString(it.hostInfo.clusterName) }

    public val CPU_COUNT: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(INT32).named("core_count"),
        ) { it.hostInfo.coreCount }

    public val MEM_CAPACITY: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("mem_capacity"),
        ) { it.hostInfo.memCapacity }

    public val TASKS_TERMINATED: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(INT32).named("tasks_terminated"),
        ) { it.tasksTerminated }

    public val TASKS_RUNNING: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(INT32).named("tasks_running"),
        ) { it.tasksActive }

    public val TASKS_ERROR: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(INT32).named("tasks_error"),
        ) { it.guestsError }

    public val TASKS_INVALID: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(INT32).named("tasks_invalid"),
        ) { it.guestsInvalid }

    public val CPU_CAPACITY: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(FLOAT).named("cpu_capacity"),
        ) { it.cpuCapacity }

    public val CPU_USAGE: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(FLOAT).named("cpu_usage"),
        ) { it.cpuUsage }

    public val CPU_DEMAND: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(FLOAT).named("cpu_demand"),
        ) { it.cpuDemand }

    public val CPU_UTILIZATION: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(FLOAT).named("cpu_utilization"),
        ) { it.cpuUtilization }

    public val CPU_TIME_ACTIVE: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("cpu_time_active"),
        ) { it.cpuActiveTime }

    public val CPU_TIME_IDLE: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("cpu_time_idle"),
        ) { it.cpuIdleTime }

    public val CPU_TIME_STEAL: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("cpu_time_steal"),
        ) { it.cpuStealTime }

    public val CPU_TIME_LOST: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("cpu_time_lost"),
        ) { it.cpuLostTime }

    public val POWER_DRAW: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(FLOAT).named("power_draw"),
        ) { it.powerDraw }

    public val ENERGY_USAGE: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(FLOAT).named("energy_usage"),
        ) { it.energyUsage }

    public val EMBODIED_CARBON: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(FLOAT).named("embodied_carbon"),
        ) { it.embodiedCarbon }

    public val UP_TIME: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("uptime"),
        ) { it.uptime }

    public val DOWN_TIME: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("downtime"),
        ) { it.downtime }

    public val BOOT_TIME: ExportColumn<HostTableReader> =
        ExportColumn(
            field = Types.optional(INT64).named("boot_time"),
        ) { it.bootTime?.toEpochMilli() }

    /**
     * Returns GPU-related export columns for the given number of GPUs.
     */
    public fun gpuColumns(count: Int): Set<ExportColumn<HostTableReader>> =
        (0 until count).flatMap { i ->
            listOf<ExportColumn<HostTableReader>>(
                ExportColumn(
                    field = Types.optional(FLOAT).named("gpu_capacity_$i"),
                ) { it.gpuCapacities.getOrNull(i) },
                ExportColumn(
                    field = Types.optional(FLOAT).named("gpu_usage_$i"),
                ) { it.gpuUsages.getOrNull(i) },
                ExportColumn(
                    field = Types.optional(FLOAT).named("gpu_demand_$i"),
                ) { it.gpuDemands.getOrNull(i) },
                ExportColumn(
                    field = Types.optional(FLOAT).named("gpu_utilization_$i"),
                ) { it.gpuUtilizations.getOrNull(i) },
                ExportColumn(
                    field = Types.optional(INT64).named("gpu_time_active_$i"),
                ) { it.gpuActiveTimes.getOrNull(i) },
                ExportColumn(
                    field = Types.optional(INT64).named("gpu_time_idle_$i"),
                ) { it.gpuIdleTimes.getOrNull(i) },
                ExportColumn(
                    field = Types.optional(INT64).named("gpu_time_steal_$i"),
                ) { it.gpuStealTimes.getOrNull(i) },
                ExportColumn(
                    field = Types.optional(INT64).named("gpu_time_lost_$i"),
                ) { it.gpuLostTimes.getOrNull(i) },
                ExportColumn(
                    field = Types.optional(FLOAT).named("gpu_power_draw_$i"),
                ) { it.gpuPowerDraws.getOrNull(i) },
            )
        }.toSet()

    /**
     * The columns that are always included in the output file.
     */
    internal val BASE_EXPORT_COLUMNS =
        setOf(
            HOST_NAME,
            CLUSTER_NAME,
            TIMESTAMP,
            TIMESTAMP_ABS,
        )
}
