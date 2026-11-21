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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.sdk.model.serialization.SdkJson

// The codes the launcher chooses start at twenty to stay clear of those it does not choose: 1 for an
// uncaught error, 3 under -XX:+ExitOnOutOfMemoryError, and 128 plus the signal when it is killed.

const val EXIT_OK = 0

/** The manifest was unreadable or unsafe. */
const val EXIT_INVALID_SPEC = 20

/** Something failed outside any one unit. */
const val EXIT_SIMULATION_ERROR = 21

/** The manifest or an input could not be fetched, or some unit's outcome could not be published. */
const val EXIT_TRANSFER_FAILED = 22

/** Every unit's outcome was published, and at least one of them is a failure. */
const val EXIT_UNITS_FAILED = 23

/** A failure the launcher recognises well enough to give it a code. */
class LaunchFailure(
    val exitCode: Int,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** Where the launcher leaves its [PeakMemory], in the directory it was started in, as it exits. */
const val PEAK_MEMORY_FILE = "peak-memory.json"

/** Memory figures, such as those in [PeakMemory], are counted in mebibytes. */
const val BYTES_PER_MB = 1024.0 * 1024.0

/** How a log line names the unit it was written under. */
val UNIT_TAG = Regex("""\[scenario=(\d+) seed=(-?\d+)]""")

/** How one unit ended, as published to its outcome target once its outputs are there. */
@Serializable
sealed interface UnitOutcome {
    /** The run finished, taking [seconds] of wall clock, and every file it wrote was published. */
    @Serializable
    @SerialName("succeeded")
    data class Succeeded(val seconds: Double) : UnitOutcome

    @Serializable
    @SerialName("failed")
    data class Failed(
        val failure: UnitFailure,
        val message: String,
    ) : UnitOutcome
}

/** Why one unit failed, which decides whether running it again could help. */
@Serializable
enum class UnitFailure {
    @SerialName("invalidSpec")
    INVALID_SPEC,

    @SerialName("simulationError")
    SIMULATION_ERROR,

    /** An output could not be published. Another attempt may well manage it. */
    @SerialName("transfer")
    TRANSFER,
}

/** The highest the process's memory reached, as far as the process itself can tell. */
@Serializable
sealed interface PeakMemory {
    /**
     * @property residentMb The most of the machine's memory the process held at once.
     * @property liveHeapMb The most heap its objects occupied at once, which is what an estimate of a
     *           unit is compared against.
     */
    @Serializable
    @SerialName("measured")
    data class Measured(
        val residentMb: Double,
        val liveHeapMb: Double,
    ) : PeakMemory

    /** Nothing could be read, such as off Linux or for a process that was killed. */
    @Serializable
    @SerialName("unmeasured")
    data object Unmeasured : PeakMemory
}

/** Reads what a launcher wrote to [PEAK_MEMORY_FILE]. Anything that does not decode is [PeakMemory.Unmeasured]. */
fun peakMemoryOf(text: String): PeakMemory =
    try {
        SdkJson.json.decodeFromString(PeakMemory.serializer(), text)
    } catch (e: IllegalArgumentException) {
        PeakMemory.Unmeasured
    }
