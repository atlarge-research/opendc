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

import io.quarkus.test.junit.QuarkusTest
import io.restassured.response.Response
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.opendc.web.server.TestAccounts
import org.opendc.web.server.TestPerson
import org.opendc.web.server.TestTraces
import org.opendc.web.server.model.TraceKind

private const val TOPOLOGY =
    """{"datacenters":[{"clusters":[{"name":"C0","hosts":[{"cpu":{"coreCount":4,"coreSpeed":"2.5 GHz"},"memory":{"size":"16 GiB"}}]}]}]}"""

/**
 * A document may use only traces its author could open: built in, their own, or shared with them,
 * of the kind asked for and whole. Someone else's private trace must read exactly like one that
 * does not exist, and a URI is never followed.
 */
@QuarkusTest
class TraceReferencesTest {
    @Test
    fun `a teammate's private trace reads exactly like one that does not exist`() {
        val person = TestAccounts.person()
        val private = TestTraces.owned(TestAccounts.person("teammate"), TraceKind.WORKLOAD)

        preview(person, workload(private.slug))
            .then()
            .statusCode(200)
            .body("issues.find { it.path == 'workloads[0].source' }.message", equalTo("no trace called ${private.slug} in your library"))
        preview(person, workload("nobody/nothing"))
            .then()
            .body("issues.find { it.path == 'workloads[0].source' }.message", equalTo("no trace called nobody/nothing in your library"))
    }

    @Test
    fun `a shared trace can be used until the share is withdrawn`() {
        val person = TestAccounts.person()
        val teammate = TestAccounts.person("teammate")
        val trace = TestTraces.owned(teammate, TraceKind.WORKLOAD)
        teammate.request().body("""{"handle":"${person.handle}"}""").post("/api/v1/traces/${trace.id}/shares").then().statusCode(201)
        val project = project(person)

        val first = draft(person, project, workload(trace.slug))
        person.request().post("/api/v1/experiments/$first/submit").then().statusCode(200)

        teammate.request().delete("/api/v1/traces/${trace.id}/shares/${person.handle}").then().statusCode(204)
        val second = draft(person, project, workload(trace.slug))
        person.request().post("/api/v1/experiments/$second/submit").then().statusCode(400)
        person.request().get("/api/v1/experiments/$first").then().body("state", not(equalTo("draft")))
    }

    @Test
    fun `a trace of the wrong kind or still uploading is named as such`() {
        val person = TestAccounts.person()
        val carbon = TestTraces.owned(person, TraceKind.CARBON)
        val uploading = TestTraces.owned(person, TraceKind.WORKLOAD, finished = false)

        preview(person, workload(carbon.slug)).then().body("issues.message", hasItem("${carbon.slug} is a carbon trace"))
        preview(person, workload(uploading.slug)).then().body("issues.message", hasItem("${uploading.slug} is still uploading"))
        preview(person, workload("bitbrains-small")).then().body("issues.path", not(hasItem("workloads[0].source")))
    }

    // A URI would be read from the dispatch host's own disk or network, so no draft may hold one.
    @Test
    fun `a URI is refused even in a draft`() {
        val person = TestAccounts.person()
        val uri = """{"type":"trace","source":{"type":"uri","uri":"file:///etc/passwd"}}"""

        person
            .request()
            .body("""{"projectId":"${project(person)}","name":"Sneaky","spec":${spec(uri)}}""")
            .post("/api/v1/experiments")
            .then()
            .statusCode(400)
            .body("issues.path", hasItem("workloads[0].source"))
    }

    // Copying a teammate's experiment works whatever it names; submitting it is what needs the traces.
    @Test
    fun `a clone keeps a reference only its author may use, and cannot be submitted with it`() {
        val author = TestAccounts.person("author")
        val editor = TestAccounts.person("editor")
        val project = project(author)
        author.request().body(
            """{"handle":"${editor.handle}","role":"editor"}""",
        ).post("/api/v1/projects/$project/members").then().statusCode(201)
        val private = TestTraces.owned(author, TraceKind.WORKLOAD)
        val original = draft(author, project, workload(private.slug))

        val clone = editor.request().post("/api/v1/experiments/$original/clone").then().statusCode(201).extract().path<String>("id")

        editor.request().post("/api/v1/experiments/$clone/submit").then().statusCode(400)
    }

    private fun workload(name: String) = """{"type":"trace","source":{"type":"named","name":"$name"}}"""

    private fun spec(workload: String) = """{"name":"refs","topologies":[$TOPOLOGY],"workloads":[$workload]}"""

    private fun preview(
        person: TestPerson,
        workload: String,
    ): Response = person.request().body("""{"spec":${spec(workload)}}""").post("/api/v1/experiments/preview")

    private fun project(person: TestPerson): String =
        person.request().body("""{"name":"Traces"}""").post("/api/v1/projects").then().statusCode(201).extract().path("id")

    private fun draft(
        person: TestPerson,
        project: String,
        workload: String,
    ): String =
        person
            .request()
            .body("""{"projectId":"$project","name":"Draft","spec":${spec(workload)}}""")
            .post("/api/v1/experiments")
            .then()
            .statusCode(201)
            .extract()
            .path("id")
}
