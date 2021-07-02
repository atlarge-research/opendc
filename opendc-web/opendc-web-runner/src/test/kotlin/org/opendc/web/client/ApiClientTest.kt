/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.web.client

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * Test suite for the [ApiClient] class.
 */
class ApiClientTest {
    /**
     * The Ktor [HttpClient] instance.
     */
    private val ktor = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                when (request.url.fullPath) {
                    "/oauth/token" -> {
                        val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(
                            """
                        {
                          "access_token": "eyJz93a...k4laUWw",
                          "token_type": "Bearer",
                          "expires_in": 86400
                        }
                            """.trimIndent(),
                            headers = responseHeaders
                        )
                    }
                    "/portfolios/5fda5daa97dca438e7cb0a4c" -> {
                        val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(
                            """
                        {
                          "data": {
                            "_id": "string",
                            "projectId": "string",
                            "name": "string",
                            "scenarioIds": [
                              "string"
                            ],
                            "targets": {
                              "enabledMetrics": [
                                "string"
                              ],
                              "repeatsPerScenario": 0
                            }
                          }
                        }
                            """.trimIndent(),
                            headers = responseHeaders
                        )
                    }
                    "/portfolios/x" -> {
                        val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(
                            """
                        {
                          "message": "Not Found"
                        }
                            """.trimIndent(),
                            headers = responseHeaders, status = HttpStatusCode.NotFound
                        )
                    }
                    "/scenarios/5fda5db297dca438e7cb0a4d" -> {
                        val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(
                            """
                        {
                          "data": {
                            "_id": "string",
                            "portfolioId": "string",
                            "name": "string",
                            "trace": {
                              "traceId": "string",
                              "loadSamplingFraction": 0
                            },
                            "topology": {
                              "topologyId": "string"
                            },
                            "operational": {
                              "failuresEnabled": true,
                              "performanceInterferenceEnabled": true,
                              "schedulerName": "string"
                            }
                          }
                        }
                            """.trimIndent(),
                            headers = responseHeaders
                        )
                    }
                    "/scenarios/x" -> {
                        val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(
                            """
                        {
                          "message": "Not Found"
                        }
                            """.trimIndent(),
                            headers = responseHeaders, status = HttpStatusCode.NotFound
                        )
                    }
                    "/topologies/5f9825a6cf6e4c24e380b86f" -> {
                        val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(
                            """
                            {
                              "data": {
                                "_id": "string",
                                "projectId": "string",
                                "name": "string",
                                "rooms": [
                                  {
                                    "_id": "string",
                                    "name": "string",
                                    "tiles": [
                                      {
                                        "_id": "string",
                                        "positionX": 0,
                                        "positionY": 0,
                                        "rack": {
                                          "_id": "string",
                                          "name": "string",
                                          "capacity": 0,
                                          "powerCapacityW": 0,
                                          "machines": [
                                            {
                                              "_id": "string",
                                              "position": 0,
                                              "cpus": [
                                                {
                                                  "_id": "string",
                                                  "name": "string",
                                                  "clockRateMhz": 0,
                                                  "numberOfCores": 0
                                                }
                                              ],
                                              "gpus": [
                                                {
                                                  "_id": "string",
                                                  "name": "string",
                                                  "clockRateMhz": 0,
                                                  "numberOfCores": 0
                                                }
                                              ],
                                              "memories": [
                                                {
                                                  "_id": "string",
                                                  "name": "string",
                                                  "speedMbPerS": 0,
                                                  "sizeMb": 0
                                                }
                                              ],
                                              "storages": [
                                                {
                                                  "_id": "string",
                                                  "name": "string",
                                                  "speedMbPerS": 0,
                                                  "sizeMb": 0
                                                }
                                              ]
                                            }
                                          ]
                                        }
                                      }
                                    ]
                                  }
                                ]
                              }
                            }
                            """.trimIndent(),
                            headers = responseHeaders
                        )
                    }
                    "/topologies/x" -> {
                        val responseHeaders =
                            headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(
                            """
                        {
                          "message": "Not Found"
                        }
                            """.trimIndent(),
                            headers = responseHeaders, status = HttpStatusCode.NotFound
                        )
                    }
                    else -> error("Unhandled ${request.url}")
                }
            }
        }
    }

    private val auth = AuthConfiguration("auth.opendc.org", "a", "b")

    @Test
    fun testPortfolioExists(): Unit = runBlocking {
        val client = ApiClient(URI("http://localhost:8081"), auth, client = ktor)
        val portfolio = client.getPortfolio("5fda5daa97dca438e7cb0a4c")
        assertNotNull(portfolio)
    }

    @Test
    fun testPortfolioDoesNotExists(): Unit = runBlocking {
        val client = ApiClient(URI("http://localhost:8081"), auth, client = ktor)
        val portfolio = client.getPortfolio("x")
        assertNull(portfolio)
    }

    @Test
    fun testScenarioExists(): Unit = runBlocking {
        val client = ApiClient(URI("http://localhost:8081"), auth, client = ktor)
        val scenario = client.getScenario("5fda5db297dca438e7cb0a4d")
        assertNotNull(scenario)
    }

    @Test
    fun testScenarioDoesNotExists(): Unit = runBlocking {
        val client = ApiClient(URI("http://localhost:8081"), auth, client = ktor)
        val scenario = client.getScenario("x")
        assertNull(scenario)
    }

    @Test
    fun testTopologyExists(): Unit = runBlocking {
        val client = ApiClient(URI("http://localhost:8081"), auth, client = ktor)
        val topology = client.getTopology("5f9825a6cf6e4c24e380b86f")
        assertNotNull(topology)
    }

    @Test
    fun testTopologyDoesNotExists(): Unit = runBlocking {
        val client = ApiClient(URI("http://localhost:8081"), auth, client = ktor)
        val topology = client.getTopology("x")
        assertNull(topology)
    }
}
