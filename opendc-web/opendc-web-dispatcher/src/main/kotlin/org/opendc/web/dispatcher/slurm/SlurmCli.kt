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

import org.opendc.web.dispatcher.CapacitySnapshot
import org.opendc.web.dispatcher.Grant
import org.opendc.web.dispatcher.JOB_NAME_PREFIX
import org.opendc.web.dispatcher.TimeCap
import org.opendc.web.dispatcher.jobName
import org.opendc.web.launcher.LAUNCHER_MAIN
import java.util.UUID
import kotlin.math.ceil

/** Pending reasons SLURM will never get past, so waiting longer only wastes the timeout. */
internal val PERMANENT_PENDING_REASONS =
    setOf("PartitionTimeLimit", "PartitionNodeLimit", "BadConstraints", "InvalidAccount", "InvalidQOS", "DependencyNeverSatisfied")

/** What sbatch says when no retry will change its mind. Anything else may pass, such as a full queue. */
private val PERMANENT_REFUSALS =
    listOf(
        "Invalid partition name",
        "Invalid account",
        "Invalid qos",
        "Requested node configuration is not available",
        "Requested time limit is invalid",
        "Memory specification can not be satisfied",
        "More processors requested than permitted",
        "unrecognized option",
        "invalid option",
    )

/** Node states that offer nothing, whatever their CPUs say. */
private val UNUSABLE_STATES = listOf("down", "drain", "fail", "maint", "unknown")

internal const val SECONDS_PER_MINUTE = 60

private const val SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE

private const val SECONDS_PER_DAY = 24 * SECONDS_PER_HOUR

/** One of this dispatcher's jobs as the queue lists it. */
internal data class QueuedJob(
    val jobId: String,
    val pending: Boolean,
    val reason: String,
)

/** What sbatch did with a job. */
internal sealed interface Submission {
    data class Queued(val jobId: String) : Submission

    /** No retry changes the answer. */
    data class Refused(val message: String) : Submission

    /** It may be accepted later. */
    data class Deferred(val message: String) : Submission
}

/** What `sinfo` says about the partition this dispatcher submits to. */
internal data class PartitionView(
    val capacity: CapacitySnapshot,
    val timeLimit: TimeCap,
    val smallestNodeMemoryMb: Double,
)

/** Where one execution's job runs on the cluster, and what it runs there. */
internal data class JobScript(
    val directory: String,
    val launcherDir: String,
    val java: String,
)

internal fun squeueCommand(user: String): List<String> = listOf("squeue", "--noheader", "--user=$user", "--format=%j|%i|%T|%r")

/** This dispatcher's jobs in a `squeue` listing, by execution. Jobs of any other name are someone else's. */
internal fun parseQueue(stdout: String): Map<UUID, QueuedJob> =
    stdout
        .lineSequence()
        .map { it.trim().split('|') }
        .filter { it.size == 4 && it[0].startsWith(JOB_NAME_PREFIX) }
        .mapNotNull { (name, id, state, reason) ->
            val execution = runCatching { UUID.fromString(name.removePrefix(JOB_NAME_PREFIX)) }.getOrNull() ?: return@mapNotNull null
            execution to QueuedJob(id, state == "PENDING", reason)
        }.toMap()

internal fun sbatchCommand(
    executionId: UUID,
    grant: Grant,
    timeMinutes: Int,
    script: JobScript,
    partition: Partition,
    siteOptions: List<String>,
): List<String> =
    buildList {
        add("sbatch")
        add("--parsable")
        add("--no-requeue")
        add("--job-name=${jobName(executionId)}")
        add("--nodes=1")
        add("--ntasks=1")
        add("--cpus-per-task=${grant.parallelism}")
        add("--mem=${grant.memoryRequestMb}M")
        add("--time=$timeMinutes")
        add("--chdir=${script.directory}")
        add("--output=${script.directory}/slurm.out")
        when (partition) {
            Partition.ClusterDefault -> {}
            is Partition.Named -> add("--partition=${partition.name}")
        }
        addAll(siteOptions)
        add("${script.launcherDir}/wrapper.sh")
        add(script.launcherDir)
        add(script.java)
        add("${grant.heapMb}")
        add(LAUNCHER_MAIN)
    }

/** Reads sbatch's answer. With `--parsable`, success is the job id, followed by the cluster's name if there are several. */
internal fun parseSubmission(result: CommandResult): Submission {
    if (result.exitCode == 0) {
        val id = result.stdout.trim().substringBefore(';')
        if (id.isNotEmpty() && id.all { it.isDigit() || it == '_' }) {
            return Submission.Queued(id)
        }
    }
    val message = result.stderr.trim().ifEmpty { result.stdout.trim() }
    val permanent = PERMANENT_REFUSALS.any { message.contains(it, ignoreCase = true) }
    return if (permanent) Submission.Refused(message) else Submission.Deferred(message)
}

internal fun scancelCommand(
    user: String,
    executionId: UUID,
): List<String> = listOf("scancel", "--user=$user", "--name=${jobName(executionId)}")

internal fun sinfoCommand(partition: Partition): List<String> =
    buildList {
        add("sinfo")
        add("--noheader")
        add("--Node")
        when (partition) {
            Partition.ClusterDefault -> {}
            is Partition.Named -> add("--partition=${partition.name}")
        }
        add("--format=%P|%n|%C|%m|%e|%T|%l")
    }

/**
 * The partition's usable nodes from a `sinfo --Node` listing: the named [partition], or with none
 * named the one marked as the cluster's default. A node listed in several partitions counts once.
 */
internal fun parsePartition(
    stdout: String,
    partition: Partition,
): PartitionView {
    val rows =
        stdout
            .lineSequence()
            .map { it.trim().split('|') }
            .filter { it.size == 7 }
            .filter { row ->
                when (partition) {
                    Partition.ClusterDefault -> row[0].endsWith('*')
                    is Partition.Named -> row[0].removeSuffix("*") == partition.name
                }
            }.toList()
    val usable = rows.filter { usable(it[5]) }.distinctBy { it[1] }
    // %C is allocated/idle/other/total.
    val cpus = usable.map { row -> row[2].split('/').map { it.toIntOrNull() ?: 0 } }
    val memory = usable.map { it[3].toDoubleOrNull() ?: 0.0 }
    val free = usable.map { it[4].toDoubleOrNull() ?: 0.0 }
    return PartitionView(
        capacity =
            CapacitySnapshot(
                totalCores = cpus.sumOf { it.getOrElse(3) { 0 } },
                totalMemoryMb = memory.sum(),
                allocatedCores = cpus.sumOf { it.getOrElse(0) { 0 } },
                allocatedMemoryMb = memory.zip(free).sumOf { (total, available) -> (total - available).coerceAtLeast(0.0) },
            ),
        timeLimit = rows.firstOrNull()?.let { parseSlurmTime(it[6]) } ?: TimeCap.Unlimited,
        smallestNodeMemoryMb = memory.minOrNull() ?: 0.0,
    )
}

/** A SLURM time limit: `infinite`, or minutes in any of SLURM's formats. */
internal fun parseSlurmTime(text: String): TimeCap {
    val value = text.trim()
    if (value.equals("infinite", ignoreCase = true) || value.equals("UNLIMITED", ignoreCase = true)) {
        return TimeCap.Unlimited
    }
    val days = if ('-' in value) value.substringBefore('-').toInt() else 0
    val clock = value.substringAfter('-').split(':').map { it.toInt() }
    val seconds =
        if ('-' in value) {
            // D-H, D-H:M, D-H:M:S
            days * SECONDS_PER_DAY + clock[0] * SECONDS_PER_HOUR + clock.getOrElse(1) { 0 } * SECONDS_PER_MINUTE + clock.getOrElse(2) { 0 }
        } else {
            // M, M:S, H:M:S
            when (clock.size) {
                1 -> clock[0] * SECONDS_PER_MINUTE
                2 -> clock[0] * SECONDS_PER_MINUTE + clock[1]
                else -> clock[0] * SECONDS_PER_HOUR + clock[1] * SECONDS_PER_MINUTE + clock[2]
            }
        }
    return TimeCap.Limited(seconds)
}

/** Whole minutes covering [seconds], which is the finest a batch script's time limit can be. */
internal fun slurmMinutes(seconds: Int): Int = ceil(seconds.toDouble() / SECONDS_PER_MINUTE).toInt().coerceAtLeast(1)

/** The major version `java -version` reports: `21.0.2` is 21, `1.8.0_412` is 8. */
internal fun javaVersionOf(output: String): Int {
    val version = Regex("""version "([^"]+)"""").find(output)?.groupValues?.get(1) ?: return 0
    val parts = version.split('.', '_', '-')
    val major = parts.firstOrNull()?.toIntOrNull() ?: return 0
    return if (major == 1) parts.getOrNull(1)?.toIntOrNull() ?: 0 else major
}

private fun usable(state: String): Boolean = !state.endsWith('*') && UNUSABLE_STATES.none { state.lowercase().startsWith(it) }
