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

package org.opendc.cli.run

import org.opendc.cli.progress.ProgressSnapshot
import org.opendc.cli.progress.ProgressSource
import org.opendc.cli.render.OutputView
import org.opendc.sdk.model.experiment.expand
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipInputStream

/** Which of the caller's projects a remote run goes into. */
internal sealed interface ProjectChoice {
    data class ById(val id: String) : ProjectChoice

    /** The project of that name, created on first use, so repeated runs gather in one place. */
    data class Named(val name: String) : ProjectChoice
}

/** A remote run that ended without every scenario succeeding; [failures] says why, scenario by scenario. */
internal class RemoteRunFailed(
    val state: RemoteState,
    val failures: List<String>,
    val outputs: OutputView,
) : RuntimeException("The experiment ended ${state.wire}")

/**
 * Runs an experiment on an OpenDC server: creates it in a project, submits it, follows its progress,
 * and unpacks the results archive into the output directory, laid out as a local run lays them out.
 *
 * The server checks the document as it would one from the web app, so references to traces have to
 * name traces in the caller's library or the deployment's own; a refusal surfaces as an [ApiFailure]
 * from [prepare], before anything has run. An interrupted run is cancelled on the server.
 */
internal class RemoteBackend(
    private val api: OpendcApi,
    private val project: ProjectChoice,
    private val pollInterval: Duration = Duration.ofSeconds(1),
    private val pollPatience: Duration = Duration.ofMinutes(2),
) : SimulationBackend {
    override fun prepare(request: RunRequest): SimulationSession {
        val projectId =
            when (project) {
                is ProjectChoice.ById -> project.id
                is ProjectChoice.Named -> api.projectNamed(project.name)
            }
        val name = request.experiment.name.ifEmpty { "(unnamed)" }
        val experimentId = api.createExperiment(projectId, name, request.experiment)
        try {
            api.submit(experimentId)
        } catch (e: ApiFailure) {
            // A refused draft is nothing anybody asked to keep, and would pile up with every retry.
            runCatching { api.delete(experimentId) }
            throw e
        }
        val submitted = api.status(experimentId)

        val scenarios = request.experiment.expand()
        val overview =
            SimulationOverview(
                name = name,
                scenarios = scenarios.distinctBy { it.id }.size,
                runs = scenarios.size,
                topologies = request.experiment.topologies.size,
                workloads = request.experiment.workloads.size,
                policies = request.experiment.allocationPolicies.size,
                totalTasks = submitted.totalTasks,
                parallelism = Parallelism.Server,
                output = request.output,
                inputRoot = request.inputRoot,
            )
        return RemoteSession(experimentId, overview, submitted, request.output)
    }

    private inner class RemoteSession(
        private val experimentId: String,
        override val overview: SimulationOverview,
        submitted: RemoteStatus,
        private val output: Path,
    ) : SimulationSession {
        private val latest = AtomicReference(ProgressSnapshot(submitted.completedTasks, submitted.totalTasks))
        private val ended = AtomicBoolean(false)

        override val progress =
            object : ProgressSource {
                override val snapshot: ProgressSnapshot get() = latest.get()
            }

        override fun run(): RunOutcome {
            val cancelOnExit = Thread(::cancelUnlessEnded)
            Runtime.getRuntime().addShutdownHook(cancelOnExit)
            try {
                val status =
                    try {
                        follow()
                    } catch (e: Exception) {
                        // A run nobody is following any more would only use up its owner's budget.
                        cancelUnlessEnded()
                        throw e
                    }
                val outputs =
                    when (status.state) {
                        RemoteState.SUCCEEDED, RemoteState.PARTIAL -> OutputView(api.archive(experimentId) { unpack(it, output) }, output)
                        else -> OutputView(0, output)
                    }
                if (status.state != RemoteState.SUCCEEDED) {
                    throw RemoteRunFailed(status.state, status.failures, outputs)
                }
                return RunOutcome(RunSummary.NotMeasured, outputs)
            } finally {
                // Removing a hook while the JVM is already shutting down throws, and by then it has run.
                runCatching { Runtime.getRuntime().removeShutdownHook(cancelOnExit) }
            }
        }

        /** Polls until the run ends, riding out a server or network that stops answering for less than [pollPatience]. */
        private fun follow(): RemoteStatus {
            var lastAnswer = Instant.now()
            while (true) {
                try {
                    val status = api.status(experimentId)
                    lastAnswer = Instant.now()
                    latest.set(ProgressSnapshot(status.completedTasks, status.totalTasks))
                    if (status.state.isTerminal) {
                        ended.set(true)
                        return status
                    }
                } catch (e: Exception) {
                    if (!isTransient(e) || Duration.between(lastAnswer, Instant.now()) > pollPatience) {
                        throw e
                    }
                }
                Thread.sleep(pollInterval.toMillis())
            }
        }

        private fun cancelUnlessEnded() {
            if (!ended.get()) {
                runCatching { api.cancel(experimentId) }
            }
        }
    }
}

/** A failure that may well pass: the network, or the server erring rather than refusing. */
private fun isTransient(e: Exception): Boolean = e is IOException || (e is ApiFailure && e.status >= HTTP_SERVER_ERROR)

private const val HTTP_SERVER_ERROR = 500

/**
 * Writes the archive's files under [root] and returns how many runs it held. An entry naming a path
 * outside [root] is refused rather than written wherever it points.
 */
internal fun unpack(
    archive: InputStream,
    root: Path,
): Int {
    val target = root.toAbsolutePath().normalize()
    val runs = mutableSetOf<Path>()
    ZipInputStream(archive).use { zip ->
        generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.forEach { entry ->
            val file = target.resolve(entry.name).normalize()
            require(file.startsWith(target)) { "The archive names a path outside $target: ${entry.name}" }
            Files.createDirectories(file.parent)
            Files.newOutputStream(file).use { zip.copyTo(it) }
            runs.add(file.parent)
        }
    }
    return runs.size
}
