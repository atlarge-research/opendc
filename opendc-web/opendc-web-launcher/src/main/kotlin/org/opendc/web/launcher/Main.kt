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

import mu.KotlinLogging
import org.apache.logging.log4j.ThreadContext
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.resource.ResourceProvisioner
import org.opendc.sdk.model.resource.UriReference
import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.sdk.runner.OpenDC
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import java.io.IOException
import java.lang.management.ManagementFactory
import java.lang.management.MemoryType
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.Executors
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.system.exitProcess

/** What the output tree of a unit is called. It never leaves this process. */
private const val RUN = "run"

/** A failure message is a marker's payload and a log line, not a stack dump. */
private const val MESSAGE_CAP = 2000

private const val KB_PER_MB = 1024.0

private val logger = KotlinLogging.logger {}

fun main(): Unit = exitProcess(launch(System.getenv(MANIFEST_URL_VARIABLE).orEmpty(), Path.of("").toAbsolutePath()))

/**
 * Runs the manifest at [manifestUrl] and returns the code to exit with.
 *
 * Everything happens in a working directory under [reportDir] that is removed before returning; only
 * the peak-memory report stays behind.
 */
internal fun launch(
    manifestUrl: String,
    reportDir: Path,
): Int {
    val workDir = Files.createTempDirectory(reportDir, "work-")
    val code =
        try {
            run(readManifest(manifestUrl), workDir)
        } catch (e: Exception) {
            logger.error(e) { "The launch failed" }
            exitCodeOf(e)
        } finally {
            workDir.toFile().deleteRecursively()
        }
    writePeakMemory(reportDir.resolve(PEAK_MEMORY_FILE))
    return code
}

private fun readManifest(url: String): LaunchManifest {
    if (url.isEmpty()) {
        throw LaunchFailure(EXIT_INVALID_SPEC, "$MANIFEST_URL_VARIABLE is not set")
    }
    var text = ""
    fetch(url) { text = it.readBytes().decodeToString() }
    return try {
        SdkJson.json.decodeFromString(LaunchManifest.serializer(), text)
    } catch (e: IllegalArgumentException) {
        throw LaunchFailure(EXIT_INVALID_SPEC, "The manifest at ${redacted(url)} could not be read", e)
    }
}

/**
 * Stages the inputs, runs every unit, and publishes what each one produced as it finishes.
 *
 * A unit's failure is published as its outcome and the rest carry on. The process fails only for what
 * is nobody's unit, such as an input that could not be staged or an outcome that could not be published.
 */
private fun run(
    manifest: LaunchManifest,
    workDir: Path,
): Int {
    val inputs = manifest.inputs.map { it to inside(workDir, it.path) }
    for ((input, path) in inputs) {
        stage(input.source, path)
    }

    val telemetry = TelemetrySink()
    val reports =
        reporting(manifest.telemetry, telemetry).use {
            val useful = minOf(manifest.units.size, Runtime.getRuntime().availableProcessors()).coerceAtLeast(1)
            val pool = Executors.newFixedThreadPool(manifest.parallelism.coerceIn(1, useful))
            try {
                manifest.units.map { unit -> pool.submit<UnitReport> { runUnit(unit, workDir, telemetry) } }.map { it.get() }
            } finally {
                pool.shutdownNow()
            }
        }
    return when {
        reports.any { it is UnitReport.Unpublished } -> EXIT_TRANSFER_FAILED
        reports.any { it is UnitReport.Published && it.outcome is UnitOutcome.Failed } -> EXIT_UNITS_FAILED
        else -> EXIT_OK
    }
}

/** Whether a unit's outcome reached its target, which is what the server settles the unit from. */
private sealed interface UnitReport {
    data class Published(val outcome: UnitOutcome) : UnitReport

    data object Unpublished : UnitReport
}

/** Runs one unit and publishes its outputs, then its outcome. */
private fun runUnit(
    unit: LaunchUnit,
    workDir: Path,
    telemetry: TelemetrySink,
): UnitReport {
    val scenario = unit.scenario
    ThreadContext.put("scenario", scenario.id.toString())
    ThreadContext.put("seed", scenario.seed.toString())
    try {
        val outcome = simulate(unit, workDir, telemetry)
        if (outcome is UnitOutcome.Failed) {
            logger.warn { "The unit failed: ${outcome.message}" }
        }
        return try {
            val body = SdkJson.json.encodeToString(UnitOutcome.serializer(), outcome).encodeToByteArray()
            publish(Payload(body.size.toLong()) { body.inputStream() }, unit.outcome)
            UnitReport.Published(outcome)
        } catch (e: LaunchFailure) {
            logger.error(e) { "The unit's outcome could not be published" }
            UnitReport.Unpublished
        }
    } finally {
        ThreadContext.clearMap()
    }
}

private fun simulate(
    unit: LaunchUnit,
    workDir: Path,
    telemetry: TelemetrySink,
): UnitOutcome {
    val scenario = unit.scenario
    val issues = scenario.validate()
    if (issues.isNotEmpty()) {
        return UnitOutcome.Failed(UnitFailure.INVALID_SPEC, issues.joinToString { "${it.path} ${it.message}" }.take(MESSAGE_CAP))
    }
    val output = workDir.resolve("output").resolve("${scenario.id}").resolve("${scenario.seed}")
    val started = System.nanoTime()
    return try {
        val report =
            OpenDC
                .builder()
                .provisioner(staged(workDir))
                .output(output)
                .sink(telemetry)
                .parallelism(1)
                .build()
                .simulate(RUN, listOf(scenario))
        val seconds = (System.nanoTime() - started) / 1e9
        val written = report.runs.firstNotNullOfOrNull { it.outputPath }
        for (target in unit.outputs) {
            val file = written?.resolve(target.file)
            if (file != null && file.exists()) {
                publish(Payload(file), target.target)
            }
        }
        UnitOutcome.Succeeded(seconds)
    } catch (e: Exception) {
        val failure = generateSequence<Throwable>(e) { it.cause }.filterIsInstance<LaunchFailure>().firstOrNull()
        val kind =
            when (failure?.exitCode) {
                EXIT_INVALID_SPEC -> UnitFailure.INVALID_SPEC
                EXIT_TRANSFER_FAILED -> UnitFailure.TRANSFER
                else -> UnitFailure.SIMULATION_ERROR
            }
        logger.error(e) { "The unit threw" }
        UnitOutcome.Failed(kind, (e.message ?: e.javaClass.name).take(MESSAGE_CAP))
    } finally {
        output.toFile().deleteRecursively()
    }
}

/** Resolves staged paths against [workDir]. A name reaching this point is the server's failure to resolve it. */
private fun staged(workDir: Path): ResourceProvisioner {
    val files = FileSystemResourceProvisioner(workDir)
    return ResourceProvisioner { reference ->
        when (reference) {
            is NamedReference ->
                throw LaunchFailure(EXIT_INVALID_SPEC, "The manifest names '${reference.name}' instead of locating it")
            is UriReference -> files.provision(reference)
        }
    }
}

/** [relative] under [workDir], refusing anything that would land outside it. */
private fun inside(
    workDir: Path,
    relative: String,
): Path {
    val path = workDir.resolve(relative).normalize()
    if (!path.startsWith(workDir) || path == workDir) {
        throw LaunchFailure(EXIT_INVALID_SPEC, "The manifest stages '$relative', which is outside the working directory")
    }
    return path
}

/**
 * Reports what [sink] measures for as long as the runs last, if [target] names somewhere. The sink is
 * attached either way, so a manifest run by hand takes the same path as one a server wrote.
 */
private fun reporting(
    target: TelemetryTarget,
    sink: TelemetrySink,
): AutoCloseable =
    when (target) {
        is TelemetryTarget.None -> AutoCloseable {}
        is TelemetryTarget.Endpoint -> TelemetryPoster(target, sink::report).also { it.start() }
    }

/** Written in place, never renamed into place: a platform may have mounted that very path to read it back. */
private fun writePeakMemory(file: Path) {
    val text = SdkJson.json.encodeToString(PeakMemory.serializer(), peakMemory())
    try {
        Files.writeString(file, text, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
    } catch (e: IOException) {
        logger.warn { "Could not write the peak memory report: ${e.message}" }
    }
}

/** The resident high-water mark from the kernel, beside the most heap the simulation's objects held. */
private fun peakMemory(): PeakMemory {
    val status = Path.of("/proc/self/status")
    if (!status.exists()) {
        return PeakMemory.Unmeasured
    }
    val residentKb =
        status.readLines().firstOrNull { it.startsWith("VmHWM:") }?.split(Regex("\\s+"))?.getOrNull(1)?.toDoubleOrNull()
            ?: return PeakMemory.Unmeasured
    val liveHeap =
        ManagementFactory
            .getMemoryPoolMXBeans()
            .filter { it.type == MemoryType.HEAP }
            .sumOf { it.peakUsage?.used ?: 0L }
    return PeakMemory.Measured(residentMb = residentKb / KB_PER_MB, liveHeapMb = liveHeap / BYTES_PER_MB)
}

/** A run's failure arrives wrapped in the pool's. Anything unrecognized is a simulation error, never retried. */
private fun exitCodeOf(error: Throwable): Int =
    generateSequence(error) { it.cause }.filterIsInstance<LaunchFailure>().firstOrNull()?.exitCode
        ?: EXIT_SIMULATION_ERROR
