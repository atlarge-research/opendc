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

import io.fabric8.kubernetes.api.model.ContainerStateTerminated
import io.fabric8.kubernetes.api.model.ContainerStatus
import io.fabric8.kubernetes.api.model.Node
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.api.model.batch.v1.Job
import org.opendc.web.dispatcher.CapacitySnapshot
import org.opendc.web.dispatcher.EXIT_KILLED
import org.opendc.web.dispatcher.EXIT_TERMINATED
import org.opendc.web.dispatcher.ExecutionSlot
import org.opendc.web.dispatcher.ExitOutcome
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.MemoryCap
import org.opendc.web.dispatcher.NO_EXIT_CODE
import org.opendc.web.dispatcher.PlatformSpan
import org.opendc.web.dispatcher.launcherExitMessage
import org.opendc.web.dispatcher.launcherExitReason
import org.opendc.web.launcher.BYTES_PER_MB
import org.opendc.web.launcher.PeakMemory
import org.opendc.web.launcher.peakMemoryOf
import java.time.Instant

/** Container waiting reasons no amount of waiting fixes. */
private val PERMANENT_WAITING = setOf("InvalidImageName", "ErrImageNeverPull", "CreateContainerConfigError")

/** What a container killed by a signal exits with, which says nothing about why. */
private val SIGNALLED = setOf(EXIT_KILLED, EXIT_TERMINATED)

/** Taint effects that keep a pod off a node unless it tolerates them, which this server's pods never do. */
private val REPELLING_TAINTS = setOf("NoSchedule", "NoExecute")

/** Where the taints live that Kubernetes itself sets for as long as a node condition lasts, such as a cordon. */
private const val CONDITION_TAINTS = "node.kubernetes.io/"

/** Cores and memory at the scale of one pod: what a pod requests, or the most a node can give one. */
internal data class PodResources(
    val cores: Double,
    val memoryMb: Double,
) {
    fun fitsIn(room: PodResources): Boolean = cores <= room.cores && memoryMb <= room.memoryMb
}

/** Where an execution stands, as its Job and pod show it. */
internal sealed interface JobState {
    /** Not running yet, since [since], for the reason [cause] gives. */
    data class Pending(
        val since: Instant,
        val cause: String,
    ) : JobState

    /** It can never start. */
    data class Refused(val message: String) : JobState

    data class Running(val startedAt: Instant) : JobState

    /** It ended. The log tail is filled in by the dispatcher, which is what can fetch one. */
    data class Ended(val outcome: ExitOutcome) : JobState
}

/**
 * Reads an execution's state from its [job], its [pods] and whether the Job is [gone] from the
 * cluster. The first rule that matches decides.
 *
 * A stop this server asked for wins over whatever the container exited with, once the pod has ended:
 * a launcher killed on cancel exits however it exits, and that is not its verdict.
 */
internal fun jobState(
    job: Job,
    pods: List<Pod>,
    gone: Boolean,
): JobState {
    val pod = pods.maxByOrNull { it.metadata.creationTimestamp.orEmpty() }
    val container = pod?.status?.containerStatuses?.firstOrNull()
    val terminated = container?.state?.terminated
    val stop = job.metadata.annotations.orEmpty()[STOP_REASON]
    val podEnded = pod == null || pod.status?.phase == "Succeeded" || pod.status?.phase == "Failed"

    if (stop != null && (gone || podEnded)) {
        val reason = ExitReason.entries.firstOrNull { it.name == stop } ?: ExitReason.UNKNOWN
        return ended(reason, job.metadata.annotations.orEmpty()[STOP_MESSAGE].orEmpty(), terminated)
    }
    if (pod?.status?.reason == "DeadlineExceeded") {
        return ended(ExitReason.TIMEOUT, "ran past its time limit", terminated)
    }
    if (terminated?.reason == "OOMKilled") {
        return ended(ExitReason.OOM, "the kernel ended it for memory", terminated)
    }
    if (pod?.status?.reason == "Evicted") {
        return ended(ExitReason.UNKNOWN, "evicted: ${pod.status?.message.orEmpty()}", terminated)
    }
    val disruption = pod?.status?.conditions.orEmpty().firstOrNull { it.type == "DisruptionTarget" && it.status == "True" }
    if (disruption != null) {
        return ended(ExitReason.UNKNOWN, "disrupted: ${disruption.reason.orEmpty()}", terminated)
    }
    if (terminated != null) {
        val code = terminated.exitCode ?: NO_EXIT_CODE
        val reason = if (code in SIGNALLED) ExitReason.UNKNOWN else launcherExitReason(code)
        return ended(reason, launcherExitMessage(code), terminated)
    }
    if (pod?.status?.phase == "Failed") {
        return ended(ExitReason.UNKNOWN, "the node refused it: ${pod.status?.reason.orEmpty()}", terminated = null)
    }
    if (pod == null) {
        conditionEnded(job)?.let { return it }
    }
    if (gone) {
        return ended(ExitReason.UNKNOWN, "removed from the cluster before it ended", terminated = null)
    }
    val running = container?.state?.running
    if (running != null) {
        return JobState.Running(instant(running.startedAt) ?: Instant.now())
    }
    if (pod?.status?.phase == "Unknown") {
        return JobState.Running(instant(pod.status?.startTime) ?: Instant.now())
    }
    val waiting = container?.state?.waiting
    if (waiting != null && waiting.reason in PERMANENT_WAITING) {
        return JobState.Refused("${waiting.reason}: ${waiting.message.orEmpty()}")
    }
    return JobState.Pending(instant(job.metadata.creationTimestamp) ?: Instant.now(), pendingCause(pod, container))
}

/**
 * The cluster's capacity against what this server's live pods request, from the [nodes] those pods can
 * be placed on. With no node visible, which is also what a cluster that refuses to list them looks
 * like, the total is [ceiling]'s.
 */
internal fun capacityOf(
    nodes: List<Node>,
    pods: List<Pod>,
    ceiling: CapacitySnapshot,
): CapacitySnapshot {
    val usable = usable(nodes)
    val live = pods.filter { it.status?.phase == "Pending" || it.status?.phase == "Running" }
    val requests = live.flatMap { pod -> pod.spec?.containers.orEmpty().map { it.resources?.requests.orEmpty() } }
    val allocatedCores = requests.sumOf { amount(it["cpu"]) }
    val allocatedMemory = requests.sumOf { amount(it["memory"]) } / BYTES_PER_MB
    if (usable.isEmpty()) {
        return ceiling.copy(allocatedCores = allocatedCores.toInt(), allocatedMemoryMb = allocatedMemory)
    }
    val allocatable = usable.map { it.status?.allocatable.orEmpty() }
    return CapacitySnapshot(
        totalCores = allocatable.sumOf { amount(it["cpu"]).toInt() },
        totalMemoryMb = allocatable.sumOf { amount(it["memory"]) } / BYTES_PER_MB,
        allocatedCores = allocatedCores.toInt(),
        allocatedMemoryMb = allocatedMemory,
    )
}

/**
 * The most each node could give one of this server's pods, were the node empty. A node that is down,
 * cordoned or under pressure takes pods again once that passes, so it counts; only a taint an operator
 * set keeps the pods off a node for good.
 */
internal fun allocatableOf(nodes: List<Node>): List<PodResources> =
    nodes.filterNot(::repelsForGood).map { node ->
        val allocatable = node.status?.allocatable.orEmpty()
        PodResources(amount(allocatable["cpu"]), amount(allocatable["memory"]) / BYTES_PER_MB)
    }

private fun repelsForGood(node: Node): Boolean =
    node.spec?.taints.orEmpty().any { it.effect in REPELLING_TAINTS && !it.key.orEmpty().startsWith(CONDITION_TAINTS) }

/** What [job]'s pod requests, which, with its requests equal to its limits, is also all it is given. */
internal fun requestOf(job: Job): PodResources {
    val requests = job.spec?.template?.spec?.containers.orEmpty().map { it.resources?.requests.orEmpty() }
    return PodResources(requests.sumOf { amount(it["cpu"]) }, requests.sumOf { amount(it["memory"]) } / BYTES_PER_MB)
}

/**
 * Whether a pod requesting [asked] fits on none of [nodes], so that no amount of waiting places it.
 * With no node visible, which is also how a cluster that refuses to list them looks, nothing is ruled out.
 */
internal fun fitsNoNode(
    asked: PodResources,
    nodes: List<PodResources>,
): Boolean = nodes.isNotEmpty() && nodes.none { asked.fitsIn(it) }

/** Why a pod requesting [asked] fits on none of [nodes]. */
internal fun tooLargeForNodes(
    asked: PodResources,
    nodes: List<PodResources>,
): String =
    "no node can hold it: it requests ${cpuAmount(asked.cores)} cpu and ${asked.memoryMb.toLong()} MB, and no node has " +
        "more than ${cpuAmount(nodes.maxOf { it.cores })} cpu and ${nodes.maxOf { it.memoryMb }.toLong()} MB allocatable"

/**
 * This slot shrunk to the node in [nodes] with the most memory, since a bag packed larger than every
 * node is never scheduled, and capped at that node's memory for a unit too large for the slot. With no
 * node visible, nothing is ruled out.
 */
internal fun ExecutionSlot.fittedTo(nodes: List<PodResources>): ExecutionSlot {
    if (nodes.isEmpty()) {
        return this
    }
    val roomiest = nodes.maxBy { it.memoryMb }
    return copy(
        cores = minOf(cores, roomiest.cores.toInt()).coerceAtLeast(1),
        memoryMb = minOf(memoryMb, roomiest.memoryMb),
        memoryCap = MemoryCap.Limited(roomiest.memoryMb),
    )
}

/** The nodes this server's pods can be placed on: Ready, not cordoned, and with no taint that repels them. */
private fun usable(nodes: List<Node>): List<Node> =
    nodes.filter { node ->
        node.spec?.unschedulable != true &&
            node.spec?.taints.orEmpty().none { it.effect in REPELLING_TAINTS } &&
            node.status?.conditions.orEmpty().any { it.type == "Ready" && it.status == "True" }
    }

/** What a Job's own conditions say when it has no pod left to read. */
private fun conditionEnded(job: Job): JobState? {
    val conditions = job.status?.conditions.orEmpty().filter { it.status == "True" }
    if (conditions.any { it.type == "Complete" }) {
        val span =
            span(instant(job.status?.startTime), instant(job.status?.completionTime))
        return JobState.Ended(ExitOutcome(ExitReason.OK, 0, "", span, PeakMemory.Unmeasured, ""))
    }
    val failed = conditions.firstOrNull { it.type == "Failed" } ?: return null
    return ended(ExitReason.UNKNOWN, "failed with no pod left to read: ${failed.reason.orEmpty()}", terminated = null)
}

private fun ended(
    reason: ExitReason,
    message: String,
    terminated: ContainerStateTerminated?,
): JobState.Ended =
    JobState.Ended(
        ExitOutcome(
            reason = reason,
            exitCode = terminated?.exitCode ?: NO_EXIT_CODE,
            message = message,
            span = span(instant(terminated?.startedAt), instant(terminated?.finishedAt)),
            peakMemory = terminated?.message?.let(::peakMemoryOf) ?: PeakMemory.Unmeasured,
            logTail = "",
        ),
    )

private fun span(
    startedAt: Instant?,
    endedAt: Instant?,
): PlatformSpan = if (startedAt != null && endedAt != null) PlatformSpan.Ran(startedAt, endedAt) else PlatformSpan.NotStarted

private fun pendingCause(
    pod: Pod?,
    container: ContainerStatus?,
): String {
    val unscheduled = pod?.status?.conditions.orEmpty().firstOrNull { it.type == "PodScheduled" && it.status == "False" }
    if (unscheduled != null) {
        return unscheduled.message ?: unscheduled.reason.orEmpty()
    }
    val waiting = container?.state?.waiting
    if (waiting != null) {
        return "${waiting.reason}: ${waiting.message.orEmpty()}"
    }
    return if (pod == null) "no pod has been created (check ResourceQuota and admission policies)" else "waiting to start"
}

private fun instant(timestamp: String?): Instant? = timestamp?.takeIf { it.isNotEmpty() }?.let(Instant::parse)

private fun amount(quantity: Quantity?): Double = quantity?.numericalAmount?.toDouble() ?: 0.0

private fun cpuAmount(cores: Double): String = cores.toBigDecimal().stripTrailingZeros().toPlainString()
