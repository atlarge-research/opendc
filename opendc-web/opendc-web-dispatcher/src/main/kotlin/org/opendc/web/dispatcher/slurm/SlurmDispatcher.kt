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

import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.web.dispatcher.CapacitySnapshot
import org.opendc.web.dispatcher.Dispatcher
import org.opendc.web.dispatcher.ExecutionSlot
import org.opendc.web.dispatcher.ExitOutcome
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.Launch
import org.opendc.web.dispatcher.LaunchRequest
import org.opendc.web.dispatcher.NO_EXIT_CODE
import org.opendc.web.dispatcher.PlatformEvent
import org.opendc.web.dispatcher.PlatformSpan
import org.opendc.web.dispatcher.PlatformVerdict
import org.opendc.web.dispatcher.TimeCap
import org.opendc.web.launcher.LaunchManifest
import org.opendc.web.launcher.PeakMemory
import org.opendc.web.launcher.fetch
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** The oldest Java the launcher runs on. */
private const val MINIMUM_JAVA = 21

/** How many executions are staged or collected at once. */
private const val TRANSFER_THREADS = 2

private val SWEEP_PERIOD = Duration.ofHours(1)

/**
 * Runs each execution as one batch job on a SLURM cluster whose compute nodes cannot reach this server.
 *
 * Launching only records the execution. A transfer pool then uploads the launcher once per version,
 * caches every input on the shared filesystem and rewrites the manifest to use them, and submits the
 * job with a wrapper that records how it ended, since the cluster keeps no accounting to ask. Once
 * the job leaves the queue, every certified unit is copied back to the server's signed targets.
 *
 * One control thread owns every execution's state, polls the queue and delivers events. It assumes it
 * is the only dispatcher using its remote root.
 */
class SlurmDispatcher(private val config: SlurmDispatcherConfig) : Dispatcher {
    private val ssh = SshConnection(config.ssh)
    private val distribution = LauncherDistribution(config.launcherLib)
    private val control = Executors.newSingleThreadScheduledExecutor { Thread(it, "opendc-slurm-control").apply { isDaemon = true } }
    private val transfers = Executors.newFixedThreadPool(TRANSFER_THREADS) { Thread(it, "opendc-slurm-transfer").apply { isDaemon = true } }
    private val listener = AtomicReference<(PlatformEvent) -> Unit>()
    private val tracked = ConcurrentHashMap<UUID, Tracked>()

    @Volatile
    private var view: ClusterView = ClusterView.Unreachable("not connected yet", CapacitySnapshot(0, 0.0, 0, 0.0))

    // Control thread only.
    private var reconciled = false
    private var lastSweep = Instant.EPOCH

    override val name: String get() = NAME

    override fun slot(): ExecutionSlot {
        val partitionLimit =
            when (val current = view) {
                is ClusterView.Reachable -> current.partition.timeLimit
                is ClusterView.Unreachable -> TimeCap.Unlimited
            }
        return config.slot.copy(timeCap = tighter(config.slot.timeCap, partitionLimit))
    }

    override fun admits(
        cores: Int,
        memoryMb: Double,
    ): Boolean =
        view is ClusterView.Reachable &&
            tracked.values.count { it is Tracked.Staging || it is Tracked.Unsubmitted || it is Tracked.Submitted } < config.maxJobs

    override fun capacity(): CapacitySnapshot =
        when (val current = view) {
            is ClusterView.Reachable -> current.partition.capacity
            is ClusterView.Unreachable -> current.lastCapacity
        }

    override fun observe(listener: (PlatformEvent) -> Unit) {
        check(this.listener.compareAndSet(null, listener)) { "a listener is already registered" }
        control.scheduleWithFixedDelay(::poll, 0, config.pollInterval.toMillis(), TimeUnit.MILLISECONDS)
    }

    /** Purely local: the work is recorded and staged in the background. Nothing remote runs here. */
    override fun launch(request: LaunchRequest): Launch {
        if (tracked.containsKey(request.executionId)) {
            return Launch.Accepted
        }
        when (val current = view) {
            is ClusterView.Unreachable -> return Launch.Unavailable(current.reason)
            is ClusterView.Reachable -> {}
        }
        tracked[request.executionId] = Tracked.Staging(request, Instant.now())
        transfers.execute { stage(request) }
        return Launch.Accepted
    }

    override fun cancel(executionId: UUID) {
        control.execute { guarded { cancelNow(executionId) } }
    }

    override fun reconcile(executionIds: List<UUID>): Map<UUID, PlatformVerdict> =
        control.submit<Map<UUID, PlatformVerdict>> { reconcileNow(executionIds) }.get()

    override fun close() {
        control.shutdownNow()
        transfers.shutdownNow()
        ssh.close()
    }

    /** Fetches what the job needs onto the cluster, then hands the execution back to be submitted. */
    private fun stage(request: LaunchRequest) {
        val id = request.executionId
        try {
            val remote = reachable().remote
            val launcher = remote.launcher()
            var text = ""
            fetch(request.manifestUrl) { text = it.readBytes().decodeToString() }
            val original = SdkJson.json.decodeFromString(LaunchManifest.serializer(), text)
            original.inputs.forEach(remote::stage)
            val cap = slot().timeCap
            val limit = cap.clamp(request.grant.timeLimitSeconds)
            val limits = Limits(limit, capped = cap is TimeCap.Limited && limit >= cap.seconds)
            remote.prepare(id, original.localized(remote.layout, id), original, limits)
            control.execute { guarded { staged(id, launcher) } }
        } catch (e: Exception) {
            LOG.warn("Could not stage execution {}", id, e)
            control.execute { if (tracked[id] is Tracked.Staging) end(id, outcome(ExitReason.UNKNOWN, "staging failed: ${e.message}")) }
        }
    }

    private fun staged(
        id: UUID,
        launcher: String,
    ) {
        when (val state = tracked[id]) {
            is Tracked.Staging -> submit(id, state.request, state.since, launcher)
            // Cancelled while it was being staged: what was put on the cluster for it goes.
            else -> reachable().remote.remove(id)
        }
    }

    /** Submits a staged execution, adopting a job already queued under its name rather than queueing a second. */
    private fun submit(
        id: UUID,
        request: LaunchRequest,
        since: Instant,
        launcher: String,
    ) {
        val remote = reachable().remote
        if (parseQueue(ssh.exec(squeueCommand(config.ssh.user)).stdout).containsKey(id)) {
            tracked[id] = Tracked.Submitted(remote.submittedAt(id), started = false)
            return
        }
        val cap = slot().timeCap
        val minutes =
            when (cap) {
                TimeCap.Unlimited -> slurmMinutes(request.grant.timeLimitSeconds)
                is TimeCap.Limited -> minOf(slurmMinutes(request.grant.timeLimitSeconds), cap.seconds / SECONDS_PER_MINUTE)
            }
        val script = JobScript(remote.layout.execution(id), launcher, config.java)
        val command = sbatchCommand(id, request.grant, minutes.coerceAtLeast(1), script, config.partition, config.sbatchOptions)
        when (val submission = parseSubmission(ssh.exec(command))) {
            is Submission.Queued -> {
                val now = Instant.now()
                remote.writeRecord(id, Record.SUBMITTED, listOf("job=${submission.jobId}", "at=${now.epochSecond}"))
                tracked[id] = Tracked.Submitted(now, started = false)
            }
            is Submission.Refused -> end(id, outcome(ExitReason.REJECTED, "SLURM refused it: ${submission.message}"))
            is Submission.Deferred -> tracked[id] = Tracked.Unsubmitted(request, since, submission.message, launcher)
        }
    }

    /** One pass over the queue and every execution this dispatcher tracks. */
    private fun poll() {
        guarded {
            val remote = reachable().remote
            val queue =
                try {
                    parseQueue(ssh.exec(squeueCommand(config.ssh.user)).stdout)
                } catch (e: Exception) {
                    // A queue that could not be read must never look like jobs that vanished.
                    disconnect(e)
                    return@guarded
                }
            refreshPartition(remote)
            val now = Instant.now()
            for ((id, state) in tracked.toList()) {
                when (state) {
                    is Tracked.Staging, Tracked.Collecting -> {}
                    is Tracked.Unsubmitted ->
                        if (Duration.between(state.since, now) >= config.pendingTimeout) {
                            val message = "SLURM did not accept it within ${config.pendingTimeout}: ${state.refusal}"
                            end(id, outcome(ExitReason.UNKNOWN, message))
                        } else {
                            submit(id, state.request, state.since, state.launcher)
                        }
                    is Tracked.Submitted -> watch(id, state, queue[id], now, remote)
                    is Tracked.Gone -> settleGone(id, state, now, remote)
                    is Tracked.Ending -> deliverEnding(id, state.outcome)
                }
            }
            if (reconciled && Duration.between(lastSweep, now) >= SWEEP_PERIOD) {
                sweep(remote, queue)
                lastSweep = now
            }
        }
    }

    private fun watch(
        id: UUID,
        state: Tracked.Submitted,
        job: QueuedJob?,
        now: Instant,
        remote: RemoteExecutions,
    ) {
        when {
            job == null -> tracked[id] = Tracked.Gone(now)
            job.pending && job.reason in PERMANENT_PENDING_REASONS ->
                withdraw(id, ExitReason.REJECTED, "SLURM will not start it: ${job.reason}")
            job.pending && Duration.between(state.since, now) >= config.pendingTimeout ->
                withdraw(id, ExitReason.UNKNOWN, "did not start within ${config.pendingTimeout}: ${job.reason}")
            job.pending -> {}
            !state.started -> {
                val at =
                    when (val start = remote.startedAt(id)) {
                        Start.NotStarted -> now
                        is Start.At -> start.instant
                    }
                tracked[id] = state.copy(started = true)
                deliver(PlatformEvent.Started(id, at))
            }
        }
    }

    /** Once a gone job's records are there, or have had their grace, its results are copied back. */
    private fun settleGone(
        id: UUID,
        state: Tracked.Gone,
        now: Instant,
        remote: RemoteExecutions,
    ) {
        val records = remote.records(id)
        val recorded = records.exit is WrapperExit.Recorded || records.stop is Stop.Requested
        if (!recorded && Duration.between(state.since, now) < config.recordGrace) {
            return
        }
        tracked[id] = Tracked.Collecting
        transfers.execute {
            try {
                val outcome = collected(id, remote)
                control.execute { if (tracked[id] == Tracked.Collecting) end(id, outcome) }
            } catch (e: Exception) {
                LOG.warn("Could not collect execution {}; trying again", id, e)
                control.execute { if (tracked[id] == Tracked.Collecting) tracked[id] = state }
            }
        }
    }

    /**
     * How a gone job ended, after copying back every unit that certified its outcome. A job this
     * dispatcher stopped is never copied back: it was stopped because its results are not wanted.
     */
    private fun collected(
        id: UUID,
        remote: RemoteExecutions,
    ): ExitOutcome {
        val records = remote.records(id)
        val tail = remote.logTail(id)
        if (records.stop is Stop.None) {
            remote.collect(id)
        }
        return when (val ending = ending(records, tail)) {
            is JobEnding.Ended -> ending.outcome
            is JobEnding.Vanished -> ExitOutcome(ExitReason.UNKNOWN, NO_EXIT_CODE, ending.message, ending.span, ending.peak, tail)
        }
    }

    private fun cancelNow(id: UUID) {
        when (tracked[id]) {
            is Tracked.Staging, is Tracked.Unsubmitted -> end(id, outcome(ExitReason.CANCELLED, "cancelled"))
            is Tracked.Submitted, is Tracked.Gone -> withdraw(id, ExitReason.CANCELLED, "cancelled")
            Tracked.Collecting, is Tracked.Ending, null -> {}
        }
    }

    /** Writes why the job is being stopped, then takes it out of the queue. The record decides the outcome. */
    private fun withdraw(
        id: UUID,
        reason: ExitReason,
        message: String,
    ) {
        val remote = reachable().remote
        if (!remote.exists(id)) return
        remote.writeRecord(id, Record.STOP, listOf("reason=${reason.name}", "message=${message.replace('\n', ' ')}"))
        ssh.exec(scancelCommand(config.ssh.user, id))
    }

    private fun end(
        id: UUID,
        outcome: ExitOutcome,
    ) {
        tracked[id] = Tracked.Ending(outcome)
        deliverEnding(id, outcome)
    }

    /** Hands over an ending; only once the listener takes it is the execution's directory removed. */
    private fun deliverEnding(
        id: UUID,
        outcome: ExitOutcome,
    ) {
        if (deliver(PlatformEvent.Finished(id, outcome))) {
            tracked.remove(id)
            try {
                reachable().remote.remove(id)
            } catch (e: Exception) {
                LOG.warn("Could not remove the directory of execution {}; the sweep will", id, e)
            }
        }
    }

    private fun deliver(event: PlatformEvent): Boolean {
        val listener = listener.get() ?: return false
        return try {
            listener(event)
            true
        } catch (e: Exception) {
            LOG.warn("The listener refused an event for execution {}; it is delivered again later", event.executionId, e)
            false
        }
    }

    private fun reconcileNow(ids: List<UUID>): Map<UUID, PlatformVerdict> {
        val remote = reachable().remote
        val queue = parseQueue(ssh.exec(squeueCommand(config.ssh.user)).stdout)
        val verdicts =
            ids.associateWith { id ->
                val job = queue[id]
                when {
                    job != null && job.pending -> {
                        tracked[id] = Tracked.Submitted(remote.submittedAt(id), started = false)
                        PlatformVerdict.Waiting
                    }
                    job != null -> {
                        val at =
                            when (val start = remote.startedAt(id)) {
                                Start.NotStarted -> Instant.now()
                                is Start.At -> start.instant
                            }
                        tracked[id] = Tracked.Submitted(remote.submittedAt(id), started = true)
                        PlatformVerdict.Running(at)
                    }
                    remote.exists(id) -> afterDowntime(id, remote)
                    else -> PlatformVerdict.Unknown
                }
            }
        reconciled = true
        return verdicts
    }

    /**
     * A job that ended while the server was down. Its certified units are copied back either way, so
     * work that went round again is credited for what it already did.
     */
    private fun afterDowntime(
        id: UUID,
        remote: RemoteExecutions,
    ): PlatformVerdict {
        val records = remote.records(id)
        if (records.stop is Stop.None) {
            remote.collect(id)
        }
        return when (val ending = ending(records, remote.logTail(id))) {
            is JobEnding.Ended -> {
                tracked[id] = Tracked.Ending(ending.outcome)
                PlatformVerdict.Ended(ending.outcome)
            }
            is JobEnding.Vanished -> {
                remote.remove(id)
                PlatformVerdict.Unknown
            }
        }
    }

    /** Removes what nobody tracks any more: directories, and the queued jobs that belong to them. */
    private fun sweep(
        remote: RemoteExecutions,
        queue: Map<UUID, QueuedJob>,
    ) {
        for (id in remote.executionIds().filterNot { tracked.containsKey(it) }) {
            if (queue.containsKey(id)) {
                ssh.exec(scancelCommand(config.ssh.user, id))
            }
            remote.remove(id)
        }
        remote.sweep(config.cacheRetention)
    }

    /** The cluster as last reached, connecting and checking it first when it is not. */
    private fun reachable(): ClusterView.Reachable =
        when (val current = view) {
            is ClusterView.Reachable -> current
            is ClusterView.Unreachable ->
                try {
                    connect().also { view = it }
                } catch (e: Exception) {
                    view = ClusterView.Unreachable(e.message ?: e.javaClass.name, current.lastCapacity)
                    throw e
                }
        }

    private fun connect(): ClusterView.Reachable {
        val java = ssh.exec(listOf(config.java, "-version"))
        val version = javaVersionOf(java.stderr + java.stdout)
        check(version >= MINIMUM_JAVA) { "${config.java} is Java $version; the launcher needs $MINIMUM_JAVA or newer" }
        val root =
            ssh.sftp { sftp ->
                sftp.mkdirs(config.remoteRoot)
                sftp.canonicalPath(config.remoteRoot)
            }
        val remote = RemoteExecutions(ssh, RemoteLayout(root), distribution)
        val partition = parsePartition(ssh.exec(sinfoCommand(config.partition)).stdout, config.partition)
        if (partition.smallestNodeMemoryMb > 0 && config.slot.memoryMb > partition.smallestNodeMemoryMb) {
            LOG.warn("A slot of {} MB is more than the smallest node's {} MB", config.slot.memoryMb, partition.smallestNodeMemoryMb)
        }
        return ClusterView.Reachable(remote, partition)
    }

    private fun refreshPartition(remote: RemoteExecutions) {
        try {
            view = ClusterView.Reachable(remote, parsePartition(ssh.exec(sinfoCommand(config.partition)).stdout, config.partition))
        } catch (e: Exception) {
            LOG.debug("Could not read the partition; keeping the last view", e)
        }
    }

    private fun disconnect(cause: Exception) {
        view = ClusterView.Unreachable(cause.message ?: cause.javaClass.name, capacity())
    }

    /** Runs [block] on the control thread, so one failing pass never stops the schedule. */
    private inline fun guarded(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            LOG.warn("The SLURM dispatcher could not finish a pass: {}", e.message)
            if (view is ClusterView.Reachable && e is IOException) disconnect(e)
        }
    }

    /** Where one execution stands, as far as this dispatcher is concerned. */
    private sealed interface Tracked {
        data class Staging(
            val request: LaunchRequest,
            val since: Instant,
        ) : Tracked

        data class Unsubmitted(
            val request: LaunchRequest,
            val since: Instant,
            val refusal: String,
            val launcher: String,
        ) : Tracked

        data class Submitted(
            val since: Instant,
            val started: Boolean,
        ) : Tracked

        data class Gone(val since: Instant) : Tracked

        data object Collecting : Tracked

        data class Ending(val outcome: ExitOutcome) : Tracked
    }

    /** Whether the cluster could last be reached, and what it looked like. */
    private sealed interface ClusterView {
        data class Unreachable(
            val reason: String,
            val lastCapacity: CapacitySnapshot,
        ) : ClusterView

        data class Reachable(
            val remote: RemoteExecutions,
            val partition: PartitionView,
        ) : ClusterView
    }

    companion object {
        const val NAME = "slurm"

        private val LOG = LoggerFactory.getLogger(SlurmDispatcher::class.java)

        private fun outcome(
            reason: ExitReason,
            message: String,
        ) = ExitOutcome(reason, NO_EXIT_CODE, message, PlatformSpan.NotStarted, PeakMemory.Unmeasured, "")

        private fun tighter(
            left: TimeCap,
            right: TimeCap,
        ): TimeCap =
            when {
                left is TimeCap.Limited && right is TimeCap.Limited -> TimeCap.Limited(minOf(left.seconds, right.seconds))
                left is TimeCap.Limited -> left
                else -> right
            }
    }
}
