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

package org.opendc.web.server.auth

import io.smallrye.config.PropertiesConfigSource
import io.smallrye.config.SmallRyeConfigBuilder
import org.eclipse.microprofile.config.Config
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional

/**
 * The sign-in mode and the OIDC tenant that verifies tokens are separate settings. A deployment
 * where they disagree would accept nobody, or anybody, so it has to refuse to start and name the
 * key at fault.
 */
class AuthSettingsTest {
    private val tenant =
        mapOf(
            "quarkus.oidc.tenant-enabled" to "true",
            "quarkus.oidc.auth-server-url" to "https://opendc.eu.auth0.com/",
            "quarkus.oidc.token.audience" to "https://api.opendc.org",
        )

    @Test
    fun `an Auth0 deployment hands the frontend the tenant it signs in through`() {
        val settings = authSettings(AuthMode.AUTH0, Optional.of("spa"), config(tenant))

        assertEquals(AuthSettings.Auth0(domain = "opendc.eu.auth0.com", clientId = "spa", audience = "https://api.opendc.org"), settings)
    }

    @Test
    fun `an Auth0 deployment missing a setting names the key it lacks`() {
        assertNames("opendc.auth.auth0.client-id") { authSettings(AuthMode.AUTH0, Optional.empty(), config(tenant)) }
        assertNames("quarkus.oidc.tenant-enabled") {
            authSettings(AuthMode.AUTH0, Optional.of("spa"), config(tenant + ("quarkus.oidc.tenant-enabled" to "false")))
        }
        assertNames("quarkus.oidc.auth-server-url") {
            authSettings(AuthMode.AUTH0, Optional.of("spa"), config(tenant - "quarkus.oidc.auth-server-url"))
        }
        assertNames("quarkus.oidc.token.audience") {
            authSettings(AuthMode.AUTH0, Optional.of("spa"), config(tenant - "quarkus.oidc.token.audience"))
        }
    }

    @Test
    fun `an anonymous deployment refuses a tenant that would verify tokens it never asks for`() {
        assertEquals(
            AuthSettings.Anonymous,
            authSettings(AuthMode.ANONYMOUS, Optional.empty(), config(mapOf("quarkus.oidc.tenant-enabled" to "false"))),
        )
        assertNames("quarkus.oidc.tenant-enabled") { authSettings(AuthMode.ANONYMOUS, Optional.empty(), config(tenant)) }
    }

    private fun assertNames(
        key: String,
        attempt: () -> Unit,
    ) {
        val failure = assertThrows<IllegalStateException> { attempt() }
        assertTrue(key in failure.message.orEmpty()) { "expected $key in: ${failure.message}" }
    }

    private fun config(properties: Map<String, String>): Config =
        SmallRyeConfigBuilder().withSources(PropertiesConfigSource(properties, "test", 100)).build()
}
