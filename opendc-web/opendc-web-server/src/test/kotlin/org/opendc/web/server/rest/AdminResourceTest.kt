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
import io.restassured.RestAssured.given
import jakarta.inject.Inject
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.launcher.UnitFailure
import org.opendc.web.launcher.UnitOutcome
import org.opendc.web.server.ApiTest
import org.opendc.web.server.TestAccounts
import org.opendc.web.server.execution.ExecutionLoop
import org.opendc.web.server.execution.RecordingDispatcher
import org.opendc.web.server.execution.ended
import org.opendc.web.server.model.Execution
import org.opendc.web.server.model.Experiment
import java.util.UUID

/** Management port in the test profile, where health checks and metrics are served. */
private const val MANAGEMENT_PORT = 9001

/**
 * What an operator sees and does: executions and their units, the logs they left, running what
 * failed again past the automatic cap, and the accounts. None of it is anyone else's to see.
 */
@QuarkusTest
class AdminResourceTest {
    @Inject
    lateinit var loop: ExecutionLoop

    @Inject
    lateinit var dispatcher: RecordingDispatcher

    private lateinit var project: String

    @BeforeEach
    fun seed() {
        loop.drain()
        dispatcher.forget()
        project =
            ApiTest.requestJson().body(
                """{"name":"Admin ${UUID.randomUUID()}"}""",
            ).post("/api/v1/projects").then().statusCode(201).extract().path("id")
    }

    @Test
    fun `only administrators see the admin endpoints`() {
        val person = TestAccounts.person()

        person.request().get("/api/v1/admin/executions").then().statusCode(403)
        person.request().get("/api/v1/admin/users").then().statusCode(403)
        ApiTest.requestJson().get("/api/v1/admin/capacity").then().statusCode(200).body("dispatcher", equalTo("recording"))
    }

    @Test
    fun `lists live executions with the experiment they carry, and the log only once there is one`() {
        val experiment = submitted("Live")
        loop.drain()
        val execution = executionOf(experiment)

        ApiTest
            .requestJson()
            .get("/api/v1/admin/executions")
            .then()
            .statusCode(200)
            .body("items.find { it.id == '$execution' }.experimentName", equalTo("Live"))
            .body("items.find { it.id == '$execution' }.phase.type", equalTo("submitted"))
        ApiTest.requestJson().get("/api/v1/admin/executions/$execution").then().statusCode(200).body("units.size()", equalTo(1))
        ApiTest.requestJson().get("/api/v1/admin/executions/$execution/logs").then().statusCode(404)
        ApiTest.requestJson().get("/api/v1/admin/executions?state=bogus").then().statusCode(400)
    }

    // A failure no retry would change is not retried automatically; an operator who knows better can.
    @Test
    fun `runs what failed again past the automatic cap, once`() {
        val experiment = submitted("Failing")
        loop.drain()
        val execution = executionOf(experiment)
        dispatcher.certify(execution, 0, UnitOutcome.Failed(UnitFailure.SIMULATION_ERROR, "threw"))
        dispatcher.finish(execution, ended(ExitReason.OK, 23))

        ApiTest.requestJson().get("/api/v1/admin/executions/$execution").then().body("retryableUnits", equalTo(1))
        ApiTest
            .requestJson()
            .post("/api/v1/admin/executions/$execution/retry")
            .then()
            .statusCode(201)
            .body("[0].attempt", equalTo(2))
            .body("[0].phase.type", equalTo("queued"))
        ApiTest.requestJson().post("/api/v1/admin/executions/$execution/retry").then().statusCode(409)
    }

    @Test
    fun `finds accounts by handle or name`() {
        val person = TestAccounts.person("findable")

        ApiTest
            .requestJson()
            .get("/api/v1/admin/users?q=${person.handle.uppercase()}")
            .then()
            .statusCode(200)
            .body("items.handle.name", hasItem(person.handle))
            .body("total", equalTo(1))
    }

    // Counters are registered for every value up front, so a dashboard shows zero rather than nothing.
    @Test
    fun `serves health and metrics on the management port`() {
        for (probe in listOf("live", "ready", "started")) {
            given().port(MANAGEMENT_PORT).get("/q/health/$probe").then().statusCode(200)
        }
        given()
            .port(MANAGEMENT_PORT)
            .get("/q/metrics")
            .then()
            .statusCode(200)
            .body(containsString("opendc_executions_settled_total{reason=\"walltime\"}"))
            .body(containsString("opendc_executions_launched_total{outcome=\"accepted\"}"))
            .body(containsString("opendc_executions{state=\"queued\"}"))
            .body(containsString("opendc_units_queued "))
            .body(containsString("opendc_platform_cores_total "))
            .body(containsString("opendc_platform_cores_allocated "))
    }

    private fun submitted(name: String): String {
        val id =
            ApiTest
                .requestJson()
                .body("""{"projectId":"$project","name":"$name","spec":${ApiTest.fixture("minimal-experiment.json")}}""")
                .post("/api/v1/experiments")
                .then()
                .statusCode(201)
                .extract()
                .path<String>("id")
        ApiTest.requestJson().post("/api/v1/experiments/$id/submit").then().statusCode(200)
        return id
    }

    private fun executionOf(experiment: String): UUID =
        QuarkusTransaction.requiringNew().call {
            val id = checkNotNull(Experiment.findByPublicId(UUID.fromString(experiment))).id
            Execution.findByExperiment(id).single().publicId
        }
}
