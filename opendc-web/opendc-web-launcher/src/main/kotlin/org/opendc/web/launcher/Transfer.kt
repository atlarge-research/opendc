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

package org.opendc.web.launcher

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.name

/** How long to wait before each attempt after the first. */
private val BACKOFF = listOf(Duration.ofSeconds(1), Duration.ofSeconds(4))

private val CONNECT_TIMEOUT = Duration.ofSeconds(30)

private const val TOO_MANY_REQUESTS = 429

private val http: HttpClient by lazy {
    HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(CONNECT_TIMEOUT).build()
}

/** Bytes to publish: how many there are, and a way to read them from the start. */
class Payload(
    val size: Long,
    val open: () -> InputStream,
)

/** The bytes of the file at [path]. */
fun Payload(path: Path): Payload = Payload(Files.size(path)) { Files.newInputStream(path) }

/**
 * Writes [payload] to [target], a `file:` or `http(s):` URL. A file appears only once whole; a URL gets
 * one PUT with a declared length, as a presigned upload expects.
 */
fun publish(
    payload: Payload,
    target: String,
) {
    when (val location = locationOf(target)) {
        is Location.Local -> retrying(target) { write(location.path) { payload.open().use { input -> input.copyTo(it) } } }
        is Location.Remote ->
            retrying(target) {
                val request =
                    HttpRequest
                        .newBuilder(location.uri)
                        .PUT(HttpRequest.BodyPublishers.fromPublisher(HttpRequest.BodyPublishers.ofInputStream(payload.open), payload.size))
                        .build()
                expectSuccess(target, send(target, request, HttpResponse.BodyHandlers.discarding()).statusCode())
            }
    }
}

/** Reads [source], a `file:` or `http(s):` URL, handing its bytes to [sink]. */
fun fetch(
    source: String,
    sink: (InputStream) -> Unit,
) {
    when (val location = locationOf(source)) {
        is Location.Local -> Files.newInputStream(existing(location.path, source)).use(sink)
        is Location.Remote ->
            retrying(source) {
                val response = send(source, HttpRequest.newBuilder(location.uri).GET().build(), HttpResponse.BodyHandlers.ofInputStream())
                response.body().use { body ->
                    expectSuccess(source, response.statusCode())
                    sink(body)
                }
            }
    }
}

/**
 * Puts the bytes [source] names at [path]. A local file is linked where the filesystem allows it, since
 * a workload trace can be gigabytes that never needed a second copy.
 */
fun stage(
    source: String,
    path: Path,
) {
    path.parent.createDirectories()
    when (val location = locationOf(source)) {
        is Location.Local -> {
            val origin = existing(location.path, source)
            val copy = { write(path) { Files.newInputStream(origin).use { input -> input.copyTo(it) } } }
            try {
                Files.createSymbolicLink(path, origin.toAbsolutePath())
            } catch (e: IOException) {
                copy()
            } catch (e: UnsupportedOperationException) {
                copy()
            }
        }
        is Location.Remote -> fetch(source) { body -> write(path) { body.copyTo(it) } }
    }
}

/** A URL as it may appear in a message: without the query string, where a signature lives. */
fun redacted(url: String): String = url.substringBefore('?')

private sealed interface Location {
    data class Local(val path: Path) : Location

    data class Remote(val uri: URI) : Location
}

private fun locationOf(url: String): Location {
    val uri =
        try {
            URI.create(url)
        } catch (e: IllegalArgumentException) {
            throw LaunchFailure(EXIT_TRANSFER_FAILED, "'${redacted(url)}' is not a URL", e)
        }
    return when (uri.scheme) {
        "file" -> Location.Local(Path.of(uri))
        "http", "https" -> Location.Remote(uri)
        else -> throw LaunchFailure(EXIT_TRANSFER_FAILED, "Cannot reach '${redacted(url)}': only file: and http(s): URLs are transferred")
    }
}

/** Writes [path] whole or not at all. */
private fun write(
    path: Path,
    body: (OutputStream) -> Unit,
) {
    path.parent.createDirectories()
    val part = path.resolveSibling(".${path.name}.part")
    try {
        Files.newOutputStream(part).use(body)
        Files.move(part, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    } finally {
        Files.deleteIfExists(part)
    }
}

private fun existing(
    path: Path,
    url: String,
): Path {
    if (!path.exists()) {
        throw LaunchFailure(EXIT_TRANSFER_FAILED, "${redacted(url)} does not exist")
    }
    return path
}

private fun <T> send(
    url: String,
    request: HttpRequest,
    handler: HttpResponse.BodyHandler<T>,
): HttpResponse<T> =
    try {
        http.send(request, handler)
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        throw LaunchFailure(EXIT_TRANSFER_FAILED, "Interrupted while reaching ${redacted(url)}", e)
    }

/** A refusal worth asking again is thrown as one; anything else ends the transfer here. */
private fun expectSuccess(
    url: String,
    status: Int,
) {
    when {
        status in 200..299 -> return
        status == TOO_MANY_REQUESTS || status >= 500 -> throw IOException("${redacted(url)} answered $status")
        else -> throw LaunchFailure(EXIT_TRANSFER_FAILED, "${redacted(url)} answered $status")
    }
}

/** Runs [attempt] again after each [BACKOFF] wait while it fails with an [IOException]. */
private fun <T> retrying(
    url: String,
    attempt: () -> T,
): T {
    for (wait in BACKOFF) {
        try {
            return attempt()
        } catch (e: IOException) {
            Thread.sleep(wait.toMillis())
        }
    }
    return try {
        attempt()
    } catch (e: IOException) {
        throw LaunchFailure(EXIT_TRANSFER_FAILED, "Could not transfer ${redacted(url)}: ${e.message}", e)
    }
}
