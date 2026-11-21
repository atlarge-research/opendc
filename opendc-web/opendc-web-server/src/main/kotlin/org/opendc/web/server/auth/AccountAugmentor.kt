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

import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.SecurityIdentityAugmentor
import io.quarkus.security.runtime.QuarkusPrincipal
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.jwt.JsonWebToken
import java.time.Instant

/**
 * Turns whoever Quarkus security recognised into an account: an Auth0 subject's, made on first sight,
 * or in anonymous mode the implicit one, with no I/O. Identities from our own tokens pass through.
 */
@ApplicationScoped
class AccountAugmentor(
    private val config: AuthConfig,
    private val implicit: ImplicitAccount,
) : SecurityIdentityAugmentor {
    override fun augment(
        identity: SecurityIdentity,
        context: AuthenticationRequestContext,
    ): Uni<SecurityIdentity> {
        val principal = identity.principal
        return when {
            principal is JsonWebToken ->
                context.runBlocking {
                    val account = signedIn(principal.subject, config.admins().orElse(emptySet()), Instant.now())
                    QuarkusSecurityIdentity.builder(identity).actingAs(account).build()
                }
            identity.isAnonymous && config.mode() == AuthMode.ANONYMOUS ->
                Uni.createFrom().item(
                    QuarkusSecurityIdentity
                        .builder(identity)
                        .setAnonymous(false)
                        .setPrincipal(QuarkusPrincipal(IMPLICIT_HANDLE))
                        .addRoles(setOf(Roles.USER, Roles.ADMIN))
                        .addAttribute(ACCOUNT_ATTRIBUTE, implicit.id)
                        .build(),
                )
            else -> Uni.createFrom().item(identity)
        }
    }
}
