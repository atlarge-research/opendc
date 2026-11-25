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

import io.quarkus.test.Mock
import jakarta.enterprise.context.ApplicationScoped
import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.web.dispatcher.CapacitySnapshot
import org.opendc.web.dispatcher.Dispatcher
import org.opendc.web.dispatcher.ExecutionSlot
import org.opendc.web.dispatcher.ExitOutcome
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.Launch
import org.opendc.web.dispatcher.LaunchRequest
import org.opendc.web.dispatcher.MemoryCap
import org.opendc.web.dispatcher.PlatformEvent
import org.opendc.web.dispatcher.PlatformSpan
import org.opendc.web.dispatcher.PlatformVerdict
import org.opendc.web.dispatcher.TimeCap
import org.opendc.web.launcher.LaunchManifest
import org.opendc.web.launcher.Payload
import org.opendc.web.launcher.PeakMemory
import org.opendc.web.launcher.UnitOutcome
import org.opendc.web.launcher.fetch
import org.opendc.web.launcher.publish
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * A platform the test suite drives itself, so nothing here starts a real process. Where a case needs a
 * launcher's results, it plays the launcher: it reads the manifest and writes outcomes where it says.
 */
@Mock
@ApplicationScoped
class RecordingDispatcher : Dispatcher {
    val launched = CopyOnWriteArrayList<LaunchRequest>()
    val cancelled = CopyOnWriteArrayList<UUID>()
    private val answers = ConcurrentLinkedQueue<Launch>()
    private val verdicts = ConcurrentHashMap<UUID, PlatformVerdict>()
    private val listener = AtomicReference<(PlatformEvent) -> Unit> { }
    private val capacity = AtomicReference(ExecutionSlot(cores = 8, memoryMb = 8192.0, timeCap = TimeCap.Unlimited))

    override val name: String get() = "recording"

    override fun slot(): ExecutionSlot = capacity.get()

    override fun admits(
        cores: Int,
        memoryMb: Double,
    ): Boolean = cores <= capacity.get().cores && memoryMb <= capacity.get().memoryMb

    override fun capacity(): CapacitySnapshot = CapacitySnapshot(capacity.get().cores, capacity.get().memoryMb, 0, 0.0)

    override fun observe(listener: (PlatformEvent) -> Unit) {
        this.listener.set(listener)
    }

    /** Accepted, unless a case scripted another answer with [answer]. */
    override fun launch(request: LaunchRequest): Launch {
        launched += request
        return answers.poll() ?: Launch.Accepted
    }

    override fun cancel(executionId: UUID) {
        cancelled += executionId
    }

    /** What a case said with [knows], and nothing for every other id. */
    override fun reconcile(executionIds: List<UUID>): Map<UUID, PlatformVerdict> =
        executionIds.associateWith { verdicts[it] ?: PlatformVerdict.Unknown }

    override fun close() {}

    /** Caps how long one execution may run here, the way a SLURM partition does. */
    fun capTime(cap: TimeCap) {
        capacity.updateAndGet { it.copy(timeCap = cap) }
    }

    /** Caps the memory one execution can be given here, the way a cluster's largest node does. */
    fun capMemory(cap: MemoryCap) {
        capacity.updateAndGet { it.copy(memoryCap = cap) }
    }

    /** The next launches are answered with [launches], in order. */
    fun answer(vararg launches: Launch) {
        answers += launches
    }

    /** The platform's answer about [executionId] when asked after a restart. */
    fun knows(
        executionId: UUID,
        verdict: PlatformVerdict,
    ) {
        verdicts[executionId] = verdict
    }

    /** Reports an execution starting, the way a platform would. */
    fun start(executionId: UUID) {
        listener.get().invoke(PlatformEvent.Started(executionId, Instant.now()))
    }

    /** Reports an execution ending, the way a platform would. */
    fun finish(
        executionId: UUID,
        outcome: ExitOutcome,
    ) {
        listener.get().invoke(PlatformEvent.Finished(executionId, outcome))
    }

    /** The manifest the launcher of [executionId] was handed. */
    fun manifestOf(executionId: UUID): LaunchManifest {
        val request = launched.last { it.executionId == executionId }
        var text = ""
        fetch(request.manifestUrl) { text = it.readBytes().decodeToString() }
        return SdkJson.json.decodeFromString(LaunchManifest.serializer(), text)
    }

    /** Plays the launcher of [executionId]: certifies its run of [scenarioIndex] and [seed] with [outcome]. */
    fun certify(
        executionId: UUID,
        scenarioIndex: Int,
        outcome: UnitOutcome,
        seed: Long = 0,
    ) {
        val unit = manifestOf(executionId).units.first { it.scenario.id == scenarioIndex && it.scenario.seed == seed }
        val body = SdkJson.json.encodeToString(UnitOutcome.serializer(), outcome).encodeToByteArray()
        publish(Payload(body.size.toLong()) { body.inputStream() }, unit.outcome)
    }

    fun forget() {
        launched.clear()
        cancelled.clear()
        answers.clear()
        verdicts.clear()
    }
}

/** An ending a platform reports, with nothing measured. */
fun ended(
    reason: ExitReason,
    exitCode: Int = 0,
    message: String = "",
): ExitOutcome = ExitOutcome(reason, exitCode, message, PlatformSpan.NotStarted, PeakMemory.Unmeasured, "")
