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
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.opendc.web.server.ApiTest
import org.opendc.web.server.TestAccounts
import org.opendc.web.server.TestPerson
import org.opendc.web.server.TestTraces
import org.opendc.web.server.model.AccountState
import org.opendc.web.server.model.Project
import org.opendc.web.server.model.ProjectMember
import org.opendc.web.server.model.ProjectRole
import org.opendc.web.server.model.Trace
import org.opendc.web.server.model.TraceKind
import org.opendc.web.server.model.UserAccount
import java.util.UUID

/** The caller's own account: who they are, choosing the handle they go by, and leaving. */
@QuarkusTest
class MeResourceTest {
    @Test
    fun `config tells the frontend that nobody has to sign in, without credentials`() {
        given().get("/api/v1/config").then().statusCode(200).body("auth.type", equalTo("anonymous"))
    }

    @Test
    fun `me exposes the implicit account and nothing about its identity provider`() {
        ApiTest
            .requestJson()
            .get("/api/v1/me")
            .then()
            .statusCode(200)
            .body("handle.type", equalTo("chosen"))
            .body("handle.name", equalTo("local"))
            .body("displayName", equalTo("Local user"))
            .body("plan", equalTo("free"))
            .body("isAdmin", equalTo(true))
            .body("$", not(hasKey("subject")))
            .body("$", not(hasKey("email")))
    }

    // Anonymous mode meters nothing, so there is no window to charge against. Reporting an uncapped
    // one instead would draw a bar that can never move and a reset time that never arrives.
    @Test
    fun `me reports no budget window when nothing is metered`() {
        ApiTest.requestJson().get("/api/v1/me").then().statusCode(200).body("budgets", empty<Any>())
    }

    @Test
    fun `projectCount follows the caller's memberships`() {
        val person = TestAccounts.person()
        person.request().body("""{"name":"Membership probe"}""").post("/api/v1/projects").then().statusCode(201)

        person.request().get("/api/v1/me").then().statusCode(200).body("projectCount", equalTo(1))
    }

    @Test
    fun `billing has no invoices and no provider fields yet`() {
        ApiTest
            .requestJson()
            .get("/api/v1/me/billing")
            .then()
            .statusCode(200)
            .body("invoices", empty<Any>())
            .body("$", not(hasKey("renewsAt")))
            .body("$", not(hasKey("paymentMethod")))
    }

    // A first sign-in gets a placeholder nothing can be shared under, so it may read its own profile
    // and choose a handle, and do nothing else until it has.
    @Test
    fun `a newcomer chooses a handle before doing anything else`() {
        val newcomer = TestAccounts.newcomer()
        newcomer.request().get("/api/v1/me").then().statusCode(200).body("handle.type", equalTo("provisional"))
        newcomer.request().get("/api/v1/projects").then().statusCode(403).body("title", equalTo("Choose a handle first"))

        val handle = "newcomer-${UUID.randomUUID().toString().take(8)}"
        newcomer
            .request()
            .body("""{"handle":"${handle.uppercase()}","displayName":"New Comer"}""")
            .put("/api/v1/me/profile")
            .then()
            .statusCode(200)
            .body("handle.name", equalTo(handle))
            .body("displayName", equalTo("New Comer"))

        newcomer.request().get("/api/v1/projects").then().statusCode(200)
    }

    @Test
    fun `refuses a handle that is reserved, malformed or taken, at the handle`() {
        val person = TestAccounts.person()
        val other = TestAccounts.person()

        person.profile("admin").then().statusCode(400).body("issues.path", hasItem("handle"))
        person.profile("x").then().statusCode(400).body("issues.path", hasItem("handle"))
        person.profile(other.handle).then().statusCode(409).body("title", equalTo("That handle is taken"))
    }

    // Slugs embed the handle, so renaming under a finished trace would break every reference to it.
    // An upload never finished is invisible, and goes with the old name.
    @Test
    fun `changes a handle only while it prefixes no finished trace`() {
        val person = TestAccounts.person()
        val unfinished = TestTraces.owned(person, TraceKind.CARBON, finished = false)

        person.profile("renamed-${UUID.randomUUID().toString().take(8)}").then().statusCode(200)
        QuarkusTransaction.requiringNew().run { assertNull(Trace.findByPublicId(unfinished.id)) }

        val finished = TestAccounts.person()
        TestTraces.owned(finished, TraceKind.CARBON)
        finished.profile("renamed-${UUID.randomUUID().toString().take(8)}").then().statusCode(409)
    }

    @Test
    fun `the implicit account cannot be deactivated`() {
        ApiTest.requestJson().delete("/api/v1/me").then().statusCode(409)
    }

    // Collaborators are never stranded: a sole owner hands a shared project over before leaving.
    @Test
    fun `refuses to deactivate the only owner of a project others work in, and names it`() {
        val owner = TestAccounts.person()
        val project = project(owner, "Shared ${UUID.randomUUID()}")
        join(project, TestAccounts.person(), ProjectRole.VIEWER)

        owner.request().delete("/api/v1/me").then().statusCode(409).body("issues.message", hasItem(project.second))
    }

    @Test
    fun `deactivation revokes tokens and leaves shared projects but not solo ones`() {
        val person = TestAccounts.person()
        val solo = project(person, "Solo ${UUID.randomUUID()}")
        val shared = project(TestAccounts.person(), "Shared ${UUID.randomUUID()}")
        join(shared, person, ProjectRole.EDITOR)

        person.request().delete("/api/v1/me").then().statusCode(204)

        person.request().get("/api/v1/me").then().statusCode(401)
        QuarkusTransaction.requiringNew().run {
            assertEquals(AccountState.DEACTIVATED, UserAccount.findById(person.id)?.state)
            assertEquals(1L, ProjectMember.count("project.id = ?1 AND user.id = ?2", solo.first, person.id))
            assertEquals(0L, ProjectMember.count("project.id = ?1 AND user.id = ?2", shared.first, person.id))
        }
    }

    private fun TestPerson.profile(handle: String) =
        request().body("""{"handle":"$handle","displayName":"Someone"}""").put("/api/v1/me/profile")

    /** A project of [owner], as its key and its name. */
    private fun project(
        owner: TestPerson,
        name: String,
    ): Pair<Long, String> {
        val id = owner.request().body("""{"name":"$name"}""").post("/api/v1/projects").then().statusCode(201).extract().path<String>("id")
        return QuarkusTransaction.requiringNew().call { checkNotNull(Project.findByPublicId(UUID.fromString(id))).id } to name
    }

    private fun join(
        project: Pair<Long, String>,
        person: TestPerson,
        role: ProjectRole,
    ) = QuarkusTransaction.requiringNew().run {
        val member = ProjectMember()
        member.project = checkNotNull(Project.findById(project.first))
        member.user = checkNotNull(UserAccount.findById(person.id))
        member.role = role
        member.persist()
    }
}
