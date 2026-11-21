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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.launcher.UnitFailure
import org.opendc.web.launcher.UnitOutcome
import org.opendc.web.server.ApiTest
import org.opendc.web.server.execution.ExecutionLoop
import org.opendc.web.server.execution.RecordingDispatcher
import org.opendc.web.server.execution.ended
import org.opendc.web.server.model.Execution
import org.opendc.web.server.model.Experiment
import org.opendc.web.server.storage.ObjectStore
import org.opendc.web.server.storage.resultKey
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * The archive is how a run's output leaves the platform. Its entries are laid out the way a local run
 * lays out its own output directory, so what comes out of the zip can be read by the same tooling
 * without anything being moved about first.
 */
@QuarkusTest
class ExperimentArchiveTest {
    @Inject
    lateinit var store: ObjectStore

    @Inject
    lateinit var loop: ExecutionLoop

    @Inject
    lateinit var dispatcher: RecordingDispatcher

    private lateinit var projectId: String

    @BeforeEach
    fun seedProject() {
        loop.drain()
        dispatcher.forget()
        projectId =
            ApiTest.requestJson()
                .body("""{"name":"Archive ${UUID.randomUUID()}"}""")
                .post("/api/v1/projects")
                .then()
                .statusCode(201)
                .extract()
                .path("id")
    }

    @Test
    fun `lays out the archive the way a local run lays out its output`() {
        val experiment = submitted("Nightly Sweep")
        write(experiment, "0/seed=0/host.parquet", "first")
        write(experiment, "0/seed=1/service.parquet", "second")
        write(experiment, "1/seed=0/host.parquet", "third")
        finished(experiment, failing = emptySet())

        val entries = archive(experiment)

        assertEquals(
            listOf(
                "nightly-sweep/raw-output/0/seed=0/host.parquet",
                "nightly-sweep/raw-output/0/seed=1/service.parquet",
                "nightly-sweep/raw-output/1/seed=0/host.parquet",
            ),
            entries.keys.toList(),
        )
        assertEquals("first", entries["nightly-sweep/raw-output/0/seed=0/host.parquet"])
    }

    // Names are written by people and end up in every entry path. One that could be read as a
    // directory of its own would unpack somewhere nobody asked for.
    @Test
    fun `a name that reads like a path does not become one`() {
        val experiment = submitted("../../etc/passwd")
        write(experiment, "0/seed=0/host.parquet", "content")
        finished(experiment, failing = emptySet())

        assertEquals(listOf("etcpasswd/raw-output/0/seed=0/host.parquet"), archive(experiment).keys.toList())
    }

    // An attempt cut off partway can have published some of its files. They are not results, and
    // must not be handed out as though they were.
    @Test
    fun `leaves out what a unit published without finishing`() {
        val experiment = submitted("Half Done")
        write(experiment, "0/seed=0/host.parquet", "certified")
        write(experiment, "1/seed=0/host.parquet", "cut off")
        finished(experiment, failing = setOf(1))

        assertEquals(listOf("half-done/raw-output/0/seed=0/host.parquet"), archive(experiment).keys.toList())
    }

    @Test
    fun `an experiment that has produced nothing has no archive to give, nor a link to one`() {
        val experiment = submitted("Empty")

        ApiTest.requestJson().get("/api/v1/experiments/$experiment/archive").then().statusCode(404)
        ApiTest.requestJson().post("/api/v1/experiments/$experiment/archive/link").then().statusCode(404)
    }

    // A browser downloads by following a link, which cannot carry an access token, so the link it is
    // given has to fetch the archive with nothing else.
    @Test
    fun `a signed link fetches the archive with no credentials at all`() {
        val experiment = submitted("Linked")
        write(experiment, "0/seed=0/host.parquet", "linked")
        finished(experiment, failing = emptySet())

        val url =
            ApiTest.requestJson().post(
                "/api/v1/experiments/$experiment/archive/link",
            ).then().statusCode(200).extract().path<String>("url")
        val bytes = given().get("/$url").then().statusCode(200).extract().asByteArray()

        assertEquals(listOf("linked/raw-output/0/seed=0/host.parquet"), unzip(bytes).keys.toList())
    }

    @Test
    fun `an experiment nobody may see reports no archive rather than refusing one`() {
        ApiTest.requestJson().get("/api/v1/experiments/${UUID.randomUUID()}/archive").then().statusCode(404)
    }

    private fun submitted(name: String): String {
        val publicId =
            ApiTest.requestJson()
                .body("""{"projectId":"$projectId","name":${ApiTest.json.encodeToString(name)},"spec":$SPEC}""")
                .post("/api/v1/experiments")
                .then()
                .statusCode(201)
                .extract()
                .path<String>("id")
        ApiTest.requestJson().post("/api/v1/experiments/$publicId/submit").then().statusCode(200)
        return publicId
    }

    private fun write(
        experiment: String,
        path: String,
        content: String,
    ) {
        store.put("${resultKey(UUID.fromString(experiment))}/$path", content.byteInputStream())
    }

    /** Runs the experiment to its end, its launcher certifying every scenario but those in [failing]. */
    private fun finished(
        experiment: String,
        failing: Set<Int>,
    ) {
        loop.drain()
        val execution = executionOf(experiment)
        for (unit in dispatcher.manifestOf(execution).units) {
            val index = unit.scenario.id
            val outcome = if (index in failing) UnitOutcome.Failed(UnitFailure.SIMULATION_ERROR, "threw") else UnitOutcome.Succeeded(1.0)
            dispatcher.certify(execution, index, outcome, seed = unit.scenario.seed)
        }
        dispatcher.finish(execution, ended(ExitReason.OK, 23))
    }

    private fun executionOf(experiment: String): UUID =
        QuarkusTransaction.requiringNew().call {
            val id = checkNotNull(Experiment.findByPublicId(UUID.fromString(experiment))).id
            Execution.findByExperiment(id).single().publicId
        }

    /** The archive, unpacked, so a case can say what is in it rather than how long it was. */
    private fun archive(experiment: String): Map<String, String> {
        val bytes =
            ApiTest
                .requestJson()
                .get("/api/v1/experiments/$experiment/archive")
                .then()
                .statusCode(200)
                .header("Content-Disposition", org.hamcrest.Matchers.containsString(".zip"))
                .extract()
                .asByteArray()
        return unzip(bytes)
    }

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val entries = LinkedHashMap<String, String>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes().decodeToString()
                entry = zip.nextEntry
            }
        }
        return entries
    }

    private companion object {
        const val TOPOLOGY =
            """{"datacenters":[{"clusters":[{"name":"C0","hosts":[{"name":"H0","count":%d,""" +
                """"cpu":{"coreCount":4,"coreSpeed":"2.5 GHz"},"memory":{"size":"16 GiB"}}]}]}]}"""

        const val WORKLOAD =
            """{"type":"inline","tasks":[{"id":0,"submissionTime":"0 ms","duration":"10 minutes",""" +
                """"cpuCoreCount":1,"cpuCapacity":"1 GHz","memory":"1 GiB",""" +
                """"fragments":[{"duration":"10 minutes","cpuUsage":"1 GHz"}]}]}"""

        /** Two scenarios of two runs each, so an archive has scenarios and seeds to lay out. */
        val SPEC = """{"name":"archive","topologies":[${TOPOLOGY.format(1)},${TOPOLOGY.format(2)}],"workloads":[$WORKLOAD],"runs":2}"""
    }
}
