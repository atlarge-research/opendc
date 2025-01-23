/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.compute.workload
import mu.KotlinLogging
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

public abstract class WorkloadLoader(private val submissionTime: String? = null) {
    private val logger = KotlinLogging.logger {}

    public fun reScheduleTasks(workload: List<Task>) {
        if (submissionTime == null) {
            return
        }

        val workloadSubmissionTime = workload.minOf({ it.submissionTime }).toEpochMilli()
        val submissionTimeLong = LocalDateTime.parse(submissionTime).toInstant(ZoneOffset.UTC).toEpochMilli()

        val timeShift = submissionTimeLong - workloadSubmissionTime

        for (task in workload) {
            task.submissionTime = Instant.ofEpochMilli(task.submissionTime.toEpochMilli() + timeShift)
        }
    }

    public abstract fun load(): List<Task>

    /**
     * Load the workload at sample tasks until a fraction of the workload is loaded
     */
    public fun sampleByLoad(fraction: Double): List<Task> {
        val workload = this.load()

        reScheduleTasks(workload)

        if (fraction >= 1.0) {
            return workload
        }

        if (fraction <= 0.0) {
            throw Error("The fraction of tasks to load cannot be 0.0 or lower")
        }

        val res = mutableListOf<Task>()

        val totalLoad = workload.sumOf { it.totalLoad }
        var currentLoad = 0.0

        val shuffledWorkload = workload.shuffled()
        for (entry in shuffledWorkload) {
            val entryLoad = entry.totalLoad

            // TODO: ask Sacheen
            if ((currentLoad + entryLoad) / totalLoad > fraction) {
                break
            }

            currentLoad += entryLoad
            res += entry
        }

        logger.info { "Sampled ${workload.size} VMs (fraction $fraction) into subset of ${res.size} VMs" }

        return res
    }
}
