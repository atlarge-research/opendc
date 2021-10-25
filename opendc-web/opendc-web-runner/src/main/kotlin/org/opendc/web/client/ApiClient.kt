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

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.opendc.web.client.model.*
import java.net.URI

/**
 * Client implementation for the OpenDC REST API (version 2).
 *
 * @param baseUrl The base url of the API.
 * @param auth The authentication configuration for the client.
 * @param client The HTTP client to use.
 */
public class ApiClient(
    private val baseUrl: URI,
    private val auth: AuthConfiguration,
    private val audience: String = "https://api.opendc.org/v2/",
    client: HttpClient = HttpClient {}
) : AutoCloseable {
    /**
     * The Ktor [HttpClient] that is used to communicate with the REST API.
     */
    private val client = client.config {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerModule(JavaTimeModule())
            }
        }
        install(Auth) {
            bearer {
                loadTokens { requestToken() }
                refreshTokens { requestToken() }
            }
        }
        expectSuccess = false
    }

    /**
     * Retrieve the topology with the specified [id].
     */
    public suspend fun getPortfolio(id: String): Portfolio? {
        val url = URLBuilder(Url(baseUrl))
            .path("portfolios", id)
            .build()
        return when (val result = client.get<ApiResult<Portfolio>>(url)) {
            is ApiResult.Success -> result.data
            else -> null
        }
    }

    /**
     * Retrieve the scenario with the specified [id].
     */
    public suspend fun getScenario(id: String): Scenario? {
        val url = URLBuilder(Url(baseUrl))
            .path("scenarios", id)
            .build()
        return when (val result = client.get<ApiResult<Scenario>>(url)) {
            is ApiResult.Success -> result.data
            else -> null
        }
    }

    /**
     * Retrieve the topology with the specified [id].
     */
    public suspend fun getTopology(id: String): Topology? {
        val url = URLBuilder(Url(baseUrl))
            .path("topologies", id)
            .build()
        return when (val result = client.get<ApiResult<Topology>>(url)) {
            is ApiResult.Success -> result.data
            else -> null
        }
    }

    /**
     * Retrieve the available jobs.
     */
    public suspend fun getJobs(): List<Job> {
        val url = URLBuilder(Url(baseUrl))
            .path("jobs")
            .build()
        return when (val result = client.get<ApiResult<List<Job>>>(url)) {
            is ApiResult.Success -> result.data
            else -> emptyList()
        }
    }

    /**
     * Update the specified job.
     *
     * @param id The identifier of the job.
     * @param state The new state of the job.
     * @param results The results of the job.
     */
    public suspend fun updateJob(id: String, state: SimulationState, results: Map<String, Any> = emptyMap()): Boolean {
        val url = URLBuilder(Url(baseUrl))
            .path("jobs", id)
            .build()

        data class Request(
            val state: SimulationState,
            val results: Map<String, Any>
        )

        val res = client.post<HttpResponse> {
            url(url)
            contentType(ContentType.Application.Json)
            body = Request(state, results)
        }
        return res.status.isSuccess()
    }

    /**
     * Request the auth token for the API.
     */
    private suspend fun requestToken(): BearerTokens {
        data class Request(
            val audience: String,
            @JsonProperty("grant_type")
            val grantType: String,
            @JsonProperty("client_id")
            val clientId: String,
            @JsonProperty("client_secret")
            val clientSecret: String
        )

        data class Response(
            @JsonProperty("access_token")
            val accessToken: String,
            @JsonProperty("token_type")
            val tokenType: String,
            val scope: String = "",
            @JsonProperty("expires_in")
            val expiresIn: Long
        )

        val result = client.post<Response> {
            url(Url("https://${auth.domain}/oauth/token"))
            contentType(ContentType.Application.Json)
            body = Request(audience, "client_credentials", auth.clientId, auth.clientSecret)
        }

        return BearerTokens(result.accessToken, "")
    }

    override fun close() = client.close()
}
