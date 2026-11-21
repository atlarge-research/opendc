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

import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.runtime.LaunchMode
import io.quarkus.runtime.StartupEvent
import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.eclipse.microprofile.config.Config
import org.eclipse.microprofile.config.ConfigProvider
import org.opendc.web.server.model.HandleKind
import org.opendc.web.server.model.PlanTier
import org.opendc.web.server.model.UserAccount
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Instant
import java.util.Optional

/** How a deployment tells its callers apart. */
enum class AuthMode {
    /** Auth0 access tokens, verified by Quarkus OIDC against the tenant in `quarkus.oidc.*`. */
    AUTH0,

    /** No sign-in: every request acts as one implicit administrator account, metered against nothing. */
    ANONYMOUS,
}

@ConfigMapping(prefix = "opendc.auth")
interface AuthConfig {
    // Secure by default: a deployment that forgets this gets sign-in, not an open administrator.
    @WithDefault("auth0")
    fun mode(): AuthMode

    /**
     * OIDC subjects made administrators when they sign in, so a fresh deployment gets its first one
     * without SQL. The database stays the source of truth: removing a subject here revokes nothing.
     */
    fun admins(): Optional<Set<String>>

    fun auth0(): Auth0Client

    interface Auth0Client {
        /** The single-page application the frontend signs in through, which `/config` hands out. */
        fun clientId(): Optional<String>
    }
}

/** How the frontend is to sign in, which it learns from `/config` rather than from its own build. */
@Serializable
sealed interface AuthSettings {
    @Serializable
    @SerialName("auth0")
    data class Auth0(
        val domain: String,
        val clientId: String,
        val audience: String,
    ) : AuthSettings

    @Serializable
    @SerialName("anonymous")
    data object Anonymous : AuthSettings
}

/**
 * The sign-in a deployment's settings describe, cross-checked against the OIDC tenant, since a
 * mismatch would silently accept nobody, or anybody. Each problem names the key at fault.
 */
fun authSettings(
    mode: AuthMode,
    clientId: Optional<String>,
    config: Config,
): AuthSettings {
    val tenantEnabled = config.getOptionalValue("quarkus.oidc.tenant-enabled", Boolean::class.java).orElse(true)
    return when (mode) {
        AuthMode.ANONYMOUS -> {
            check(!tenantEnabled) { "opendc.auth.mode=anonymous cannot run with quarkus.oidc.tenant-enabled=true" }
            AuthSettings.Anonymous
        }
        AuthMode.AUTH0 -> {
            check(tenantEnabled) { "opendc.auth.mode=auth0 needs quarkus.oidc.tenant-enabled=true" }
            val server =
                config.getOptionalValue("quarkus.oidc.auth-server-url", String::class.java).orElseThrow {
                    IllegalStateException("opendc.auth.mode=auth0 needs quarkus.oidc.auth-server-url")
                }
            val audience =
                config.getOptionalValues("quarkus.oidc.token.audience", String::class.java).orElse(emptyList()).firstOrNull()
                    ?: throw IllegalStateException("opendc.auth.mode=auth0 needs quarkus.oidc.token.audience")
            AuthSettings.Auth0(
                domain = URI.create(server).host,
                clientId = clientId.orElseThrow { IllegalStateException("opendc.auth.mode=auth0 needs opendc.auth.auth0.client-id") },
                audience = audience,
            )
        }
    }
}

/** Who the implicit account of anonymous mode is to the identity provider, which it never meets. */
const val IMPLICIT_SUBJECT = "anonymous"

const val IMPLICIT_HANDLE = "local"

/** The account every request of anonymous mode acts as, seeded at boot before the socket opens. */
@ApplicationScoped
class ImplicitAccount {
    val id: Long by lazy { QuarkusTransaction.requiringNew().call { seeded().id } }

    private fun seeded(): UserAccount =
        UserAccount.findBySubject(IMPLICIT_SUBJECT) ?: UserAccount().apply {
            subject = IMPLICIT_SUBJECT
            handle = IMPLICIT_HANDLE
            handleKind = HandleKind.CHOSEN
            displayName = "Local user"
            planTier = PlanTier.FREE
            isAdmin = true
            createdAt = Instant.now()
            persist()
        }
}

/**
 * Settles how this deployment signs callers in at boot, so a misconfigured one fails to start. Anonymous
 * mode is warned about, since anyone who can reach the port is the administrator.
 */
@ApplicationScoped
class AuthSetup(private val config: AuthConfig) {
    @Produces
    @Singleton
    fun settings(): AuthSettings = authSettings(config.mode(), config.auth0().clientId(), ConfigProvider.getConfig())

    internal fun onStart(
        @Suppress("UNUSED_PARAMETER") @Observes event: StartupEvent,
        settings: AuthSettings,
        implicit: ImplicitAccount,
    ) {
        if (settings is AuthSettings.Anonymous) {
            implicit.id
            if (LaunchMode.current() == LaunchMode.NORMAL) {
                LOG.warn("opendc.auth.mode=anonymous: every request acts as one implicit administrator with no budget")
            }
        }
    }

    private companion object {
        val LOG = LoggerFactory.getLogger(AuthSetup::class.java)
    }
}
