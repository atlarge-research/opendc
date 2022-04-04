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

package org.opendc.web.client.auth

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.opendc.web.client.internal.OAuthTokenRequest
import org.opendc.web.client.internal.OAuthTokenResponse
import org.opendc.web.client.internal.OpenIdConfiguration
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * An [AuthController] for OpenID Connect protected APIs.
 */
public class OpenIdAuthController(
    private val domain: String,
    private val clientId: String,
    private val clientSecret: String,
    private val audience: String = "https://api.opendc.org/v2/",
    private val client: HttpClient = HttpClient.newHttpClient()
) : AuthController {
    /**
     * The Jackson object mapper to convert messages from/to JSON.
     */
    private val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    /**
     * The cached [OpenIdConfiguration].
     */
    private val openidConfig: OpenIdConfiguration
        get() {
            var openidConfig = _openidConfig
            if (openidConfig == null) {
                openidConfig = requestConfig()
                _openidConfig = openidConfig
            }

            return openidConfig
        }
    private var _openidConfig: OpenIdConfiguration? = null

    /**
     * The cached OAuth token.
     */
    private var _token: OAuthTokenResponse? = null

    override fun injectToken(request: HttpRequest.Builder) {
        var token = _token
        if (token == null) {
            token = requestToken()
            _token = token
        }

        request.header("Authorization", "Bearer ${token.accessToken}")
    }

    /**
     * Refresh the current access token.
     */
    override fun refreshToken() {
        val refreshToken = _token?.refreshToken
        if (refreshToken == null) {
            requestToken()
            return
        }

        _token = refreshToken(openidConfig, refreshToken)
    }

    /**
     * Request the OpenID configuration from the chosen auth domain
     */
    private fun requestConfig(): OpenIdConfiguration {
        val request = HttpRequest.newBuilder(URI("https://$domain/.well-known/openid-configuration"))
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        return mapper.readValue(response.body())
    }

    /**
     * Request the auth token from the server.
     */
    private fun requestToken(openidConfig: OpenIdConfiguration): OAuthTokenResponse {
        val body = OAuthTokenRequest.ClientCredentials(audience, clientId, clientSecret)
        val request = HttpRequest.newBuilder(openidConfig.tokenEndpoint)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofByteArray(mapper.writeValueAsBytes(body)))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        return mapper.readValue(response.body())
    }

    /**
     * Helper method to refresh the auth token.
     */
    private fun refreshToken(openidConfig: OpenIdConfiguration, refreshToken: String): OAuthTokenResponse {
        val body = OAuthTokenRequest.RefreshToken(refreshToken, clientId, clientSecret)
        val request = HttpRequest.newBuilder(openidConfig.tokenEndpoint)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofByteArray(mapper.writeValueAsBytes(body)))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        return mapper.readValue(response.body())
    }

    /**
     * Fetch a new access token.
     */
    private fun requestToken(): OAuthTokenResponse {
        val token = requestToken(openidConfig)
        _token = token
        return token
    }
}
