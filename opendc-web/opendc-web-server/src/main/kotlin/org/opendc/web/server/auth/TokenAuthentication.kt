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
import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.IdentityProvider
import io.quarkus.security.identity.IdentityProviderManager
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.request.AuthenticationRequest
import io.quarkus.security.identity.request.BaseAuthenticationRequest
import io.quarkus.security.runtime.QuarkusPrincipal
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.quarkus.vertx.http.runtime.security.ChallengeData
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism
import io.smallrye.mutiny.Uni
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.HttpHeaders
import org.opendc.web.server.model.AccessToken
import org.opendc.web.server.model.AccountState
import org.opendc.web.server.model.EXECUTION_TOKEN_PREFIX
import org.opendc.web.server.model.Execution
import org.opendc.web.server.model.PAT_PREFIX
import java.time.Instant

/** The scheme of every credential this server accepts, and of the challenge a 401 carries. */
const val BEARER_SCHEME = "Bearer"

private const val BEARER = "$BEARER_SCHEME "

/** Every token this server mints starts so, which is what tells one apart from an Auth0 JWT. */
private const val OPENDC_TOKEN = "odc_"

/** Ahead of OIDC (1001), so a token of ours is never handed to the JWT verifier. */
private const val OPAQUE_TOKEN_PRIORITY = 2000

/** A token this server minted, presented as a bearer credential. */
class OpaqueTokenRequest(val secret: String) : BaseAuthenticationRequest()

/**
 * Claims `Authorization: Bearer odc_...` and nothing else, so personal access tokens and launcher
 * tokens work the same in either sign-in mode, while every other bearer goes on to OIDC.
 */
@ApplicationScoped
class OpaqueTokenMechanism : HttpAuthenticationMechanism {
    override fun authenticate(
        context: RoutingContext,
        identities: IdentityProviderManager,
    ): Uni<SecurityIdentity> {
        val header = context.request().getHeader(HttpHeaders.AUTHORIZATION).orEmpty()
        val secret = header.removePrefix(BEARER).trim()
        if (!header.startsWith(BEARER) || !secret.startsWith(OPENDC_TOKEN)) {
            return Uni.createFrom().nullItem()
        }
        return identities.authenticate(OpaqueTokenRequest(secret))
    }

    override fun getChallenge(context: RoutingContext): Uni<ChallengeData> =
        Uni.createFrom().item(ChallengeData(401, HttpHeaders.WWW_AUTHENTICATE, BEARER_SCHEME))

    override fun getCredentialTypes(): Set<Class<out AuthenticationRequest>> = setOf(OpaqueTokenRequest::class.java)

    override fun getPriority(): Int = OPAQUE_TOKEN_PRIORITY
}

/**
 * Resolves a token of ours to whoever it stands for. A token that resolves to nothing is refused
 * outright rather than falling back to anonymous, so a revoked token never acts as anyone.
 */
@ApplicationScoped
class OpaqueTokenIdentityProvider : IdentityProvider<OpaqueTokenRequest> {
    override fun getRequestType(): Class<OpaqueTokenRequest> = OpaqueTokenRequest::class.java

    override fun authenticate(
        request: OpaqueTokenRequest,
        context: AuthenticationRequestContext,
    ): Uni<SecurityIdentity> = context.runBlocking { QuarkusTransaction.requiringNew().call { identityOf(request.secret, Instant.now()) } }
}

private fun identityOf(
    secret: String,
    now: Instant,
): SecurityIdentity? =
    when {
        secret.startsWith(PAT_PREFIX) -> personalIdentity(secret, now)
        secret.startsWith(EXECUTION_TOKEN_PREFIX) -> executionIdentity(secret)
        else -> null
    }

private fun personalIdentity(
    secret: String,
    now: Instant,
): SecurityIdentity? {
    val token = AccessToken.findBySecret(secret) ?: return null
    if (token.user.state != AccountState.ACTIVE) {
        return null
    }
    token.recordUse(now)
    return QuarkusSecurityIdentity.builder().actingAs(token.user).build()
}

/** A launcher, which may report on its own execution and do nothing else. */
private fun executionIdentity(secret: String): SecurityIdentity? {
    val execution = Execution.findByToken(secret) ?: return null
    return QuarkusSecurityIdentity
        .builder()
        .setPrincipal(QuarkusPrincipal("execution:${execution.publicId}"))
        .addRole(Roles.EXECUTION)
        .addAttribute(EXECUTION_ATTRIBUTE, execution.publicId)
        .build()
}
