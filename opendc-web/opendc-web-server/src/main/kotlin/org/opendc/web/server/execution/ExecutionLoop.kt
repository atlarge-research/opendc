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

package org.opendc.web.server.execution

import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.runtime.StartupEvent
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.web.dispatcher.Dispatcher
import org.opendc.web.dispatcher.ExitOutcome
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.Grant
import org.opendc.web.dispatcher.Launch
import org.opendc.web.dispatcher.LaunchRequest
import org.opendc.web.dispatcher.NO_EXIT_CODE
import org.opendc.web.dispatcher.PlatformEvent
import org.opendc.web.dispatcher.PlatformSpan
import org.opendc.web.dispatcher.PlatformVerdict
import org.opendc.web.launcher.LaunchManifest
import org.opendc.web.launcher.PeakMemory
import org.opendc.web.server.metrics.ServerMetrics
import org.opendc.web.server.model.Execution
import org.opendc.web.server.model.ExecutionState
import org.opendc.web.server.model.ExecutionUnit
import org.opendc.web.server.model.RunUnit
import org.opendc.web.server.model.UnitState
import org.opendc.web.server.storage.ObjectStore
import org.opendc.web.server.storage.manifestKey
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private val URL_LIFETIME_CAP: Duration = Duration.parse(MAX_URL_LIFETIME)

/** An execution taken out of the queue and ready to hand over, with the manifest written for it. */
private class Claim(
    val executionId: UUID,
    val experimentId: UUID,
    val grant: Grant,
    val manifest: LaunchManifest,
)

/**
 * Keeps the platform as busy as the queue and its capacity allow, and records what it reports. Each
 * pass shapes the oldest outstanding work against the dispatcher's slot and hands it over if there is room.
 *
 * Transactions are opened by hand because a database write and a platform call must not share one: a
 * launch inside the claiming transaction would survive a rollback as a process nothing has a record of.
 */
@ApplicationScoped
class ExecutionLoop(
    private val dispatcher: Dispatcher,
    private val planner: ExecutionPlanner,
    private val settlement: Settlement,
    private val store: ObjectStore,
    private val config: ExecutionConfig,
    private val metrics: ServerMetrics,
) {
    private val reconciled = AtomicBoolean(false)
    private val pausedUntil = AtomicReference(Instant.MIN)

    /** Attaches the observer. Reconciling waits for the first pass of the queue, on the same thread. */
    fun onStart(
        @Suppress("UNUSED_PARAMETER") @Observes event: StartupEvent,
    ) {
        check(config.urlLifetime() <= URL_LIFETIME_CAP) {
            "opendc.execution.url-lifetime is ${config.urlLifetime()}, above the $URL_LIFETIME_CAP a signed URL can last"
        }
        dispatcher.observe(::onEvent)
    }

    @Scheduled(every = "2s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    fun drain() {
        if (!reconciled.get()) {
            try {
                reconcile()
                reconciled.set(true)
            } catch (e: Exception) {
                LOG.error("Could not reconcile with the platform; trying again on the next pass", e)
                return
            }
        }
        if (Instant.now() < pausedUntil.get()) {
            return
        }
        while (true) {
            val claim =
                try {
                    QuarkusTransaction.requiringNew().call<Claim?> { claim() } ?: return
                } catch (e: Exception) {
                    LOG.error("Could not shape queued work", e)
                    return
                }
            val launch = handOver(claim)
            metrics.launched(launch)
            when (launch) {
                Launch.Accepted ->
                    if (QuarkusTransaction.requiringNew().call<Boolean> { abandoned(claim.executionId) }) {
                        dispatcher.cancel(claim.executionId)
                    }
                is Launch.Rejected -> settlement.settle(claim.executionId, refused(launch.message))
                is Launch.Unavailable -> {
                    LOG.warn("The platform could not take execution {}: {}", claim.executionId, launch.message)
                    QuarkusTransaction.requiringNew().run { release(claim.executionId) }
                    pausedUntil.set(Instant.now().plus(config.launchRetryDelay()))
                    return
                }
            }
        }
    }

    /**
     * Asks the platform about every execution this server believes it holds, and settles accordingly.
     * One the platform no longer knows goes round again at the same attempt.
     */
    fun reconcile() {
        val ids = QuarkusTransaction.requiringNew().call<List<UUID>> { Execution.findOnPlatform().map { it.publicId } }
        if (ids.isEmpty()) {
            return
        }
        val verdicts = dispatcher.reconcile(ids)
        for (id in ids) {
            when (val verdict = verdicts[id] ?: PlatformVerdict.Unknown) {
                PlatformVerdict.Waiting -> {}
                is PlatformVerdict.Running -> markStarted(id, verdict.startedAt)
                is PlatformVerdict.Ended -> settlement.settle(id, verdict.outcome)
                PlatformVerdict.Unknown -> {
                    LOG.info("The platform no longer knows execution {}; its work goes round again", id)
                    settlement.requeue(id)
                }
            }
        }
    }

    /** Where the platform's events land. Failures propagate, so the platform delivers the event again. */
    private fun onEvent(event: PlatformEvent) {
        when (event) {
            is PlatformEvent.Started -> markStarted(event.executionId, event.at)
            is PlatformEvent.Finished -> settlement.settle(event.executionId, event.outcome)
        }
    }

    private fun markStarted(
        executionId: UUID,
        at: Instant,
    ) {
        QuarkusTransaction.requiringNew().run { Execution.lockByPublicId(executionId)?.start(at) }
    }

    /**
     * The next execution to hand over, taken out of the queue before this returns. One a retry wrote
     * down comes first; otherwise the oldest submitted units are shaped into a new execution.
     */
    private fun claim(): Claim? {
        val execution = admissible() ?: return null
        val token = execution.submit(Instant.now())
        return Claim(
            executionId = execution.publicId,
            experimentId = execution.experiment.publicId,
            grant = execution.grant,
            manifest = planner.manifest(execution, token, config.urlLifetime()),
        )
    }

    /** An execution the platform would take right now, written down but not yet handed over. */
    private fun admissible(): Execution? {
        val waiting = Execution.findWaiting().firstOrNull()
        if (waiting != null) {
            if (!dispatcher.admits(waiting.parallelism, waiting.memoryRequestMb.toDouble())) {
                return null
            }
            return Execution.lockByPublicId(waiting.publicId)?.takeIf { it.state == ExecutionState.QUEUED }
        }
        val experiment = RunUnit.findQueued().firstOrNull()?.experiment ?: return null
        val units = RunUnit.lockQueued(experiment.id)
        val bag = planner.plan(experiment, units, dispatcher.slot()).firstOrNull() ?: return null
        if (!dispatcher.admits(bag.grant.parallelism, bag.grant.memoryRequestMb.toDouble())) {
            return null
        }
        return planner.queue(experiment, bag, units, attempt = 1, dispatcher = dispatcher.name)
    }

    /**
     * Writes the manifest where the launcher will read it and hands the execution over. Anything that
     * goes wrong on the way is the platform being unavailable, which the next pass tries again.
     */
    private fun handOver(claim: Claim): Launch =
        try {
            val key = manifestKey(claim.experimentId, claim.executionId)
            val manifest = SdkJson.json.encodeToString(LaunchManifest.serializer(), claim.manifest).encodeToByteArray()
            store.put(key, manifest.inputStream())
            dispatcher.launch(LaunchRequest(claim.executionId, store.readUrl(key, config.urlLifetime()), claim.grant))
        } catch (e: Exception) {
            Launch.Unavailable(e.message ?: e.javaClass.name)
        }

    /** Whether everything [executionId] carries was cancelled while it was being handed over. */
    private fun abandoned(executionId: UUID): Boolean {
        val execution = Execution.findByPublicId(executionId) ?: return false
        return ExecutionUnit.findByExecution(execution.id).all { it.unit.state == UnitState.CANCELLED }
    }

    /** Puts work the platform could not take back in the queue, as the same execution at the same attempt. */
    private fun release(executionId: UUID) {
        val execution = Execution.lockByPublicId(executionId) ?: return
        if (execution.state == ExecutionState.SUBMITTED) {
            execution.release()
        }
    }

    private companion object {
        val LOG = LoggerFactory.getLogger(ExecutionLoop::class.java)

        fun refused(message: String) =
            ExitOutcome(
                ExitReason.REJECTED,
                NO_EXIT_CODE,
                message,
                PlatformSpan.NotStarted,
                PeakMemory.Unmeasured,
                "",
            )
    }
}
