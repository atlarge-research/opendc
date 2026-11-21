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

import com.sun.net.httpserver.HttpServer
import io.quarkus.test.junit.QuarkusTest
import io.restassured.path.json.JsonPath
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.web.server.ApiTest
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val SETTLE_TIMEOUT: Duration = Duration.ofSeconds(20)

/**
 * Importing a trace from a URL: it joins the library only once every table has arrived and passed
 * the checks an upload gets, and a failure says why and frees the name again.
 */
@QuarkusTest
class TraceImportsResourceTest {
    private lateinit var files: HttpServer
    private val held = CountDownLatch(1)

    @BeforeEach
    fun serve() {
        files = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        for (name in listOf("tasks", "fragments")) {
            val bytes = ApiTest.fixtureBytes("traces/workload/$name.parquet")
            files.createContext("/$name.parquet") { exchange ->
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
        }
        files.createContext("/held.parquet") { exchange ->
            held.await(SETTLE_TIMEOUT.seconds, TimeUnit.SECONDS)
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
        }
        files.executor = Executors.newCachedThreadPool()
        files.start()
    }

    @AfterEach
    fun stop() {
        held.countDown()
        files.stop(0)
    }

    @Test
    fun `imports a workload into the library once both tables have landed`() {
        val name = unique("imported")
        val import = start(name, mapOf("tasks" to url("tasks"), "fragments" to url("fragments")))
        assertEquals("running", import.getString("progress.type"))

        assertEquals("succeeded", settle(import.getString("id")).type)
        val trace = ApiTest.requestJson().get("/api/v1/traces/${import.getString("traceId")}").then().statusCode(200).extract().jsonPath()
        assertTrue(trace.getLong("tables[0].rowCount") > 0, "the footer gives a row count")
        ApiTest.requestJson().get("/api/v1/traces").then().body("slug", hasItem(containsString(name)))
    }

    @Test
    fun `fails an import whose file is not the table it was filed as, and frees the name`() {
        val name = unique("swapped")
        val import = start(name, mapOf("tasks" to url("fragments"), "fragments" to url("fragments")))

        val settled = settle(import.getString("id"))

        assertEquals("failed", settled.type)
        assertTrue(settled.reason.contains("not a tasks table"), settled.reason)
        ApiTest.requestJson().get("/api/v1/traces").then().body("slug", not(hasItem(containsString(name))))

        ApiTest.requestJson().delete("/api/v1/traces/imports/${import.getString("id")}").then().statusCode(204)
        val again = start(name, mapOf("tasks" to url("tasks"), "fragments" to url("fragments")))
        assertEquals("succeeded", settle(again.getString("id")).type)
    }

    @Test
    fun `fails an import whose server answers with an error`() {
        val import = start(unique("missing"), mapOf("tasks" to url("absent"), "fragments" to url("fragments")))

        val settled = settle(import.getString("id"))

        assertTrue(settled.reason.contains("404"), settled.reason)
    }

    @Test
    fun `holds the name and refuses to dismiss while the fetch is still running`() {
        val name = unique("held")
        val import = start(name, mapOf("tasks" to url("held"), "fragments" to url("fragments")))

        ApiTest.requestJson().delete("/api/v1/traces/imports/${import.getString("id")}").then().statusCode(409)
        importing(name, mapOf("tasks" to url("tasks"), "fragments" to url("fragments"))).then().statusCode(409)

        held.countDown()
        assertEquals("failed", settle(import.getString("id")).type)
    }

    @Test
    fun `refuses a request missing a table or naming something other than a web URL`() {
        importing(unique("partial"), mapOf("tasks" to url("tasks")))
            .then()
            .statusCode(400)
            .body("issues.path", hasItem("sources.fragments"))
        importing(unique("local"), mapOf("tasks" to "file:///etc/passwd", "fragments" to url("fragments")))
            .then()
            .statusCode(400)
            .body("issues.find { it.path == 'sources.tasks' }.message", equalTo("only http and https URLs can be imported"))
    }

    private fun start(
        name: String,
        sources: Map<String, String>,
    ): JsonPath = importing(name, sources).then().statusCode(202).extract().jsonPath()

    private fun importing(
        name: String,
        sources: Map<String, String>,
    ) = ApiTest
        .requestJson()
        .body(
            """{"kind":"workload","name":"$name","sources":{${sources.entries.joinToString(
                ",",
            ) { (table, url) -> "\"$table\":\"$url\"" }}}}""",
        ).post("/api/v1/traces/imports")

    /** How the import ended, read from the caller's list as the frontend reads it. */
    private fun settle(id: String): Settled {
        val until = Instant.now().plus(SETTLE_TIMEOUT)
        while (Instant.now() < until) {
            val list = ApiTest.requestJson().get("/api/v1/traces/imports").then().statusCode(200).extract().jsonPath()
            val index = list.getList<String>("id").indexOf(id)
            val type = list.getString("[$index].progress.type")
            if (type != "running") {
                return Settled(type, list.getString("[$index].progress.reason").orEmpty())
            }
            Thread.sleep(POLL_MILLIS)
        }
        throw AssertionError("import $id was still running after $SETTLE_TIMEOUT")
    }

    private data class Settled(
        val type: String,
        val reason: String,
    )

    private fun url(name: String) = "http://127.0.0.1:${files.address.port}/$name.parquet"

    private fun unique(prefix: String) = "$prefix-${UUID.randomUUID().toString().take(8)}"

    private companion object {
        const val POLL_MILLIS = 100L
    }
}
