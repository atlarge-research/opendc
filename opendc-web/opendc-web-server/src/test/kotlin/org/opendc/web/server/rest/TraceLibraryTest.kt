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
import io.restassured.response.Response
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.web.server.ApiTest
import org.opendc.web.server.TestAccounts
import org.opendc.web.server.model.Experiment
import org.opendc.web.server.model.ExperimentResource
import org.opendc.web.server.model.Trace
import org.opendc.web.server.model.TraceKind
import org.opendc.web.server.model.TraceOrigin
import org.opendc.web.server.model.UserAccount
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipInputStream

private const val CARBON = "traces/carbon.parquet"
private const val TASKS = "traces/workload/tasks.parquet"
private const val FRAGMENTS = "traces/workload/fragments.parquet"

/** The tables each kind is made of, and the fixture standing in for each one. */
private val FIXTURES =
    mapOf(
        "carbon" to mapOf("carbon" to CARBON),
        "workload" to mapOf("tasks" to TASKS, "fragments" to FRAGMENTS),
    )

@QuarkusTest
class TraceLibraryTest {
    @Test
    fun `registering claims a name and hands back somewhere to put each table`() {
        val name = unique("registered")
        val registered = register("workload", name)

        registered.then().statusCode(201).body("trace.slug", equalTo("local/$name")).body("trace.sizeBytes", equalTo(0))
        assertEquals(listOf("tasks", "fragments"), registered.jsonPath().getList<String>("uploads.table"))
    }

    // The development store is a directory a browser cannot write to, so the slot points at this server.
    @Test
    fun `a store the browser cannot reach asks for the bytes through the server`() {
        val registered = register("carbon", unique("through"))

        assertEquals(listOf(false), registered.jsonPath().getList<Boolean>("uploads.direct"))
        assertEquals(1, registered.jsonPath().getList<Any>("uploads[0].parts").size, "one request carries the whole file")
        assertTrue(registered.jsonPath().getString("uploads[0].parts[0].url").startsWith("api/v1/traces/"))
    }

    // Parts are cut when the targets are handed out, so every file needs a size, and an empty one is no table.
    @Test
    fun `a registration has to say how large each of its files is`() {
        ApiTest.requestJson()
            .body("""{"kind":"workload","name":"${unique("sizeless")}"}""")
            .post("/api/v1/traces")
            .then()
            .statusCode(400)
            .body("issues.path", equalTo(listOf("tasks", "fragments")))

        ApiTest.requestJson()
            .body("""{"kind":"carbon","name":"${unique("empty")}","files":[{"table":"carbon","sizeBytes":0}]}""")
            .post("/api/v1/traces")
            .then()
            .statusCode(400)
            .body("issues[0].path", equalTo("carbon"))
    }

    @Test
    fun `a carbon trace becomes usable once its table is uploaded and completed`() {
        val name = unique("carbon")
        val id = register("carbon", name, description = "Dutch grid").id()

        put(id, "carbon", CARBON).then().statusCode(204)
        val completed = ApiTest.requestJson().post("/api/v1/traces/$id/complete")

        completed
            .then()
            .statusCode(200)
            .body("slug", equalTo("local/$name"))
            .body("description", equalTo("Dutch grid"))
        assertEquals(ApiTest.fixtureBytes(CARBON).size.toLong(), completed.jsonPath().getLong("sizeBytes"))
        assertTrue(completed.jsonPath().getLong("tables[0].rowCount") > 0, "the footer gives a row count")
    }

    @Test
    fun `a workload trace carries both of its tables`() {
        val id = register("workload", unique("workload")).id()

        put(id, "tasks", TASKS).then().statusCode(204)
        put(id, "fragments", FRAGMENTS).then().statusCode(204)
        val completed = ApiTest.requestJson().post("/api/v1/traces/$id/complete")

        completed.then().statusCode(200)
        assertEquals(listOf("fragments", "tasks"), completed.jsonPath().getList<String>("tables.name"))
        assertEquals(
            (ApiTest.fixtureBytes(TASKS).size + ApiTest.fixtureBytes(FRAGMENTS).size).toLong(),
            completed.jsonPath().getLong("sizeBytes"),
        )
    }

    // A truncated file lacks the parquet footer, which is what proves the file whole.
    @Test
    fun `a table cut off partway through is refused`() {
        val id = register("carbon", unique("truncated")).id()
        val half = ApiTest.fixtureBytes(CARBON).copyOfRange(0, ApiTest.fixtureBytes(CARBON).size / 2)
        given().body(half).put("/api/v1/traces/$id/tables/carbon").then().statusCode(204)

        ApiTest.requestJson()
            .post("/api/v1/traces/$id/complete")
            .then()
            .statusCode(400)
            .body("issues[0].path", equalTo("carbon"))
    }

    @Test
    fun `completing before everything has arrived says what is still missing`() {
        val id = register("workload", unique("half")).id()
        put(id, "tasks", TASKS).then().statusCode(204)

        ApiTest.requestJson().post("/api/v1/traces/$id/complete").then().statusCode(409)
    }

    // A file that is not the table it was filed as is refused here, where the person who uploaded
    // it is still watching, rather than inside a run days later.
    @Test
    fun `a file that is not the table it was filed as is refused`() {
        val id = register("carbon", unique("junk")).id()
        given().body("not a trace".toByteArray()).put("/api/v1/traces/$id/tables/carbon").then().statusCode(204)

        ApiTest.requestJson()
            .post("/api/v1/traces/$id/complete")
            .then()
            .statusCode(400)
            .body("issues[0].path", equalTo("carbon"))
    }

    @Test
    fun `a table the kind does not have cannot be uploaded`() {
        val id = register("carbon", unique("extra")).id()

        given().body(ByteArray(4)).put("/api/v1/traces/$id/tables/fragments").then().statusCode(404)
    }

    @Test
    fun `names that would not survive a url or a document are refused`() {
        for (bad in listOf("", "  ", "with space", "Upper/Case", "slash/inside", "-leading")) {
            ApiTest.requestJson()
                .body("""{"kind":"carbon","name":"$bad"}""")
                .post("/api/v1/traces")
                .then()
                .statusCode(400)
                .body("issues[0].path", equalTo("name"))
        }
    }

    @Test
    fun `two traces cannot share a name`() {
        val name = unique("taken")
        completedCarbon(name)

        register("carbon", name).then().statusCode(409)
    }

    // The library shows a trace only once registering, uploading and completing have all happened.
    @Test
    fun `an unfinished upload is not in the library`() {
        val name = unique("abandoned")
        register("carbon", name).then().statusCode(201)

        assertFalse(rows().any { it["slug"] == "local/$name" })
    }

    // An unfinished upload is invisible, so starting again over it is what frees its name.
    @Test
    fun `a name held by an unfinished upload can be claimed again`() {
        val name = unique("retried")
        register("carbon", name).then().statusCode(201)

        val id = register("carbon", name).id()
        put(id, "carbon", CARBON).then().statusCode(204)
        ApiTest.requestJson().post("/api/v1/traces/$id/complete").then().statusCode(200)

        assertEquals(1, rows().count { it["slug"] == "local/$name" })
    }

    @Test
    fun `the description is editable and the name can be corrected`() {
        val id = completedCarbon(unique("editable"))
        val renamed = unique("corrected")

        ApiTest.requestJson()
            .body("""{"description":"now with a note"}""")
            .patch("/api/v1/traces/$id")
            .then()
            .statusCode(200)
            .body("description", equalTo("now with a note"))

        ApiTest.requestJson()
            .body("""{"name":"$renamed"}""")
            .patch("/api/v1/traces/$id")
            .then()
            .statusCode(200)
            .body("slug", equalTo("local/$renamed"))
    }

    // The slug is what a submitted document names, so once something references it, correcting it
    // would leave that experiment pointing at a trace that no longer answers.
    @Test
    fun `a referenced trace can no longer be renamed or deleted`() {
        val name = unique("referenced")
        val id = completedCarbon(name)
        reference("local/$name")

        ApiTest.requestJson().body("""{"name":"${unique("other")}"}""").patch("/api/v1/traces/$id").then().statusCode(409)
        ApiTest.requestJson().delete("/api/v1/traces/$id").then().statusCode(409)
        ApiTest.requestJson().body("""{"description":"still editable"}""").patch("/api/v1/traces/$id").then().statusCode(200)
    }

    @Test
    fun `deleting a trace removes it from the library`() {
        val id = completedCarbon(unique("doomed"))

        ApiTest.requestJson().delete("/api/v1/traces/$id").then().statusCode(204)
        ApiTest.requestJson().get("/api/v1/traces/$id").then().statusCode(404)
    }

    // The caller can see a built-in trace, so changing one is a 403 rather than a 404.
    @Test
    fun `built-in traces cannot be changed or removed`() {
        val builtIn = rows().first { it["access"] == "builtin" }["id"]

        ApiTest.requestJson().body("""{"description":"mine now"}""").patch("/api/v1/traces/$builtIn").then().statusCode(403)
        ApiTest.requestJson().delete("/api/v1/traces/$builtIn").then().statusCode(403)
    }

    @Test
    fun `downloading gives every table of the trace in one archive`() {
        val id = register("workload", unique("archive")).id()
        put(id, "tasks", TASKS)
        put(id, "fragments", FRAGMENTS)
        ApiTest.requestJson().post("/api/v1/traces/$id/complete").then().statusCode(200)

        val body = ApiTest.requestJson().get("/api/v1/traces/$id/content").then().statusCode(200).extract().asByteArray()

        val entries = mutableMapOf<String, Int>()
        ZipInputStream(ByteArrayInputStream(body)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name.substringAfterLast('/')] = zip.readBytes().size
            }
        }
        assertEquals(setOf("tasks.parquet", "fragments.parquet"), entries.keys)
        assertEquals(ApiTest.fixtureBytes(TASKS).size, entries.getValue("tasks.parquet"))
    }

    @Test
    fun `sharing puts a trace in somebody else's library and revoking takes it back out`() {
        val id = completedCarbon(unique("shared"))
        val handle = seedStranger()

        ApiTest.requestJson()
            .body("""{"handle":"$handle"}""")
            .post("/api/v1/traces/$id/shares")
            .then()
            .statusCode(201)
            .body("handle", equalTo(handle))
        ApiTest.requestJson().get("/api/v1/traces/$id/shares").then().statusCode(200).body("[0].handle", equalTo(handle))

        ApiTest.requestJson().delete("/api/v1/traces/$id/shares/$handle").then().statusCode(204)
        ApiTest.requestJson().get("/api/v1/traces/$id/shares").then().statusCode(200).body("size()", equalTo(0))
    }

    @Test
    fun `sharing with somebody who does not exist is a miss`() {
        val id = completedCarbon(unique("nobody"))

        ApiTest.requestJson().body("""{"handle":"no-such-person"}""").post("/api/v1/traces/$id/shares").then().statusCode(404)
    }

    @Test
    fun `a trace nobody shared is invisible rather than refused`() {
        val id = seedForeignTrace()

        ApiTest.requestJson().get("/api/v1/traces/$id").then().statusCode(404)
        ApiTest.requestJson().delete("/api/v1/traces/$id").then().statusCode(404)
        ApiTest.requestJson().get("/api/v1/traces/$id/content").then().statusCode(404)
        assertFalse(rows().any { it["id"] == id })
    }

    @Test
    fun `the kind filter narrows the listing and an unknown kind is a bad request`() {
        val id = completedCarbon(unique("filtered"))

        assertTrue(rows("?kind=carbon").any { it["id"] == id })
        assertFalse(rows("?kind=workload").any { it["id"] == id })
        ApiTest.requestJson().get("/api/v1/traces?kind=carbn").then().statusCode(400).body("issues[0].path", equalTo("kind"))
    }

    private fun register(
        kind: String,
        name: String,
        description: String? = null,
    ): Response {
        val files =
            FIXTURES.getValue(kind).entries.joinToString(",") { (table, fixture) ->
                """{"table":"$table","sizeBytes":${ApiTest.fixtureBytes(fixture).size}}"""
            }
        val body =
            buildString {
                append("""{"kind":"$kind","name":"$name","files":[$files]""")
                if (description != null) append(""","description":"$description"""")
                append("}")
            }
        return ApiTest.requestJson().body(body).post("/api/v1/traces")
    }

    private fun put(
        id: String,
        table: String,
        fixture: String,
    ): Response = given().body(ApiTest.fixtureBytes(fixture)).put("/api/v1/traces/$id/tables/$table")

    private fun completedCarbon(name: String): String {
        val id = register("carbon", name).id()
        put(id, "carbon", CARBON).then().statusCode(204)
        ApiTest.requestJson().post("/api/v1/traces/$id/complete").then().statusCode(200)
        return id
    }

    private fun Response.id(): String = then().statusCode(201).extract().path("trace.id")

    private fun rows(query: String = ""): List<Map<String, Any?>> =
        ApiTest.requestJson().get("/api/v1/traces$query").then().statusCode(200).extract().path("")

    private fun unique(prefix: String) = "$prefix-${UUID.randomUUID()}"

    /** Records an experiment reference to [slug], which is what freezes a trace's name. */
    private fun reference(slug: String) {
        val projectId =
            ApiTest.requestJson()
                .body("""{"name":"Trace reference ${UUID.randomUUID()}"}""")
                .post("/api/v1/projects")
                .then()
                .statusCode(201)
                .extract()
                .path<String>("id")
        val experiment =
            ApiTest.requestJson()
                .body("""{"projectId":"$projectId","name":"References","spec":${ApiTest.fixture("minimal-experiment.json")}}""")
                .post("/api/v1/experiments")
                .then()
                .statusCode(201)
                .extract()
                .path<String>("id")
        QuarkusTransaction.requiringNew().run {
            val resource = ExperimentResource()
            resource.experiment = checkNotNull(Experiment.findByPublicId(UUID.fromString(experiment)))
            resource.kind = TraceKind.CARBON
            resource.reference = """{"type":"named","name":"$slug"}"""
            resource.referenceName = slug
            resource.persist()
        }
    }

    private fun seedStranger(): String = TestAccounts.person("stranger").handle

    private fun seedForeignTrace(): String {
        val stranger = TestAccounts.person("stranger")
        return QuarkusTransaction.requiringNew().call {
            val now = Instant.now()
            val trace = Trace()
            trace.slug = "${stranger.handle}/private"
            trace.kind = TraceKind.CARBON
            trace.origin = TraceOrigin.UPLOADED
            trace.owner = UserAccount.findById(stranger.id)
            trace.createdAt = now
            trace.updatedAt = now
            trace.persist()
            trace.publicId.toString()
        }
    }
}
