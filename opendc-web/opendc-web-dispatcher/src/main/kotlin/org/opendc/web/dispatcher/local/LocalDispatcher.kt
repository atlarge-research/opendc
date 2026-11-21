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

package org.opendc.web.dispatcher.local

import org.opendc.web.dispatcher.CapacitySnapshot
import org.opendc.web.dispatcher.Dispatcher
import org.opendc.web.dispatcher.EXIT_KILLED
import org.opendc.web.dispatcher.EXIT_ON_OUT_OF_MEMORY
import org.opendc.web.dispatcher.ExecutionSlot
import org.opendc.web.dispatcher.ExitOutcome
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.Grant
import org.opendc.web.dispatcher.Launch
import org.opendc.web.dispatcher.LaunchRequest
import org.opendc.web.dispatcher.PlatformEvent
import org.opendc.web.dispatcher.PlatformSpan
import org.opendc.web.dispatcher.PlatformVerdict
import org.opendc.web.dispatcher.TimeCap
import org.opendc.web.dispatcher.launcherExitMessage
import org.opendc.web.dispatcher.launcherExitReason
import org.opendc.web.dispatcher.logTail
import org.opendc.web.launcher.LAUNCHER_MAIN
import org.opendc.web.launcher.MANIFEST_URL_VARIABLE
import org.opendc.web.launcher.PEAK_MEMORY_FILE
import org.opendc.web.launcher.PeakMemory
import org.opendc.web.launcher.peakMemoryOf
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

/**
 * How much of this machine the local dispatcher may use, and where the launcher it starts lives.
 *
 * @property classpath Where [mainClass] and everything it needs is found.
 * @property workDir Where each execution gets a directory to run in, removed once it has ended.
 */
data class LocalDispatcherConfig(
    val cores: Int,
    val memoryMb: Double,
    val classpath: String,
    val workDir: Path,
    val mainClass: String = LAUNCHER_MAIN,
)

/**
 * Runs bags as subprocesses of this server.
 *
 * The process handle is the terminal observer: an exit code the launcher chose is taken at face
 * value, and anything else is read as the machine having ended the process. Nothing outlives this
 * server's lifetime, so reconciling after a restart has nothing to adopt.
 */
class LocalDispatcher(private val config: LocalDispatcherConfig) : Dispatcher {
    private val running = ConcurrentHashMap<UUID, Running>()
    private val pendingCancels = ConcurrentHashMap.newKeySet<UUID>()
    private val listener = AtomicReference<(PlatformEvent) -> Unit> { }

    override val name: String get() = NAME

    override fun slot(): ExecutionSlot = ExecutionSlot(config.cores, config.memoryMb, TimeCap.Unlimited)

    /**
     * Work larger than the whole machine is admitted once nothing else is running, since it would
     * otherwise wait for room that will never appear.
     */
    override fun admits(
        cores: Int,
        memoryMb: Double,
    ): Boolean {
        val live = running.values.toList()
        return live.isEmpty() ||
            (
                live.sumOf { it.grant.parallelism } + cores <= config.cores &&
                    live.sumOf { it.grant.memoryRequestMb.toDouble() } + memoryMb <= config.memoryMb
            )
    }

    override fun capacity(): CapacitySnapshot {
        val live = running.values.toList()
        return CapacitySnapshot(
            totalCores = config.cores,
            totalMemoryMb = config.memoryMb,
            allocatedCores = live.sumOf { it.grant.parallelism },
            allocatedMemoryMb = live.sumOf { it.grant.memoryRequestMb.toDouble() },
        )
    }

    override fun observe(listener: (PlatformEvent) -> Unit) {
        this.listener.set(listener)
    }

    override fun launch(request: LaunchRequest): Launch {
        if (running.containsKey(request.executionId)) {
            return Launch.Accepted
        }
        val directory = directoryOf(request.executionId)
        val process =
            try {
                Files.createDirectories(directory)
                ProcessBuilder(command(request))
                    .directory(directory.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(directory.resolve(LOG_FILE).toFile())
                    .also { it.environment()[MANIFEST_URL_VARIABLE] = request.manifestUrl }
                    .start()
            } catch (e: IOException) {
                directory.toFile().deleteRecursively()
                return Launch.Rejected("Could not start a launcher: ${e.message}")
            }
        val execution = Running(process, request.grant, Instant.now(), AtomicReference(Intent.RUN))
        running[request.executionId] = execution
        // A cancel that arrived while the process was being started finds it here.
        if (pendingCancels.remove(request.executionId)) {
            cancel(request.executionId)
        }
        watch(request.executionId, execution, request.grant.timeLimitSeconds)
        return Launch.Accepted
    }

    override fun cancel(executionId: UUID) {
        val execution = running[executionId]
        if (execution == null) {
            pendingCancels += executionId
            return
        }
        execution.intent.set(Intent.CANCEL)
        execution.process.destroyForcibly()
    }

    /**
     * A launcher from an earlier lifetime is never adopted.
     *
     * One left over is ended rather than waited for, so the retry that replaces it cannot race a
     * process still writing the same output, and every directory nothing is running in is swept.
     */
    override fun reconcile(executionIds: List<UUID>): Map<UUID, PlatformVerdict> {
        for (executionId in executionIds.filterNot { running.containsKey(it) }) {
            orphan(executionId)?.destroyForcibly()
        }
        if (config.workDir.exists()) {
            config.workDir
                .listDirectoryEntries()
                .filter { entry -> running.keys.none { it.toString() == entry.fileName.toString() } }
                .forEach { it.toFile().deleteRecursively() }
        }
        return executionIds.associateWith { executionId ->
            running[executionId]?.let { PlatformVerdict.Running(it.startedAt) } ?: PlatformVerdict.Unknown
        }
    }

    /** Launchers still running are left running; the next [reconcile] ends them. */
    override fun close() {}

    private fun directoryOf(executionId: UUID): Path = config.workDir.resolve(executionId.toString())

    private fun command(request: LaunchRequest): List<String> =
        listOf(
            Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-Xmx${request.grant.heapMb}m",
            EXIT_ON_OUT_OF_MEMORY,
            "-D$EXECUTION_PROPERTY=${request.executionId}",
            "-cp",
            config.classpath,
            config.mainClass,
        )

    private fun watch(
        executionId: UUID,
        execution: Running,
        timeLimitSeconds: Int,
    ) {
        Thread
            .ofVirtual()
            .name("opendc-local-$executionId")
            .start {
                val process = execution.process
                listener.get().invoke(PlatformEvent.Started(executionId, execution.startedAt))
                if (!process.waitFor(timeLimitSeconds.toLong(), TimeUnit.SECONDS)) {
                    execution.intent.compareAndSet(Intent.RUN, Intent.TIMEOUT)
                    process.destroyForcibly()
                    process.waitFor()
                }
                val directory = directoryOf(executionId)
                val outcome = outcome(process.exitValue(), execution, Instant.now(), directory)
                try {
                    listener.get().invoke(PlatformEvent.Finished(executionId, outcome))
                    // Released only once the outcome is in the server's hands: a listener that threw
                    // leaves the directory for the next reconcile to sweep.
                    directory.toFile().deleteRecursively()
                } finally {
                    running.remove(executionId)
                }
            }
    }

    private fun outcome(
        code: Int,
        execution: Running,
        endedAt: Instant,
        directory: Path,
    ): ExitOutcome {
        val (reason, message) =
            when (execution.intent.get()) {
                Intent.CANCEL -> ExitReason.CANCELLED to "cancelled"
                Intent.TIMEOUT -> ExitReason.TIMEOUT to "ran past its time limit"
                Intent.RUN ->
                    if (code == EXIT_KILLED) {
                        ExitReason.OOM to "killed, most likely for memory"
                    } else {
                        launcherExitReason(code) to launcherExitMessage(code)
                    }
            }
        return ExitOutcome(
            reason = reason,
            exitCode = code,
            message = message,
            span = PlatformSpan.Ran(execution.startedAt, endedAt),
            peakMemory = readOr(directory.resolve(PEAK_MEMORY_FILE), PeakMemory.Unmeasured) { peakMemoryOf(it) },
            logTail = readOr(directory.resolve(LOG_FILE), "") { logTail(it) },
        )
    }

    private fun <T> readOr(
        file: Path,
        absent: T,
        read: (String) -> T,
    ): T =
        try {
            if (file.exists()) read(file.readText()) else absent
        } catch (e: IOException) {
            absent
        }

    private fun orphan(executionId: UUID): ProcessHandle? =
        ProcessHandle
            .allProcesses()
            .filter { it.info().commandLine().orElse("").contains("$EXECUTION_PROPERTY=$executionId") }
            .findFirst()
            .orElse(null)

    private class Running(
        val process: Process,
        val grant: Grant,
        val startedAt: Instant,
        val intent: AtomicReference<Intent>,
    )

    /** Why a process ended, as far as this dispatcher is responsible for it. */
    private enum class Intent { RUN, CANCEL, TIMEOUT }

    companion object {
        const val NAME = "local"

        /** What the launcher's output is written to, inside its execution's directory. */
        const val LOG_FILE = "launcher.log"

        /** Identifies a launcher process as belonging to one execution of this server. */
        private const val EXECUTION_PROPERTY = "opendc.execution"
    }
}
