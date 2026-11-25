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

import jakarta.ws.rs.WebApplicationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.Base64
import java.util.Optional

/**
 * A download link is a capability: it fetches what it names, for five minutes, for whoever holds
 * it. So it must not be possible to make one, stretch one or point one somewhere else.
 */
class DownloadLinksTest {
    private val now = Instant.parse("2026-10-05T12:00:00Z")
    private val links = DownloadLinks(config(ByteArray(32) { 7 }))

    @Test
    fun `a link grants exactly what it was signed for, until it runs out`() {
        val download = Download.Archive("3f5b1f4a-7c8e-4c3d-9d5a-2f9a1c7b6d21")
        val link = links.sign(download, now)

        assertEquals(download, links.verify(ticketOf(link), now.plusSeconds(299)))
        assertRefused { links.verify(ticketOf(link), now.plusSeconds(300)) }
    }

    @Test
    fun `a link whose payload or signature was changed is refused`() {
        val ticket = ticketOf(links.sign(Download.TraceContent("a"), now))
        val (payload, signature) = ticket.split('.')
        val forged =
            Base64.getUrlEncoder().withoutPadding().encodeToString(
                """{"download":{"type":"trace","traceId":"b"},"expiresAt":9999999999}""".toByteArray(),
            )

        assertRefused { links.verify("$forged.$signature", now) }
        assertRefused { links.verify("$payload.${signature.reversed()}", now) }
        assertRefused { links.verify("not a ticket", now) }
    }

    @Test
    fun `a link signed with another key is refused`() {
        val other = DownloadLinks(config(ByteArray(32) { 9 }))

        assertRefused { links.verify(ticketOf(other.sign(Download.Archive("x"), now)), now) }
    }

    private fun ticketOf(link: DownloadLink): String = link.url.removePrefix("api/v1/downloads/")

    private fun assertRefused(attempt: () -> Unit) {
        val refused = assertThrows<WebApplicationException> { attempt() }
        assertEquals(404, refused.response.status)
    }

    private fun config(key: ByteArray): DownloadConfig =
        object : DownloadConfig {
            override fun signingKey(): Optional<String> = Optional.of(Base64.getEncoder().encodeToString(key))
        }
}
