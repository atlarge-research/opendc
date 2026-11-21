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
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.web.server.ApiTest
import org.opendc.web.server.auth.Identity
import org.opendc.web.server.model.Project
import org.opendc.web.server.model.ProjectMember
import org.opendc.web.server.model.ProjectRole
import java.time.Instant
import java.util.UUID

@QuarkusTest
class ProjectsResourceTest {
    @Inject
    lateinit var identity: Identity

    @Test
    fun lifecycleCreateGetRenameDelete() {
        val created =
            ApiTest.requestJson()
                .body("""{"name":"Lifecycle"}""")
                .post("/api/v1/projects")
        created.then().statusCode(201).body("name", equalTo("Lifecycle")).body("role", equalTo("owner"))
        val id = created.jsonPath().getString("id")
        Instant.parse(created.jsonPath().getString("createdAt"))

        ApiTest.requestJson().get("/api/v1/projects/$id").then().statusCode(200).body("name", equalTo("Lifecycle"))

        ApiTest.requestJson()
            .body("""{"name":"Renamed"}""")
            .patch("/api/v1/projects/$id")
            .then()
            .statusCode(200)
            .body("name", equalTo("Renamed"))

        ApiTest.requestJson().delete("/api/v1/projects/$id").then().statusCode(204)
        ApiTest.requestJson().get("/api/v1/projects/$id").then().statusCode(404)
    }

    // The public id is what leaves the server; the sequential key must never appear, or the number
    // of projects the platform holds can be read off two consecutive creates.
    @Test
    fun theListCarriesOpaqueIdentifiersOnly() {
        val id = createProject("Opaque id ${UUID.randomUUID()}")
        UUID.fromString(id)

        val body = ApiTest.requestJson().get("/api/v1/projects").then().statusCode(200).extract().asString()
        assertFalse(body.contains("\"projectId\""), "the list must not leak an internal key")
        assertTrue(body.contains(id))
    }

    @Test
    fun blankAndOverlongNamesAreRejected() {
        ApiTest.requestJson()
            .body("""{"name":"   "}""")
            .post("/api/v1/projects")
            .then()
            .statusCode(400)
            .body("issues[0].path", equalTo("name"))

        ApiTest.requestJson()
            .body("""{"name":"${"x".repeat(256)}"}""")
            .post("/api/v1/projects")
            .then()
            .statusCode(400)
            .body("issues[0].path", equalTo("name"))
    }

    // 255 is the column width, so it is the last name that can be stored rather than the first that
    // cannot: an off-by-one here is a 500 in production.
    @Test
    fun aNameOfExactlyTheColumnWidthIsAccepted() {
        ApiTest.requestJson()
            .body("""{"name":"${"x".repeat(255)}"}""")
            .post("/api/v1/projects")
            .then()
            .statusCode(201)
    }

    @Test
    fun surroundingWhitespaceIsTrimmedFromTheName() {
        ApiTest.requestJson()
            .body("""{"name":"  Padded  "}""")
            .post("/api/v1/projects")
            .then()
            .statusCode(201)
            .body("name", equalTo("Padded"))
    }

    @Test
    fun renamingIsAlsoValidated() {
        val id = createProject("To rename ${UUID.randomUUID()}")
        ApiTest.requestJson().body("""{"name":""}""").patch("/api/v1/projects/$id").then().statusCode(400)
        ApiTest.requestJson()
            .body("""{"name":"${"x".repeat(256)}"}""")
            .patch("/api/v1/projects/$id")
            .then()
            .statusCode(400)
    }

    @Test
    fun theFilterSelectsByRoleAndRejectsAnythingElse() {
        val mine = createProject("Owned ${UUID.randomUUID()}")
        val shared = seedProject("Shared ${UUID.randomUUID()}", ProjectRole.VIEWER)

        assertTrue(idsOf("own").contains(mine))
        assertFalse(idsOf("own").contains(shared))
        assertTrue(idsOf("shared").contains(shared))
        assertFalse(idsOf("shared").contains(mine))
        assertTrue(idsOf("all").containsAll(listOf(mine, shared)))

        ApiTest.requestJson()
            .get("/api/v1/projects?filter=everything")
            .then()
            .statusCode(400)
            .body("issues[0].path", equalTo("filter"))
    }

    @Test
    fun theSearchMatchesPartOfTheNameIgnoringCase() {
        val id = createProject("Zeppelin ${UUID.randomUUID()}")
        assertTrue(namesOf("zeppelin").any { it.startsWith("Zeppelin") })
        assertTrue(idsOf(query = "ZEPPELIN").contains(id))
        assertFalse(idsOf(query = "no such project anywhere").contains(id))
    }

    // A viewer can already see the project, so refusing the write tells them nothing new: 403 is the
    // honest answer where 404 would be for a project that is not theirs at all.
    @Test
    fun aViewerMayNotRenameOrDelete() {
        val id = seedProject("Read only ${UUID.randomUUID()}", ProjectRole.VIEWER)

        ApiTest.requestJson().get("/api/v1/projects/$id").then().statusCode(200)
        ApiTest.requestJson().body("""{"name":"Nope"}""").patch("/api/v1/projects/$id").then().statusCode(403)
        ApiTest.requestJson().delete("/api/v1/projects/$id").then().statusCode(403)
    }

    @Test
    fun anEditorMayRenameButNotDelete() {
        val id = seedProject("Editable ${UUID.randomUUID()}", ProjectRole.EDITOR)

        ApiTest.requestJson().body("""{"name":"Edited"}""").patch("/api/v1/projects/$id").then().statusCode(200)
        ApiTest.requestJson().delete("/api/v1/projects/$id").then().statusCode(403)
    }

    // A project the caller is not a member of reports exactly what a nonexistent one reports, so
    // membership cannot be probed by watching which identifiers answer differently.
    @Test
    fun aProjectSomebodyElseOwnsLooksExactlyLikeOneThatDoesNotExist() {
        val foreign = seedForeignProject()
        for (id in listOf(foreign, UUID.randomUUID().toString(), "not-an-id")) {
            ApiTest.requestJson().get("/api/v1/projects/$id").then().statusCode(404)
            ApiTest.requestJson().body("""{"name":"X"}""").patch("/api/v1/projects/$id").then().statusCode(404)
            ApiTest.requestJson().delete("/api/v1/projects/$id").then().statusCode(404)
        }
    }

    // The interesting delete is one with children. The membership that authorised it is loaded and
    // points at the project, and everything the project owns hangs off it through database
    // cascades the session never sees, so this is where the flush breaks if the order is wrong.
    @Test
    fun deletingAProjectTakesItsTopologiesAndExperimentsWithIt() {
        val id = createProject("Full project ${UUID.randomUUID()}")
        val topology =
            ApiTest.requestJson()
                .body("""{"projectId":"$id","name":"T","topology":$TOPOLOGY}""")
                .post("/api/v1/topologies")
                .then()
                .statusCode(201)
                .extract()
                .path<String>("id")
        val experiment =
            ApiTest.requestJson()
                .body("""{"projectId":"$id","name":"E","spec":$MINIMAL_SPEC}""")
                .post("/api/v1/experiments")
                .then()
                .statusCode(201)
                .extract()
                .path<String>("id")

        ApiTest.requestJson().delete("/api/v1/projects/$id").then().statusCode(204)

        ApiTest.requestJson().get("/api/v1/projects/$id").then().statusCode(404)
        ApiTest.requestJson().get("/api/v1/topologies/$topology").then().statusCode(404)
        ApiTest.requestJson().get("/api/v1/experiments/$experiment").then().statusCode(404)
    }

    @Test
    fun deletingAProjectRemovesItFromTheListing() {
        val id = createProject("Doomed ${UUID.randomUUID()}")
        assertTrue(idsOf("all").contains(id))
        ApiTest.requestJson().delete("/api/v1/projects/$id").then().statusCode(204)
        assertFalse(idsOf("all").contains(id))
    }

    private fun createProject(name: String): String =
        ApiTest.requestJson()
            .body("""{"name":"$name"}""")
            .post("/api/v1/projects")
            .then()
            .statusCode(201)
            .extract()
            .path("id")

    private fun idsOf(
        filter: String? = null,
        query: String? = null,
    ): List<String> {
        val params = listOfNotNull(filter?.let { "filter=$it" }, query?.let { "q=$it" }).joinToString("&")
        return ApiTest.requestJson()
            .get("/api/v1/projects${if (params.isEmpty()) "" else "?$params"}")
            .then()
            .statusCode(200)
            .extract()
            .path("id")
    }

    private fun namesOf(query: String): List<String> =
        ApiTest.requestJson().get("/api/v1/projects?q=$query").then().statusCode(200).extract().path("name")

    /** A project the caller belongs to with [role], which the API alone cannot yet create. */
    private fun seedProject(
        name: String,
        role: ProjectRole,
    ): String =
        QuarkusTransaction.requiringNew().call<String> {
            val project = newProject(name)
            val member = ProjectMember()
            member.project = project
            member.user = identity.currentUser()
            member.role = role
            member.persist()
            project.publicId.toString()
        }

    private fun seedForeignProject(): String =
        QuarkusTransaction.requiringNew().call<String> {
            newProject("Somebody else's ${UUID.randomUUID()}").publicId.toString()
        }

    private fun newProject(name: String): Project {
        val now = Instant.now()
        val project = Project()
        project.name = name
        project.createdAt = now
        project.updatedAt = now
        project.persist()
        return project
    }

    private companion object {
        const val TOPOLOGY =
            """{"datacenters":[{"clusters":[{"name":"C0","hosts":[{"name":"H0","cpu":{"coreCount":4,"coreSpeed":"2.5 GHz"},""" +
                """"memory":{"size":"16 GiB"}}]}]}]}"""

        val MINIMAL_SPEC: String = ApiTest.fixture("minimal-experiment.json")
    }
}
