/*
 * Copyright (c) 2026 AtLarge Research
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
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BINARY
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.FLOAT
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT32
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT64
import org.apache.parquet.schema.Types
import org.opendc.sdk.runner.telemetry.table.simulation.SimulationMeta
import org.opendc.sdk.runner.telemetry.table.task.TaskMeta
import org.opendc.sdk.runner.telemetry.table.topology.BatteryMeta
import org.opendc.sdk.runner.telemetry.table.topology.ClusterMeta
import org.opendc.sdk.runner.telemetry.table.topology.DataCenterMeta
import org.opendc.sdk.runner.telemetry.table.topology.HostMeta
import org.opendc.sdk.runner.telemetry.table.topology.PowerSourceMeta
import org.opendc.trace.parquet.exporter.ExportColumn
import java.util.concurrent.ConcurrentHashMap

/**
 * The columns of the meta files, which map the ids used in the samples to names, parents and static attributes.
 *
 * Unlike the sample columns these are not configurable: a meta file always contains all of its columns.
 */
internal object MetaColumns {
    val SIMULATION: List<ExportColumn<SimulationMeta>> =
        listOf(
            ExportColumn(field = Types.required(INT64).named("start_time")) { it.startTime },
        )

    val DATA_CENTER: List<ExportColumn<DataCenterMeta>> =
        listOf(
            ExportColumn(field = id("data_center_id")) { it.dataCenterId },
            ExportColumn(field = name("data_center_name")) { Binary.fromString(it.dataCenterName) },
        )

    val CLUSTER: List<ExportColumn<ClusterMeta>> =
        listOf(
            ExportColumn(field = id("cluster_id")) { it.clusterId },
            ExportColumn(field = name("cluster_name")) { Binary.fromString(it.clusterName) },
            ExportColumn(field = id("data_center_id")) { it.dataCenterId },
        )

    /** The host columns per number of GPU columns, as every [ExportColumn] is registered for the rest of the JVM. */
    private val hostColumns = ConcurrentHashMap<Int, List<ExportColumn<HostMeta>>>()

    /**
     * The host columns, with a `gpu_capacity_$i` column for each of the first [gpuCount] GPUs of a host.
     */
    fun host(gpuCount: Int): List<ExportColumn<HostMeta>> =
        hostColumns.computeIfAbsent(gpuCount) {
            listOf<ExportColumn<HostMeta>>(
                ExportColumn(field = id("host_id")) { it.hostId },
                ExportColumn(field = name("host_name")) { Binary.fromString(it.hostName) },
                ExportColumn(field = id("cluster_id")) { it.clusterId },
                ExportColumn(field = Types.required(INT32).named("core_count")) { it.coreCount },
                ExportColumn(field = Types.required(FLOAT).named("cpu_capacity")) { it.cpuCapacity },
                ExportColumn(field = Types.required(INT64).named("mem_capacity")) { it.memCapacity },
            ) +
                (0 until gpuCount).map { i ->
                    ExportColumn<HostMeta>(field = Types.optional(FLOAT).named("gpu_capacity_$i")) { it.gpuCapacities.getOrNull(i) }
                }
        }

    val POWER_SOURCE: List<ExportColumn<PowerSourceMeta>> =
        listOf(
            ExportColumn(field = id("power_source_id")) { it.powerSourceId },
            ExportColumn(field = name("power_source_name")) { Binary.fromString(it.powerSourceName) },
            ExportColumn(field = id("data_center_id")) { it.dataCenterId },
        )

    val BATTERY: List<ExportColumn<BatteryMeta>> =
        listOf(
            ExportColumn(field = id("battery_id")) { it.batteryId },
            ExportColumn(field = name("battery_name")) { Binary.fromString(it.batteryName) },
            ExportColumn(field = id("data_center_id")) { it.dataCenterId },
            ExportColumn(field = Types.required(FLOAT).named("capacity")) { it.capacity },
        )

    val TASK: List<ExportColumn<TaskMeta>> =
        listOf(
            ExportColumn(field = id("task_id")) { it.taskId },
            ExportColumn(field = Types.required(INT32).named("cpu_count")) { it.cpuCount },
            ExportColumn(field = Types.required(INT64).named("mem_capacity")) { it.memCapacity },
            ExportColumn(field = Types.required(INT32).named("gpu_count")) { it.gpuCount },
            ExportColumn(field = Types.required(INT64).named("submission_time")) { it.submissionTime },
        )

    private fun id(column: String): PrimitiveType = Types.required(INT32).named(column)

    private fun name(column: String): PrimitiveType =
        Types.required(BINARY)
            .`as`(LogicalTypeAnnotation.stringType())
            .named(column)
}
