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

import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.opendc.sdk.model.serialization.SdkJson
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** In wall-clock time, not simulated time. */
private val POST_INTERVAL = Duration.ofSeconds(5)

/** A report that has not gone out in this long has been overtaken by the next one anyway. */
private val POST_TIMEOUT = Duration.ofSeconds(10)

/**
 * Sends what the runs have reported so far, over and over, until the process is done with them.
 * Fire-and-forget: nothing here can change what the process does or exits with.
 */
class TelemetryPoster(
    private val endpoint: TelemetryTarget.Endpoint,
    private val measured: () -> TelemetryReport,
) : AutoCloseable {
    private val json = Json(from = SdkJson.json) { prettyPrint = false }
    private val client = HttpClient.newBuilder().connectTimeout(POST_TIMEOUT).build()
    private val worker = Executors.newSingleThreadScheduledExecutor { runnable -> Thread(runnable, "telemetry").apply { isDaemon = true } }

    fun start() {
        worker.scheduleWithFixedDelay(::post, POST_INTERVAL.toMillis(), POST_INTERVAL.toMillis(), TimeUnit.MILLISECONDS)
    }

    /** Stops reporting, having sent one last report so the final numbers are not the ones lost. */
    override fun close() {
        worker.shutdownNow()
        post()
        client.close()
    }

    private fun post() {
        val report = measured()
        if (report.runs.isEmpty()) {
            return
        }
        try {
            val request =
                HttpRequest
                    .newBuilder(URI.create(endpoint.url))
                    .timeout(POST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer ${endpoint.token}")
                    .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(TelemetryReport.serializer(), report)))
                    .build()
            val response = client.send(request, HttpResponse.BodyHandlers.discarding())
            if (response.statusCode() !in 200..299) {
                logger.warn { "Telemetry was refused with ${response.statusCode()}" }
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            logger.warn { "Telemetry could not be sent: ${e.message}" }
        }
    }

    private companion object {
        val logger = KotlinLogging.logger {}
    }
}
