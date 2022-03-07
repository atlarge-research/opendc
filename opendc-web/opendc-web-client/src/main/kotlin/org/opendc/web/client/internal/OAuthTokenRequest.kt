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

package org.opendc.web.client.internal

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Token request sent to the OAuth server.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "grant_type")
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = OAuthTokenRequest.ClientCredentials::class, name = "client_credentials"),
        JsonSubTypes.Type(value = OAuthTokenRequest.RefreshToken::class, name = "refresh_token")
    ]
)
internal sealed class OAuthTokenRequest {
    /**
     * Client credentials grant for OAuth2
     */
    data class ClientCredentials(
        val audience: String,
        @JsonProperty("client_id")
        val clientId: String,
        @JsonProperty("client_secret")
        val clientSecret: String
    ) : OAuthTokenRequest()

    /**
     * Refresh token grant for OAuth2.
     */
    data class RefreshToken(
        @JsonProperty("refresh_token")
        val refreshToken: String,
        @JsonProperty("client_id")
        val clientId: String,
        @JsonProperty("client_secret")
        val clientSecret: String
    ) : OAuthTokenRequest()
}
