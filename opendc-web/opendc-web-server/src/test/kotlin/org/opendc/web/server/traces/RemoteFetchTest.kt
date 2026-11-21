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

package org.opendc.web.server.traces

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.time.Duration
import java.time.Instant

/**
 * Fetching on a user's behalf: which addresses a URL may lead to, and where a download is cut off.
 * The server under test is on loopback, which is itself one of the addresses the guard refuses, so
 * the downloads here run with private hosts allowed and the guard is tested on its own.
 */
class RemoteFetchTest {
    private lateinit var server: HttpServer

    @BeforeEach
    fun serve() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/small") { exchange ->
            exchange.sendResponseHeaders(200, SMALL.size.toLong())
            exchange.responseBody.use { it.write(SMALL) }
        }
        // No content length: the cap has to catch it while reading, not from the headers.
        server.createContext("/streamed") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            exchange.responseBody.use { body -> repeat(LARGE_CHUNKS) { body.write(ByteArray(CHUNK)) } }
        }
        server.createContext("/moved") { exchange ->
            exchange.responseHeaders.add("Location", "/small")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        // Answers, sends a little, then goes quiet without closing.
        server.createContext("/stalled") { exchange ->
            exchange.sendResponseHeaders(200, SMALL.size * 2L)
            exchange.responseBody.write(SMALL)
            exchange.responseBody.flush()
            Thread.sleep(STALL_MILLIS)
            exchange.close()
        }
        server.createContext("/loop") { exchange ->
            exchange.responseHeaders.add("Location", "/loop")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server.start()
    }

    @AfterEach
    fun stop() {
        server.stop(0)
    }

    @Test
    fun `refuses private, loopback, link-local and unique-local addresses unless they are allowed`() {
        val uri = URI("https://example.org/tasks.parquet")
        val addresses =
            listOf(
                "127.0.0.1", "10.1.2.3", "192.168.1.1", "172.16.0.9", "169.254.169.254", "0.0.0.0", "0.1.2.3",
                "100.100.100.200", "198.18.0.1", "192.0.0.8", "255.255.255.255", "::1", "fd12::1", "::ffff:10.0.0.1",
            )
        for (address in addresses) {
            val check = checkSource(uri, listOf(InetAddress.getByName(address)), allowPrivateHosts = false)
            assertInstanceOf(SourceCheck.Refused::class.java, check, address)
        }
        assertEquals(SourceCheck.Allowed, checkSource(uri, listOf(InetAddress.getByName("10.1.2.3")), allowPrivateHosts = true))
    }

    @Test
    fun `refuses a host with one private address among public ones`() {
        val addresses = listOf(InetAddress.getByName("93.184.216.34"), InetAddress.getByName("10.0.0.1"))

        assertInstanceOf(SourceCheck.Refused::class.java, checkSource(URI("https://mixed.example/"), addresses, false))
        assertEquals(SourceCheck.Allowed, checkSource(URI("https://mixed.example/"), addresses.take(1), false))
    }

    @Test
    fun `fetches only over http and https, even where private hosts are allowed`() {
        for (url in listOf("file:///etc/passwd", "ftp://example.org/tasks.parquet", "jar:file:/x.jar!/y")) {
            assertInstanceOf(SourceCheck.Refused::class.java, checkSource(URI(url), emptyList(), allowPrivateHosts = true), url)
        }
    }

    @Test
    fun `follows a redirect and reads what it leads to`() {
        val body = fetch().read(url("/moved")) { it.readBytes() }

        assertTrue(body.contentEquals(SMALL))
    }

    @Test
    fun `refuses a loopback server when private hosts are not allowed`() {
        val failure = assertThrows<FetchFailure> { RemoteFetch(false, MAX, later()).read(url("/small")) { it.readBytes() } }

        assertTrue(failure.message!!.contains("not a public address"))
    }

    @Test
    fun `cuts off a body larger than the cap, whether or not it declared its length`() {
        assertThrows<FetchFailure> { RemoteFetch(true, SMALL.size - 1L, later()).read(url("/small")) { it.readBytes() } }
        assertThrows<FetchFailure> { RemoteFetch(true, CHUNK * 2L, later()).read(url("/streamed")) { it.readBytes() } }
    }

    @Test
    fun `gives up on redirects that never end and on missing files`() {
        assertThrows<FetchFailure> { fetch().read(url("/loop")) { it.readBytes() } }
        val missing = assertThrows<FetchFailure> { fetch().read(url("/absent")) { it.readBytes() } }
        assertTrue(missing.message!!.contains("404"))
    }

    @Test
    fun `gives up on a server that answers and then goes quiet`() {
        val failure =
            assertThrows<FetchFailure> {
                RemoteFetch(true, MAX, later(), idleTimeout = Duration.ofMillis(200)).read(url("/stalled")) { it.readBytes() }
            }

        assertTrue(failure.message!!.contains("stopped sending"), failure.message)
    }

    @Test
    fun `starts nothing once the deadline has passed`() {
        assertThrows<FetchFailure> { RemoteFetch(true, MAX, Instant.now().minusSeconds(1)).read(url("/small")) { it.readBytes() } }
    }

    private fun fetch() = RemoteFetch(true, MAX, later())

    private fun later() = Instant.now().plus(Duration.ofMinutes(1))

    private fun url(path: String) = URI("http://127.0.0.1:${server.address.port}$path")

    private companion object {
        val SMALL = "PAR1 tiny".encodeToByteArray()
        const val CHUNK = 8192
        const val LARGE_CHUNKS = 64
        const val MAX = 1L shl 30
        const val STALL_MILLIS = 2000L
    }
}
