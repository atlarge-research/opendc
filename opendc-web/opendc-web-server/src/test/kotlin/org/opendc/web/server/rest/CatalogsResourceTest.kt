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
import io.restassured.RestAssured.get
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.validation.ValidationIssue
import org.opendc.web.server.ApiTest

@QuarkusTest
class CatalogsResourceTest {
    @Test
    fun `catalogs lists every catalog name`() {
        val names = getArray("/api/v1/catalogs").map { it.jsonPrimitive.content }
        assertEquals(
            listOf("schedulers", "failure-prefabs", "power-models", "battery-policies", "export-columns", "host-templates"),
            names,
        )
    }

    // Exact counts and id sets pin the catalogs to the sdk-model surface: adding or renaming an
    // option in the SDK must fail here until the web contract is looked at.
    @Test
    fun `reflected catalogs pin the sdk option surface`() {
        assertEquals(13, ids("schedulers").size)
        assertTrue("Timeshift" in ids("schedulers"))
        assertEquals(36, ids("failure-prefabs").size)
        assertTrue("G5k06Exp" in ids("failure-prefabs"))
        assertEquals(
            setOf("constant", "linear", "square", "cubic", "sqrt", "mse", "asymptotic"),
            ids("power-models").toSet(),
        )
        assertEquals(setOf("single", "double", "runningMean", "runningMeanPlus"), ids("battery-policies").toSet())
        assertEquals(
            setOf("battery", "cluster", "datacenter", "host", "powerSource", "service", "task"),
            ids("export-columns").toSet(),
        )
    }

    @Test
    fun `unknown catalog is not found`() {
        get("/api/v1/catalogs/quantum-annealers").then().statusCode(404)
    }

    // Wrapping each template host in a minimal topology and strict-decoding it proves the curated
    // file cannot drift from the sdk-model host schema.
    @Test
    fun `host templates decode strictly as sdk hosts`() {
        val templates = getArray("/api/v1/catalogs/host-templates")
        assertEquals(
            listOf("compute-epyc", "compute-xeon", "memory-node", "gpu-a100", "edge-node"),
            templates.map { it.jsonObject.getValue("id").jsonPrimitive.content },
        )
        for (template in templates) {
            val topology =
                buildJsonObject {
                    putJsonArray("datacenters") {
                        addJsonObject {
                            putJsonArray("clusters") {
                                addJsonObject {
                                    putJsonArray("hosts") { add(template.jsonObject.getValue("host")) }
                                }
                            }
                        }
                    }
                }
            val spec = SdkJson.strictJson.decodeFromJsonElement<TopologySpec>(topology)
            assertEquals(emptyList<ValidationIssue>(), spec.validate())
        }
    }

    private fun getArray(path: String): JsonArray =
        ApiTest.json.parseToJsonElement(get(path).then().statusCode(200).extract().asString()).jsonArray

    private fun ids(catalog: String): List<String> =
        getArray("/api/v1/catalogs/$catalog").map { it.jsonObject.getValue("id").jsonPrimitive.content }
}
