/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.web.client.transport

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.opendc.web.client.auth.AuthController
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Paths

/**
 * A [TransportClient] that accesses the OpenDC API over HTTP.
 *
 * @param baseUrl The base url of the API.
 * @param auth Helper class for managing authentication.
 * @param client The HTTP client to use.
 */
public class HttpTransportClient(
    private val baseUrl: URI,
    private val auth: AuthController?,
    private val client: HttpClient = HttpClient.newHttpClient()
) : TransportClient {
    /**
     * The Jackson object mapper to convert messages from/to JSON.
     */
    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    /**
     * Obtain a resource at [path] of [targetType].
     */
    override fun <T> get(path: String, targetType: TypeReference<T>): T? {
        val request = HttpRequest.newBuilder(buildUri(path))
            .GET()
            .also { auth?.injectToken(it) }
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())

        return when (val code = response.statusCode()) {
            in 200..299 -> mapper.readValue(response.body(), targetType)
            401 -> {
                val auth = auth
                if (auth != null) {
                    auth.refreshToken()
                    get(path, targetType)
                } else {
                    throw IllegalStateException("Authorization required")
                }
            }
            404 -> null
            else -> throw IllegalStateException("Invalid response $code")
        }
    }

    /**
     * Update a resource at [path] of [targetType].
     */
    override fun <B, T> post(path: String, body: B, targetType: TypeReference<T>): T? {
        val request = HttpRequest.newBuilder(buildUri(path))
            .POST(HttpRequest.BodyPublishers.ofByteArray(mapper.writeValueAsBytes(body)))
            .header("Content-Type", "application/json")
            .also { auth?.injectToken(it) }
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())

        return when (val code = response.statusCode()) {
            in 200..299 -> mapper.readValue(response.body(), targetType)
            401 -> {
                val auth = auth
                if (auth != null) {
                    auth.refreshToken()
                    post(path, body, targetType)
                } else {
                    throw IllegalStateException("Authorization required")
                }
            }
            404 -> null
            else -> throw IllegalStateException("Invalid response $code")
        }
    }

    /**
     * Replace a resource at [path] of [targetType].
     */
    override fun <B, T> put(path: String, body: B, targetType: TypeReference<T>): T? {
        val request = HttpRequest.newBuilder(buildUri(path))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(mapper.writeValueAsBytes(body)))
            .header("Content-Type", "application/json")
            .also { auth?.injectToken(it) }
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())

        return when (val code = response.statusCode()) {
            in 200..299 -> mapper.readValue(response.body(), targetType)
            401 -> {
                val auth = auth
                if (auth != null) {
                    auth.refreshToken()
                    put(path, body, targetType)
                } else {
                    throw IllegalStateException("Authorization required")
                }
            }
            404 -> null
            else -> throw IllegalStateException("Invalid response $code")
        }
    }

    /**
     * Delete a resource at [path] of [targetType].
     */
    override fun <T> delete(path: String, targetType: TypeReference<T>): T? {
        val request = HttpRequest.newBuilder(buildUri(path))
            .DELETE()
            .also { auth?.injectToken(it) }
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())

        return when (val code = response.statusCode()) {
            in 200..299 -> mapper.readValue(response.body(), targetType)
            401 -> {
                val auth = auth
                if (auth != null) {
                    auth.refreshToken()
                    delete(path, targetType)
                } else {
                    throw IllegalStateException("Authorization required")
                }
            }
            404 -> null
            else -> throw IllegalStateException("Invalid response $code")
        }
    }

    /**
     * Build the absolute [URI] to which the request should be sent.
     */
    private fun buildUri(path: String): URI = baseUrl.resolve(Paths.get(baseUrl.path, path).toString())
}
