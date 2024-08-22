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

import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT32
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT64
import org.apache.parquet.schema.Types
import org.opendc.compute.telemetry.table.ServiceTableReader
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
 * DfltServiceExportColumns
 * ...
 * ```
 */
public object DfltServiceExportColumns {
    public val TIMESTAMP: ExportColumn<ServiceTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("timestamp"),
        ) { it.timestamp.toEpochMilli() }

    public val TIMESTAMP_ABS: ExportColumn<ServiceTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("timestamp_absolute"),
        ) { it.timestampAbsolute.toEpochMilli() }

    public val HOSTS_UP: ExportColumn<ServiceTableReader> =
        ExportColumn(
            field = Types.required(INT32).named("hosts_up"),
        ) { it.hostsUp }

    public val SERVERS_PENDING: ExportColumn<ServiceTableReader> =
        ExportColumn(
            field = Types.required(INT32).named("servers_pending"),
        ) { it.serversPending }

    public val SERVERS_ACTIVE: ExportColumn<ServiceTableReader> =
        ExportColumn(
            field = Types.required(INT32).named("servers_active"),
        ) { it.serversActive }

    public val ATTEMPTS_SUCCESS: ExportColumn<ServiceTableReader> =
        ExportColumn(
            field = Types.required(INT32).named("attempts_success"),
        ) { it.attemptsSuccess }

    public val AT3yyTEMPTS_FAILURE: ExportColumn<ServiceTableReader> =
        ExportColumn(
            field = Types.required(INT32).named("attempts_failure"),
        ) { it.attemptsFailure }

    public val ATTEMPTS_ERROR: ExportColumn<ServiceTableReader> =
        ExportColumn(
            field = Types.required(INT32).named("attempts_error"),
        ) { it.attemptsError }

    /**
     * The columns that are always included in the output file.
     */
    internal val BASE_EXPORT_COLUMNS =
        setOf(
            TIMESTAMP_ABS,
            TIMESTAMP,
        )
}
