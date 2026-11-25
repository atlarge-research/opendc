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

package org.opendc.web.dispatcher.slurm

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.opendc.sdk.model.dsl.ghz
import org.opendc.sdk.model.dsl.gib
import org.opendc.sdk.model.resource.UriReference
import org.opendc.sdk.model.scenario.ScenarioSpec
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicySpec
import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.sdk.model.topology.ClusterSpec
import org.opendc.sdk.model.topology.CpuSpec
import org.opendc.sdk.model.topology.DataCenterSpec
import org.opendc.sdk.model.topology.HostSpec
import org.opendc.sdk.model.topology.MemorySpec
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec
import org.opendc.web.dispatcher.ExecutionSlot
import org.opendc.web.dispatcher.ExitOutcome
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.Grant
import org.opendc.web.dispatcher.Launch
import org.opendc.web.dispatcher.LaunchRequest
import org.opendc.web.dispatcher.PlatformEvent
import org.opendc.web.dispatcher.PlatformVerdict
import org.opendc.web.dispatcher.TimeCap
import org.opendc.web.launcher.LaunchManifest
import org.opendc.web.launcher.LaunchUnit
import org.opendc.web.launcher.OutputTarget
import org.opendc.web.launcher.StagedInput
import org.opendc.web.launcher.TelemetryTarget
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * A SLURM run crosses an SSH connection, a shared filesystem the compute nodes cannot see past, and a
 * queue that keeps no accounting, so what is worth pinning is what goes over, what comes back, and how
 * an ending is told. These run against [FakeSlurm], with a JDK HTTP server standing in for the store.
 */
class SlurmDispatcherTest {
    @TempDir
    lateinit var dir: Path

    private lateinit var slurm: FakeSlurm
    private lateinit var store: HttpServer
    private val dispatchers = mutableListOf<SlurmDispatcher>()
    private val events = LinkedBlockingQueue<PlatformEvent>()
    private val puts = CopyOnWriteArrayList<String>()
    private val inputFetches = AtomicInteger()
    private val userKey = keyPair()

    @BeforeEach
    fun start() {
        Files.createDirectories(dir.resolve("cluster"))
        slurm = FakeSlurm(dir.resolve("cluster"), USER, userKey, JAVA)
        store = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        store.createContext("/") { exchange ->
            val path = exchange.requestURI.path
            when {
                exchange.requestMethod == "PUT" -> {
                    exchange.requestBody.readBytes()
                    puts += path
                    exchange.sendResponseHeaders(200, -1)
                }
                path.startsWith("/manifests/") -> {
                    val body = Files.readAllBytes(dir.resolve(path.removePrefix("/")))
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.use { it.write(body) }
                }
                path == "/traces/tasks.parquet" -> {
                    inputFetches.incrementAndGet()
                    val body = "trace bytes".encodeToByteArray()
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.use { it.write(body) }
                }
                else -> exchange.sendResponseHeaders(404, -1)
            }
            exchange.close()
        }
        store.start()
    }

    @AfterEach
    fun stop() {
        dispatchers.forEach { it.close() }
        slurm.close()
        store.stop(0)
    }

    private fun dispatcher(
        maxJobs: Int = 4,
        pendingTimeout: Duration = Duration.ofHours(1),
        recordGrace: Duration = Duration.ZERO,
        timeCap: TimeCap = TimeCap.Limited(3600),
        observing: Boolean = true,
    ): SlurmDispatcher {
        val lib = Files.createDirectories(dir.resolve("launcher-${UUID.randomUUID()}/lib"))
        Files.writeString(lib.resolve("opendc-launcher.jar"), "the launcher")
        val knownHosts = Files.writeString(dir.resolve("known_hosts"), knownHostLine(slurm.hostKey, slurm.port) + "\n")
        val config =
            SlurmDispatcherConfig(
                ssh = SshTarget("127.0.0.1", slurm.port, USER, writeIdentity(userKey, dir.resolve("id")), knownHosts, JumpHost.Direct),
                remoteRoot = "opendc",
                java = JAVA,
                launcherLib = lib,
                partition = Partition.ClusterDefault,
                sbatchOptions = listOf("--account=opendc"),
                slot = ExecutionSlot(16, 56000.0, timeCap),
                maxJobs = maxJobs,
                pendingTimeout = pendingTimeout,
                pollInterval = Duration.ofMillis(100),
                recordGrace = recordGrace,
                cacheRetention = Duration.ofDays(7),
            )
        return SlurmDispatcher(config).also { dispatcher ->
            dispatchers += dispatcher
            // Observing starts the polls, the first of which connects. A dispatcher asked only to
            // reconcile connects when it is asked.
            if (observing) {
                dispatcher.observe { events.add(it) }
                awaitReachable(dispatcher)
            }
        }
    }

    /** A manifest of two units over one trace, served the way a signed URL would be. */
    private fun request(id: UUID = UUID.randomUUID()): LaunchRequest {
        val manifest =
            LaunchManifest(
                inputs = listOf(StagedInput("inputs/0/tasks.parquet", url("/traces/tasks.parquet"), "traces/trace-1/tasks.parquet")),
                units = (0..1).map { unit(id, it) },
                parallelism = 2,
                telemetry = TelemetryTarget.Endpoint("http://server.example/telemetry", "odc_exec_token"),
            )
        Files.createDirectories(dir.resolve("manifests"))
        Files.writeString(dir.resolve("manifests/$id.json"), SdkJson.json.encodeToString(LaunchManifest.serializer(), manifest))
        return LaunchRequest(id, url("/manifests/$id.json"), Grant(2, 1536, 2048, 600))
    }

    private fun unit(
        id: UUID,
        index: Int,
    ) = LaunchUnit(
        scenario = SCENARIO.copy(id = index),
        outputs = listOf(OutputTarget("host.parquet", url("/out/$id/$index/host.parquet"))),
        outcome = url("/outcome/$id/$index"),
    )

    private fun url(path: String) = "http://127.0.0.1:${store.address.port}$path?X-Amz-Signature=secret"

    private fun next(): PlatformEvent = events.poll(30, TimeUnit.SECONDS) ?: error("nothing arrived")

    private fun finished(): ExitOutcome {
        while (true) {
            when (val event = next()) {
                is PlatformEvent.Started -> continue
                is PlatformEvent.Finished -> return event.outcome
            }
        }
    }

    private fun awaitSubmitted(id: UUID) = await("$id to be queued") { slurm.queued(id) }

    @Test
    fun `stages the launcher once, caches each trace once, and submits the grant`() {
        val dispatcher = dispatcher()
        val first = request()
        val second = request()

        assertEquals(Launch.Accepted, dispatcher.launch(first))
        assertEquals(Launch.Accepted, dispatcher.launch(second))
        awaitSubmitted(first.executionId)
        awaitSubmitted(second.executionId)

        val launchers = dir.resolve("cluster/opendc/launchers").listDirectoryEntries().filterNot { it.name.startsWith(".") }
        assertEquals(1, launchers.size, "one upload of the same launcher")
        assertEquals(1, inputFetches.get(), "a cached trace is not fetched twice")
        val argv = slurm.submissions.first()
        assertTrue("--cpus-per-task=2" in argv, "$argv")
        assertTrue("--mem=2048M" in argv)
        assertTrue("--time=10" in argv, "600 s of limit is ten minutes")
        assertTrue("--no-requeue" in argv && "--account=opendc" in argv)
        assertEquals(listOf(JAVA, "1536", "org.opendc.web.launcher.MainKt"), argv.takeLast(3))
    }

    // Compute nodes reach only the shared filesystem, so the manifest the job reads points at the
    // cache and the execution's own directory, and the signed targets stay behind for collection.
    @Test
    fun `rewrites the manifest for the cluster and keeps the original for collection`() {
        val dispatcher = dispatcher()
        val request = request()

        dispatcher.launch(request)
        awaitSubmitted(request.executionId)

        val directory = slurm.directory(request.executionId)
        val localized = SdkJson.json.decodeFromString(LaunchManifest.serializer(), directory.resolve("manifest.json").readText())
        assertTrue(localized.inputs.single().source.endsWith("/opendc/cache/traces/trace-1/tasks.parquet"))
        assertTrue(localized.units[1].outcome.endsWith("/executions/${request.executionId}/units/1.json"))
        assertEquals(TelemetryTarget.None, localized.telemetry)
        assertTrue(directory.resolve("manifest.original.json").readText().contains("X-Amz-Signature"))
    }

    @Test
    fun `copies back each certified unit's files and then its marker before reporting the end`() {
        val dispatcher = dispatcher()
        val request = request()
        val id = request.executionId
        dispatcher.launch(request)
        awaitSubmitted(id)

        slurm.start(id)
        assertTrue(next() is PlatformEvent.Started)
        slurm.certify(id, 0, """{"type":"succeeded","seconds":4.0}""", mapOf("host.parquet" to "rows"))
        slurm.finish(id, exit = 23)
        val outcome = finished()

        assertEquals(ExitReason.OK, outcome.reason)
        assertEquals(listOf("/out/$id/0/host.parquet", "/outcome/$id/0"), puts.toList(), "the unit without a marker is not copied back")
        await("the execution's directory to go") { !slurm.directory(id).exists() }
    }

    @Test
    fun `stops a cancelled job and copies nothing back`() {
        val dispatcher = dispatcher()
        val request = request()
        dispatcher.launch(request)
        awaitSubmitted(request.executionId)
        slurm.start(request.executionId)
        slurm.certify(request.executionId, 0, """{"type":"succeeded","seconds":1.0}""")

        dispatcher.cancel(request.executionId)

        assertEquals(ExitReason.CANCELLED, finished().reason)
        assertTrue(request.executionId in slurm.cancelled)
        assertTrue(puts.isEmpty(), "nothing of a cancelled run goes back to the store")
    }

    @Test
    fun `fails work SLURM refuses for good, and submits again what it may accept later`() {
        val dispatcher = dispatcher()
        slurm.refuseNextSubmit("sbatch: error: Batch job submission failed: Invalid account or account/partition combination specified")
        val refused = request()
        dispatcher.launch(refused)
        assertEquals(ExitReason.REJECTED, finished().reason)

        slurm.refuseNextSubmit("sbatch: error: QOSMaxSubmitJobPerUserLimit")
        val deferred = request()
        dispatcher.launch(deferred)
        awaitSubmitted(deferred.executionId)

        val jobs = slurm.submissions.count { "--job-name=opendc-${deferred.executionId}" in it }
        assertEquals(1, jobs, "exactly one job once it is accepted")
    }

    @Test
    fun `withdraws a job SLURM will never start`() {
        val dispatcher = dispatcher()
        val request = request()
        dispatcher.launch(request)
        awaitSubmitted(request.executionId)

        slurm.pend(request.executionId, "PartitionTimeLimit")

        val outcome = finished()
        assertEquals(ExitReason.REJECTED, outcome.reason)
        assertTrue("PartitionTimeLimit" in outcome.message)
        assertTrue(request.executionId in slurm.cancelled)
    }

    // A job that vanished still has whatever its units certified, and work that goes round again is
    // credited for it.
    @Test
    fun `copies back what a vanished job certified before calling it unexplained`() {
        val dispatcher = dispatcher()
        val request = request()
        dispatcher.launch(request)
        awaitSubmitted(request.executionId)
        slurm.certify(request.executionId, 1, """{"type":"succeeded","seconds":2.0}""")

        slurm.vanish(request.executionId)

        assertEquals(ExitReason.UNKNOWN, finished().reason)
        assertTrue("/outcome/${request.executionId}/1" in puts)
    }

    @Test
    fun `answers a restarted server from the queue and the records`() {
        val launcher = dispatcher()
        val pending = request()
        val running = request()
        val ended = request()
        listOf(pending, running, ended).forEach { launcher.launch(it) }
        listOf(pending, running, ended).forEach { awaitSubmitted(it.executionId) }
        launcher.close()
        dispatchers.remove(launcher)
        slurm.start(running.executionId)
        slurm.finish(ended.executionId, exit = 0)

        val asked = listOf(pending, running, ended).map { it.executionId } + UUID.randomUUID()
        val verdicts = dispatcher(observing = false).reconcile(asked)

        assertEquals(PlatformVerdict.Waiting, verdicts[pending.executionId])
        assertTrue(verdicts[running.executionId] is PlatformVerdict.Running)
        assertTrue(verdicts[ended.executionId] is PlatformVerdict.Ended, "${verdicts[ended.executionId]}")
        assertEquals(1, verdicts.values.count { it == PlatformVerdict.Unknown })
    }

    @Test
    fun `admits no more jobs than the cluster allows this account`() {
        val dispatcher = dispatcher(maxJobs = 1)

        dispatcher.launch(request())

        assertFalse(dispatcher.admits(1, 1.0))
    }

    @Test
    fun `caps a slot's time at the tighter of its own cap and the partition's`() {
        val partitionLimited = dispatcher(timeCap = TimeCap.Limited(3600)).slot().timeCap
        assertEquals(TimeCap.Limited(900), partitionLimited, "the partition allows fifteen minutes")
        assertEquals(TimeCap.Limited(600), dispatcher(timeCap = TimeCap.Limited(600)).slot().timeCap)
    }

    // A JRE too old for the launcher would fail every job with the same error; saying so once, as an
    // unavailable platform, is what lets an operator see it.
    @Test
    fun `refuses to stage onto a cluster whose Java is too old`() {
        slurm.javaVersion = "17.0.9"
        val lib = Files.createDirectories(dir.resolve("old-java/lib"))
        Files.writeString(lib.resolve("opendc-launcher.jar"), "the launcher")
        val knownHosts = Files.writeString(dir.resolve("known_hosts"), knownHostLine(slurm.hostKey, slurm.port) + "\n")
        val dispatcher =
            SlurmDispatcher(
                SlurmDispatcherConfig(
                    ssh = SshTarget("127.0.0.1", slurm.port, USER, writeIdentity(userKey, dir.resolve("id")), knownHosts, JumpHost.Direct),
                    remoteRoot = "opendc",
                    java = JAVA,
                    launcherLib = lib,
                    partition = Partition.ClusterDefault,
                    sbatchOptions = emptyList(),
                    slot = ExecutionSlot(16, 56000.0, TimeCap.Unlimited),
                    maxJobs = 4,
                    pendingTimeout = Duration.ofHours(1),
                    pollInterval = Duration.ofMillis(100),
                    recordGrace = Duration.ZERO,
                    cacheRetention = Duration.ofDays(7),
                ),
            ).also { dispatchers += it }
        dispatcher.observe { events.add(it) }
        Thread.sleep(1000)

        val launch = dispatcher.launch(request())

        assertTrue(launch is Launch.Unavailable && "Java 17" in launch.message, "$launch")
    }

    private fun awaitReachable(dispatcher: SlurmDispatcher) {
        // The first poll connects; until then the cluster is not known to be reachable.
        await("the cluster to be reached") { dispatcher.capacity().totalCores > 0 }
    }

    private fun await(
        what: String,
        condition: () -> Boolean,
    ) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (!condition()) {
            check(System.nanoTime() < deadline) { "timed out waiting for $what" }
            Thread.sleep(50)
        }
    }

    private companion object {
        const val USER = "opendc"
        const val JAVA = "/opt/jdk-21/bin/java"

        val HOST = HostSpec(cpu = CpuSpec(coreCount = 4, coreSpeed = 3.ghz), memory = MemorySpec(size = 16.gib))

        val SCENARIO =
            ScenarioSpec(
                topology =
                    TopologySpec(
                        listOf(
                            DataCenterSpec(
                                clusters = listOf(ClusterSpec(hosts = listOf(HOST))),
                            ),
                        ),
                    ),
                workload = TraceWorkloadSpec(source = UriReference("inputs/0")),
                allocationPolicy = PrefabAllocationPolicySpec(),
            )
    }
}
