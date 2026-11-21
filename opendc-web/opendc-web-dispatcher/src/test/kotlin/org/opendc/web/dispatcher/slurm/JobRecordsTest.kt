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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.PlatformSpan
import org.opendc.web.launcher.PeakMemory
import java.time.Instant

/**
 * A SLURM job leaves only the records its wrapper and this dispatcher wrote, so reading them is the
 * whole of telling a memory kill from a walltime from a job that simply vanished.
 */
class JobRecordsTest {
    private val start = 1_000_000L

    private fun reasonOf(
        files: Map<String, String>,
        lastOutput: Instant = Instant.ofEpochSecond(start + 10),
    ): ExitReason {
        val ending = ending(records(files, lastOutput), logTail = "")
        return (ending as JobEnding.Ended).outcome.reason
    }

    private fun base(
        limit: Int = 900,
        capped: Boolean = false,
    ) = mapOf(Record.LIMITS to "time-limit=$limit\ncapped=$capped", Record.STARTED to "at=$start")

    @Test
    fun `reads a stop this dispatcher wrote over an exit that looks like success`() {
        val files = base() + (Record.STOP to "reason=CANCELLED\nmessage=cancelled") + (Record.EXIT to "code=0\nended=${start + 5}")

        assertEquals(ExitReason.CANCELLED, reasonOf(files))
    }

    @Test
    fun `reads the kernel's out-of-memory count from the job's cgroup as memory`() {
        val files = base() + (Record.EXIT to "code=137\nended=${start + 5}\noom_kill=1")

        assertEquals(ExitReason.OOM, reasonOf(files))
    }

    @Test
    fun `reads a kill without a cgroup reading as memory, the safer guess`() {
        assertEquals(ExitReason.OOM, reasonOf(base() + (Record.EXIT to "code=137\nended=${start + 5}")))
    }

    @Test
    fun `reads a kill the cgroup clears of memory as the cluster's doing`() {
        assertEquals(ExitReason.UNKNOWN, reasonOf(base() + (Record.EXIT to "code=137\nended=${start + 5}\noom_kill=0")))
    }

    // The same SIGTERM means different things at the cap and below it: at the cap, more time is not
    // on offer, which is what WALLTIME tells the retry policy.
    @Test
    fun `reads a run signalled at its limit as walltime at the cap and timeout below it`() {
        val atLimit = Record.EXIT to "code=143\nended=${start + 899}"

        assertEquals(ExitReason.WALLTIME, reasonOf(base(capped = true) + atLimit + (Record.TERMINATED to "")))
        assertEquals(ExitReason.TIMEOUT, reasonOf(base(capped = false) + atLimit + (Record.TERMINATED to "")))
    }

    @Test
    fun `reads a signal well before the limit as unexplained`() {
        val files = base() + (Record.EXIT to "code=143\nended=${start + 30}") + (Record.TERMINATED to "")

        assertEquals(ExitReason.UNKNOWN, reasonOf(files))
    }

    @Test
    fun `reads a job that died at its limit without an exit record as out of time`() {
        assertEquals(ExitReason.TIMEOUT, reasonOf(base(), lastOutput = Instant.ofEpochSecond(start + 899)))
    }

    @Test
    fun `reads the launcher's own exit codes as everywhere else`() {
        assertEquals(ExitReason.OK, reasonOf(base() + (Record.EXIT to "code=23\nended=${start + 5}")))
        assertEquals(ExitReason.INVALID_SPEC, reasonOf(base() + (Record.EXIT to "code=20\nended=${start + 5}")))
    }

    @Test
    fun `reports a job gone without any record as vanished`() {
        val ending = ending(records(base(), Instant.ofEpochSecond(start + 10)), logTail = "")

        assertTrue(ending is JobEnding.Vanished, "$ending")
    }

    @Test
    fun `prefers the cgroup's resident peak, keeping the launcher's heap beside it`() {
        val launcher = SdkJson.json.encodeToString(PeakMemory.serializer(), PeakMemory.Measured(800.0, 500.0))
        val files =
            base() + (Record.EXIT to "code=0\nended=${start + 5}\nmemory_peak=${1024L * 1024 * 1024}") + (Record.PEAK_MEMORY to launcher)

        val outcome = (ending(records(files, Instant.ofEpochSecond(start + 5)), "") as JobEnding.Ended).outcome

        assertEquals(PeakMemory.Measured(1024.0, 500.0), outcome.peakMemory)
        assertEquals(PlatformSpan.Ran(Instant.ofEpochSecond(start), Instant.ofEpochSecond(start + 5)), outcome.span)
    }
}
