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
import org.opendc.web.dispatcher.TimeCap
import java.util.UUID

/**
 * Everything the dispatcher knows about the cluster it reads out of SLURM's command output, so a
 * line read wrongly is a job forgotten, a refusal retried forever, or a partition limit ignored.
 */
class SlurmCliTest {
    @Test
    fun `finds this dispatcher's jobs in the queue and ignores everyone else's`() {
        val ours = UUID.randomUUID()
        val listing =
            """
            opendc-$ours|4242|PENDING|PartitionTimeLimit
            somebody-elses-job|4243|RUNNING|None
            opendc-not-a-uuid|4244|RUNNING|None
            """.trimIndent()

        assertEquals(mapOf(ours to QueuedJob("4242", pending = true, reason = "PartitionTimeLimit")), parseQueue(listing))
    }

    @Test
    fun `counts only usable nodes of the default partition, once each`() {
        val listing =
            """
            defq*|node001|4/12/0/16|65536|30000|mixed|15:00
            defq*|node002|0/0/16/16|65536|65536|drained|15:00
            defq*|node003|0/16/0/16|65536|65536|idle*|15:00
            other|node004|0/32/0/32|131072|131072|idle|infinite
            defq*|node001|4/12/0/16|65536|30000|mixed|15:00
            """.trimIndent()

        val view = parsePartition(listing, Partition.ClusterDefault)

        assertEquals(16, view.capacity.totalCores, "only node001 is usable: node002 is drained, node003 not responding")
        assertEquals(4, view.capacity.allocatedCores)
        assertEquals(65536.0, view.capacity.totalMemoryMb, 1e-6)
        assertEquals(35536.0, view.capacity.allocatedMemoryMb, 1e-6)
        assertEquals(TimeCap.Limited(900), view.timeLimit)
    }

    @Test
    fun `reads every time format SLURM writes, and infinity`() {
        assertEquals(TimeCap.Unlimited, parseSlurmTime("infinite"))
        assertEquals(TimeCap.Unlimited, parseSlurmTime("UNLIMITED"))
        assertEquals(TimeCap.Limited(15 * 60), parseSlurmTime("15"))
        assertEquals(TimeCap.Limited(15 * 60 + 30), parseSlurmTime("15:30"))
        assertEquals(TimeCap.Limited(2 * 3600 + 5 * 60 + 7), parseSlurmTime("2:05:07"))
        assertEquals(TimeCap.Limited(86_400 + 3 * 3600), parseSlurmTime("1-3"))
        assertEquals(TimeCap.Limited(86_400 + 3 * 3600 + 4 * 60), parseSlurmTime("1-03:04"))
        assertEquals(TimeCap.Limited(2 * 86_400 + 1), parseSlurmTime("2-00:00:01"))
    }

    @Test
    fun `reads a job id, with or without the cluster it went to`() {
        assertEquals(Submission.Queued("1234"), parseSubmission(CommandResult(0, "1234\n", "")))
        assertEquals(Submission.Queued("1234"), parseSubmission(CommandResult(0, "1234;das5\n", "")))
    }

    // A refusal no retry can change must end the work at once, and one that may pass must not.
    @Test
    fun `tells a refusal for good from one that may pass`() {
        val invalid = parseSubmission(CommandResult(1, "", "sbatch: error: Batch job submission failed: Invalid partition name specified"))
        val busy = parseSubmission(CommandResult(1, "", "sbatch: error: QOSMaxSubmitJobPerUserLimit"))

        assertTrue(invalid is Submission.Refused, "$invalid")
        assertTrue(busy is Submission.Deferred, "$busy")
    }

    @Test
    fun `reads Java versions in both numbering schemes`() {
        assertEquals(21, javaVersionOf("""openjdk version "21.0.2" 2024-01-16"""))
        assertEquals(17, javaVersionOf("""openjdk version "17.0.9" 2023-10-17"""))
        assertEquals(8, javaVersionOf("""java version "1.8.0_412""""))
        assertEquals(0, javaVersionOf("bash: java: command not found"))
    }

    @Test
    fun `asks for whole minutes, never fewer than the job needs`() {
        assertEquals(1, slurmMinutes(1))
        assertEquals(2, slurmMinutes(61))
        assertEquals(15, slurmMinutes(900))
    }
}
