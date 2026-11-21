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

import io.smallrye.config.ConfigMapping
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.opendc.web.server.rest.API_ROOT
import org.opendc.web.server.rest.ApiProblem
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Optional
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Something a browser may download by following a plain link. */
@Serializable
sealed interface Download {
    @Serializable
    @SerialName("archive")
    data class Archive(val experimentId: String) : Download

    @Serializable
    @SerialName("trace")
    data class TraceContent(val traceId: String) : Download
}

@Serializable
private data class DownloadGrant(
    val download: Download,
    val expiresAt: Long,
)

/** A link to a download, relative to the frontend's API base, and when it stops working. */
@Serializable
data class DownloadLink(
    val url: String,
    val expiresAt: String,
)

@ConfigMapping(prefix = "opendc.downloads")
interface DownloadConfig {
    /**
     * Base64, at least 32 bytes, shared by every replica. Unset, each server signs with a random key
     * and links do not survive a restart.
     */
    fun signingKey(): Optional<String>
}

private val LIFETIME = Duration.ofMinutes(5)

private const val ALGORITHM = "HmacSHA256"

private const val KEY_BYTES = 32

/**
 * Signs and checks download links. A link is a capability, like a presigned URL: whoever holds it may
 * fetch what it names until it expires, which lets a browser follow it without an access token.
 */
@ApplicationScoped
class DownloadLinks(config: DownloadConfig) {
    private val key: ByteArray =
        config.signingKey().map { Base64.getDecoder().decode(it) }.orElseGet {
            ByteArray(KEY_BYTES).also(SecureRandom()::nextBytes)
        }

    init {
        check(key.size >= KEY_BYTES) { "opendc.downloads.signing-key must be at least $KEY_BYTES bytes" }
    }

    fun sign(
        download: Download,
        now: Instant,
    ): DownloadLink {
        val expiresAt = now.plus(LIFETIME)
        val payload = Json.encodeToString(DownloadGrant.serializer(), DownloadGrant(download, expiresAt.epochSecond)).encodeToByteArray()
        val ticket = "${encode(payload)}.${encode(mac(payload))}"
        return DownloadLink(url = "$API_ROOT/downloads/$ticket", expiresAt = expiresAt.toString())
    }

    /** What [ticket] grants, or a 404 when it was not signed here or has run out. */
    fun verify(
        ticket: String,
        now: Instant,
    ): Download {
        val parts = ticket.split('.')
        if (parts.size != 2) {
            throw expired()
        }
        val payload = decode(parts[0]) ?: throw expired()
        val signature = decode(parts[1]) ?: throw expired()
        if (!MessageDigest.isEqual(signature, mac(payload))) {
            throw expired()
        }
        val grant =
            try {
                Json.decodeFromString(DownloadGrant.serializer(), payload.decodeToString())
            } catch (e: SerializationException) {
                throw expired()
            }
        if (now.epochSecond >= grant.expiresAt) {
            throw expired()
        }
        return grant.download
    }

    private fun mac(payload: ByteArray): ByteArray {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(SecretKeySpec(key, ALGORITHM))
        return mac.doFinal(payload)
    }

    private fun encode(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun decode(text: String): ByteArray? =
        try {
            Base64.getUrlDecoder().decode(text)
        } catch (e: IllegalArgumentException) {
            null
        }

    private fun expired(): WebApplicationException =
        WebApplicationException(
            Response
                .status(404)
                .entity(ApiProblem(status = 404, title = "This download link is invalid or has expired"))
                .type(MediaType.APPLICATION_JSON)
                .build(),
        )
}
