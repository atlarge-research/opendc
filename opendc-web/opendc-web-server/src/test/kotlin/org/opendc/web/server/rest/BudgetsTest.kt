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
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.web.dispatcher.ExitOutcome
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.PlatformSpan
import org.opendc.web.launcher.PeakMemory
import org.opendc.web.launcher.UnitOutcome
import org.opendc.web.server.ApiTest
import org.opendc.web.server.TestAccounts
import org.opendc.web.server.TestPerson
import org.opendc.web.server.execution.ExecutionLoop
import org.opendc.web.server.execution.RecordingDispatcher
import org.opendc.web.server.model.BudgetWindow
import org.opendc.web.server.model.Execution
import org.opendc.web.server.model.Experiment
import org.opendc.web.server.model.Invoice
import org.opendc.web.server.model.SimulationCap
import org.opendc.web.server.model.UserAccount
import org.opendc.web.server.service.charge
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Simulation is metered per account: a submit is admitted against what is left in its windows,
 * counting what unfinished work holds in reserve, and the submitter is charged the cores the
 * platform ran the work with.
 */
@QuarkusTest
class BudgetsTest {
    @Inject
    lateinit var loop: ExecutionLoop

    @Inject
    lateinit var dispatcher: RecordingDispatcher

    private lateinit var person: TestPerson
    private lateinit var project: String

    @BeforeEach
    fun seed() {
        loop.drain()
        dispatcher.forget()
        person = TestAccounts.person("payer")
        project = person.request().body("""{"name":"Budget"}""").post("/api/v1/projects").then().statusCode(201).extract().path("id")
    }

    @Test
    fun `a submit holds its quote in reserve until the work ends`() {
        val experiment = draft()
        val quote = person.request().get("/api/v1/experiments/$experiment").then().extract().path<Float>("estimate.estimatedBudgetSeconds")
        assertTrue(quote > 0)

        person.request().post("/api/v1/experiments/$experiment/submit").then().statusCode(200)
        assertEquals(quote.toDouble(), reserved(), 1e-3)

        person.request().post("/api/v1/experiments/$experiment/cancel").then().statusCode(200)
        assertEquals(0.0, reserved())
    }

    @Test
    fun `a submit past a limit is refused at the window, and the experiment stays a draft`() {
        capAt(0.001)
        val experiment = draft()

        person
            .request()
            .post("/api/v1/experiments/$experiment/submit")
            .then()
            .statusCode(409)
            .body("issues.path", hasItem("budget.session"))
        person.request().get("/api/v1/experiments/$experiment").then().body("state", equalTo("draft"))
    }

    // Admissions of one account take turns, so two submits cannot both squeeze under one limit.
    @Test
    fun `two submits that together pass the limit are not both admitted`() {
        val first = draft()
        val second = draft()
        val quote = person.request().get("/api/v1/experiments/$first").then().extract().path<Float>("estimate.estimatedBudgetSeconds")
        capAt(quote * 1.5)

        val pool = Executors.newFixedThreadPool(2)
        val statuses =
            listOf(first, second)
                .map { id -> pool.submit<Int> { person.request().post("/api/v1/experiments/$id/submit").statusCode } }
                .map { it.get() }
        pool.shutdown()
        pool.awaitTermination(10, TimeUnit.SECONDS)

        assertEquals(listOf(200, 409), statuses.sorted())
    }

    @Test
    fun `the submitter is charged the cores the platform ran the work with`() {
        val experiment = draft()
        person.request().post("/api/v1/experiments/$experiment/submit").then().statusCode(200)
        loop.drain()
        val execution = executionOf(experiment)
        val started = Instant.now().minusSeconds(10)
        dispatcher.certify(execution, 0, UnitOutcome.Succeeded(10.0))

        dispatcher.finish(
            execution,
            ExitOutcome(ExitReason.OK, 0, "", PlatformSpan.Ran(started, started.plusSeconds(10)), PeakMemory.Unmeasured, ""),
        )

        val cores = QuarkusTransaction.requiringNew().call { checkNotNull(Execution.findByPublicId(execution)).parallelism }
        assertEquals(10.0 * cores, used(), 1e-6)
        assertEquals(0.0, reserved())
    }

    @Test
    fun `deleting running work charges what it has used so far`() {
        val experiment = draft()
        person.request().post("/api/v1/experiments/$experiment/submit").then().statusCode(200)
        loop.drain()
        val execution = executionOf(experiment)
        dispatcher.start(execution)
        val cores =
            QuarkusTransaction.requiringNew().call {
                val row = checkNotNull(Execution.findByPublicId(execution))
                row.platformStartedAt = Instant.now().minusSeconds(60)
                row.parallelism
            }

        person.request().delete("/api/v1/experiments/$experiment").then().statusCode(204)

        assertEquals(60.0 * cores, used(), 5.0 * cores)
    }

    @Test
    fun `a window that has run out starts afresh at the next charge`() {
        val now = Instant.now()
        QuarkusTransaction.requiringNew().run {
            for (window in BudgetWindow.findByUser(person.id)) {
                window.usedSeconds = 500.0
                window.resetsAt = now.minusSeconds(1)
            }
        }

        QuarkusTransaction.requiringNew().run { charge(checkNotNull(UserAccount.findById(person.id)), 5.0, now) }

        QuarkusTransaction.requiringNew().run {
            for (window in BudgetWindow.findByUser(person.id)) {
                assertEquals(5.0, window.usedSeconds)
                assertTrue(window.resetsAt.isAfter(now))
            }
        }
    }

    @Test
    fun `invoices are the caller's own, newest first`() {
        QuarkusTransaction.requiringNew().run {
            invoice(person.id, "2026-08-01T00:00:00Z")
            invoice(person.id, "2026-09-01T00:00:00Z")
            invoice(TestAccounts.person().id, "2026-09-15T00:00:00Z")
        }

        person
            .request()
            .get("/api/v1/me/billing")
            .then()
            .statusCode(200)
            .body("invoices.issuedAt", contains("2026-09-01T00:00:00Z", "2026-08-01T00:00:00Z"))
    }

    private fun draft(): String =
        person
            .request()
            .body("""{"projectId":"$project","name":"Metered ${UUID.randomUUID()}","spec":${ApiTest.fixture("minimal-experiment.json")}}""")
            .post("/api/v1/experiments")
            .then()
            .statusCode(201)
            .extract()
            .path("id")

    private fun capAt(seconds: Number) =
        QuarkusTransaction.requiringNew().run {
            BudgetWindow.findByUser(person.id).forEach { it.cap = SimulationCap.Limited(seconds.toDouble()) }
        }

    private fun reserved(): Double =
        person.request().get("/api/v1/me").then().extract().path<List<Float>>("budgets.reservedSeconds").first().toDouble()

    private fun used(): Double =
        person.request().get("/api/v1/me").then().extract().path<List<Float>>("budgets.usedSeconds").first().toDouble()

    private fun executionOf(experiment: String): UUID =
        QuarkusTransaction.requiringNew().call {
            val id = checkNotNull(Experiment.findByPublicId(UUID.fromString(experiment))).id
            Execution.findByExperiment(id).single().publicId
        }

    private fun invoice(
        userId: Long,
        issued: String,
    ) {
        val invoice = Invoice()
        invoice.user = checkNotNull(UserAccount.findById(userId))
        invoice.issuedAt = Instant.parse(issued)
        invoice.amountEur = BigDecimal("12.50")
        invoice.persist()
    }
}
