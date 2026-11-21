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

package org.opendc.web.server.execution

import jakarta.enterprise.context.ApplicationScoped
import org.opendc.sdk.model.experiment.expand
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.resource.ResourceReference
import org.opendc.sdk.model.resource.UriReference
import org.opendc.sdk.model.scenario.ScenarioSpec
import org.opendc.sdk.model.telemetry.MetaFile
import org.opendc.sdk.model.workload.EfficientTraceWorkloadSpec
import org.opendc.sdk.model.workload.InlineWorkloadSpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec
import org.opendc.sdk.model.workload.WorkloadSpec
import org.opendc.trace.conv.TABLE_FRAGMENTS
import org.opendc.trace.conv.TABLE_TASKS
import org.opendc.web.dispatcher.ExecutionSlot
import org.opendc.web.dispatcher.PlannedBag
import org.opendc.web.dispatcher.PlannedUnit
import org.opendc.web.dispatcher.Unfit
import org.opendc.web.dispatcher.estimate.ResourceEstimator
import org.opendc.web.dispatcher.estimate.TraceExtent
import org.opendc.web.dispatcher.estimate.TraceSizeEstimator
import org.opendc.web.dispatcher.estimate.scaledBy
import org.opendc.web.dispatcher.planBags
import org.opendc.web.dispatcher.unfit
import org.opendc.web.launcher.LaunchManifest
import org.opendc.web.launcher.LaunchUnit
import org.opendc.web.launcher.OutputTarget
import org.opendc.web.launcher.StagedInput
import org.opendc.web.launcher.TelemetryTarget
import org.opendc.web.server.model.Execution
import org.opendc.web.server.model.ExecutionState
import org.opendc.web.server.model.ExecutionUnit
import org.opendc.web.server.model.Experiment
import org.opendc.web.server.model.RunUnit
import org.opendc.web.server.model.Trace
import org.opendc.web.server.model.TracePart
import org.opendc.web.server.model.UnitState
import org.opendc.web.server.service.SpecCodec
import org.opendc.web.server.storage.ObjectStore
import org.opendc.web.server.storage.outcomeKey
import org.opendc.web.server.storage.runKey
import org.opendc.web.server.storage.traceKey
import java.time.Duration
import java.time.Instant

/** Where a launcher puts the bytes it is handed, relative to its working directory. */
private const val INPUTS = "inputs"

/**
 * Works out what an experiment's queued units will cost, how they are shaped into executions, and
 * what a launcher is handed to run one. Shaping happens at dispatch, against the slot then on offer.
 */
@ApplicationScoped
class ExecutionPlanner(
    private val config: ExecutionConfig,
    private val codec: SpecCodec,
    private val store: ObjectStore,
) {
    /** The executions [units] would form on [slot], largest first, from fresh estimates. */
    fun plan(
        experiment: Experiment,
        units: List<RunUnit>,
        slot: ExecutionSlot,
    ): List<PlannedBag> = planBags(estimate(scenariosOf(experiment), units), slot, config.toPolicy())

    /** The scenarios no execution on [slot] can hold, whatever they are packed with. */
    fun unfit(
        scenarios: List<ScenarioSpec>,
        slot: ExecutionSlot,
    ): List<Unfit> {
        val model = estimator()
        return unfit(scenarios.map { planned(model, it) }, slot, config.toPolicy())
    }

    /**
     * What the units [carried] were expected to cost when they last ran, kept rather than recomputed so
     * growth after an out-of-memory kill carries into the retry.
     */
    fun storedEstimates(carried: List<ExecutionUnit>): List<PlannedUnit> =
        carried.map { PlannedUnit(it.unit.scenarioIndex, it.unit.seed, it.estimatedSeconds, it.estimatedPeakMemoryMb) }

    /** Writes down a queued execution of [units] shaped as [bag]; its units are carried from here on. */
    fun queue(
        experiment: Experiment,
        bag: PlannedBag,
        units: List<RunUnit>,
        attempt: Int,
        dispatcher: String,
    ): Execution {
        val execution = Execution()
        execution.experiment = experiment
        execution.state = ExecutionState.QUEUED
        execution.attempt = attempt
        execution.dispatcher = dispatcher
        execution.parallelism = bag.grant.parallelism
        execution.heapMb = bag.grant.heapMb
        execution.memoryRequestMb = bag.grant.memoryRequestMb
        execution.timeLimitSeconds = bag.grant.timeLimitSeconds
        execution.estimatedMakespanSeconds = bag.makespanSeconds
        execution.runtimeMultiplier = config.estimator().runtimeMultiplier()
        execution.memoryMultiplier = config.estimator().memoryMultiplier()
        execution.createdAt = Instant.now()
        execution.persist()

        val byRun = units.associateBy { it.scenarioIndex to it.seed }
        for (planned in bag.units) {
            val unit = byRun[planned.scenarioIndex to planned.seed] ?: continue
            unit.state = UnitState.CARRIED
            // The attempt starts over, so an earlier one's progress no longer counts.
            unit.completedTasks = 0
            val row = ExecutionUnit()
            row.execution = execution
            row.unit = unit
            row.estimatedSeconds = planned.cpuSeconds
            row.estimatedPeakMemoryMb = planned.peakMemoryMb
            row.persist()
            execution.carried += row
        }
        return execution
    }

    /**
     * What one launcher process is handed for [execution]. Every trace the units name is staged and
     * every reference rewritten to its staged path, so the launcher never resolves a name. Every URL is
     * signed for [lifetime]; [token] can only report progress.
     */
    fun manifest(
        execution: Execution,
        token: String,
        lifetime: Duration,
    ): LaunchManifest {
        val experiment = execution.experiment.publicId
        val scenarios = scenariosOf(execution.experiment)
        val staging = Staging(store, lifetime)
        val units =
            ExecutionUnit.findByExecution(execution.id).mapNotNull { row ->
                val unit = row.unit
                val scenario = scenarios[unit.scenarioIndex to unit.seed] ?: return@mapNotNull null
                val run = runKey(experiment, unit.scenarioIndex, unit.seed)
                val files = scenario.exportModel.filesToExport.distinct().map { it.fileName } + MetaFile.entries.map { it.path }
                LaunchUnit(
                    scenario = scenario.mapReferences { staging.stage(it.reference) },
                    outputs = files.map { file -> OutputTarget(file, store.writeUrl("$run/$file", lifetime)) },
                    outcome = store.writeUrl(outcomeKey(experiment, execution.publicId, unit.scenarioIndex, unit.seed), lifetime),
                )
            }
        return LaunchManifest(
            inputs = staging.inputs,
            units = units,
            parallelism = execution.parallelism,
            telemetry = TelemetryTarget.Endpoint(config.telemetryUrl(), token),
        )
    }

    private fun estimate(
        scenarios: Map<Pair<Int, Long>, ScenarioSpec>,
        units: List<RunUnit>,
    ): List<PlannedUnit> {
        val model = estimator()
        return units.mapNotNull { unit -> scenarios[unit.scenarioIndex to unit.seed]?.let { planned(model, it) } }
    }

    /** The dispatch estimate, corrected by this deployment's multipliers. */
    private fun estimator(): ResourceEstimator =
        TraceSizeEstimator(config.estimator().toCoefficients())
            .scaledBy(config.estimator().runtimeMultiplier(), config.estimator().memoryMultiplier())

    private fun planned(
        model: ResourceEstimator,
        scenario: ScenarioSpec,
    ): PlannedUnit {
        val estimate = model.estimate(scenario, traceExtentOf(scenario.workload))
        return PlannedUnit(scenario.id, scenario.seed, estimate.cpuSeconds, estimate.peakMemoryMb)
    }

    private fun scenariosOf(experiment: Experiment): Map<Pair<Int, Long>, ScenarioSpec> =
        codec.decodeExperiment(codec.parseStored(experiment.spec)).expand().associateBy { it.id to it.seed }
}

/**
 * The inputs one manifest stages, each trace once however many units name it. A trace of several
 * tables is referred to as its directory, one of a single table as the file. An unknown name is left
 * as it is, for the launcher to refuse.
 */
private class Staging(
    private val store: ObjectStore,
    private val lifetime: Duration,
) {
    private val directories = mutableMapOf<Long, String>()

    val inputs = mutableListOf<StagedInput>()

    fun stage(reference: ResourceReference): ResourceReference {
        if (reference !is NamedReference) {
            return reference
        }
        val trace = Trace.findBySlug(reference.name) ?: return reference
        val directory =
            directories.getOrPut(trace.id) {
                val path = "$INPUTS/${directories.size}"
                for (table in trace.kind.tables) {
                    val key = traceKey(trace.publicId, table)
                    inputs += StagedInput("$path/$table.parquet", store.readUrl(key, lifetime), key)
                }
                path
            }
        val table = trace.kind.tables.singleOrNull()
        return UriReference(if (table == null) directory else "$directory/$table.parquet")
    }
}

/**
 * How much workload a scenario has to get through: an inline workload counted from the document, a
 * stored trace from what was measured when it was stored, an unmeasured one as nothing. [find] says
 * which traces may be counted: a quote counts only those its author may use.
 */
fun traceExtentOf(
    workload: WorkloadSpec,
    find: (String) -> Trace? = Trace::findBySlug,
): TraceExtent =
    when (workload) {
        is InlineWorkloadSpec ->
            TraceExtent(
                taskCount = workload.tasks.size.toLong(),
                fragmentCount = workload.tasks.sumOf { it.fragments.size }.toLong(),
            )

        is TraceWorkloadSpec -> storedExtentOf(workload.source, find)
        is EfficientTraceWorkloadSpec -> storedExtentOf(workload.source, find)
    }

private fun storedExtentOf(
    source: ResourceReference,
    find: (String) -> Trace?,
): TraceExtent {
    val trace = (source as? NamedReference)?.let { find(it.name) }
    val parts = trace?.let { TracePart.findByTrace(it.id).associateBy { part -> part.tableName } }.orEmpty()
    return TraceExtent(
        taskCount = parts[TABLE_TASKS]?.rowCount ?: 0,
        fragmentCount = parts[TABLE_FRAGMENTS]?.rowCount ?: 0,
    )
}
