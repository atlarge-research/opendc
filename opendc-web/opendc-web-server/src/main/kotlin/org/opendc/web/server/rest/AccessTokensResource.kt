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

import jakarta.transaction.Transactional
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.web.server.auth.Identity
import org.opendc.web.server.model.AccessToken
import org.opendc.web.server.model.TokenUsage
import java.time.Instant

/**
 * The caller's personal access tokens, which is how the CLI and scripts act as them. A token's
 * secret is in the response that mints it and nowhere else, ever.
 */
@Path("me/tokens")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class AccessTokensResource(private val identity: Identity) {
    @GET
    fun list(): List<AccessTokenWire> = AccessToken.findOwnedBy(identity.currentUser().id).map { it.toWire() }

    @POST
    @Transactional
    fun mint(request: TokenRequest): Response {
        val minted = AccessToken.mint(identity.currentUser(), validName(request.name, "Token"), Instant.now())
        return Response.status(201).entity(MintedToken(minted.token.toWire(), minted.secret)).build()
    }

    /** Revokes one of the caller's tokens. Someone else's reads as one that does not exist. */
    @DELETE
    @Path("{id}")
    @Transactional
    fun revoke(
        @PathParam("id") id: String,
    ): Response {
        val token =
            AccessToken
                .find("publicId = ?1 AND user.id = ?2", publicId(id, "Token"), identity.currentUser().id)
                .firstResult() ?: throw notFound("Token")
        token.delete()
        return Response.noContent().build()
    }
}

private fun AccessToken.toWire(): AccessTokenWire =
    AccessTokenWire(
        id = publicId.toString(),
        name = name,
        prefix = tokenPrefix,
        createdAt = createdAt.toString(),
        lastUse =
            when (val use = lastUse) {
                TokenUsage.Unused -> TokenUsageWire.Unused
                is TokenUsage.Used -> TokenUsageWire.Used(use.at.toString())
            },
    )

@Serializable
data class TokenRequest(val name: String)

@Serializable
data class AccessTokenWire(
    val id: String,
    val name: String,
    val prefix: String,
    val createdAt: String,
    val lastUse: TokenUsageWire,
)

@Serializable
sealed interface TokenUsageWire {
    @Serializable
    @SerialName("unused")
    data object Unused : TokenUsageWire

    @Serializable
    @SerialName("used")
    data class Used(val at: String) : TokenUsageWire
}

@Serializable
data class MintedToken(
    val token: AccessTokenWire,
    val secret: String,
)
