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
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BOOLEAN
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.DOUBLE
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT32
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT64
import org.apache.parquet.schema.Types
import org.opendc.compute.simulator.telemetry.table.scheduler.SchedulerTableReader
import org.opendc.trace.util.parquet.exporter.ExportColumn

public object DfltSchedulerExportColumns {
    public val TIMESTAMP: ExportColumn<SchedulerTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("timestamp"),
        ) { it.timestamp.toEpochMilli() }

    public val TIMESTAMP_ABS: ExportColumn<SchedulerTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("timestamp_absolute"),
        ) { it.timestampAbsolute.toEpochMilli() }

    public val DECISION_ID: ExportColumn<SchedulerTableReader> =
        ExportColumn(
            field = Types.required(INT64).named("decision_id"),
        ) { it.decisionId }

    public val TASK_ID: ExportColumn<SchedulerTableReader> =
        ExportColumn(
            field = Types.required(INT32).named("task_id"),
        ) { it.taskId }

    public val POLICY_INDEX: ExportColumn<SchedulerTableReader> =
        ExportColumn(
            field = Types.required(INT32).named("policy_index"),
        ) { it.policyIndex }

    public val CANDIDATE_HOST_NAME: ExportColumn<SchedulerTableReader> =
        ExportColumn(
            field = Types.required(BINARY).`as`(LogicalTypeAnnotation.stringType()).named("candidate_host_name"),
        ) { Binary.fromString(it.candidateHostName) }

    public val SCORE: ExportColumn<SchedulerTableReader> =
        ExportColumn(
            field = Types.required(DOUBLE).named("score"),
        ) { it.score }

    public val SELECTED: ExportColumn<SchedulerTableReader> =
        ExportColumn(
            field = Types.required(BOOLEAN).named("selected"),
        ) { it.selected }

    public val WINNING_SCORE: ExportColumn<SchedulerTableReader> =
        ExportColumn(
            field = Types.required(DOUBLE).named("winning_score"),
        ) { it.winningScore }

    public val DRR_SCORE: ExportColumn<SchedulerTableReader> =
        ExportColumn(
            field = Types.required(DOUBLE).named("drr_score"),
        ) { it.drrScore }

    public val OR_SCORE: ExportColumn<SchedulerTableReader> =
        ExportColumn(
            field = Types.required(DOUBLE).named("or_score"),
        ) { it.orScore }

    internal val BASE_EXPORT_COLUMNS =
        setOf(
            TIMESTAMP,
            TIMESTAMP_ABS,
            DECISION_ID,
        )
}
