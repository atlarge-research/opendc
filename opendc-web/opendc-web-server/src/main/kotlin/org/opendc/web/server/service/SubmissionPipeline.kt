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

package org.opendc.web.server.service

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Event
import jakarta.transaction.Transactional
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import org.opendc.sdk.model.experiment.ExperimentSpec
import org.opendc.sdk.model.experiment.expand
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.resource.ResourceReference
import org.opendc.sdk.model.scenario.ScenarioSpec
import org.opendc.web.dispatcher.Dispatcher
import org.opendc.web.dispatcher.Unfit
import org.opendc.web.dispatcher.estimate.TraceSizeEstimator
import org.opendc.web.dispatcher.estimate.sampledShare
import org.opendc.web.server.execution.DiscardRequested
import org.opendc.web.server.execution.ExecutionConfig
import org.opendc.web.server.execution.ExecutionPlanner
import org.opendc.web.server.execution.Settlement
import org.opendc.web.server.execution.StopRequested
import org.opendc.web.server.execution.toCoefficients
import org.opendc.web.server.execution.traceExtentOf
import org.opendc.web.server.model.Execution
import org.opendc.web.server.model.ExecutionState
import org.opendc.web.server.model.Experiment
import org.opendc.web.server.model.ExperimentResource
import org.opendc.web.server.model.HeldCores
import org.opendc.web.server.model.Project
import org.opendc.web.server.model.RunUnit
import org.opendc.web.server.model.Submission
import org.opendc.web.server.model.TraceKind
import org.opendc.web.server.model.UnitState
import org.opendc.web.server.model.UserAccount
import org.opendc.web.server.rest.DocumentIssue
import org.opendc.web.server.rest.conflict
import org.opendc.web.server.rest.invalidDocument
import org.opendc.web.server.rest.notFound
import org.opendc.web.server.rest.toWire
import org.opendc.web.server.results.ResultsReader
import org.opendc.web.server.traces.TraceResolution
import org.opendc.web.server.traces.refuseUriReferences
import org.opendc.web.server.traces.resolveTraces
import java.time.Instant
import java.util.UUID

/** What an experiment is expected to cost its owner. Billing is in simulation seconds, never memory. */
@Serializable
data class CostEstimate(
    val scenarioCount: Int,
    val estimatedSimulationSeconds: Double,
    val estimatedBudgetSeconds: Double,
)

/** What the draft editor needs to know about a spec before anything is persisted. */
@Serializable
data class SubmissionPreview(
    val scenarioCount: Int,
    val estimate: CostEstimate,
    val issues: List<DocumentIssue>,
)

/**
 * The write path for experiments. Drafts must strict-parse (an unknown key is a 400) but may
 * carry validation issues while being edited; submission is the gate that requires a clean
 * validate(), freezes the document forever and turns it into queued execution records.
 */
@ApplicationScoped
class SubmissionPipeline(
    private val codec: SpecCodec,
    private val config: ExecutionConfig,
    private val planner: ExecutionPlanner,
    private val dispatcher: Dispatcher,
    private val settlement: Settlement,
    private val results: ResultsReader,
    private val stops: Event<StopRequested>,
    private val discards: Event<DiscardRequested>,
) {
    @Transactional
    fun createDraft(
        project: Project,
        name: String,
        document: JsonElement,
        author: UserAccount,
    ): Experiment {
        val spec = codec.decodeExperiment(document)
        refuseUriReferences(spec)
        return newDraft(project, name, spec, author)
    }

    @Transactional
    fun replaceDraft(
        experiment: Experiment,
        name: String,
        document: JsonElement,
        author: UserAccount,
    ) {
        if (!experiment.isDraft) {
            throw conflict("A submitted experiment can no longer be edited")
        }
        val spec = codec.decodeExperiment(document)
        refuseUriReferences(spec)
        applyDraft(experiment, name, spec, author)
    }

    /**
     * What submitting [document] would amount to for [caller]: its issues, including traces they may
     * not use, and its cost counted over only the traces they may.
     */
    fun preview(
        document: JsonElement,
        caller: UserAccount,
    ): SubmissionPreview {
        val spec = codec.decodeExperiment(document)
        val traces = resolveTraces(spec, caller)
        val scenarios = spec.expand()
        return SubmissionPreview(
            scenarioCount = scenarios.distinctBy { it.id }.size,
            estimate = estimate(scenarios, traces),
            issues = spec.validate().toWire() + traces.issues + unfitIssues(scenarios),
        )
    }

    /**
     * One issue per scenario no execution on this deployment can hold, by time or memory, since it
     * would only fail at its limit on every attempt. Its seeds cost the same, so one stands for all.
     */
    private fun unfitIssues(scenarios: List<ScenarioSpec>): List<DocumentIssue> =
        planner.unfit(scenarios.distinctBy { it.id }, dispatcher.slot()).map { unfit ->
            val scenario = "Scenario ${unfit.unit.scenarioIndex}"
            when (unfit) {
                is Unfit.TooLong ->
                    DocumentIssue(
                        "",
                        "$scenario is estimated at ${unfit.seconds} s; an execution here may run at most ${unfit.capSeconds} s",
                    )
                is Unfit.TooLarge -> {
                    val needs = unfit.memoryMb.toLong()
                    DocumentIssue("", "$scenario needs about $needs MB; an execution here may have at most ${unfit.capMb.toLong()} MB")
                }
            }
        }

    /**
     * Freezes a draft and queues its work, if [submitter] may use every trace it names. A teammate's
     * draft can name traces only they hold; whoever submits it has to be able to use them too.
     */
    @Transactional
    fun submit(
        experiment: Experiment,
        submitter: UserAccount,
    ) {
        if (!experiment.isDraft) {
            throw conflict("This experiment has already been submitted")
        }
        val spec = codec.decodeExperiment(codec.parseStored(experiment.spec))
        val issues = spec.validate().toWire() + resolveTraces(spec, submitter).issues
        if (issues.isNotEmpty()) {
            throw invalidDocument("The experiment document is invalid", issues)
        }
        val scenarios = spec.expand()
        if (scenarios.isEmpty()) {
            throw conflict("This experiment expands to no scenarios")
        }
        val unfit = unfitIssues(scenarios)
        if (unfit.isNotEmpty()) {
            throw invalidDocument("Some scenarios cannot run on this deployment", unfit)
        }

        val now = Instant.now()
        // Every seed of a scenario costs the same and runs the same tasks, so each is worked out once.
        val distinct = scenarios.distinctBy { it.id }
        val quotes = distinct.associate { it.id to quoteOf(it) }
        val tasks = distinct.associate { it.id to plannedTaskCount(it) }
        admit(submitter, scenarios.sumOf { quotes.getValue(it.id) }, now)

        extractResources(experiment, spec)
        // Only units are written; shaping them into executions waits for a dispatcher's slot.
        for (scenario in scenarios) {
            val unit = RunUnit()
            unit.experiment = experiment
            unit.scenarioIndex = scenario.id
            unit.seed = scenario.seed
            unit.state = UnitState.QUEUED
            unit.totalTasks = tasks.getValue(scenario.id)
            unit.quotedSeconds = quotes.getValue(scenario.id)
            unit.persist()
        }
        experiment.markSubmitted(submitter, now)
        experiment.updatedAt = now
    }

    /** What one run of [scenario] is expected to cost, before any deployment's correction. */
    private fun quoteOf(scenario: ScenarioSpec): Double =
        TraceSizeEstimator(config.estimator().toCoefficients()).estimate(scenario, traceExtentOf(scenario.workload)).cpuSeconds

    /**
     * Stops everything the experiment still has to do, for good. Its units stay cancelled whatever
     * their executions report; a queued execution is closed outright, and one on the platform is
     * asked to stop once this commits.
     */
    @Transactional
    fun cancel(experiment: Experiment) {
        if (experiment.isDraft) {
            throw conflict("A draft experiment is not running")
        }
        val live =
            Execution
                .findByExperiment(experiment.id)
                .filterNot { it.state.isTerminal }
                .mapNotNull { Execution.lockByPublicId(it.publicId) }
        RunUnit.lockQueued(experiment.id)
        val units = RunUnit.findByExperiment(experiment.id)
        if (units.all { it.state.isTerminal }) {
            throw conflict("This experiment has already finished")
        }
        for (unit in units.filterNot { it.state.isTerminal }) {
            unit.state = UnitState.CANCELLED
        }
        val onPlatform = mutableListOf<UUID>()
        for (execution in live) {
            when (execution.state) {
                ExecutionState.QUEUED -> settlement.withdraw(execution)
                ExecutionState.SUBMITTED, ExecutionState.RUNNING -> onPlatform += execution.publicId
                ExecutionState.SUCCEEDED, ExecutionState.FAILED, ExecutionState.CANCELLED -> {}
            }
        }
        stops.fire(StopRequested(onPlatform))
        experiment.updatedAt = Instant.now()
    }

    /**
     * Queues every run of one stopped scenario again. The output lands where it did before, replacing
     * the earlier attempt's results.
     */
    @Transactional
    fun retryScenario(
        experiment: Experiment,
        scenarioIndex: Int,
    ): List<RunUnit> {
        if (experiment.isDraft) {
            throw conflict("A draft experiment has nothing to restart")
        }
        val units = RunUnit.findByExperiment(experiment.id).filter { it.scenarioIndex == scenarioIndex }
        if (units.isEmpty()) {
            throw notFound("Scenario")
        }
        if (units.any { !it.state.isTerminal }) {
            throw conflict("This scenario is still running")
        }
        // Whoever submitted the experiment pays for it, retries included.
        when (val submission = experiment.submission) {
            Submission.Draft -> {}
            is Submission.Submitted -> admit(submission.by, units.sumOf { it.quotedSeconds }, Instant.now())
        }
        // The earlier attempt's output is about to be overwritten, so no cached reading of it may survive.
        results.forget(experiment.publicId, units)
        for (unit in units) {
            unit.state = UnitState.QUEUED
            unit.completedTasks = 0
        }
        experiment.updatedAt = Instant.now()
        return units
    }

    /** A draft copy of [experiment], kept even when it names traces only its author may use. */
    @Transactional
    fun clone(
        experiment: Experiment,
        name: String,
        author: UserAccount,
    ): Experiment = newDraft(experiment.project, name, codec.decodeExperiment(codec.parseStored(experiment.spec)), author)

    /** Removes an experiment whatever state it is in; immutability governs editing, not deleting. */
    @Transactional
    fun delete(experiment: Experiment) {
        chargeHeld(Execution.heldByExperiment(experiment.id))
        discard(Execution.onPlatformOfExperiment(experiment.id), listOf(experiment.publicId))
        experiment.delete()
    }

    /** Arranges for every experiment of [project], about to be deleted in the caller's transaction, to leave nothing behind. */
    fun discardAll(project: Project) {
        chargeHeld(Execution.heldByProject(project.id))
        discard(Execution.onPlatformOfProject(project.id), Experiment.publicIdsOfProject(project.id))
    }

    /** Charges for what running executions have used so far, since a deleted experiment is never settled. */
    private fun chargeHeld(held: List<HeldCores>) {
        val now = Instant.now()
        for (cores in held) {
            val payer = UserAccount.findById(cores.payerId) ?: continue
            charge(payer, coreSeconds(cores.since, now, cores.cores), now)
        }
    }

    /**
     * Stops what the platform holds of experiments about to be deleted and removes what they kept in
     * the store, once the deletion commits. Addressed by id, never loaded: a loaded row pointing at a
     * deleted experiment fails the flush.
     */
    private fun discard(
        executions: List<UUID>,
        experiments: List<UUID>,
    ) {
        stops.fire(StopRequested(executions))
        discards.fire(DiscardRequested(experiments))
    }

    /**
     * What a document will cost its owner: the dispatch model without any per-deployment correction.
     * Only [traces] the author may use are counted, so a private trace's size never leaks.
     */
    private fun estimate(
        scenarios: List<ScenarioSpec>,
        traces: TraceResolution,
    ): CostEstimate {
        val model = TraceSizeEstimator(config.estimator().toCoefficients())
        val seconds =
            scenarios.sumOf { scenario -> model.estimate(scenario, traceExtentOf(scenario.workload, traces.usable::get)).cpuSeconds }
        return CostEstimate(
            scenarioCount = scenarios.distinctBy { it.id }.size,
            estimatedSimulationSeconds = seconds,
            estimatedBudgetSeconds = seconds,
        )
    }

    /**
     * How many tasks one run of [scenario] has to get through, fixed at submit so a progress bar's
     * denominator never moves. A sampled run picks tasks by load, so it lands near this, not on it.
     */
    private fun plannedTaskCount(scenario: ScenarioSpec): Int {
        val sampled = traceExtentOf(scenario.workload).taskCount * sampledShare(scenario.workload)
        return sampled.toLong().coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    }

    private fun newDraft(
        project: Project,
        name: String,
        spec: ExperimentSpec,
        author: UserAccount,
    ): Experiment {
        val experiment = Experiment()
        experiment.project = project
        experiment.createdAt = Instant.now()
        applyDraft(experiment, name, spec, author)
        experiment.persist()
        project.updatedAt = experiment.updatedAt
        return experiment
    }

    private fun applyDraft(
        experiment: Experiment,
        name: String,
        spec: ExperimentSpec,
        author: UserAccount,
    ) {
        val canonical = codec.canonical(spec)
        val estimate = estimate(spec.expand(), resolveTraces(spec, author))
        experiment.name = name
        experiment.spec = canonical
        experiment.specHash = codec.hash(canonical)
        experiment.scenarioCount = estimate.scenarioCount
        experiment.estimatedSimulationSeconds = estimate.estimatedSimulationSeconds
        experiment.estimatedBudgetSeconds = estimate.estimatedBudgetSeconds
        experiment.updatedAt = Instant.now()
    }

    private fun extractResources(
        experiment: Experiment,
        spec: ExperimentSpec,
    ) {
        for (use in spec.references()) {
            val row = ExperimentResource()
            row.experiment = experiment
            row.kind = TraceKind.of(use.role)
            row.reference = codec.json.encodeToString<ResourceReference>(use.reference)
            row.referenceName = (use.reference as? NamedReference)?.name
            row.persist()
        }
    }
}
