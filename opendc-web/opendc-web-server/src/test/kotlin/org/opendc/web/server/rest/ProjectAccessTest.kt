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
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.web.server.ApiTest
import org.opendc.web.server.TestAccounts
import org.opendc.web.server.TestPerson
import org.opendc.web.server.model.Project
import org.opendc.web.server.model.ProjectMember
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val TOPOLOGY =
    """{"datacenters":[{"clusters":[{"name":"C0","hosts":[{"cpu":{"coreCount":4,"coreSpeed":"2.5 GHz"},"memory":{"size":"16 GiB"}}]}]}]}"""

/**
 * Who may do what in a project. A viewer reads, an editor changes what the project holds, an owner
 * also decides who is in it, and someone outside cannot tell it exists.
 */
@QuarkusTest
class ProjectAccessTest {
    private lateinit var owner: TestPerson
    private lateinit var editor: TestPerson
    private lateinit var viewer: TestPerson
    private lateinit var stranger: TestPerson
    private lateinit var project: String
    private lateinit var topology: String
    private lateinit var experiment: String

    @BeforeEach
    fun seed() {
        owner = TestAccounts.person("owner")
        editor = TestAccounts.person("editor")
        viewer = TestAccounts.person("viewer")
        stranger = TestAccounts.person("stranger")
        project = owner.request().body("""{"name":"Shared"}""").post("/api/v1/projects").then().statusCode(201).extract().path("id")
        invite(editor, "editor").then().statusCode(201)
        invite(viewer, "viewer").then().statusCode(201)
        topology =
            owner
                .request()
                .body("""{"projectId":"$project","name":"T","topology":$TOPOLOGY}""")
                .post("/api/v1/topologies")
                .then()
                .statusCode(201)
                .extract()
                .path("id")
        experiment =
            owner
                .request()
                .body("""{"projectId":"$project","name":"E","spec":${ApiTest.fixture("minimal-experiment.json")}}""")
                .post("/api/v1/experiments")
                .then()
                .statusCode(201)
                .extract()
                .path("id")
    }

    @Test
    fun `viewers read everything and change nothing`() {
        viewer.request().get("/api/v1/projects/$project").then().statusCode(200).body("role", equalTo("viewer"))
        viewer.request().get("/api/v1/topologies/$topology").then().statusCode(200)
        viewer.request().get("/api/v1/experiments/$experiment/status").then().statusCode(200)
        viewer.request().get("/api/v1/projects/$project/members").then().statusCode(200)

        viewer.request().body("""{"name":"Renamed"}""").patch("/api/v1/projects/$project").then().statusCode(403)
        viewer.request().body("""{"name":"T","topology":$TOPOLOGY}""").put("/api/v1/topologies/$topology").then().statusCode(403)
        viewer.request().delete("/api/v1/topologies/$topology").then().statusCode(403)
        viewer.request().post("/api/v1/experiments/$experiment/submit").then().statusCode(403)
        viewer.request().post("/api/v1/experiments/$experiment/clone").then().statusCode(403)
    }

    @Test
    fun `editors change what a project holds but not who is in it`() {
        editor.request().body("""{"name":"Renamed"}""").patch("/api/v1/projects/$project").then().statusCode(200)
        editor.request().body("""{"name":"T2","topology":$TOPOLOGY}""").put("/api/v1/topologies/$topology").then().statusCode(200)
        editor.request().post("/api/v1/experiments/$experiment/clone").then().statusCode(201)

        invite(stranger, "viewer", by = editor).then().statusCode(403).body("title", equalTo("Only an owner can do this"))
        editor.request().delete("/api/v1/projects/$project").then().statusCode(403)
    }

    @Test
    fun `someone outside cannot tell the project exists`() {
        stranger.request().get("/api/v1/projects/$project").then().statusCode(404)
        stranger.request().get("/api/v1/topologies/$topology").then().statusCode(404)
        stranger.request().get("/api/v1/experiments/$experiment").then().statusCode(404)
        stranger.request().body("""{"name":"Mine"}""").patch("/api/v1/projects/$project").then().statusCode(404)
        stranger.request().get("/api/v1/projects/$project/members").then().statusCode(404)
    }

    @Test
    fun `members are listed owners first, and are invited by handle once`() {
        val expected = listOf(owner.handle) + listOf(editor.handle, viewer.handle).sorted()
        owner.request().get("/api/v1/projects/$project/members").then().statusCode(200).body("handle", equalTo(expected))

        invite(viewer, "editor").then().statusCode(409)
        val unknown = TestPerson(0, "nobody-${UUID.randomUUID().toString().take(8)}", "")
        invite(unknown, "viewer").then().statusCode(404)
        invite(TestAccounts.newcomer(), "viewer").then().statusCode(404)
    }

    @Test
    fun `a project keeps at least one owner`() {
        owner.request().body("""{"role":"editor"}""").patch("/api/v1/projects/$project/members/${owner.handle}").then().statusCode(409)
        owner.request().delete("/api/v1/projects/$project/members/${owner.handle}").then().statusCode(409)

        owner.request().body("""{"role":"owner"}""").patch("/api/v1/projects/$project/members/${editor.handle}").then().statusCode(200)
        owner.request().delete("/api/v1/projects/$project/members/${owner.handle}").then().statusCode(204)
    }

    @Test
    fun `anyone may leave, and the project goes from their list`() {
        viewer.request().delete("/api/v1/projects/$project/members/${viewer.handle}").then().statusCode(204)

        viewer.request().get("/api/v1/projects").then().statusCode(200).body("id", not(hasItem(project)))
        viewer.request().get("/api/v1/projects/$project").then().statusCode(404)
    }

    // Two owners demoting each other at once is the one way to an ownerless project.
    @Test
    fun `owners demoting each other at once leave exactly one owner`() {
        owner.request().body("""{"role":"owner"}""").patch("/api/v1/projects/$project/members/${editor.handle}").then().statusCode(200)
        val pool = Executors.newFixedThreadPool(2)
        val demotions =
            listOf(owner to editor, editor to owner).map { (by, target) ->
                pool.submit<Int> {
                    by.request().body("""{"role":"viewer"}""").patch("/api/v1/projects/$project/members/${target.handle}").statusCode
                }
            }
        demotions.forEach { it.get() }
        pool.shutdown()
        pool.awaitTermination(10, TimeUnit.SECONDS)

        QuarkusTransaction.requiringNew().run {
            val id = checkNotNull(Project.findByPublicId(UUID.fromString(project))).id
            assertEquals(1L, ProjectMember.countOwners(id))
        }
    }

    private fun invite(
        person: TestPerson,
        role: String,
        by: TestPerson = owner,
    ) = by.request().body("""{"handle":"${person.handle}","role":"$role"}""").post("/api/v1/projects/$project/members")
}
