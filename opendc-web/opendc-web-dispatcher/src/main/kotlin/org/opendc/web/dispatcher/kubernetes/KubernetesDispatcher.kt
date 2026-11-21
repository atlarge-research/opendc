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

import io.fabric8.kubernetes.api.model.DeletionPropagation
import io.fabric8.kubernetes.api.model.Node
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder
import io.fabric8.kubernetes.client.Config
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.informers.ResourceEventHandler
import io.fabric8.kubernetes.client.informers.SharedIndexInformer
import io.fabric8.kubernetes.client.jdkhttp.JdkHttpClientFactory
import org.opendc.web.dispatcher.CapacitySnapshot
import org.opendc.web.dispatcher.Dispatcher
import org.opendc.web.dispatcher.ExecutionSlot
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.Launch
import org.opendc.web.dispatcher.LaunchRequest
import org.opendc.web.dispatcher.PlatformEvent
import org.opendc.web.dispatcher.PlatformSpan
import org.opendc.web.dispatcher.PlatformVerdict
import org.opendc.web.dispatcher.jobName
import org.opendc.web.dispatcher.logTail
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** How often every known Job is looked at again, which fires pending timeouts and redelivers. */
private val SCAN_PERIOD = Duration.ofSeconds(30)

/** How often the informers list everything again, in case a watch event was lost. */
private val RESYNC_PERIOD = Duration.ofMinutes(5)

/** How many lines of a pod's log are fetched for its tail. Trimmed to bytes afterwards. */
private const val LOG_LINES = 4096

/** Where executions run when neither the config nor the client names a namespace. */
private const val DEFAULT_NAMESPACE = "default"

/**
 * Runs each execution as one Job on a Kubernetes cluster.
 *
 * Everything the cluster says arrives through informers, whose handlers only record it and hand the
 * execution to one events thread. That thread works out where the execution stands, fetches its log
 * and calls the listener, so a slow listener never stalls the client and a start is always delivered
 * before an end. A stop this server asks for is written onto the Job before the Job is deleted, so
 * the verdict is read back from the cluster by whichever replica sees the end.
 *
 * The cluster is taken to be the nodes it shows, once it has shown all of them. Executions are shaped
 * to fit the one with the most memory, and a pod larger than every node is refused, at launch or once
 * seen pending, rather than left waiting for a node that will never come.
 */
class KubernetesDispatcher internal constructor(
    private val client: KubernetesClient,
    private val config: KubernetesDispatcherConfig,
) : Dispatcher {
    private val namespace: String =
        when (val namespace = config.namespace) {
            Namespace.OfThisServer -> client.namespace ?: DEFAULT_NAMESPACE
            is Namespace.Named -> namespace.name
        }

    private val events = Executors.newSingleThreadScheduledExecutor { Thread(it, "opendc-kubernetes-events").apply { isDaemon = true } }
    private val listener = AtomicReference<(PlatformEvent) -> Unit>()
    private val informers = mutableListOf<SharedIndexInformer<*>>()

    // Written only on the events thread.
    private val jobs = mutableMapOf<UUID, SeenJob>()
    private val pods = mutableMapOf<UUID, MutableMap<String, Pod>>()
    private val nodes = mutableMapOf<String, Node>()
    private val started = mutableSetOf<UUID>()
    private val finished = mutableSetOf<UUID>()

    /** Jobs launched or seen and not yet finished. Read by [admits] from any thread. */
    private val inFlight = ConcurrentHashMap.newKeySet<UUID>()

    @Volatile
    private var capacity: CapacitySnapshot = ceiling()

    /** Started by [observe]. Its own store is read for fit, which is complete as soon as it has synced. */
    private val nodeInformer: SharedIndexInformer<Node> = client.nodes().runnableInformer(RESYNC_PERIOD.toMillis())

    override val name: String get() = NAME

    override fun slot(): ExecutionSlot = config.slot.fittedTo(nodeRooms())

    override fun admits(
        cores: Int,
        memoryMb: Double,
    ): Boolean = listener.get() != null && inFlight.size < config.maxConcurrent

    override fun capacity(): CapacitySnapshot = capacity

    override fun observe(listener: (PlatformEvent) -> Unit) {
        check(this.listener.compareAndSet(null, listener)) { "a listener is already registered" }
        informers +=
            client
                .batch()
                .v1()
                .jobs()
                .inNamespace(namespace)
                .withLabel(MANAGED_BY_LABEL, MANAGED_BY)
                .runnableInformer(RESYNC_PERIOD.toMillis())
                .apply { addEventHandler(handler(::onJob)) }
        informers +=
            client
                .pods()
                .inNamespace(namespace)
                .withLabel(MANAGED_BY_LABEL, MANAGED_BY)
                .runnableInformer(RESYNC_PERIOD.toMillis())
                .apply { addEventHandler(handler(::onPod)) }
        informers += nodeInformer.apply { addEventHandler(handler(::onNode)) }
        for (informer in informers) {
            informer.start().exceptionally { failure ->
                // A cluster that will not list nodes leaves capacity at this dispatcher's own ceiling.
                LOG.warn("An informer could not start: {}", failure.message)
                null
            }
        }
        events.scheduleWithFixedDelay({
            jobs.keys.toList().forEach(::process)
        }, SCAN_PERIOD.toMillis(), SCAN_PERIOD.toMillis(), TimeUnit.MILLISECONDS)
    }

    override fun launch(request: LaunchRequest): Launch {
        val job = executionJob(request, config)
        val asked = requestOf(job)
        val nodes = nodeRooms()
        if (fitsNoNode(asked, nodes)) {
            return Launch.Rejected(tooLargeForNodes(asked, nodes))
        }
        return try {
            jobs().resource(job).create()
            inFlight += request.executionId
            Launch.Accepted
        } catch (e: KubernetesClientException) {
            when (e.code) {
                CONFLICT -> {
                    inFlight += request.executionId
                    Launch.Accepted
                }
                BAD_REQUEST, UNPROCESSABLE -> Launch.Rejected(e.status?.message ?: e.message.orEmpty())
                else -> Launch.Unavailable(e.status?.message ?: e.message.orEmpty())
            }
        }
    }

    override fun cancel(executionId: UUID) {
        stop(executionId, ExitReason.CANCELLED, "cancelled")
    }

    /**
     * Answered from a direct listing, which is authoritative before the informers have synced. A live
     * Job is never removed here: with several replicas, another one may just have launched it.
     */
    override fun reconcile(executionIds: List<UUID>): Map<UUID, PlatformVerdict> {
        val listed = jobs().withLabel(MANAGED_BY_LABEL, MANAGED_BY).list().items.associateBy { it.executionId() }
        val podsOf = client.pods().inNamespace(namespace).withLabel(MANAGED_BY_LABEL, MANAGED_BY).list().items.groupBy { it.executionId() }
        return executionIds.associateWith { id ->
            val job = listed[id] ?: return@associateWith PlatformVerdict.Unknown
            val jobPods = podsOf[id].orEmpty()
            when (val state = jobState(job, jobPods, gone = false)) {
                is JobState.Pending, is JobState.Refused -> PlatformVerdict.Waiting.also { inFlight += id }
                is JobState.Running -> PlatformVerdict.Running(state.startedAt).also { inFlight += id }
                is JobState.Ended -> PlatformVerdict.Ended(state.outcome.copy(logTail = logOf(jobPods)))
            }
        }
    }

    override fun close() {
        informers.forEach { it.stop() }
        events.shutdownNow()
        client.close()
    }

    private fun jobs() = client.batch().v1().jobs().inNamespace(namespace)

    private fun <T> handler(record: (T, Boolean) -> Unit): ResourceEventHandler<T> =
        object : ResourceEventHandler<T> {
            override fun onAdd(obj: T) = events.execute { record(obj, false) }

            override fun onUpdate(
                oldObj: T,
                newObj: T,
            ) = events.execute { record(newObj, false) }

            override fun onDelete(
                obj: T,
                deletedFinalStateUnknown: Boolean,
            ) = events.execute { record(obj, true) }
        }

    private fun onJob(
        job: Job,
        deleted: Boolean,
    ) {
        val id = job.executionId()
        jobs[id] = SeenJob(job, gone = deleted)
        if (!deleted && job.status?.completionTime == null) {
            if (id !in finished) inFlight += id
        }
        process(id)
        if (deleted) {
            forget(id)
        }
    }

    private fun onPod(
        pod: Pod,
        deleted: Boolean,
    ) {
        val id = pod.executionId()
        val byName = pods.getOrPut(id) { mutableMapOf() }
        if (deleted) byName.remove(pod.metadata.name) else byName[pod.metadata.name] = pod
        refreshCapacity()
        process(id)
    }

    private fun onNode(
        node: Node,
        deleted: Boolean,
    ) {
        if (deleted) nodes.remove(node.metadata.name) else nodes[node.metadata.name] = node
        refreshCapacity()
    }

    /** What each node could give one pod; nothing, so nothing is ruled out, until the node list is read in full. */
    private fun nodeRooms(): List<PodResources> = if (nodeInformer.hasSynced()) allocatableOf(nodeInformer.store.list()) else emptyList()

    /** Works out where [id] stands and tells the listener anything new. Runs on the events thread only. */
    private fun process(id: UUID) {
        val seen = jobs[id] ?: return
        val listener = listener.get() ?: return
        val jobPods = pods[id].orEmpty().values.toList()
        when (val state = jobState(seen.job, jobPods, seen.gone)) {
            is JobState.Pending -> {
                val asked = requestOf(seen.job)
                val nodes = nodeRooms()
                if (fitsNoNode(asked, nodes)) {
                    stop(id, ExitReason.REJECTED, tooLargeForNodes(asked, nodes))
                } else if (Duration.between(state.since, Instant.now()) >= config.pendingTimeout) {
                    stop(id, ExitReason.UNKNOWN, "did not start within ${config.pendingTimeout}: ${state.cause}")
                }
            }
            is JobState.Refused -> stop(id, ExitReason.REJECTED, state.message)
            is JobState.Running ->
                if (started.add(id)) {
                    deliver(listener, PlatformEvent.Started(id, state.startedAt))
                }
            is JobState.Ended ->
                if (id !in finished) {
                    if (started.add(id)) {
                        deliver(listener, PlatformEvent.Started(id, startOf(state)))
                    }
                    // A listener that throws leaves the Job in place, and the next pass delivers again.
                    if (deliver(listener, PlatformEvent.Finished(id, state.outcome.copy(logTail = logOf(jobPods))))) {
                        finished += id
                        inFlight -= id
                        if (!seen.gone) {
                            deleteJob(id)
                        }
                    }
                }
        }
    }

    private fun startOf(state: JobState.Ended): Instant =
        when (val span = state.outcome.span) {
            is PlatformSpan.Ran -> span.startedAt
            PlatformSpan.NotStarted -> Instant.now()
        }

    private fun deliver(
        listener: (PlatformEvent) -> Unit,
        event: PlatformEvent,
    ): Boolean =
        try {
            listener(event)
            true
        } catch (e: Exception) {
            LOG.warn("The listener refused an event for execution {}; it is delivered again later", event.executionId, e)
            false
        }

    /** Writes why the Job is being stopped onto it, then deletes it. A Job that is already gone was never held. */
    private fun stop(
        id: UUID,
        reason: ExitReason,
        message: String,
    ) {
        try {
            jobs().withName(jobName(id)).edit { job ->
                JobBuilder(job)
                    .editMetadata()
                    .addToAnnotations(STOP_REASON, reason.name)
                    .addToAnnotations(STOP_MESSAGE, message)
                    .endMetadata()
                    .build()
            }
            deleteJob(id)
        } catch (e: KubernetesClientException) {
            if (e.code != NOT_FOUND) {
                throw e
            }
        }
    }

    private fun deleteJob(id: UUID) {
        try {
            jobs().withName(jobName(id)).withPropagationPolicy(DeletionPropagation.BACKGROUND).delete()
        } catch (e: KubernetesClientException) {
            LOG.warn("Could not delete the Job of execution {}: {}", id, e.message)
        }
    }

    /** The end of what the execution's pod printed, while the pod is still there to ask. */
    private fun logOf(jobPods: List<Pod>): String {
        val pod = jobPods.maxByOrNull { it.metadata.creationTimestamp.orEmpty() } ?: return ""
        return try {
            logTail(
                client
                    .pods()
                    .inNamespace(namespace)
                    .withName(pod.metadata.name)
                    .tailingLines(LOG_LINES)
                    .log
                    .orEmpty(),
            )
        } catch (e: KubernetesClientException) {
            ""
        }
    }

    private fun forget(id: UUID) {
        jobs.remove(id)
        pods.remove(id)
        started.remove(id)
        finished.remove(id)
    }

    private fun refreshCapacity() {
        capacity = capacityOf(nodes.values.toList(), pods.values.flatMap { it.values }, ceiling())
    }

    /** What this dispatcher may use at most, whatever the cluster shows. */
    private fun ceiling(): CapacitySnapshot =
        CapacitySnapshot(
            totalCores = config.maxConcurrent * config.slot.cores,
            totalMemoryMb = config.maxConcurrent * config.slot.memoryMb,
            allocatedCores = 0,
            allocatedMemoryMb = 0.0,
        )

    /** The Job as last seen, and whether it has since been deleted. */
    private data class SeenJob(
        val job: Job,
        val gone: Boolean,
    )

    companion object {
        const val NAME = "kubernetes"

        private const val BAD_REQUEST = 400
        private const val NOT_FOUND = 404
        private const val CONFLICT = 409
        private const val UNPROCESSABLE = 422

        private val LOG = LoggerFactory.getLogger(KubernetesDispatcher::class.java)

        /**
         * A dispatcher on the cluster the environment configures: the service account the server runs
         * under, or a kubeconfig outside a cluster.
         */
        fun connect(config: KubernetesDispatcherConfig): KubernetesDispatcher =
            KubernetesDispatcher(
                KubernetesClientBuilder().withHttpClientFactory(JdkHttpClientFactory()).withConfig(Config.autoConfigure(null)).build(),
                config,
            )
    }
}
