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

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.opendc.sdk.model.dsl.ghz
import org.opendc.sdk.model.dsl.gib
import org.opendc.sdk.model.dsl.mhz
import org.opendc.sdk.model.dsl.mib
import org.opendc.sdk.model.dsl.ms
import org.opendc.sdk.model.scenario.ScenarioSpec
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicySpec
import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.sdk.model.telemetry.OutputFileSpec
import org.opendc.sdk.model.topology.ClusterSpec
import org.opendc.sdk.model.topology.CpuSpec
import org.opendc.sdk.model.topology.DataCenterSpec
import org.opendc.sdk.model.topology.HostSpec
import org.opendc.sdk.model.topology.MemorySpec
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.workload.InlineWorkloadSpec
import org.opendc.sdk.model.workload.TaskFragmentSpec
import org.opendc.sdk.model.workload.TaskSpec
import java.net.InetSocketAddress
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * A launcher on a cluster reaches its inputs and outputs only through signed URLs, which carry their
 * signature in the query string. What is worth pinning is that it fetches and publishes through
 * exactly those URLs, keeps them out of where files land, and fails in a way the server can tell
 * apart: a unit's failure is the unit's, an outcome it could not publish is the process's.
 */
class LaunchTest {
    @TempDir
    lateinit var reportDir: Path

    private lateinit var server: HttpServer

    /** What was PUT, by path, with the query string it arrived under. */
    private val received = ConcurrentHashMap<String, Received>()

    /** Paths the store answers with a refusal. */
    private val refused = ConcurrentHashMap.newKeySet<String>()

    private val served = ConcurrentHashMap<String, ByteArray>()

    @BeforeEach
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val path = exchange.requestURI.path
            when {
                path in refused -> exchange.sendResponseHeaders(403, -1)
                exchange.requestMethod == "PUT" -> {
                    received[path] = Received(exchange.requestURI.rawQuery.orEmpty(), exchange.requestBody.readBytes())
                    exchange.sendResponseHeaders(200, -1)
                }
                else -> {
                    val body = served[path]
                    if (body == null) {
                        exchange.sendResponseHeaders(404, -1)
                    } else {
                        exchange.sendResponseHeaders(200, body.size.toLong())
                        exchange.responseBody.use { it.write(body) }
                    }
                }
            }
            exchange.close()
        }
        server.start()
    }

    @AfterEach
    fun stop() {
        server.stop(0)
    }

    @Test
    fun `stages and publishes through signed URLs, and certifies the unit last`() {
        served["/inputs/table.parquet"] = "bytes".encodeToByteArray()
        val manifest =
            LaunchManifest(
                inputs = listOf(StagedInput("inputs/0/table.parquet", url("/inputs/table.parquet"), "traces/x/table.parquet")),
                units = listOf(unit(id = 0, outcome = url("/outcome/0"))),
                parallelism = 1,
            )

        val code = launch(serve(manifest), reportDir)

        assertEquals(EXIT_OK, code)
        val host = received.getValue("/out/0/${OutputFileSpec.HOST.fileName}")
        assertEquals(SIGNATURE, host.query, "the signature travels with the request")
        assertTrue(host.body.isNotEmpty())
        val outcome = SdkJson.json.decodeFromString(UnitOutcome.serializer(), received.getValue("/outcome/0").body.decodeToString())
        assertTrue(outcome is UnitOutcome.Succeeded, "the unit was certified: $outcome")
        assertTrue(reportDir.resolve(PEAK_MEMORY_FILE).toFile().exists(), "the memory report is left for the platform")
    }

    // A manifest is written by the server, but it is still input: a path escaping the working
    // directory would let a launcher write anywhere its user can.
    @Test
    fun `refuses to stage anything outside its working directory`() {
        served["/inputs/table.parquet"] = "bytes".encodeToByteArray()
        val manifest =
            LaunchManifest(
                inputs = listOf(StagedInput("../escaped.parquet", url("/inputs/table.parquet"), "traces/x/table.parquet")),
                units = listOf(unit(id = 0, outcome = url("/outcome/0"))),
                parallelism = 1,
            )

        assertEquals(EXIT_INVALID_SPEC, launch(serve(manifest), reportDir))
        assertTrue(received.isEmpty(), "nothing ran")
    }

    @Test
    fun `fails the process when a unit's outcome cannot be published`() {
        refused += "/outcome/0"
        val manifest = LaunchManifest(inputs = emptyList(), units = listOf(unit(id = 0, outcome = url("/outcome/0"))), parallelism = 1)

        assertEquals(EXIT_TRANSFER_FAILED, launch(serve(manifest), reportDir))
    }

    @Test
    fun `fails a unit whose files cannot be published without failing the process`() {
        refused += "/out/0/${OutputFileSpec.HOST.fileName}"
        val manifest = LaunchManifest(inputs = emptyList(), units = listOf(unit(id = 0, outcome = url("/outcome/0"))), parallelism = 1)

        val code = launch(serve(manifest), reportDir)

        assertEquals(EXIT_UNITS_FAILED, code)
        val outcome = SdkJson.json.decodeFromString(UnitOutcome.serializer(), received.getValue("/outcome/0").body.decodeToString())
        assertTrue(
            outcome is UnitOutcome.Failed && outcome.failure == UnitFailure.TRANSFER,
            "an output that did not land is a transfer failure: $outcome",
        )
        assertTrue(SIGNATURE !in (outcome as UnitOutcome.Failed).message, "a signature never reaches a marker")
    }

    @Test
    fun `reports a manifest nobody can find as one it could not fetch`() {
        assertEquals(EXIT_TRANSFER_FAILED, launch(url("/nowhere.json"), reportDir))
    }

    private fun serve(manifest: LaunchManifest): String {
        served["/manifest.json"] = SdkJson.json.encodeToString(LaunchManifest.serializer(), manifest).encodeToByteArray()
        return url("/manifest.json")
    }

    private fun url(path: String): String = "http://127.0.0.1:${server.address.port}$path?$SIGNATURE"

    private fun unit(
        id: Int,
        outcome: String,
    ): LaunchUnit {
        val scenario =
            ScenarioSpec(
                topology =
                    TopologySpec(
                        listOf(
                            DataCenterSpec(
                                clusters =
                                    listOf(
                                        ClusterSpec(hosts = listOf(HOST)),
                                    ),
                            ),
                        ),
                    ),
                workload = InlineWorkloadSpec(listOf(TASK)),
                allocationPolicy = PrefabAllocationPolicySpec(),
                id = id,
            )
        return LaunchUnit(
            scenario = scenario,
            outputs = scenario.exportModel.filesToExport.map { OutputTarget(it.fileName, url("/out/$id/${it.fileName}")) },
            outcome = outcome,
        )
    }

    private class Received(
        val query: String,
        val body: ByteArray,
    )

    private companion object {
        const val SIGNATURE = "X-Amz-Signature=secret"

        val HOST = HostSpec(cpu = CpuSpec(coreCount = 2, coreSpeed = 3.ghz), memory = MemorySpec(size = 4.gib))

        val TASK =
            TaskSpec(
                id = 0,
                submissionTime = 0.ms,
                duration = 60_000.ms,
                cpuCoreCount = 1,
                cpuCapacity = 1000.mhz,
                memory = 0.mib,
                fragments = listOf(TaskFragmentSpec(duration = 60_000.ms, cpuUsage = 1000.mhz)),
            )
    }
}
