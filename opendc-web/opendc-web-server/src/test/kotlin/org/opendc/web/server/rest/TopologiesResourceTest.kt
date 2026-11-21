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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.web.server.ApiTest
import org.opendc.web.server.auth.Identity
import org.opendc.web.server.model.Project
import org.opendc.web.server.model.ProjectMember
import org.opendc.web.server.model.ProjectRole
import org.opendc.web.server.model.TopologyTemplate
import java.time.Instant
import java.util.UUID

@QuarkusTest
class TopologiesResourceTest {
    @Inject
    lateinit var identity: Identity

    private lateinit var projectId: String
    private lateinit var foreignProjectId: String
    private lateinit var foreignTopologyId: String

    @BeforeEach
    fun seedProjects() {
        QuarkusTransaction.requiringNew().run {
            val now = Instant.now()
            val mine = Project()
            mine.name = "Mine"
            mine.createdAt = now
            mine.updatedAt = now
            mine.persist()
            val membership = ProjectMember()
            membership.project = mine
            membership.user = identity.currentUser()
            membership.role = ProjectRole.OWNER
            membership.persist()
            projectId = mine.publicId.toString()

            val foreign = Project()
            foreign.name = "Foreign"
            foreign.createdAt = now
            foreign.updatedAt = now
            foreign.persist()
            foreignProjectId = foreign.publicId.toString()

            val hidden = TopologyTemplate()
            hidden.project = foreign
            hidden.name = "Hidden"
            hidden.topology = topology("2.5 GHz")
            hidden.topologyHash = "unused"
            hidden.createdAt = now
            hidden.updatedAt = now
            hidden.persist()
            foreignTopologyId = hidden.publicId.toString()
        }
    }

    @Test
    fun lifecycleCreateGetReplaceDelete() {
        val layout = """{"version":1,"tiles":[{"id":"C0","x":0,"y":2.5}]}"""
        val created =
            ApiTest.requestJson()
                .body("""{"projectId":"$projectId","name":"Base","topology":${topology("2.5 GHz")},"layout":$layout}""")
                .post("/api/v1/topologies")
        created.then().statusCode(201)
        val id = created.jsonPath().getString("id")
        val hash = created.jsonPath().getString("topologyHash")
        assertEquals(projectId, created.jsonPath().getString("projectId"))
        Instant.parse(created.jsonPath().getString("createdAt"))

        val fetched = ApiTest.requestJson().get("/api/v1/topologies/$id")
        fetched.then().statusCode(200)
        assertEquals(hash, fetched.jsonPath().getString("topologyHash"))
        val fetchedLayout = ApiTest.json.parseToJsonElement(fetched.asString()).jsonObject.getValue("layout")
        assertEquals(layout, ApiTest.json.encodeToString(JsonElement.serializer(), fetchedLayout))

        ApiTest.requestJson()
            .body("""{"projectId":"$projectId","name":"Second","topology":${topology("2 GHz")}}""")
            .post("/api/v1/topologies")
            .then()
            .statusCode(201)

        val listed = ApiTest.requestJson().get("/api/v1/topologies?project=$projectId")
        listed.then().statusCode(200)
        assertEquals(listOf("Base", "Second"), listed.jsonPath().getList<String>("name"))

        val renamed =
            ApiTest.requestJson()
                .body("""{"name":"Renamed","topology":${topology("2.5 GHz")},"layout":$layout}""")
                .put("/api/v1/topologies/$id")
        renamed.then().statusCode(200).body("name", equalTo("Renamed"))
        assertEquals(hash, renamed.jsonPath().getString("topologyHash"))

        val changed =
            ApiTest.requestJson()
                .body("""{"name":"Renamed","topology":${topology("3 GHz")}}""")
                .put("/api/v1/topologies/$id")
        changed.then().statusCode(200).body("layout", nullValue())
        assertNotEquals(hash, changed.jsonPath().getString("topologyHash"))

        ApiTest.requestJson().delete("/api/v1/topologies/$id").then().statusCode(204)
        ApiTest.requestJson().get("/api/v1/topologies/$id").then().statusCode(404)
        val remaining = ApiTest.requestJson().get("/api/v1/topologies?project=$projectId")
        remaining.then().statusCode(200)
        assertEquals(listOf("Second"), remaining.jsonPath().getList<String>("name"))
    }

    @Test
    fun blankAndOverlongNamesAreRejected() {
        ApiTest.requestJson()
            .body("""{"projectId":"$projectId","name":"   ","topology":${topology("2.5 GHz")}}""")
            .post("/api/v1/topologies")
            .then()
            .statusCode(400)
            .body("issues[0].path", equalTo("name"))

        ApiTest.requestJson()
            .body("""{"projectId":"$projectId","name":"${"x".repeat(256)}","topology":${topology("2.5 GHz")}}""")
            .post("/api/v1/topologies")
            .then()
            .statusCode(400)
            .body("issues[0].path", equalTo("name"))
    }

    @Test
    fun surroundingWhitespaceIsTrimmedFromTheName() {
        ApiTest.requestJson()
            .body("""{"projectId":"$projectId","name":"  Padded  ","topology":${topology("2.5 GHz")}}""")
            .post("/api/v1/topologies")
            .then()
            .statusCode(201)
            .body("name", equalTo("Padded"))
    }

    @Test
    fun equivalentUnitSpellingsShareOneHash() {
        val mhz =
            ApiTest.requestJson()
                .body("""{"projectId":"$projectId","name":"MHz","topology":${topology("2400 MHz")}}""")
                .post("/api/v1/topologies")
        mhz.then().statusCode(201)
        val ghz =
            ApiTest.requestJson()
                .body("""{"projectId":"$projectId","name":"GHz","topology":${topology("2.4 GHz")}}""")
                .post("/api/v1/topologies")
        ghz.then().statusCode(201)
        assertEquals(mhz.jsonPath().getString("topologyHash"), ghz.jsonPath().getString("topologyHash"))
    }

    @Test
    fun emptyClustersAreRejectedWithIssuePath() {
        ApiTest.requestJson()
            .body("""{"projectId":"$projectId","name":"Bad","topology":{"clusters":[]}}""")
            .post("/api/v1/topologies")
            .then()
            .statusCode(400)
            .body("status", equalTo(400))
            .body("issues[0].path", equalTo("clusters"))
    }

    // Data centers beside the clusters they would be converted from are two topologies in one
    // document, and storing either would quietly discard the other.
    @Test
    fun dataCentersBesideClustersAreRejectedAtClusters() {
        ApiTest.requestJson()
            .body(
                """{"projectId":"$projectId","name":"Both","topology":""" +
                    """{"datacenters":[{"clusters":${clusters("2.5 GHz")}}],"clusters":${clusters("3 GHz")}}}""",
            )
            .post("/api/v1/topologies")
            .then()
            .statusCode(400)
            .body("issues[0].path", equalTo("clusters"))
    }

    // An editor sending back the null it was given has not written clusters.
    @Test
    fun aNullClustersBesideDataCentersIsAccepted() {
        ApiTest.requestJson()
            .body(
                """{"projectId":"$projectId","name":"Null","topology":{"datacenters":[{"clusters":${clusters(
                    "2.5 GHz",
                )}}],"clusters":null}}""",
            )
            .post("/api/v1/topologies")
            .then()
            .statusCode(201)
    }

    // A cluster's own power source is where documents kept it before data centers existed. Stored
    // as it came, the topology would carry two answers to where that cluster's power comes from.
    @Test
    fun aClustersOnlyDocumentIsStoredAsDataCentersAlone() {
        val legacy =
            """{"clusters":[{"name":"C0","powerSource":{"name":"Grid"},""" +
                """"hosts":[{"name":"H0","cpu":{"coreCount":4,"coreSpeed":"2.5 GHz"},"memory":{"size":"16 GiB"}}]}]}"""

        val created =
            ApiTest.requestJson()
                .body("""{"projectId":"$projectId","name":"Legacy","topology":$legacy}""")
                .post("/api/v1/topologies")

        created
            .then()
            .statusCode(201)
            .body("topology.clusters", nullValue())
            .body("topology.datacenters[0].powerSource.name", equalTo("Grid"))
            .body("topology.datacenters[0].clusters[0].name", equalTo("C0"))
            .body("topology.datacenters[0].clusters[0].powerSource", nullValue())
    }

    @Test
    fun unknownTopologyKeyIsRejectedByStrictParsing() {
        ApiTest.requestJson()
            .body(
                """{"projectId":"$projectId","name":"Bad","topology":{"datacenters":[{"clusters":${clusters("2.5 GHz")}}],"wattage":5}}""",
            )
            .post("/api/v1/topologies")
            .then()
            .statusCode(400)
            .body("status", equalTo(400))
    }

    // A well-formed identifier nobody owns and a string that is not an identifier at all must be
    // indistinguishable: were the malformed one a 400, the difference would confirm which ids exist.
    @Test
    fun unknownAndMalformedIdsAreBoth404() {
        for (id in listOf(UUID.randomUUID().toString(), "999999999", "not-an-id")) {
            ApiTest.requestJson().get("/api/v1/topologies/$id").then().statusCode(404)
            ApiTest.requestJson()
                .body("""{"name":"X","topology":${topology("2.5 GHz")}}""")
                .put("/api/v1/topologies/$id")
                .then()
                .statusCode(404)
            ApiTest.requestJson().delete("/api/v1/topologies/$id").then().statusCode(404)
        }
    }

    @Test
    fun inaccessibleProjectAndTopologyAre404() {
        ApiTest.requestJson().get("/api/v1/topologies?project=$foreignProjectId").then().statusCode(404)
        ApiTest.requestJson()
            .body("""{"projectId":"$foreignProjectId","name":"X","topology":${topology("2.5 GHz")}}""")
            .post("/api/v1/topologies")
            .then()
            .statusCode(404)
        ApiTest.requestJson().get("/api/v1/topologies/$foreignTopologyId").then().statusCode(404)
        ApiTest.requestJson().delete("/api/v1/topologies/$foreignTopologyId").then().statusCode(404)
    }

    @Test
    fun listWithoutProjectIs400() {
        ApiTest.requestJson().get("/api/v1/topologies").then().statusCode(400)
    }

    private fun clusters(coreSpeed: String): String =
        """[{"name":"C0","hosts":[{"name":"H0","cpu":{"coreCount":4,"coreSpeed":"$coreSpeed"},"memory":{"size":"16 GiB"}}]}]"""

    private fun topology(coreSpeed: String): String = """{"datacenters":[{"clusters":${clusters(coreSpeed)}}]}"""
}
