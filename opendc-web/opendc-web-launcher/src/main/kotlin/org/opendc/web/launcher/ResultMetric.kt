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

package org.opendc.web.launcher

import org.opendc.sdk.model.telemetry.MetaFile
import org.opendc.sdk.model.telemetry.OutputFileSpec

/** How several values become one. */
enum class Reduction {
    MEAN,
    SUM,
    MAX,
    LAST,
    ;

    /** Applies this to [values], which are in the order they were measured. */
    fun of(values: List<Double>): Double =
        when (this) {
            MEAN -> values.sum() / values.size
            SUM -> values.sum()
            MAX -> values.max()
            LAST -> values.last()
        }
}

/** What one entity contributed to a metric at one instant. [whole] is read only by a share. */
data class Reading(
    val part: Double,
    val whole: Double = 1.0,
)

/** How the entities reporting at one instant become the single reading a chart draws. */
sealed interface EntityFold {
    /** Each entity's own value, folded: watts add up, a queue depth is taken at its worst. */
    data class Each(val reduction: Reduction) : EntityFold

    /**
     * A share of the whole: [part] and [whole] are added up across the entities and then divided.
     * Averaging per-entity shares would weight a small host as heavily as a large one.
     */
    data class Share(val part: String, val whole: String) : EntityFold

    /**
     * A share of what the entities can do: [part] is added up across them and divided by the sum of
     * their [capacity], which is not sampled but read from the entity's row of the table's meta file.
     */
    data class ShareOfCapacity(val part: String, val capacity: String) : EntityFold
}

/**
 * A series a result chart can show, read both from live samples and from the parquet a run leaves.
 *
 * [entities] folds the rows sharing one instant into one chart line. [overTime] folds the instants
 * inside a chart bucket, and must match the reduction the frontend applies in
 * `lib/experiment/results.ts`.
 */
enum class ResultMetric(
    val table: OutputFileSpec,
    val column: String,
    val entities: EntityFold,
    val overTime: Reduction,
) {
    // Not from the column of the same name, which is each host's own share and cannot be averaged.
    HOST_CPU_UTILIZATION(
        OutputFileSpec.HOST,
        "cpu_utilization",
        EntityFold.ShareOfCapacity("cpu_usage", "cpu_capacity"),
        Reduction.MEAN,
    ),
    HOST_POWER_DRAW(OutputFileSpec.HOST, "power_draw", EntityFold.Each(Reduction.SUM), Reduction.MEAN),
    POWER_SOURCE_POWER_DRAW(OutputFileSpec.POWER_SOURCE, "power_draw", EntityFold.Each(Reduction.SUM), Reduction.MEAN),
    POWER_SOURCE_ENERGY_USAGE(OutputFileSpec.POWER_SOURCE, "energy_usage", EntityFold.Each(Reduction.SUM), Reduction.SUM),
    POWER_SOURCE_CARBON_EMISSION(
        OutputFileSpec.POWER_SOURCE,
        "carbon_emission",
        EntityFold.Each(Reduction.SUM),
        Reduction.SUM,
    ),

    // A share too: a source supplying almost nothing must not weigh as much as the one carrying the site.
    POWER_SOURCE_CARBON_INTENSITY(
        OutputFileSpec.POWER_SOURCE,
        "carbon_intensity",
        EntityFold.Share("carbon_emission", "energy_usage"),
        Reduction.MEAN,
    ),

    // The service table has one row per instant, so the entity reduction below cannot change an answer.
    SERVICE_TASKS_ACTIVE(OutputFileSpec.SERVICE, "tasks_active", EntityFold.Each(Reduction.SUM), Reduction.MEAN),
    SERVICE_TASKS_PENDING(OutputFileSpec.SERVICE, "tasks_pending", EntityFold.Each(Reduction.SUM), Reduction.MAX),
    SERVICE_TASKS_COMPLETED(OutputFileSpec.SERVICE, "tasks_completed", EntityFold.Each(Reduction.SUM), Reduction.LAST),
    SERVICE_TASKS_TERMINATED(OutputFileSpec.SERVICE, "tasks_terminated", EntityFold.Each(Reduction.SUM), Reduction.LAST),
    SERVICE_HOSTS_DOWN(OutputFileSpec.SERVICE, "hosts_down", EntityFold.Each(Reduction.SUM), Reduction.MAX),
    BATTERY_CHARGE(OutputFileSpec.BATTERY, "charge", EntityFold.Each(Reduction.SUM), Reduction.MEAN),
    ;

    /** The sampled columns of [table] this metric is read from. */
    val columns: List<String>
        get() =
            when (val fold = entities) {
                is EntityFold.Each -> listOf(column)
                is EntityFold.Share -> listOf(fold.part, fold.whole)
                is EntityFold.ShareOfCapacity -> listOf(fold.part)
            }

    /** Folds what the entities reporting at one instant contributed into one reading. */
    fun fold(readings: Collection<Reading>): Double =
        when (val it = entities) {
            is EntityFold.Each -> it.reduction.of(readings.map { reading -> reading.part })
            is EntityFold.Share, is EntityFold.ShareOfCapacity -> {
                val whole = readings.sumOf { reading -> reading.whole }
                // Nothing supplied is nothing used, not a share of nothing.
                if (whole == 0.0) 0.0 else readings.sumOf { reading -> reading.part } / whole
            }
        }

    /** What this metric is called on the wire and in the frontend's catalog: `<table file stem>.<column>`. */
    val id: String get() = "${table.fileName.substringBeforeLast('.')}.$column"

    companion object {
        fun byId(id: String): ResultMetric? = entries.firstOrNull { it.id == id }

        /** The metrics read out of [table], in catalog order. */
        fun of(table: OutputFileSpec): List<ResultMetric> = entries.filter { it.table == table }
    }
}

/**
 * What tells one row of a table from another at the same instant.
 *
 * The last instant of a run is reported twice, so folding without an identity would add a fleet to
 * itself and finish a run at twice its power.
 */
sealed interface RowIdentity {
    /** One row per entity, identified by [column], which also keys the entity's row of [meta]. */
    data class PerEntity(val column: String, val meta: MetaFile) : RowIdentity

    /** One row per instant, so there is nothing to tell apart and the last one stands. */
    data object Singleton : RowIdentity
}

/** How the rows of [this] table are told apart within one instant. */
val OutputFileSpec.rows: RowIdentity
    get() =
        when (this) {
            OutputFileSpec.BATTERY -> RowIdentity.PerEntity("battery_id", MetaFile.BATTERY)
            OutputFileSpec.CLUSTER -> RowIdentity.PerEntity("cluster_id", MetaFile.CLUSTER)
            OutputFileSpec.DATA_CENTER -> RowIdentity.PerEntity("data_center_id", MetaFile.DATA_CENTER)
            OutputFileSpec.HOST -> RowIdentity.PerEntity("host_id", MetaFile.HOST)
            OutputFileSpec.POWER_SOURCE -> RowIdentity.PerEntity("power_source_id", MetaFile.POWER_SOURCE)
            OutputFileSpec.SERVICE -> RowIdentity.Singleton
            OutputFileSpec.TASK -> RowIdentity.PerEntity("task_id", MetaFile.TASK)
        }
