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
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.web.dispatcher.ExitOutcome
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.PlatformSpan
import org.opendc.web.launcher.PeakMemory
import org.opendc.web.launcher.UnitFailure
import org.opendc.web.launcher.UnitOutcome
import org.opendc.web.server.ApiTest
import org.opendc.web.server.execution.ExecutionLoop
import org.opendc.web.server.execution.RecordingDispatcher
import org.opendc.web.server.model.Execution
import org.opendc.web.server.model.Experiment
import java.util.UUID

/** A scenario seen up close: each run's attempts and what its launcher wrote about it. */
@QuarkusTest
class ScenarioDetailTest {
    @Inject
    lateinit var loop: ExecutionLoop

    @Inject
    lateinit var dispatcher: RecordingDispatcher

    private lateinit var experiment: String

    @BeforeEach
    fun submit() {
        loop.drain()
        dispatcher.forget()
        val project =
            ApiTest.requestJson().body(
                """{"name":"Scenarios ${UUID.randomUUID()}"}""",
            ).post("/api/v1/projects").then().statusCode(201).extract().path<String>("id")
        experiment =
            ApiTest
                .requestJson()
                .body("""{"projectId":"$project","name":"Close up","spec":${ApiTest.fixture("minimal-experiment.json")}}""")
                .post("/api/v1/experiments")
                .then()
                .statusCode(201)
                .extract()
                .path("id")
        ApiTest.requestJson().post("/api/v1/experiments/$experiment/submit").then().statusCode(200)
        loop.drain()
    }

    @Test
    fun `shows each attempt at a run and how it went`() {
        val execution = executionOf(experiment)
        ApiTest
            .requestJson()
            .get("/api/v1/experiments/$experiment/scenarios/0")
            .then()
            .statusCode(200)
            .body("runs[0].attempts[0].outcome.type", equalTo("carried"))

        dispatcher.certify(execution, 0, UnitOutcome.Failed(UnitFailure.SIMULATION_ERROR, "threw"))
        dispatcher.finish(execution, endedWithLog(""))

        ApiTest
            .requestJson()
            .get("/api/v1/experiments/$experiment/scenarios/0")
            .then()
            .statusCode(200)
            .body("status.state", equalTo("failed"))
            .body("runs[0].attempts[0].attempt", equalTo(1))
            .body("runs[0].attempts[0].outcome.type", equalTo("failed"))
            .body("runs[0].attempts[0].outcome.reason", equalTo("simulationError"))
            .body("runs[0].files.size()", equalTo(0))
        ApiTest.requestJson().get("/api/v1/experiments/$experiment/scenarios/7").then().statusCode(404)
    }

    @Test
    fun `serves only the scenario's own log lines, once the execution has ended`() {
        val execution = executionOf(experiment)
        ApiTest.requestJson().get("/api/v1/experiments/$experiment/scenarios/0/logs").then().statusCode(404)

        dispatcher.certify(execution, 0, UnitOutcome.Failed(UnitFailure.SIMULATION_ERROR, "threw"))
        dispatcher.finish(execution, endedWithLog(LOG))

        ApiTest
            .requestJson()
            .get("/api/v1/experiments/$experiment/scenarios/0/logs")
            .then()
            .statusCode(200)
            .body(containsString("== Attempt 1, execution $execution =="))
            .body(containsString("[scenario=0 seed=0] Main - The unit failed"))
            .body(containsString("java.lang.IllegalStateException: boom"))
            .body(not(containsString("Reading the manifest")))
            .body(not(containsString("[scenario=1 ")))
    }

    private fun endedWithLog(log: String) = ExitOutcome(ExitReason.OK, 23, "", PlatformSpan.NotStarted, PeakMemory.Unmeasured, log)

    private fun executionOf(experiment: String): UUID =
        QuarkusTransaction.requiringNew().call {
            val id = checkNotNull(Experiment.findByPublicId(UUID.fromString(experiment))).id
            Execution.findByExperiment(id).single().publicId
        }

    private companion object {
        val LOG =
            """
            2026-10-05T10:00:00,000 INFO  Main - Reading the manifest
            2026-10-05T10:00:01,000 ERROR [scenario=0 seed=0] Main - The unit failed
            java.lang.IllegalStateException: boom
            2026-10-05T10:00:02,000 INFO  [scenario=1 seed=0] Main - starting
            """.trimIndent()
    }
}
