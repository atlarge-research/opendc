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

import org.opendc.web.dispatcher.EXIT_KILLED
import org.opendc.web.dispatcher.ExitOutcome
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.NO_EXIT_CODE
import org.opendc.web.dispatcher.PlatformSpan
import org.opendc.web.dispatcher.launcherExitMessage
import org.opendc.web.dispatcher.launcherExitReason
import org.opendc.web.launcher.BYTES_PER_MB
import org.opendc.web.launcher.PeakMemory
import org.opendc.web.launcher.peakMemoryOf
import java.time.Instant

/** The record files of one execution's directory, by name. */
internal object Record {
    const val LIMITS = "limits"
    const val SUBMITTED = "submitted"
    const val STOP = "stop"
    const val STARTED = "started"
    const val TERMINATED = "terminated"
    const val EXIT = "exit"
    const val PEAK_MEMORY = "peak-memory.json"
}

/** A job is taken to have run into its limit within this much of it, since SLURM signals a little early. */
private const val LIMIT_SLACK_SECONDS = 60

/** The time limit a job was submitted with, and whether that was the most the cluster allows. */
internal data class Limits(
    val timeLimitSeconds: Int,
    val capped: Boolean,
)

/** Whether this dispatcher stopped the job, and why. */
internal sealed interface Stop {
    data object None : Stop

    data class Requested(
        val reason: ExitReason,
        val message: String,
    ) : Stop
}

internal sealed interface Start {
    data object NotStarted : Start

    data class At(val instant: Instant) : Start
}

/** How often the kernel's out-of-memory killer struck inside the job, where the job's cgroup could be read. */
internal sealed interface OomKills {
    data object Unread : OomKills

    data class Counted(val count: Long) : OomKills
}

/** What the wrapper wrote as the launcher exited. Missing where the job never got that far. */
internal sealed interface WrapperExit {
    data object Missing : WrapperExit

    data class Recorded(
        val code: Int,
        val endedAt: Instant,
        val oomKills: OomKills,
        val residentPeak: ResidentPeak,
    ) : WrapperExit
}

/** The job's peak memory as its cgroup accounted it. */
internal sealed interface ResidentPeak {
    data object Unread : ResidentPeak

    data class Read(val megabytes: Double) : ResidentPeak
}

/**
 * Everything one execution's directory says about how its job went.
 *
 * @property lastOutput When the job last wrote to its output, the only clock left for a job that
 *           died without the wrapper recording its end.
 */
internal data class JobRecords(
    val stop: Stop,
    val start: Start,
    val terminated: Boolean,
    val exit: WrapperExit,
    val limits: Limits,
    val lastOutput: Instant,
    val launcherPeak: PeakMemory,
)

internal sealed interface JobEnding {
    data class Ended(val outcome: ExitOutcome) : JobEnding

    /** Gone from the queue without a word, which only another attempt can explain. */
    data class Vanished(
        val message: String,
        val span: PlatformSpan,
        val peak: PeakMemory,
    ) : JobEnding
}

/** The records in [files], each a record's name and its `key=value` lines, as one execution's account. */
internal fun records(
    files: Map<String, String>,
    lastOutput: Instant,
): JobRecords {
    val fields = files.mapValues { (_, text) -> fieldsOf(text) }
    val limits = fields[Record.LIMITS].orEmpty()
    val stop = fields[Record.STOP]
    val started = fields[Record.STARTED]?.get("at")?.toLongOrNull()
    val exit = fields[Record.EXIT]
    return JobRecords(
        stop =
            if (stop == null) {
                Stop.None
            } else {
                val reason = ExitReason.entries.firstOrNull { it.name == stop["reason"] } ?: ExitReason.UNKNOWN
                Stop.Requested(reason, stop["message"].orEmpty())
            },
        start = if (started == null) Start.NotStarted else Start.At(Instant.ofEpochSecond(started)),
        terminated = Record.TERMINATED in files,
        exit = exit?.let(::wrapperExit) ?: WrapperExit.Missing,
        limits = Limits(limits["time-limit"]?.toIntOrNull() ?: Int.MAX_VALUE, limits["capped"] == "true"),
        lastOutput = lastOutput,
        launcherPeak = files[Record.PEAK_MEMORY]?.let(::peakMemoryOf) ?: PeakMemory.Unmeasured,
    )
}

/**
 * How a job ended, read off its records. The first rule that matches decides.
 *
 * A stop this dispatcher wrote wins over everything, since a job it cancelled exits however it exits.
 * The kernel's out-of-memory count, read from the job's own cgroup, separates a memory kill from the
 * cluster ending a job for time, which looks the same from the exit code alone.
 */
internal fun ending(
    records: JobRecords,
    logTail: String,
): JobEnding {
    val exit = records.exit
    val end = if (exit is WrapperExit.Recorded) exit.endedAt else records.lastOutput
    val span = spanOf(records.start, end)
    val peak = peakOf(exit, records.launcherPeak)
    val code = if (exit is WrapperExit.Recorded) exit.code else NO_EXIT_CODE
    val ended = { reason: ExitReason, message: String -> JobEnding.Ended(ExitOutcome(reason, code, message, span, peak, logTail)) }
    val limitReached = limitReached(records.start, end, records.limits)
    val outOfTime =
        if (records.limits.capped) {
            ended(ExitReason.WALLTIME, "ran past ${records.limits.timeLimitSeconds} s, the longest this cluster allows")
        } else {
            ended(ExitReason.TIMEOUT, "ran past its time limit")
        }
    val stoppedByCluster = { otherwise: String -> if (limitReached) outOfTime else ended(ExitReason.UNKNOWN, otherwise) }

    val stop = records.stop
    if (stop is Stop.Requested) {
        return ended(stop.reason, stop.message)
    }
    if (exit is WrapperExit.Recorded) {
        val kills = exit.oomKills
        return when {
            kills is OomKills.Counted && kills.count > 0 && exit.code != 0 -> ended(ExitReason.OOM, "the kernel ended it for memory")
            records.terminated -> stoppedByCluster("signalled by the cluster before its time limit")
            exit.code == EXIT_KILLED ->
                when (kills) {
                    OomKills.Unread -> ended(ExitReason.OOM, "killed, most likely for memory (no cgroup reading)")
                    is OomKills.Counted -> stoppedByCluster("killed by the cluster")
                }
            else -> ended(launcherExitReason(exit.code), launcherExitMessage(exit.code))
        }
    }
    if ((records.terminated || records.start is Start.At) && limitReached) {
        return outOfTime
    }
    return JobEnding.Vanished("gone from the queue without an exit record", span, peak)
}

private fun wrapperExit(fields: Map<String, String>): WrapperExit {
    val code = fields["code"]?.toIntOrNull() ?: return WrapperExit.Missing
    val ended = fields["ended"]?.toLongOrNull()?.let(Instant::ofEpochSecond) ?: Instant.now()
    return WrapperExit.Recorded(
        code = code,
        endedAt = ended,
        oomKills = fields["oom_kill"]?.toLongOrNull()?.let { OomKills.Counted(it) } ?: OomKills.Unread,
        residentPeak = fields["memory_peak"]?.toLongOrNull()?.let { ResidentPeak.Read(it / BYTES_PER_MB) } ?: ResidentPeak.Unread,
    )
}

private fun limitReached(
    start: Start,
    end: Instant,
    limits: Limits,
): Boolean =
    when (start) {
        Start.NotStarted -> false
        is Start.At -> end.epochSecond - start.instant.epochSecond >= limits.timeLimitSeconds.toLong() - LIMIT_SLACK_SECONDS
    }

private fun spanOf(
    start: Start,
    end: Instant,
): PlatformSpan =
    when (start) {
        Start.NotStarted -> PlatformSpan.NotStarted
        is Start.At -> PlatformSpan.Ran(start.instant, maxOf(start.instant, end))
    }

/**
 * The job's peak: the cgroup's resident figure, which counts what the JVM's own report cannot, with
 * the launcher's live heap beside it. A launcher that was killed left no report, and then the heap is
 * known only to have stayed under the resident figure, which is what is reported for it.
 */
private fun peakOf(
    exit: WrapperExit,
    launcher: PeakMemory,
): PeakMemory {
    val resident = (exit as? WrapperExit.Recorded)?.residentPeak
    return when {
        resident is ResidentPeak.Read && launcher is PeakMemory.Measured -> PeakMemory.Measured(resident.megabytes, launcher.liveHeapMb)
        resident is ResidentPeak.Read -> PeakMemory.Measured(resident.megabytes, resident.megabytes)
        else -> launcher
    }
}

/** The `key=value` lines of a record. */
private fun fieldsOf(text: String): Map<String, String> =
    text
        .lineSequence()
        .map { it.trim() }
        .filter { '=' in it }
        .associate { it.substringBefore('=') to it.substringAfter('=') }
