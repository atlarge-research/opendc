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

import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.sdk.model.resource.UriReference
import org.opendc.sdk.model.telemetry.MetaFile
import org.opendc.sdk.model.telemetry.OutputFileSpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.Launch
import org.opendc.web.dispatcher.PlatformVerdict
import org.opendc.web.launcher.UnitFailure
import org.opendc.web.launcher.UnitOutcome
import org.opendc.web.server.ApiTest
import org.opendc.web.server.model.Execution
import org.opendc.web.server.model.ExecutionState
import org.opendc.web.server.model.ExecutionUnit
import org.opendc.web.server.model.Experiment
import org.opendc.web.server.model.ObservedSpan
import org.opendc.web.server.model.RunUnit
import org.opendc.web.server.model.UnitState
import org.opendc.web.server.storage.ObjectStore
import org.opendc.web.server.storage.experimentKey
import java.time.Instant
import java.util.UUID

/** What a machine is asked to run and what happens to work that failed, with the platform driven by hand. */
@QuarkusTest
class ExecutionLoopTest {
    @Inject
    lateinit var loop: ExecutionLoop

    @Inject
    lateinit var dispatcher: RecordingDispatcher

    @Inject
    lateinit var store: ObjectStore

    private lateinit var projectId: String

    @BeforeEach
    fun seedProject() {
        // Work other cases left queued would otherwise be handed out first, and take the answers a
        // case scripted for its own launch.
        loop.drain()
        dispatcher.forget()
        projectId =
            ApiTest.requestJson()
                .body("""{"name":"Executions ${UUID.randomUUID()}"}""")
                .post("/api/v1/projects")
                .then()
                .statusCode(201)
                .extract()
                .path("id")
    }

    // A bag that fits is dispatched whole, not one scenario at a time.
    @Test
    fun `runs everything that fits in one execution rather than one scenario at a time`() {
        val experiment = submitted(topologies = 3, runs = 2)

        loop.drain()

        val execution = executionsOf(experiment).single()
        assertEquals(6, carriedBy(execution).size, "six runs fit in one slot and should have gone together")
        assertEquals(6, execution.parallelism, "a bag runs every unit it holds at once")
        assertEquals(ExecutionState.SUBMITTED, execution.state)
        assertTrue(unitsOf(experiment).all { it.state == UnitState.CARRIED })
    }

    @Test
    fun `never hands the same work to the platform twice`() {
        val experiment = submitted()

        loop.drain()
        loop.drain()

        assertEquals(1, executionsOf(experiment).size)
        assertEquals(1, launchedFor(experiment).size)
    }

    @Test
    fun `marks the work done once the platform says it ended and the launcher certified it`() {
        val experiment = submitted()
        loop.drain()
        val execution = executionsOf(experiment).single().publicId

        dispatcher.certify(execution, 0, UnitOutcome.Succeeded(12.0))
        dispatcher.finish(execution, ended(ExitReason.OK))

        assertTrue(unitsOf(experiment).all { it.state == UnitState.SUCCEEDED })
        assertEquals(ExecutionState.SUCCEEDED, executionsOf(experiment).single().state)
    }

    // A process that ended normally without certifying a unit did not finish it, whatever its exit
    // code said, so the unit is not reported as a success it never was.
    @Test
    fun `does not take a normal exit as having done work nobody certified`() {
        val experiment = submitted()
        loop.drain()

        dispatcher.finish(executionsOf(experiment).single().publicId, ended(ExitReason.OK))

        assertTrue(unitsOf(experiment).none { it.state == UnitState.SUCCEEDED })
        assertEquals(2, executionsOf(experiment).size, "an unexplained failure goes round again")
    }

    // An out-of-memory kill says the units needed more than they were given. The one a launcher
    // certified before the kill is done; the others come back with more memory each.
    @Test
    fun `retries only what an out-of-memory kill left unfinished, with more memory per unit`() {
        val experiment = submitted(topologies = 3)
        loop.drain()
        val first = executionsOf(experiment).single()
        val estimated = carriedBy(first).associate { it.unit.scenarioIndex to it.estimatedPeakMemoryMb }

        dispatcher.certify(first.publicId, 0, UnitOutcome.Succeeded(5.0))
        dispatcher.finish(first.publicId, ended(ExitReason.OOM, 3, "ran out of heap"))

        val units = unitsOf(experiment).associateBy { it.scenarioIndex }
        assertEquals(UnitState.SUCCEEDED, units.getValue(0).state, "certified work is not run again")
        val retried = executionsOf(experiment).filter { it.id != first.id }.flatMap { carriedBy(it) }
        assertEquals(setOf(1, 2), retried.map { it.unit.scenarioIndex }.toSet())
        for (row in retried) {
            assertTrue(
                row.estimatedPeakMemoryMb > estimated.getValue(row.unit.scenarioIndex),
                "unit ${row.unit.scenarioIndex} must come back bigger than the estimate that ran out",
            )
        }
        assertTrue(executionsOf(experiment).filter { it.id != first.id }.all { it.attempt == 2 })
    }

    // A spec the simulator refused will be refused again however much is thrown at it.
    @Test
    fun `gives up on a failure that no retry could change`() {
        val experiment = submitted()
        loop.drain()

        dispatcher.finish(executionsOf(experiment).single().publicId, ended(ExitReason.SIMULATION_ERROR, 21, "the simulation failed"))

        assertEquals(1, executionsOf(experiment).size, "nothing should have been tried again")
        assertTrue(unitsOf(experiment).all { it.state == UnitState.FAILED })
    }

    @Test
    fun `reports an experiment whose units ended differently as partial`() {
        val experiment = submitted(topologies = 2)
        loop.drain()
        val execution = executionsOf(experiment).single().publicId

        dispatcher.certify(execution, 0, UnitOutcome.Succeeded(5.0))
        dispatcher.certify(execution, 1, UnitOutcome.Failed(UnitFailure.SIMULATION_ERROR, "threw"))
        dispatcher.finish(execution, ended(ExitReason.OK, 23, "some units failed"))

        ApiTest.requestJson().get(
            "/api/v1/experiments/${experiment.first}/status",
        ).then().statusCode(200).extract().path<String>("state").let {
            assertEquals("partial", it)
        }
        val failed = ApiTest.requestJson().get("/api/v1/experiments/${experiment.first}/scenarios/1").then().extract()
        assertEquals("simulationError", failed.path<String>("status.exitInfo.reason"))
        assertEquals("threw", failed.path<String>("status.exitInfo.message"))
    }

    // A refusal is the platform saying it will never run this, so asking again would loop forever.
    @Test
    fun `fails work the platform refused, and does not offer it again`() {
        val experiment = submitted()
        dispatcher.answer(Launch.Rejected("no such image"))

        loop.drain()
        loop.drain()

        assertEquals(1, launchedFor(experiment).size, "a refused execution is not offered twice")
        assertTrue(unitsOf(experiment).all { it.state == UnitState.FAILED })
        val status = ApiTest.requestJson().get("/api/v1/experiments/${experiment.first}/scenarios/0").then().extract()
        assertEquals("rejected", status.path<String>("status.exitInfo.reason"))
    }

    // A platform that cannot take work now has not said anything about the work, so the same
    // execution goes again later rather than a fresh one at a later attempt.
    @Test
    fun `offers the same execution again after the platform could not take it`() {
        val experiment = submitted()
        dispatcher.answer(Launch.Unavailable("quota"))

        loop.drain()
        loop.drain()

        assertEquals(1, executionsOf(experiment).size)
        val launches = launchedFor(experiment)
        assertEquals(2, launches.size)
        assertEquals(launches.first(), launches.last(), "the same execution was offered again")
        assertEquals(1, executionsOf(experiment).single().attempt, "an unavailable platform does not use up an attempt")
    }

    @Test
    fun `records when the platform started the work`() {
        val experiment = submitted()
        loop.drain()
        val execution = executionsOf(experiment).single().publicId

        dispatcher.start(execution)

        val started = executionsOf(experiment).single()
        assertEquals(ExecutionState.RUNNING, started.state)
        assertTrue(started.span is ObservedSpan.Started)
    }

    @Test
    fun `adopts what the platform is still running after a restart instead of launching it again`() {
        val experiment = submitted()
        loop.drain()
        val execution = executionsOf(experiment).single().publicId
        dispatcher.knows(execution, PlatformVerdict.Running(Instant.now()))

        loop.reconcile()

        assertEquals(ExecutionState.RUNNING, executionsOf(experiment).single().state)
        assertEquals(1, launchedFor(experiment).size)
    }

    @Test
    fun `settles what ended while nobody was listening from what it left behind`() {
        val experiment = submitted()
        loop.drain()
        val execution = executionsOf(experiment).single().publicId
        dispatcher.certify(execution, 0, UnitOutcome.Succeeded(3.0))
        dispatcher.knows(execution, PlatformVerdict.Ended(ended(ExitReason.OK)))

        loop.reconcile()

        assertTrue(unitsOf(experiment).all { it.state == UnitState.SUCCEEDED })
        assertEquals(1, executionsOf(experiment).size, "finished work is not run again")
    }

    // Losing work while the server was down says nothing about the work.
    @Test
    fun `sends work the platform forgot round again without using up an attempt`() {
        val experiment = submitted()
        loop.drain()

        loop.reconcile()

        val executions = executionsOf(experiment)
        assertEquals(2, executions.size)
        assertEquals(listOf(1, 1), executions.map { it.attempt })
    }

    // Cancelling has to beat a retry that was already written down, or a cancelled experiment wakes
    // up again on the next pass.
    @Test
    fun `keeps a cancelled experiment stopped, retries and all`() {
        val experiment = submitted(topologies = 2)
        loop.drain()
        val first = executionsOf(experiment).single().publicId
        dispatcher.finish(first, ended(ExitReason.OOM, 3))
        val retry = executionsOf(experiment).first { it.publicId != first }

        ApiTest.requestJson().post("/api/v1/experiments/${experiment.first}/cancel").then().statusCode(200)
        loop.drain()

        assertTrue(unitsOf(experiment).all { it.state == UnitState.CANCELLED })
        assertEquals(ExecutionState.CANCELLED, executionsOf(experiment).single { it.id == retry.id }.state)
        assertEquals(1, launchedFor(experiment).size, "the cancelled retry was never handed out")
    }

    @Test
    fun `asks the platform to stop work it holds, and does not run what it reports afterwards`() {
        val experiment = submitted()
        loop.drain()
        val execution = executionsOf(experiment).single().publicId

        ApiTest.requestJson().post("/api/v1/experiments/${experiment.first}/cancel").then().statusCode(200)
        dispatcher.finish(execution, ended(ExitReason.OOM, 3))

        assertTrue(execution in dispatcher.cancelled)
        assertTrue(unitsOf(experiment).all { it.state == UnitState.CANCELLED }, "a cancelled unit is not retried")
        assertEquals(1, executionsOf(experiment).size)
    }

    @Test
    fun `stops and removes everything a deleted experiment had`() {
        val experiment = submitted()
        loop.drain()
        val execution = executionsOf(experiment).single().publicId
        val publicId = UUID.fromString(experiment.first)
        assertTrue(store.list(experimentKey(publicId)).isNotEmpty(), "a launched execution has a manifest in the store")

        ApiTest.requestJson().delete("/api/v1/experiments/${experiment.first}").then().statusCode(204)

        assertTrue(execution in dispatcher.cancelled)
        assertTrue(store.list(experimentKey(publicId)).isEmpty())
    }

    @Test
    fun `restarting one scenario queues it again without touching the others`() {
        val experiment = submitted(topologies = 2)
        loop.drain()
        val execution = executionsOf(experiment).single().publicId
        dispatcher.certify(execution, 0, UnitOutcome.Succeeded(1.0))
        dispatcher.certify(execution, 1, UnitOutcome.Succeeded(1.0))
        dispatcher.finish(execution, ended(ExitReason.OK))
        dispatcher.forget()

        ApiTest.requestJson().post("/api/v1/experiments/${experiment.first}/scenarios/0/retry").then().statusCode(200)
        loop.drain()

        val restarted = executionsOf(experiment).last()
        assertEquals(listOf(0), carriedBy(restarted).map { it.unit.scenarioIndex }.distinct())
        assertEquals(1, launchedFor(experiment).size, "only the restarted scenario should have been dispatched")
        assertEquals(
            UnitState.SUCCEEDED,
            unitsOf(experiment).single {
                it.scenarioIndex == 1
            }.state,
            "a scenario nobody restarted keeps its result",
        )
    }

    // The launcher resolves nothing itself: every trace is staged under its working directory and
    // every reference points there, and every file the run writes has a target of its own.
    @Test
    fun `hands a launcher its traces to stage and a target for each file it writes`() {
        val experiment = submitted(workload = BUILT_IN_WORKLOAD)
        loop.drain()

        val manifest = dispatcher.manifestOf(executionsOf(experiment).single().publicId)

        assertEquals(listOf("inputs/0/tasks.parquet", "inputs/0/fragments.parquet"), manifest.inputs.map { it.path })
        assertTrue(manifest.inputs.all { it.source.startsWith("file:") })
        val unit = manifest.units.single()
        assertEquals(UriReference("inputs/0"), (unit.scenario.workload as TraceWorkloadSpec).source)
        assertEquals(
            (OutputFileSpec.entries.map { it.fileName } + MetaFile.entries.map { it.path }).toSet(),
            unit.outputs.map { it.file }.toSet(),
        )
    }

    @Test
    fun `hands each seed of a scenario to a launcher as its own unit`() {
        val experiment = submitted(runs = 3)
        loop.drain()

        val units = executionsOf(experiment).flatMap { dispatcher.manifestOf(it.publicId).units }

        assertEquals(listOf(0L, 1L, 2L), units.map { it.scenario.seed }.sorted())
        assertTrue(units.all { it.scenario.id == 0 })
    }

    /** An experiment of [topologies] x [runs] units, submitted and waiting for the platform. */
    private fun submitted(
        topologies: Int = 1,
        runs: Int = 1,
        workload: String = WORKLOAD,
    ): Pair<String, Long> {
        val publicId =
            ApiTest.requestJson()
                .body("""{"projectId":"$projectId","name":"Loop","spec":${spec(topologies, runs, workload)}}""")
                .post("/api/v1/experiments")
                .then()
                .statusCode(201)
                .extract()
                .path<String>("id")
        ApiTest.requestJson().post("/api/v1/experiments/$publicId/submit").then().statusCode(200)
        val id = QuarkusTransaction.requiringNew().call { checkNotNull(Experiment.findByPublicId(UUID.fromString(publicId))).id }
        return publicId to id
    }

    private fun executionsOf(experiment: Pair<String, Long>): List<Execution> =
        QuarkusTransaction.requiringNew().call { Execution.findByExperiment(experiment.second) }

    private fun carriedBy(execution: Execution): List<ExecutionUnit> =
        QuarkusTransaction.requiringNew().call { ExecutionUnit.findByExecution(execution.id) }

    private fun unitsOf(experiment: Pair<String, Long>): List<RunUnit> =
        QuarkusTransaction.requiringNew().call { RunUnit.findByExperiment(experiment.second) }

    /** What the platform was asked to start for this experiment, ignoring other cases' work. */
    private fun launchedFor(experiment: Pair<String, Long>): List<UUID> {
        val ours = executionsOf(experiment).map { it.publicId }.toSet()
        return dispatcher.launched.map { it.executionId }.filter { it in ours }
    }

    private fun spec(
        topologies: Int,
        runs: Int,
        workload: String,
    ): String {
        val hosts = (1..topologies).joinToString(",") { count -> TOPOLOGY.format(count) }
        return """{"name":"loop","topologies":[$hosts],"workloads":[$workload],"runs":$runs}"""
    }

    private companion object {
        const val TOPOLOGY =
            """{"datacenters":[{"clusters":[{"name":"C0","hosts":[{"name":"H0","count":%d,""" +
                """"cpu":{"coreCount":4,"coreSpeed":"2.5 GHz"},"memory":{"size":"16 GiB"}}]}]}]}"""

        const val WORKLOAD =
            """{"type":"inline","tasks":[{"id":0,"submissionTime":"0 ms","duration":"10 minutes",""" +
                """"cpuCoreCount":1,"cpuCapacity":"1 GHz","memory":"1 GiB",""" +
                """"fragments":[{"duration":"10 minutes","cpuUsage":"1 GHz"}]}]}"""

        const val BUILT_IN_WORKLOAD = """{"type":"trace","source":{"type":"named","name":"bitbrains-small"}}"""
    }
}
