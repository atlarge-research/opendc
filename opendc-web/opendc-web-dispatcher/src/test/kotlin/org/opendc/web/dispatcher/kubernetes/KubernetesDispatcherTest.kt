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

package org.opendc.web.dispatcher.kubernetes

import io.fabric8.kubernetes.api.model.StatusBuilder
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.server.mock.KubernetesMixedDispatcher
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer
import io.fabric8.mockwebserver.Context
import io.fabric8.mockwebserver.MockWebServer
import io.fabric8.mockwebserver.ServerRequest
import io.fabric8.mockwebserver.ServerResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.web.dispatcher.ExecutionSlot
import org.opendc.web.dispatcher.ExitOutcome
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.Grant
import org.opendc.web.dispatcher.Launch
import org.opendc.web.dispatcher.LaunchRequest
import org.opendc.web.dispatcher.MemoryCap
import org.opendc.web.dispatcher.PlatformEvent
import org.opendc.web.dispatcher.PlatformSpan
import org.opendc.web.dispatcher.PlatformVerdict
import org.opendc.web.dispatcher.TimeCap
import org.opendc.web.dispatcher.jobName
import org.opendc.web.launcher.PeakMemory
import java.time.Duration
import java.time.Instant
import java.util.Queue
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A cluster run is only as good as what the dispatcher asks for and how it reads what comes back.
 * These run against fabric8's in-process API server, with [JobController] playing the Job
 * controller and the kubelet.
 */
class KubernetesDispatcherTest {
    private lateinit var server: KubernetesMockServer
    private lateinit var admin: KubernetesClient
    private lateinit var cluster: JobController
    private val dispatchers = mutableListOf<KubernetesDispatcher>()
    private val events = LinkedBlockingQueue<PlatformEvent>()

    @BeforeEach
    fun start() {
        val responses = HashMap<ServerRequest, Queue<ServerResponse>>()
        server = KubernetesMockServer(Context(), MockWebServer(), responses, KubernetesMixedDispatcher(responses), false)
        server.init()
        admin = server.createClient()
        cluster = JobController(admin, NAMESPACE)
    }

    @AfterEach
    fun stop() {
        dispatchers.forEach { it.close() }
        admin.close()
        server.destroy()
    }

    private fun config(
        pendingTimeout: Duration = Duration.ofHours(1),
        maxConcurrent: Int = 4,
        priorityClass: PriorityClass = PriorityClass.ClusterDefault,
    ) = KubernetesDispatcherConfig(
        namespace = Namespace.Named(NAMESPACE),
        image = "ghcr.io/atlarge-research/opendc-launcher:test",
        pullPolicy = PullPolicy.IF_NOT_PRESENT,
        serviceAccount = "default",
        priorityClass = priorityClass,
        slot = ExecutionSlot(4, 8192.0, TimeCap.Unlimited),
        maxConcurrent = maxConcurrent,
        scratchMb = 2048,
        ttl = Duration.ofDays(7),
        pendingTimeout = pendingTimeout,
    )

    private fun dispatcher(
        config: KubernetesDispatcherConfig = config(),
        listener: (PlatformEvent) -> Unit = { events.add(it) },
        observing: Boolean = true,
    ): KubernetesDispatcher =
        KubernetesDispatcher(server.createClient(), config).also {
            dispatchers += it
            if (observing) it.observe(listener)
        }

    private fun request(id: UUID = UUID.randomUUID()) =
        LaunchRequest(
            id,
            "https://store.example/manifest.json?X-Amz-Signature=s",
            Grant(2, 1536, 2048, 600),
        )

    private fun job(id: UUID): Job? = admin.batch().v1().jobs().inNamespace(NAMESPACE).withName(jobName(id)).get()

    private fun next(): PlatformEvent = events.poll(30, TimeUnit.SECONDS) ?: error("nothing arrived")

    private fun finished(): ExitOutcome {
        while (true) {
            when (val event = next()) {
                is PlatformEvent.Started -> continue
                is PlatformEvent.Finished -> return event.outcome
            }
        }
    }

    @Test
    fun `asks the cluster for exactly the grant, once, and locked down`() {
        val dispatcher = dispatcher(config(priorityClass = PriorityClass.Named("opendc-batch")))
        val request = request()

        assertEquals(Launch.Accepted, dispatcher.launch(request))
        assertEquals(Launch.Accepted, dispatcher.launch(request), "launching what the cluster holds is accepted again")

        val job = checkNotNull(job(request.executionId))
        assertEquals(1, admin.batch().v1().jobs().inNamespace(NAMESPACE).list().items.size, "one Job however often it was launched")
        assertEquals(0, job.spec.backoffLimit, "the server retries, never the Job controller")
        val pod = job.spec.template.spec
        assertEquals("Never", pod.restartPolicy)
        assertEquals(600L, pod.activeDeadlineSeconds)
        assertEquals(false, pod.automountServiceAccountToken)
        assertEquals(true, pod.securityContext.runAsNonRoot)
        assertEquals("opendc-batch", pod.priorityClassName)
        val container = pod.containers.single()
        assertEquals(container.resources.requests["cpu"], container.resources.limits["cpu"])
        assertEquals(container.resources.requests["memory"], container.resources.limits["memory"])
        assertEquals("2048Mi", container.resources.limits["memory"].toString())
        val env = container.env.associate { it.name to it.value }
        assertEquals(request.manifestUrl, env.getValue("MANIFEST_URL"))
        assertTrue("-Xmx1536m" in env.getValue("OPENDC_LAUNCHER_OPTS"))
        assertEquals("/tmp/peak-memory.json", container.terminationMessagePath)
        assertEquals(true, container.securityContext.readOnlyRootFilesystem)
    }

    @Test
    fun `tells a refusal it will never get past from trouble that may pass`() {
        val dispatcher = dispatcher()
        // A quota refusal is a 403, which passes once the quota frees up. The client retries a 5xx by
        // itself, so a server error never reaches the dispatcher as a single answer.
        server.expect().post().withPath(
            "/apis/batch/v1/namespaces/$NAMESPACE/jobs",
        ).andReturn(422, status(422, "Job.batch is invalid")).once()
        server.expect().post().withPath("/apis/batch/v1/namespaces/$NAMESPACE/jobs").andReturn(403, status(403, "exceeded quota")).once()

        val refused = dispatcher.launch(request())
        val unavailable = dispatcher.launch(request())

        assertTrue(refused is Launch.Rejected, "an invalid Job is refused for good: $refused")
        assertTrue(unavailable is Launch.Unavailable, "a quota refusal may pass: $unavailable")
    }

    @Test
    fun `reports a run from start to end with its span and memory, and removes its Job`() {
        val dispatcher = dispatcher()
        val request = request()
        dispatcher.launch(request)
        val begun = Instant.parse("2026-10-04T10:00:00Z")

        cluster.running(request.executionId, begun)
        assertEquals(PlatformEvent.Started(request.executionId, begun), next())
        cluster.terminated(request.executionId, 0, "Completed", report(), begun, begun.plusSeconds(60))
        val outcome = finished()

        assertEquals(ExitReason.OK, outcome.reason)
        assertEquals(PlatformSpan.Ran(begun, begun.plusSeconds(60)), outcome.span)
        assertEquals(PeakMemory.Measured(900.0, 600.0), outcome.peakMemory)
        awaitGone(request.executionId)
    }

    @Test
    fun `reads the kernel's memory kill and an overrun as what they are`() {
        val dispatcher = dispatcher()
        val oom = request()
        val late = request()
        dispatcher.launch(oom)
        dispatcher.launch(late)
        val now = Instant.now()

        cluster.terminated(oom.executionId, 137, "OOMKilled", "", now, now)
        cluster.terminated(late.executionId, 143, "Error", "", now, now, podReason = "DeadlineExceeded")

        val reasons = setOf(finished().reason, finished().reason)
        assertEquals(setOf(ExitReason.OOM, ExitReason.TIMEOUT), reasons)
    }

    // The server not taking an outcome is not the run's fault, and the Job is all that still knows
    // how the run ended, so it stays until the outcome is taken.
    @Test
    fun `keeps a Job whose outcome the server refused, and delivers it again`() {
        val refuse = AtomicBoolean(true)
        val dispatcher =
            dispatcher(listener = { event ->
                if (event is PlatformEvent.Finished && refuse.getAndSet(false)) error("the database is down")
                events.add(event)
            })
        val request = request()
        dispatcher.launch(request)
        val now = Instant.now()

        cluster.terminated(request.executionId, 0, "Completed", "", now, now)
        Thread.sleep(500)
        assertTrue(job(request.executionId) != null, "the Job is all that still knows how it ended")
        cluster.terminated(request.executionId, 0, "Completed", "", now, now.plusSeconds(1))

        assertEquals(ExitReason.OK, finished().reason)
        awaitGone(request.executionId)
    }

    // The verdict is written on the Job, so a replica that did not cancel reads it just the same.
    @Test
    fun `reads a cancel back from the cluster, whichever replica sees the end`() {
        val canceller = dispatcher(listener = {})
        val observer = dispatcher()
        val request = request()
        canceller.launch(request)
        cluster.running(request.executionId, Instant.now())

        canceller.cancel(request.executionId)
        cluster.removePod(request.executionId)

        assertEquals(ExitReason.CANCELLED, finished().reason)
    }

    @Test
    fun `withdraws a run that never got a node once its time to start is up`() {
        val dispatcher = dispatcher(config(pendingTimeout = Duration.ZERO))
        val request = request()
        // The pod is there before its Job is first seen, so the timeout reads the scheduler's reason.
        cluster.pending(request.executionId, "0/3 nodes are available: insufficient memory")

        dispatcher.launch(request)

        val outcome = finished()
        assertEquals(ExitReason.UNKNOWN, outcome.reason)
        assertTrue("insufficient memory" in outcome.message, outcome.message)
    }

    // No amount of waiting places a pod larger than every node, so it is refused while the reader can
    // still be told why, not after an hour of pending and a retry that pends again.
    @Test
    fun `refuses at launch a pod larger than every node the cluster shows`() {
        val dispatcher = dispatcher()
        cluster.node("worker-1", cpu = "2", memory = "4Gi")
        cluster.node("worker-2", cpu = "2", memory = "4Gi")
        awaitMemoryCap(dispatcher, MemoryCap.Limited(4096.0))
        val request = LaunchRequest(UUID.randomUUID(), "https://store.example/manifest.json", Grant(1, 7680, 8192, 600))

        val launch = dispatcher.launch(request)

        assertTrue(launch is Launch.Rejected && "8192 MB" in launch.message && "4096 MB" in launch.message, "$launch")
        assertNull(job(request.executionId), "nothing is left pending on the cluster")
    }

    @Test
    fun `withdraws a pending pod once every node the cluster shows is too small for it`() {
        val dispatcher = dispatcher()
        val request = request()
        assertEquals(Launch.Accepted, dispatcher.launch(request), "with no node visible, nothing is ruled out")
        cluster.node("small", cpu = "1", memory = "1Gi")
        awaitMemoryCap(dispatcher, MemoryCap.Limited(1024.0))

        cluster.pending(request.executionId, "0/1 nodes are available: 1 Insufficient cpu, 1 Insufficient memory.")

        val outcome = finished()
        assertEquals(ExitReason.REJECTED, outcome.reason)
        assertTrue("no node can hold it" in outcome.message, outcome.message)
    }

    // A slot configured for larger nodes than the cluster has would pack bags that no node can hold.
    @Test
    fun `packs executions no larger than the node with the most memory`() {
        val dispatcher = dispatcher()
        cluster.node("worker", cpu = "2", memory = "4Gi")

        awaitMemoryCap(dispatcher, MemoryCap.Limited(4096.0))

        assertEquals(ExecutionSlot(2, 4096.0, TimeCap.Unlimited, MemoryCap.Limited(4096.0)), dispatcher.slot())
    }

    // An operator's taint keeps this server's pods off a node for good; a cordon only for now.
    @Test
    fun `gives one execution at most the memory of the largest node its pods could ever be placed on`() {
        val dispatcher = dispatcher()
        cluster.node("control-plane", cpu = "8", memory = "64Gi", taints = listOf("NoSchedule"))
        cluster.node("cordoned", cpu = "8", memory = "32Gi", cordoned = true)
        cluster.node("worker", cpu = "4", memory = "16Gi", taints = listOf("PreferNoSchedule"))

        awaitMemoryCap(dispatcher, MemoryCap.Limited(32_768.0))
    }

    // Draining a node for an upgrade must not fail, for good, the work only that node can hold.
    @Test
    fun `keeps launching work that only a node cordoned for now can hold`() {
        val dispatcher = dispatcher()
        cluster.node("worker", cpu = "2", memory = "4Gi")
        cluster.node("large", cpu = "8", memory = "16Gi", cordoned = true)
        awaitMemoryCap(dispatcher, MemoryCap.Limited(16_384.0))
        val request = LaunchRequest(UUID.randomUUID(), "https://store.example/manifest.json", Grant(1, 7680, 8192, 600))

        assertEquals(Launch.Accepted, dispatcher.launch(request))
    }

    @Test
    fun `leaves a pending run alone while it still has time to start`() {
        val dispatcher = dispatcher()
        val request = request()
        dispatcher.launch(request)

        cluster.pending(request.executionId, "waiting for a node")

        assertNull(events.poll(1, TimeUnit.SECONDS))
        assertTrue(job(request.executionId) != null)
    }

    @Test
    fun `refuses at once an image that can never be pulled`() {
        val dispatcher = dispatcher()
        val request = request()
        dispatcher.launch(request)

        cluster.waiting(request.executionId, "InvalidImageName", "couldn't parse image reference")

        assertEquals(ExitReason.REJECTED, finished().reason)
    }

    @Test
    fun `answers a restarted server from the cluster as it stands`() {
        val launcher = dispatcher(listener = {})
        val waiting = request()
        val running = request()
        val ended = request()
        listOf(waiting, running, ended).forEach { launcher.launch(it) }
        val begun = Instant.parse("2026-10-04T10:00:00Z")
        cluster.running(running.executionId, begun)
        cluster.terminated(ended.executionId, 0, "Completed", "", begun, begun.plusSeconds(5))
        val absent = UUID.randomUUID()

        val verdicts = dispatcher(observing = false).reconcile(listOf(waiting.executionId, running.executionId, ended.executionId, absent))

        assertEquals(PlatformVerdict.Waiting, verdicts[waiting.executionId])
        assertEquals(PlatformVerdict.Running(begun), verdicts[running.executionId])
        assertTrue(verdicts[ended.executionId] is PlatformVerdict.Ended)
        assertEquals(PlatformVerdict.Unknown, verdicts[absent])
    }

    @Test
    fun `admits no more than its ceiling of Jobs in flight, and room again once one ends`() {
        val dispatcher = dispatcher(config(maxConcurrent = 1))
        val request = request()

        dispatcher.launch(request)
        assertFalse(dispatcher.admits(1, 1.0))
        cluster.terminated(request.executionId, 0, "Completed", "", Instant.now(), Instant.now())
        finished()

        assertTrue(dispatcher.admits(1, 1.0))
    }

    @Test
    fun `delivers nothing before anyone is listening`() {
        val dispatcher = dispatcher(observing = false)
        assertFalse(dispatcher.admits(1, 1.0), "nothing is admitted with nobody to hear how it ends")

        dispatcher.launch(request())

        assertNull(events.poll(500, TimeUnit.MILLISECONDS))
    }

    private fun awaitMemoryCap(
        dispatcher: KubernetesDispatcher,
        cap: MemoryCap,
    ) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (dispatcher.slot().memoryCap != cap && System.nanoTime() < deadline) {
            Thread.sleep(50)
        }
        assertEquals(cap, dispatcher.slot().memoryCap)
    }

    private fun awaitGone(id: UUID) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (job(id) != null) {
            check(System.nanoTime() < deadline) { "the Job of $id was never removed" }
            Thread.sleep(50)
        }
    }

    private fun report(): String = SdkJson.json.encodeToString(PeakMemory.serializer(), PeakMemory.Measured(900.0, 600.0))

    private fun status(
        code: Int,
        message: String,
    ) = StatusBuilder().withCode(code).withMessage(message).build()

    private companion object {
        const val NAMESPACE = "test"
    }
}
