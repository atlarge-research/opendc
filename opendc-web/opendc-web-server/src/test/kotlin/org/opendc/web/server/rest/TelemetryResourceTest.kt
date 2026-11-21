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

package org.opendc.web.server.rest

import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.launcher.ResultMetric
import org.opendc.web.launcher.TelemetryTarget
import org.opendc.web.launcher.UnitOutcome
import org.opendc.web.server.ApiTest
import org.opendc.web.server.TestAccounts
import org.opendc.web.server.execution.ExecutionLoop
import org.opendc.web.server.execution.RecordingDispatcher
import org.opendc.web.server.execution.ended
import org.opendc.web.server.model.Execution
import org.opendc.web.server.model.Experiment
import java.util.UUID

/** The one endpoint whose caller is a launcher's token rather than a person. */
@QuarkusTest
class TelemetryResourceTest {
    @Inject
    lateinit var loop: ExecutionLoop

    @Inject
    lateinit var dispatcher: RecordingDispatcher

    private lateinit var projectId: String

    @BeforeEach
    fun seedProject() {
        dispatcher.forget()
        projectId =
            ApiTest.requestJson()
                .body("""{"name":"Telemetry ${UUID.randomUUID()}"}""")
                .post("/api/v1/projects")
                .then()
                .statusCode(201)
                .extract()
                .path("id")
    }

    @Test
    fun `progress a launcher reports becomes the progress the experiment shows`() {
        val experiment = running()

        report(tokenFor(experiment), completedTasks = 3).then().statusCode(204)

        ApiTest
            .requestJson()
            .get("/api/v1/experiments/$experiment/status")
            .then()
            .statusCode(200)
            .body("completedTasks", equalTo(3))
            .body("totalTasks", equalTo(TASK_COUNT))
            .body("scenarios[0].completedTasks", equalTo(3))
    }

    // The total is read off the document, so a bar has a fixed scale from the moment of submission.
    @Test
    fun `knows how much work an experiment is before any of it has run`() {
        val experiment = submitted()

        ApiTest
            .requestJson()
            .get("/api/v1/experiments/$experiment/status")
            .then()
            .statusCode(200)
            .body("state", equalTo("queued"))
            .body("completedTasks", equalTo(0))
            .body("totalTasks", equalTo(TASK_COUNT))
    }

    // Telemetry is not authoritative, so a launcher that has miscounted must not be able to show an
    // experiment as more than finished.
    @Test
    fun `clamps a launcher that reports more work done than there is`() {
        val experiment = running()

        report(tokenFor(experiment), completedTasks = 99).then().statusCode(204)

        ApiTest
            .requestJson()
            .get("/api/v1/experiments/$experiment/status")
            .then()
            .body("completedTasks", equalTo(TASK_COUNT))
    }

    // The last report of a run can be lost, and a sampled run never lands on the planned count in the
    // first place. Neither is a reason to leave the bar short of the end of a scenario that is over.
    @Test
    fun `a run that finished counts as all of its work, whatever its last report said`() {
        val experiment = running()
        report(tokenFor(experiment), completedTasks = 3).then().statusCode(204)

        succeeded(experiment)

        ApiTest
            .requestJson()
            .get("/api/v1/experiments/$experiment/status")
            .then()
            .body("completedTasks", equalTo(TASK_COUNT))
            .body("totalTasks", equalTo(TASK_COUNT))
    }

    // Otherwise the scenario would read complete until the new run reported over the top of it.
    @Test
    fun `running a scenario again puts its progress back to nothing`() {
        val experiment = running()
        report(tokenFor(experiment), completedTasks = TASK_COUNT).then().statusCode(204)
        succeeded(experiment)

        ApiTest.requestJson().post("/api/v1/experiments/$experiment/scenarios/0/retry").then().statusCode(200)

        ApiTest
            .requestJson()
            .get("/api/v1/experiments/$experiment/status")
            .then()
            .body("completedTasks", equalTo(0))
            .body("state", equalTo("queued"))
            // The denominator belongs to the workload, not the attempt.
            .body("totalTasks", equalTo(TASK_COUNT))
    }

    @Test
    fun `refuses a token nobody minted`() {
        running()

        report("odc_exec_invented", completedTasks = 1).then().statusCode(401)
    }

    // A launcher's token reports progress on its own execution and can do nothing else, whichever
    // mode the deployment signs people in with.
    @Test
    fun `a launcher token cannot act as a person`() {
        val token = tokenFor(running())

        ApiTest.requestJson().header("Authorization", "Bearer $token").get("/api/v1/projects").then().statusCode(403)
    }

    // In anonymous mode a caller with no credential is the local account, which is a person and not a
    // launcher; a person's access token is no launcher's either.
    @Test
    fun `refuses a report from anyone but a launcher`() {
        running()

        ApiTest.requestJson().body(body(completedTasks = 0)).post("/api/v1/telemetry").then().statusCode(403)
        TestAccounts.person().request().body(body(completedTasks = 0)).post("/api/v1/telemetry").then().statusCode(403)
    }

    // A token dies with the attempt it was minted for. A process nobody managed to stop must not be
    // able to write over the account of a run that has already been settled.
    @Test
    fun `refuses a report for an execution that has already finished`() {
        val experiment = running()
        val token = tokenFor(experiment)
        succeeded(experiment)

        report(token, completedTasks = 5).then().statusCode(409)
    }

    // One token writes one bag's progress. A launcher that names work it was not handed is not a
    // reason to lose the runs it got right, so the rest of the report still lands.
    @Test
    fun `ignores progress for work its execution is not carrying`() {
        val experiment = running()
        val body =
            """
            {"runs":[
                {"scenarioIndex":0,"seed":0,"completedTasks":4,"series":[]},
                {"scenarioIndex":99,"seed":0,"completedTasks":7,"series":[]}
            ]}
            """.trimIndent()

        ApiTest
            .requestJson()
            .header("Authorization", "Bearer ${tokenFor(experiment)}")
            .body(body)
            .post("/api/v1/telemetry")
            .then()
            .statusCode(204)

        ApiTest
            .requestJson()
            .get("/api/v1/experiments/$experiment/status")
            .then()
            .body("completedTasks", equalTo(4))
            .body("scenarios.size()", equalTo(1))
    }

    @Test
    fun `samples a launcher posts come back as the chart series for its scenario`() {
        val experiment = running()
        val body =
            """
            {"runs":[{"scenarioIndex":0,"seed":0,"completedTasks":1,"series":[
                {"metric":"${ResultMetric.HOST_CPU_UTILIZATION.id}","points":[{"t":0,"value":0.25},{"t":$EXPORT_INTERVAL_MS,"value":0.75}]}
            ]}]}
            """.trimIndent()

        ApiTest
            .requestJson()
            .header("Authorization", "Bearer ${tokenFor(experiment)}")
            .body(body)
            .post("/api/v1/telemetry")
            .then()
            .statusCode(204)

        ApiTest
            .requestJson()
            .get("/api/v1/experiments/$experiment/results")
            .then()
            .statusCode(200)
            .body("complete", equalTo(false))
            .body("scenarios[0].scenarioIndex", equalTo(0))
            .body("scenarios[0].series[0].metric", equalTo(ResultMetric.HOST_CPU_UTILIZATION.id))
            .body("scenarios[0].series[0].points.size()", equalTo(2))
            .body("scenarios[0].series[0].points[1].value", equalTo(0.75f))
    }

    @Test
    fun `folds a metric into no more buckets than were asked for`() {
        val experiment = running()
        val points = (0 until 20).joinToString(",") { """{"t":${it * EXPORT_INTERVAL_MS},"value":$it.0}""" }
        val body =
            """
            {"runs":[{"scenarioIndex":0,"seed":0,"completedTasks":1,"series":[
                {"metric":"${ResultMetric.HOST_CPU_UTILIZATION.id}","points":[$points]}
            ]}]}
            """.trimIndent()
        ApiTest
            .requestJson()
            .header("Authorization", "Bearer ${tokenFor(experiment)}")
            .body(body)
            .post("/api/v1/telemetry")
            .then()
            .statusCode(204)

        ApiTest
            .requestJson()
            .get("/api/v1/experiments/$experiment/results?buckets=4")
            .then()
            .statusCode(200)
            .body("scenarios[0].series[0].points.size()", allOf(greaterThan(1), lessThanOrEqualTo(4)))
    }

    /** An experiment whose single unit is queued, with nothing yet handed to the platform. */
    private fun submitted(): String {
        val publicId =
            ApiTest.requestJson()
                .body("""{"projectId":"$projectId","name":"Telemetry","spec":$SPEC}""")
                .post("/api/v1/experiments")
                .then()
                .statusCode(201)
                .extract()
                .path<String>("id")
        ApiTest.requestJson().post("/api/v1/experiments/$publicId/submit").then().statusCode(200)
        return publicId
    }

    /** An experiment whose single unit the platform has been handed, so a token exists for it. */
    private fun running(): String = submitted().also { loop.drain() }

    /** The credential the server handed the launcher, read from its manifest: only the hash is stored. */
    private fun tokenFor(experiment: String): String {
        val target = dispatcher.manifestOf(executionOf(experiment)).telemetry
        check(target is TelemetryTarget.Endpoint) { "$experiment was launched with nowhere to report to" }
        return target.token
    }

    /** The platform reports the run ended, after its launcher certified it. */
    private fun succeeded(experiment: String) {
        val execution = executionOf(experiment)
        dispatcher.certify(execution, 0, UnitOutcome.Succeeded(1.0))
        dispatcher.finish(execution, ended(ExitReason.OK))
    }

    /** Which execution is carrying this experiment, since other cases' work shares the platform. */
    private fun executionOf(experiment: String): UUID =
        QuarkusTransaction.requiringNew().call {
            val id = checkNotNull(Experiment.findByPublicId(UUID.fromString(experiment))).id
            Execution.findByExperiment(id).single().publicId
        }

    private fun report(
        token: String,
        completedTasks: Int,
    ) = ApiTest
        .requestJson()
        .header("Authorization", "Bearer $token")
        .body(body(completedTasks))
        .post("/api/v1/telemetry")

    private fun body(completedTasks: Int) = """{"runs":[{"scenarioIndex":0,"seed":0,"completedTasks":$completedTasks,"series":[]}]}"""

    private companion object {
        const val TOPOLOGY =
            """{"datacenters":[{"clusters":[{"name":"C0","hosts":[{"name":"H0","count":1,""" +
                """"cpu":{"coreCount":4,"coreSpeed":"2.5 GHz"},"memory":{"size":"16 GiB"}}]}]}]}"""

        /** How many tasks the document describes, and so the denominator every bar here reads against. */
        const val TASK_COUNT = 8

        val WORKLOAD =
            (0 until TASK_COUNT).joinToString(
                separator = ",",
                prefix = """{"type":"inline","tasks":[""",
                postfix = "]}",
            ) { id ->
                """{"id":$id,"submissionTime":"0 ms","duration":"10 minutes",""" +
                    """"cpuCoreCount":1,"cpuCapacity":"1 GHz","memory":"1 GiB",""" +
                    """"fragments":[{"duration":"10 minutes","cpuUsage":"1 GHz"}]}"""
            }

        val SPEC = """{"name":"telemetry","topologies":[$TOPOLOGY],"workloads":[$WORKLOAD],"runs":1}"""

        /** How often a run of [SPEC] samples: it names no export model, so the default's five minutes. */
        const val EXPORT_INTERVAL_MS = 300_000L
    }
}
