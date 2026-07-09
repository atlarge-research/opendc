/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.runner.internal

import org.opendc.compute.simulator.provisioner.Provisioner
import org.opendc.compute.simulator.provisioner.ProvisioningContext
import org.opendc.compute.simulator.provisioner.registerComputeMonitor
import org.opendc.compute.simulator.provisioner.setupComputeService
import org.opendc.compute.simulator.provisioner.setupHosts
import org.opendc.compute.simulator.scheduler.ComputeScheduler
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.simulator.telemetry.OutputFiles
import org.opendc.compute.topology.specs.ClusterSpec
import org.opendc.sdk.model.experiment.Scenario
import org.opendc.sdk.model.resource.ResourceProvisioner
import org.opendc.sdk.model.scheduler.TimeShiftAllocationPolicy
import org.opendc.sdk.runner.RunResult
import org.opendc.sdk.runner.sink.OutputSink
import org.opendc.sdk.runner.sink.RunContext
import org.opendc.sdk.runner.sink.SinkSession
import org.opendc.simulator.compute.power.CarbonModel
import org.opendc.simulator.compute.power.CarbonReceiver
import org.opendc.simulator.kotlin.SimulationCoroutineScope
import org.opendc.simulator.kotlin.runSimulation
import java.time.Duration
import java.time.InstantSource
import java.util.Random
import kotlin.coroutines.coroutineContext

private const val SERVICE_DOMAIN = "compute.opendc.org"

/**
 * Runs a single [scenario] for one [seed] to completion on a fresh simulated clock and returns the
 * metrics harvested by the [sinks].
 */
internal fun runScenario(
    scenario: Scenario,
    experimentName: String,
    scenarioId: Int,
    seed: Long,
    sinks: List<OutputSink>,
    provisioner: ResourceProvisioner,
): RunResult {
    var result: RunResult? = null
    runSimulation {
        ResourceScope(provisioner).use { resources ->
            Provisioner(dispatcher, seed).use { engine ->
                result = ScenarioRun(this, engine, resources, scenario, experimentName, scenarioId, seed, sinks).execute()
            }
        }
    }
    return result ?: error("scenario produced no result")
}

/**
 * The wiring and execution of one scenario run, holding the simulation scope and engine provisioner
 * so each step reads as a single intention.
 */
private class ScenarioRun(
    private val sim: SimulationCoroutineScope,
    private val engine: Provisioner,
    private val resources: ResourceScope,
    private val scenario: Scenario,
    private val experimentName: String,
    private val scenarioId: Int,
    private val seed: Long,
    private val sinks: List<OutputSink>,
) {
    private val clock: InstantSource get() = sim.timeSource
    private val service: ComputeService get() = engine.resolve(ComputeService::class.java)

    suspend fun execute(): RunResult {
        val workload = scenario.workload.toServiceTasks(scenario.checkpointModel, resources::resolve)
        val startTime = workload.minOf { it.submittedAt }
        val clusters = scenario.topology.toClusterSpecs(resources::resolve)

        provisionDatacenter(clusters, startTime)
        val sessions = attachSinks(clusters.gpuCount(), startTime, workload.size)
        connectCarbonModel()

        service.replay(clock, workload, scenario.failureModel, seed, resources::resolve)
        return RunResult(seed, sessions.mapNotNull { it.result() })
    }

    private fun provisionDatacenter(
        clusters: List<ClusterSpec>,
        startTime: Long,
    ) {
        val numHosts = clusters.sumOf { it.hostSpecs.size }
        engine.runSteps(
            setupComputeService(SERVICE_DOMAIN, { it.createScheduler(numHosts) }, maxNumFailures = scenario.maxNumFailures),
            setupHosts(SERVICE_DOMAIN, clusters, startTime),
        )
    }

    private fun ProvisioningContext.createScheduler(numHosts: Int): ComputeScheduler {
        val scheduler = scenario.allocationPolicy.toScheduler(Random(seeder.nextLong()), clock, numHosts)
        registry.register(SERVICE_DOMAIN, ComputeScheduler::class.java, scheduler)
        return scheduler
    }

    private fun attachSinks(
        gpuCount: Int,
        startTime: Long,
        taskCount: Int,
    ): List<SinkSession> {
        val export = scenario.exportModel.toExportSettings(gpuCount)
        val context = RunContext(scenario, experimentName, scenarioId, seed, gpuCount, taskCount, export)
        val sessions = sinks.map { it.open(context) }
        val monitor = CompositeComputeMonitor(sessions.map { it.monitor })
        val recorded = OutputFiles.entries.associateWith { file -> sessions.any { file in it.tables } }
        engine.runStep(
            registerComputeMonitor(
                SERVICE_DOMAIN,
                monitor,
                export.exportInterval,
                Duration.ofMillis(startTime),
                recorded,
                export.printFrequency,
            ),
        )
        service.setTasksExpected(taskCount)
        service.setMetricReader(engine.getMonitor()!!)
        return sessions
    }

    /** Connects a carbon-intensity trace, if the topology declares one, to every carbon-aware component. */
    private suspend fun connectCarbonModel() {
        val carbon = engine.resolveOrNull(CarbonModel::class.java) ?: return
        val scheduler = engine.resolve(ComputeScheduler::class.java)
        if (scheduler is CarbonReceiver) {
            carbon.addReceiver(scheduler)
            carbon.addReceiver(service)
        }
        connectTaskStopper(carbon)
    }

    private suspend fun connectTaskStopper(carbon: CarbonModel) {
        val policy = scenario.allocationPolicy as? TimeShiftAllocationPolicy ?: return
        val taskStopper = policy.taskStopper.toEngine(coroutineContext, clock) ?: return
        taskStopper.setService(service)
        carbon.addReceiver(taskStopper)
    }
}

private fun List<ClusterSpec>.gpuCount(): Int = flatMap { it.hostSpecs }.maxOfOrNull { it.model.gpuModels.size } ?: 0

private fun <T : Any> Provisioner.resolve(type: Class<T>): T = registry.resolve(SERVICE_DOMAIN, type)!!

private fun <T : Any> Provisioner.resolveOrNull(type: Class<T>): T? =
    if (registry.hasService(SERVICE_DOMAIN, type)) registry.resolve(SERVICE_DOMAIN, type) else null
