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

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.sdk.model.serialization.SdkJson
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Telemetry is fire-and-forget in both directions. A launcher whose reports cannot be delivered still
 * simulates, still writes its parquet and still exits the way it would have, so the only thing worth
 * pinning here is that nothing this class does can change that.
 */
class TelemetryPosterTest {
    private lateinit var server: HttpServer
    private val received = CopyOnWriteArrayList<Posted>()

    @Volatile
    private var answer = 204

    @BeforeEach
    fun listen() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/telemetry") { exchange -> take(exchange) }
        server.start()
    }

    @AfterEach
    fun stop() {
        server.stop(0)
    }

    @Test
    fun `sends what was measured, as the execution it was minted for`() {
        poster().use { it.start() }

        val posted = received.single()
        assertEquals("Bearer odc_exec_test", posted.authorization)
        assertEquals(REPORT.runs, CODEC.decodeFromString(TelemetryReport.serializer(), posted.body).runs)
    }

    // A server that is down, restarting or simply refusing is not an error the launcher can act on:
    // its work is unaffected and the next report supersedes the one that was lost.
    @Test
    fun `keeps going when the endpoint refuses a report`() {
        answer = 500

        poster().use { it.start() }

        assertTrue(received.isNotEmpty()) { "the report was never attempted" }
    }

    @Test
    fun `says nothing at all before a run has reported`() {
        TelemetryPoster(endpoint(), { TelemetryReport(emptyList()) }).use { it.start() }

        assertTrue(received.isEmpty()) { "an empty report should not have been sent" }
    }

    private fun poster() = TelemetryPoster(endpoint(), { REPORT })

    private fun endpoint() = TelemetryTarget.Endpoint("http://127.0.0.1:${server.address.port}/telemetry", "odc_exec_test")

    private fun take(exchange: HttpExchange) {
        received += Posted(exchange.requestHeaders.getFirst("Authorization"), exchange.requestBody.readBytes().decodeToString())
        exchange.sendResponseHeaders(answer, -1)
        exchange.close()
    }

    private data class Posted(
        val authorization: String?,
        val body: String,
    )

    private companion object {
        val REPORT =
            TelemetryReport(
                listOf(
                    RunTelemetry(
                        scenarioIndex = 2,
                        seed = 7,
                        completedTasks = 3,
                        series = listOf(MetricSeries(ResultMetric.HOST_CPU_UTILIZATION.id, listOf(MetricPoint(0, 0.5)))),
                    ),
                ),
            )

        /** The launcher encodes with the SDK's boundary, so a test decoding it has to read the same one. */
        val CODEC = Json(from = SdkJson.json) { prettyPrint = false }
    }
}
