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

package org.opendc.cli.run

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** How the fake answers a submit: by accepting the experiment, or by refusing it with one issue. */
internal sealed interface SubmitAnswer {
    data object Accept : SubmitAnswer

    data class Refuse(
        val path: String,
        val message: String,
    ) : SubmitAnswer
}

/**
 * The slice of the OpenDC API a remote run talks to, on loopback: projects, one experiment that runs
 * for [runningPolls] status reads and then ends [finalState], and a results archive with one run.
 */
internal class FakeOpendcServer(
    private val submit: SubmitAnswer = SubmitAnswer.Accept,
    private val finalState: String = "succeeded",
    private val runningPolls: Int = 2,
    /** How many status reads after the first one answer 503, as a server restarting would. */
    private val unavailablePolls: Int = 0,
) : AutoCloseable {
    val projects = CopyOnWriteArrayList<String>()
    val authorizations = CopyOnWriteArrayList<String>()
    val cancelled = CopyOnWriteArrayList<String>()
    val deleted = CopyOnWriteArrayList<String>()
    private val polls = AtomicInteger()
    private val statusReads = AtomicInteger()
    private val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)

    val url: String get() = "http://127.0.0.1:${server.address.port}"

    init {
        server.createContext("/api/v1/") { exchange ->
            authorizations += exchange.requestHeaders.getFirst("Authorization") ?: "none"
            route(exchange)
        }
        server.start()
    }

    override fun close() = server.stop(0)

    private fun route(exchange: HttpExchange) {
        val path = exchange.requestURI.path.removePrefix("/api/v1/")
        val method = exchange.requestMethod
        when {
            path == "projects" && method == "GET" ->
                json(exchange, 200, projects.joinToString(",", "[", "]") { """{"id":"p-$it","name":"$it"}""" })
            path == "projects" && method == "POST" -> {
                val name = Regex(""""name":"([^"]+)"""").find(exchange.requestBody.readBytes().decodeToString())!!.groupValues[1]
                projects += name
                json(exchange, 201, """{"id":"p-$name","name":"$name"}""")
            }
            path == "experiments" && method == "POST" -> json(exchange, 201, """{"id":"x-1"}""")
            path == "experiments/x-1" && method == "DELETE" -> {
                deleted += "x-1"
                exchange.sendResponseHeaders(204, -1)
                exchange.close()
            }
            path == "experiments/x-1/submit" -> submitted(exchange)
            path == "experiments/x-1/status" && statusReads.getAndIncrement() in 1..unavailablePolls ->
                json(exchange, 503, """{"status":503,"title":"Service unavailable","issues":[]}""")
            path == "experiments/x-1/status" -> json(exchange, 200, status())
            path == "experiments/x-1/cancel" -> {
                cancelled += "x-1"
                json(exchange, 200, "{}")
            }
            path == "experiments/x-1/archive/link" -> json(exchange, 200, """{"url":"api/v1/downloads/ticket","expiresAt":"later"}""")
            // The real endpoint produces only a zip, and refuses a request that accepts only JSON.
            path == "downloads/ticket" && exchange.requestHeaders.getFirst("Accept") == "application/json" ->
                json(exchange, 406, """{"status":406,"title":"The accept header value did not match","issues":[]}""")
            path == "downloads/ticket" -> {
                val zip = archive()
                exchange.sendResponseHeaders(200, zip.size.toLong())
                exchange.responseBody.use { it.write(zip) }
            }
            else -> json(exchange, 404, """{"status":404,"title":"Not found","issues":[]}""")
        }
    }

    private fun submitted(exchange: HttpExchange) =
        when (submit) {
            SubmitAnswer.Accept -> json(exchange, 200, """{"id":"x-1"}""")
            is SubmitAnswer.Refuse ->
                json(
                    exchange,
                    400,
                    """{"status":400,"title":"The experiment cannot run",""" +
                        """"issues":[{"path":"${submit.path}","message":"${submit.message}"}]}""",
                )
        }

    private fun status(): String {
        val poll = polls.getAndIncrement()
        val running = poll < runningPolls
        val state = if (running) "running" else finalState
        val completed = if (running) poll else TOTAL_TASKS
        val exit =
            if (finalState == "succeeded" || running) {
                ""
            } else {
                ""","exitInfo":{"exitCode":23,"reason":"simulationError","message":"threw"}"""
            }
        return """{"id":"x-1","name":"tiny","state":"$state","completedTasks":$completed,"totalTasks":$TOTAL_TASKS,""" +
            """"scenarioCount":1,"scenarios":[{"scenarioIndex":0,"state":"$state","completedTasks":$completed,""" +
            """"totalTasks":$TOTAL_TASKS,"attempt":1$exit}]}"""
    }

    private fun archive(): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("tiny/raw-output/0/seed=0/host.parquet"))
            zip.write("PAR1".encodeToByteArray())
            zip.closeEntry()
        }
        return bytes.toByteArray()
    }

    private fun json(
        exchange: HttpExchange,
        status: Int,
        body: String,
    ) {
        val bytes = body.encodeToByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    companion object {
        const val TOTAL_TASKS = 4
    }
}
