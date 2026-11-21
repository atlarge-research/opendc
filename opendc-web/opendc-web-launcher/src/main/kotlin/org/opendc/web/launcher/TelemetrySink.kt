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

import org.opendc.sdk.model.telemetry.OutputFileSpec
import org.opendc.sdk.runner.telemetry.MetricExporter
import org.opendc.sdk.runner.telemetry.sink.OutputSink
import org.opendc.sdk.runner.telemetry.sink.RunContext
import org.opendc.sdk.runner.telemetry.sink.SinkResult
import org.opendc.sdk.runner.telemetry.sink.SinkSession
import org.opendc.sdk.runner.telemetry.table.battery.BatterySample
import org.opendc.sdk.runner.telemetry.table.host.HostSample
import org.opendc.sdk.runner.telemetry.table.powerSource.PowerSourceSample
import org.opendc.sdk.runner.telemetry.table.service.ServiceSample
import org.opendc.sdk.runner.telemetry.table.topology.TopologyMeta
import java.util.concurrent.CopyOnWriteArrayList

/** How many points one metric of one run is held at, so a report stays the same size however long the trace. */
private const val SAMPLE_CAP = 512

/** Nothing has been read yet. Simulated time starts at zero, so it cannot stand in for this. */
private const val NO_INSTANT = Long.MIN_VALUE

/**
 * Watches the runs in this process so their progress can be reported while they are still going.
 * Reads only the tables the result charts are drawn from, never the far larger task table.
 */
class TelemetrySink : OutputSink {
    private val runs = CopyOnWriteArrayList<RunSamples>()

    override fun open(context: RunContext): SinkSession {
        val samples = RunSamples(context.scenarioId, context.seed)
        runs += samples
        return object : SinkSession {
            override val monitor: MetricExporter = TelemetryMonitor(samples)

            override val tables: Set<OutputFileSpec> =
                setOf(OutputFileSpec.HOST, OutputFileSpec.SERVICE, OutputFileSpec.POWER_SOURCE, OutputFileSpec.BATTERY)

            /** Called once the run has finished, which is the last chance to close its final instant. */
            override fun result(): SinkResult? {
                samples.finish()
                return null
            }
        }
    }

    /** Everything measured so far, as it would be reported now. */
    fun report(): TelemetryReport = TelemetryReport(runs.map { it.snapshot() })
}

/** There is one scheduling service, so every row it reports at an instant is the same row again. */
private const val THE_SERVICE = "service"

private class TelemetryMonitor(private val samples: RunSamples) : MetricExporter {
    /** Exported once before any sample, so every host sampled has its capacity here. */
    private var cpuCapacities = emptyMap<Int, Double>()

    override fun export(meta: TopologyMeta) {
        cpuCapacities = meta.hosts.associate { it.hostId to it.cpuCapacity }
    }

    override fun export(reader: HostSample) {
        samples.record(
            reader.timestamp.toEpochMilli(),
            reader.hostId.toString(),
            ResultMetric.HOST_CPU_UTILIZATION to Reading(reader.cpuUsage, cpuCapacities.getValue(reader.hostId)),
            ResultMetric.HOST_POWER_DRAW to Reading(reader.powerDraw),
        )
    }

    override fun export(reader: PowerSourceSample) {
        samples.record(
            reader.timestamp.toEpochMilli(),
            reader.powerSourceId.toString(),
            ResultMetric.POWER_SOURCE_POWER_DRAW to Reading(reader.powerDraw),
            ResultMetric.POWER_SOURCE_ENERGY_USAGE to Reading(reader.energyUsage),
            ResultMetric.POWER_SOURCE_CARBON_EMISSION to Reading(reader.carbonEmission),
            ResultMetric.POWER_SOURCE_CARBON_INTENSITY to Reading(reader.carbonEmission, reader.energyUsage),
        )
    }

    override fun export(reader: BatterySample) {
        samples.record(reader.timestamp.toEpochMilli(), reader.batteryId.toString(), ResultMetric.BATTERY_CHARGE to Reading(reader.charge))
    }

    override fun export(reader: ServiceSample) {
        samples.progress(reader.tasksCompleted + reader.tasksTerminated)
        samples.record(
            reader.timestamp.toEpochMilli(),
            THE_SERVICE,
            ResultMetric.SERVICE_TASKS_ACTIVE to Reading(reader.tasksActive.toDouble()),
            ResultMetric.SERVICE_TASKS_PENDING to Reading(reader.tasksPending.toDouble()),
            ResultMetric.SERVICE_TASKS_COMPLETED to Reading(reader.tasksCompleted.toDouble()),
            ResultMetric.SERVICE_TASKS_TERMINATED to Reading(reader.tasksTerminated.toDouble()),
            ResultMetric.SERVICE_HOSTS_DOWN to Reading(reader.hostsDown.toDouble()),
        )
    }
}

/**
 * What one `(scenario, seed)` run has reported. Written from the run's thread and read from the
 * poster's, so every path takes the lock.
 *
 * The engine hands an instant over one row per entity, so an instant is complete exactly when a later
 * one begins. Rows are keyed by entity, not counted, because the last instant is reported twice.
 */
private class RunSamples(
    private val scenarioIndex: Int,
    private val seed: Long,
) {
    private val lock = Any()
    private var completedTasks = 0
    private var instant = NO_INSTANT
    private val reporting = ResultMetric.entries.associateWith { mutableMapOf<String, Reading>() }
    private val series = ResultMetric.entries.associateWith { PointBuffer(it.overTime) }

    fun progress(completed: Int) {
        synchronized(lock) { completedTasks = completed }
    }

    fun record(
        timestamp: Long,
        entity: String,
        vararg readings: Pair<ResultMetric, Reading>,
    ) {
        synchronized(lock) {
            if (timestamp != instant) {
                closeInstant()
                instant = timestamp
            }
            for ((metric, reading) in readings) {
                reporting.getValue(metric)[entity] = reading
            }
        }
    }

    /** Closes the instant left open when the run stopped producing rows. */
    fun finish() {
        synchronized(lock) { closeInstant() }
    }

    fun snapshot(): RunTelemetry =
        synchronized(lock) {
            RunTelemetry(
                scenarioIndex = scenarioIndex,
                seed = seed,
                completedTasks = completedTasks,
                series =
                    ResultMetric.entries.mapNotNull { metric ->
                        series.getValue(metric).points().takeIf { it.isNotEmpty() }?.let { MetricSeries(metric.id, it) }
                    },
            )
        }

    /** Folds the entities that reported at the open instant into one value per metric. */
    private fun closeInstant() {
        if (instant == NO_INSTANT) {
            return
        }
        for ((metric, byEntity) in reporting) {
            if (byEntity.isNotEmpty()) {
                series.getValue(metric).add(instant, metric.fold(byEntity.values))
                byEntity.clear()
            }
        }
        instant = NO_INSTANT
    }
}

/**
 * One metric's points, kept under [SAMPLE_CAP] however many instants arrive.
 *
 * Each point covers [stride] instants folded by the metric's reduction. Overflow doubles the stride
 * and pairs the points up, which is exact because every point covers the same span. Instants pending
 * toward the next point are reported as a provisional last point, so a live chart keeps moving.
 */
private class PointBuffer(private val fold: Reduction) {
    private val points = mutableListOf<MetricPoint>()
    private val pending = mutableListOf<Double>()
    private var pendingAt = NO_INSTANT
    private var stride = 1

    fun add(
        timestamp: Long,
        value: Double,
    ) {
        if (pending.isEmpty()) {
            pendingAt = timestamp
        }
        pending += value
        if (pending.size < stride) {
            return
        }
        points += MetricPoint(pendingAt, fold.of(pending))
        pending.clear()
        if (points.size >= SAMPLE_CAP) {
            merge()
        }
    }

    fun points(): List<MetricPoint> = if (pending.isEmpty()) points.toList() else points + MetricPoint(pendingAt, fold.of(pending))

    private fun merge() {
        val merged = points.chunked(2) { pair -> MetricPoint(pair.first().t, fold.of(pair.map { it.value })) }
        points.clear()
        points += merged
        stride *= 2
    }
}
