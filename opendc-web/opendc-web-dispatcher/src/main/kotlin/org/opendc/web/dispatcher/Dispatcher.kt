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

package org.opendc.web.dispatcher

import org.opendc.web.launcher.EXIT_INVALID_SPEC
import org.opendc.web.launcher.EXIT_OK
import org.opendc.web.launcher.EXIT_SIMULATION_ERROR
import org.opendc.web.launcher.EXIT_TRANSFER_FAILED
import org.opendc.web.launcher.EXIT_UNITS_FAILED
import org.opendc.web.launcher.PeakMemory
import java.time.Instant
import java.util.UUID

/**
 * Turns an admitted bag of work into a running process on one execution platform, and reports what
 * became of it.
 *
 * An execution is addressed by the id the server minted for it, so a restarted server can cancel and
 * reconcile work it did not start. Terminal facts come from the platform, never from the launcher.
 *
 * [slot], [admits] and [capacity] never make a remote call: they are asked inside transactions.
 */
interface Dispatcher : AutoCloseable {
    /** Identifies this dispatcher in the database and in metrics. Stable across restarts. */
    val name: String

    /** The shape of one execution here, which is what bags are packed for. Cores are at least one. */
    fun slot(): ExecutionSlot

    /** Whether an execution of this shape could start now. */
    fun admits(
        cores: Int,
        memoryMb: Double,
    ): Boolean

    /** How much of the platform there is and how much of it this server's work holds. */
    fun capacity(): CapacitySnapshot

    /**
     * Registers where platform events arrive. Called once, before [launch] or [reconcile].
     *
     * Events arrive at least once, possibly more than once, and [PlatformEvent.Started] before
     * [PlatformEvent.Finished] for any one execution. A [PlatformEvent.Finished] whose listener
     * returns normally lets the platform release what it kept for that execution; one whose listener
     * throws is delivered again. Listeners must not block the platform's threads for long.
     */
    fun observe(listener: (PlatformEvent) -> Unit)

    /**
     * Hands an execution to the platform. Idempotent: an id the platform already holds is
     * [Launch.Accepted] again. Never waits on moving data; a transfer that fails after acceptance
     * arrives as a [PlatformEvent.Finished].
     */
    fun launch(request: LaunchRequest): Launch

    /**
     * Stops the execution behind [executionId], ignoring an id the platform never held. The ending is
     * reported as a [PlatformEvent.Finished] in the platform's own time.
     */
    fun cancel(executionId: UUID)

    /**
     * What the platform knows of each of [executionIds], asked once at startup about everything this
     * server believed was live. A missing id reads as [PlatformVerdict.Unknown]; one answered
     * [PlatformVerdict.Waiting] or [PlatformVerdict.Running] is watched from then on.
     */
    fun reconcile(executionIds: List<UUID>): Map<UUID, PlatformVerdict>
}

/**
 * What one execution is given: the concurrency it may use, the memory it must stay under, and how long it may run.
 *
 * @property memoryCap The most memory the platform can give any one execution, including a unit too
 *   large for [memoryMb] that is run alone.
 */
data class ExecutionSlot(
    val cores: Int,
    val memoryMb: Double,
    val timeCap: TimeCap,
    val memoryCap: MemoryCap = MemoryCap.Unlimited,
)

/** The longest an execution may run on a platform, as the platform sees it. */
sealed interface TimeCap {
    data object Unlimited : TimeCap

    data class Limited(val seconds: Int) : TimeCap

    /** [seconds], or the cap where that is shorter. */
    fun clamp(seconds: Int): Int =
        when (this) {
            Unlimited -> seconds
            is Limited -> minOf(seconds, this.seconds)
        }
}

/** The most memory a platform can give one execution, as the platform sees it. */
sealed interface MemoryCap {
    data object Unlimited : MemoryCap

    data class Limited(val megabytes: Double) : MemoryCap

    /** [megabytes], or the cap where that is smaller. */
    fun clamp(megabytes: Double): Double =
        when (this) {
            Unlimited -> megabytes
            is Limited -> minOf(megabytes, this.megabytes)
        }
}

/** The platform's size, and how much of it this server's executions hold. */
data class CapacitySnapshot(
    val totalCores: Int,
    val totalMemoryMb: Double,
    val allocatedCores: Int,
    val allocatedMemoryMb: Double,
)

/** One bag, ready to run. [manifestUrl] is all the environment the launcher gets. */
data class LaunchRequest(
    val executionId: UUID,
    val manifestUrl: String,
    val grant: Grant,
)

/** What a platform said to being handed an execution. */
sealed interface Launch {
    data object Accepted : Launch

    /** The platform will never take this execution, however often it is asked. */
    data class Rejected(val message: String) : Launch

    /** The platform cannot take it now; asking again later may work. */
    data class Unavailable(val message: String) : Launch
}

/** Something the platform saw happen to an execution. */
sealed interface PlatformEvent {
    val executionId: UUID

    data class Started(
        override val executionId: UUID,
        val at: Instant,
    ) : PlatformEvent

    data class Finished(
        override val executionId: UUID,
        val outcome: ExitOutcome,
    ) : PlatformEvent
}

/** What a platform knows of an execution when asked after a restart. */
sealed interface PlatformVerdict {
    /** Accepted and not started yet. */
    data object Waiting : PlatformVerdict

    data class Running(val startedAt: Instant) : PlatformVerdict

    /** It ended while nobody was listening. */
    data class Ended(val outcome: ExitOutcome) : PlatformVerdict

    /** The platform has never heard of it, or has forgotten it. */
    data object Unknown : PlatformVerdict
}

/**
 * How an execution ended, as the platform saw it.
 *
 * @property exitCode What the process exited with, or [NO_EXIT_CODE] where the platform recorded none.
 * @property logTail The end of what the process wrote, or the empty string where there is none.
 */
data class ExitOutcome(
    val reason: ExitReason,
    val exitCode: Int,
    val message: String,
    val span: PlatformSpan,
    val peakMemory: PeakMemory,
    val logTail: String,
)

/** Whether an execution ever ran on the platform, and from when to when. */
sealed interface PlatformSpan {
    data object NotStarted : PlatformSpan

    data class Ran(
        val startedAt: Instant,
        val endedAt: Instant,
    ) : PlatformSpan
}

/** Why an execution ended, in one vocabulary across every platform. */
enum class ExitReason {
    OK,

    /** The simulation threw. */
    SIMULATION_ERROR,

    /** The spec did not parse or did not validate. */
    INVALID_SPEC,

    /** The kernel ended it for memory. */
    OOM,

    /** It outlived the limit this server set. */
    TIMEOUT,

    /** It outlived a limit the platform set, such as a SLURM partition's. */
    WALLTIME,

    CANCELLED,

    /** The platform refused it for good. */
    REJECTED,

    /** Gone, with no verdict. */
    UNKNOWN,

    ;

    /** Whether a different grant, or less work in the bag, could change the outcome. */
    val isResourceShaped: Boolean
        get() = this == OOM || this == TIMEOUT || this == WALLTIME || this == UNKNOWN
}

/** How much of a process's output an outcome carries. */
const val LOG_TAIL_BYTES = 256 * 1024

/** What [ExitOutcome.exitCode] holds where the platform recorded no exit code. */
const val NO_EXIT_CODE = -1

/** Every launcher JVM runs with this, so running out of heap ends it with [JVM_OUT_OF_MEMORY]. */
const val EXIT_ON_OUT_OF_MEMORY = "-XX:+ExitOnOutOfMemoryError"

/** What a JVM run with [EXIT_ON_OUT_OF_MEMORY] exits with when its heap runs out. */
const val JVM_OUT_OF_MEMORY = 3

/** What a process ended by SIGKILL exits with, as the kernel's out-of-memory killer does. */
const val EXIT_KILLED = 137

/** What a process ended by SIGTERM exits with. */
const val EXIT_TERMINATED = 143

/** What every platform names an execution's job, after its id. */
const val JOB_NAME_PREFIX = "opendc-"

fun jobName(executionId: UUID): String = "$JOB_NAME_PREFIX$executionId"

/** Why a launcher that exited by itself with [code] ended. */
fun launcherExitReason(code: Int): ExitReason =
    when (code) {
        EXIT_OK, EXIT_UNITS_FAILED -> ExitReason.OK
        EXIT_INVALID_SPEC -> ExitReason.INVALID_SPEC
        EXIT_SIMULATION_ERROR -> ExitReason.SIMULATION_ERROR
        JVM_OUT_OF_MEMORY -> ExitReason.OOM
        else -> ExitReason.UNKNOWN
    }

/** What a launcher that exited with [code] is reported as having done. */
fun launcherExitMessage(code: Int): String =
    when (code) {
        EXIT_OK -> ""
        EXIT_UNITS_FAILED -> "some units failed"
        EXIT_INVALID_SPEC -> "the manifest was unreadable or unsafe"
        EXIT_SIMULATION_ERROR -> "the launcher failed outside any unit"
        EXIT_TRANSFER_FAILED -> "an input or an outcome could not be transferred"
        JVM_OUT_OF_MEMORY -> "ran out of heap"
        else -> "exited with code $code"
    }

/** The last [LOG_TAIL_BYTES] of [text], starting at a line boundary when anything was cut. */
fun logTail(text: String): String {
    val bytes = text.encodeToByteArray()
    if (bytes.size <= LOG_TAIL_BYTES) {
        return text
    }
    val cut = bytes.copyOfRange(bytes.size - LOG_TAIL_BYTES, bytes.size).decodeToString()
    return cut.substringAfter('\n', cut)
}
