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

package org.opendc.compute.telemetry.export.parquet

import org.apache.parquet.io.api.Binary
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BINARY
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.DOUBLE
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT32
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT64
import org.apache.parquet.schema.Types
import org.opendc.compute.telemetry.table.ServerTableReader
import org.opendc.trace.util.parquet.exporter.ExportColumn

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
 * DfltServerExportColumns
 * ...
 * ```
 */
public object DfltServerExportColumns {
    public val TIMESTAMP: ExportColumn<ServerTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("timestamp"),
        ) { it.timestamp.toEpochMilli() }

    public val TIMESTAMP_ABS: ExportColumn<ServerTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("timestamp_absolute"),
        ) { it.timestampAbsolute.toEpochMilli() }

    public val SERVER_ID: ExportColumn<ServerTableReader> =
        ExportColumn(
            field =
                Types.required(BINARY)
                    .`as`(LogicalTypeAnnotation.stringType())
                    .named("server_id"),
        ) { Binary.fromString(it.server.id) }

    public val HOST_ID: ExportColumn<ServerTableReader> =
        ExportColumn(
            field =
                Types.optional(BINARY)
                    .`as`(LogicalTypeAnnotation.stringType())
                    .named("host_id"),
        ) { it.host?.id?.let { Binary.fromString(it) } }

    public val SERVER_NAME: ExportColumn<ServerTableReader> =
        ExportColumn(
            field =
                Types.required(BINARY)
                    .`as`(LogicalTypeAnnotation.stringType())
                    .named("server_name"),
        ) { Binary.fromString(it.server.name) }

    public val CPU_COUNT: ExportColumn<ServerTableReader> =
        ExportColumn(
            field = Types.required(INT32).named("cpu_count"),
        ) { it.server.cpuCount }

    public val MEM_CAPACITY: ExportColumn<ServerTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("mem_capacity"),
        ) { it.server.memCapacity }

    public val CPU_LIMIT: ExportColumn<ServerTableReader> =
        ExportColumn(
            field = Types.required(DOUBLE).named("cpu_limit"),
        ) { it.cpuLimit }

    public val CPU_TIME_ACTIVE: ExportColumn<ServerTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("cpu_time_active"),
        ) { it.cpuActiveTime }

    public val CPU_TIME_IDLE: ExportColumn<ServerTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("cpu_time_idle"),
        ) { it.cpuIdleTime }

    public val CPU_TIME_STEAL: ExportColumn<ServerTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("cpu_time_steal"),
        ) { it.cpuStealTime }

    public val CPU_TIME_LOST: ExportColumn<ServerTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("cpu_time_lost"),
        ) { it.cpuLostTime }

    public val UP_TIME: ExportColumn<ServerTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("uptime"),
        ) { it.uptime }

    public val DOWN_TIME: ExportColumn<ServerTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("downtime"),
        ) { it.downtime }

    public val PROVISION_TIME: ExportColumn<ServerTableReader> =
        ExportColumn(
            field = Types.optional(INT64).named("provision_time"),
        ) { it.provisionTime?.toEpochMilli() }

    public val BOOT_TIME: ExportColumn<ServerTableReader> =
        ExportColumn(
            field = Types.optional(INT64).named("boot_time"),
        ) { it.bootTime?.toEpochMilli() }

    public val BOOT_TIME_ABS: ExportColumn<ServerTableReader> =
        ExportColumn(
            field = Types.optional(INT64).named("boot_time_absolute"),
        ) { it.bootTimeAbsolute?.toEpochMilli() }

    /**
     * The columns that are always included in the output file.
     */
    internal val BASE_EXPORT_COLUMNS =
        setOf(
            TIMESTAMP_ABS,
            TIMESTAMP,
        )
}
