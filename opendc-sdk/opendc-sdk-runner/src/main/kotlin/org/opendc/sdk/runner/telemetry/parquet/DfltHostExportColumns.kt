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
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BINARY
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.FLOAT
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT32
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT64
import org.apache.parquet.schema.Types
import org.opendc.sdk.runner.telemetry.table.host.HostSample
import org.opendc.simulator.compute.models.HostState
import org.opendc.trace.parquet.exporter.ExportColumn

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
    public val TIMESTAMP: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(INT64).named("timestamp"),
        ) { it.timestamp.toEpochMilli() }

    public val HOST_ID: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(INT32).named("host_id"),
        ) { it.hostId }

    /**
     * The names of the host states, indexed by ordinal, so that exporting a state does not allocate.
     */
    private val HOST_STATE_NAMES = HostState.entries.map { Binary.fromString(it.name) }

    public val HOST_STATE: ExportColumn<HostSample> =
        ExportColumn(
            field =
                Types.required(BINARY)
                    .`as`(LogicalTypeAnnotation.stringType())
                    .named("host_state"),
        ) { HOST_STATE_NAMES[it.hostState.ordinal] }

    public val TASKS_RUNNING: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(INT32).named("tasks_running"),
        ) { it.tasksActive }

    public val TASKS_ERROR: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(INT32).named("tasks_error"),
        ) { it.guestsError }

    public val TASKS_INVALID: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(INT32).named("tasks_invalid"),
        ) { it.guestsInvalid }

    public val CPU_USAGE: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(FLOAT).named("cpu_usage"),
        ) { it.cpuUsage }

    public val CPU_DEMAND: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(FLOAT).named("cpu_demand"),
        ) { it.cpuDemand }

    public val CPU_UTILIZATION: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(FLOAT).named("cpu_utilization"),
        ) { it.cpuUtilization }

    public val CPU_TIME_ACTIVE: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(INT64).named("cpu_time_active"),
        ) { it.cpuActiveTime }

    public val CPU_TIME_IDLE: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(INT64).named("cpu_time_idle"),
        ) { it.cpuIdleTime }

    public val CPU_TIME_STEAL: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(INT64).named("cpu_time_steal"),
        ) { it.cpuStealTime }

    public val CPU_TIME_LOST: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(INT64).named("cpu_time_lost"),
        ) { it.cpuLostTime }

    public val POWER_DRAW: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(FLOAT).named("power_draw"),
        ) { it.powerDraw }

    public val ENERGY_USAGE: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(FLOAT).named("energy_usage"),
        ) { it.energyUsage }

    public val CARBON_INTENSITY: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(FLOAT).named("carbon_intensity"),
        ) { it.carbonIntensity }

    public val CARBON_EMISSION: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(FLOAT).named("carbon_emission"),
        ) { it.carbonEmission }

    public val EMBODIED_CARBON: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(FLOAT).named("embodied_carbon"),
        ) { it.embodiedCarbon }

    public val UP_TIME: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(INT64).named("uptime"),
        ) { it.uptime }

    public val DOWN_TIME: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.required(INT64).named("downtime"),
        ) { it.downtime }

    public val BOOT_TIME: ExportColumn<HostSample> =
        ExportColumn(
            field = Types.optional(INT64).named("boot_time"),
        ) { it.bootTime?.toEpochMilli() }

    /**
     * A metric that is exported once per GPU of a host.
     */
    private class GpuMetric(val name: String, val type: PrimitiveTypeName, val value: (HostSample, Int) -> Any?)

    private val GPU_METRIC_DEFINITIONS =
        listOf(
            GpuMetric("gpu_usage", FLOAT) { sample, i -> sample.gpuUsages.getOrNull(i) },
            GpuMetric("gpu_demand", FLOAT) { sample, i -> sample.gpuDemands.getOrNull(i) },
            GpuMetric("gpu_utilization", FLOAT) { sample, i -> sample.gpuUtilizations.getOrNull(i) },
            GpuMetric("gpu_time_active", INT64) { sample, i -> sample.gpuActiveTimes.getOrNull(i) },
            GpuMetric("gpu_time_idle", INT64) { sample, i -> sample.gpuIdleTimes.getOrNull(i) },
            GpuMetric("gpu_time_steal", INT64) { sample, i -> sample.gpuStealTimes.getOrNull(i) },
            GpuMetric("gpu_time_lost", INT64) { sample, i -> sample.gpuLostTimes.getOrNull(i) },
            GpuMetric("gpu_power_draw", FLOAT) { sample, i -> sample.gpuPowerDraws.getOrNull(i) },
        )

    /**
     * The names of the metrics that are exported once per GPU. The column of a metric for GPU `i` is named
     * `<metric>_<i>`, e.g. `gpu_usage_0`.
     */
    public val GPU_METRICS: List<String> = GPU_METRIC_DEFINITIONS.map { it.name }

    /**
     * Returns the columns of the given GPU [metrics] for each of [count] GPUs. The columns are generated for the topology
     * of a single simulation, so they are not registered.
     */
    public fun gpuColumns(
        count: Int,
        metrics: Collection<String> = GPU_METRICS,
    ): List<ExportColumn<HostSample>> =
        (0 until count).flatMap { i ->
            GPU_METRIC_DEFINITIONS.filter { it.name in metrics }.map { metric ->
                ExportColumn<HostSample>(field = Types.optional(metric.type).named("${metric.name}_$i"), register = false) {
                    metric.value(it, i)
                }
            }
        }

    /**
     * The columns that are always included in the output file.
     */
    internal val BASE_EXPORT_COLUMNS =
        setOf(
            HOST_ID,
            TIMESTAMP,
        )
}
