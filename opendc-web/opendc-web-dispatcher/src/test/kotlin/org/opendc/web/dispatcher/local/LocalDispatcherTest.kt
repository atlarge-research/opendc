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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.opendc.sdk.model.dsl.ghz
import org.opendc.sdk.model.dsl.gib
import org.opendc.sdk.model.dsl.mhz
import org.opendc.sdk.model.dsl.mib
import org.opendc.sdk.model.dsl.ms
import org.opendc.sdk.model.resource.UriReference
import org.opendc.sdk.model.scenario.ScenarioSpec
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicySpec
import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.sdk.model.telemetry.OutputFileSpec
import org.opendc.sdk.model.topology.ClusterSpec
import org.opendc.sdk.model.topology.CpuSpec
import org.opendc.sdk.model.topology.DataCenterSpec
import org.opendc.sdk.model.topology.HostSpec
import org.opendc.sdk.model.topology.MemorySpec
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.workload.InlineWorkloadSpec
import org.opendc.sdk.model.workload.TaskFragmentSpec
import org.opendc.sdk.model.workload.TaskSpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec
import org.opendc.sdk.model.workload.WorkloadSpec
import org.opendc.web.dispatcher.ExitOutcome
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.dispatcher.Grant
import org.opendc.web.dispatcher.Launch
import org.opendc.web.dispatcher.LaunchRequest
import org.opendc.web.dispatcher.PlatformEvent
import org.opendc.web.dispatcher.PlatformSpan
import org.opendc.web.dispatcher.PlatformVerdict
import org.opendc.web.launcher.EXIT_UNITS_FAILED
import org.opendc.web.launcher.LAUNCHER_MAIN
import org.opendc.web.launcher.LaunchManifest
import org.opendc.web.launcher.LaunchUnit
import org.opendc.web.launcher.OutputTarget
import org.opendc.web.launcher.PEAK_MEMORY_FILE
import org.opendc.web.launcher.PeakMemory
import org.opendc.web.launcher.UNIT_TAG
import org.opendc.web.launcher.UnitFailure
import org.opendc.web.launcher.UnitOutcome
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * The local dispatcher is the terminal observer for a subprocess, so the behaviour that matters is
 * how it classifies an ending it did not choose, that an ending it did choose is not mistaken for
 * one of those, and that what the process leaves behind reaches the outcome.
 */
class LocalDispatcherTest {
    @TempDir
    lateinit var workDir: Path

    private val events = LinkedBlockingQueue<PlatformEvent>()

    private fun dispatcher(
        mainClass: String = SleepingLauncher::class.java.name,
        executions: Path = workDir.resolve("executions"),
    ): LocalDispatcher =
        LocalDispatcher(
            LocalDispatcherConfig(
                cores = 4,
                memoryMb = 4096.0,
                classpath = System.getProperty("java.class.path"),
                workDir = executions,
                mainClass = mainClass,
            ),
        ).also { dispatcher -> dispatcher.observe { events.add(it) } }

    private fun request(
        manifest: LaunchManifest = manifest(),
        timeLimitSeconds: Int = 120,
    ): LaunchRequest {
        val file = Files.createTempFile(workDir, "manifest-", ".json")
        Files.writeString(file, SdkJson.json.encodeToString(LaunchManifest.serializer(), manifest))
        return LaunchRequest(UUID.randomUUID(), file.toUri().toString(), Grant(1, 256, 768, timeLimitSeconds))
    }

    private fun manifest(vararg units: LaunchUnit): LaunchManifest =
        LaunchManifest(
            inputs = emptyList(),
            units = units.toList(),
            parallelism = 2,
        )

    private fun scenario(
        id: Int,
        workload: WorkloadSpec,
    ) = ScenarioSpec(
        topology =
            TopologySpec(
                listOf(
                    DataCenterSpec(
                        clusters =
                            listOf(
                                ClusterSpec(
                                    hosts =
                                        listOf(
                                            HostSpec(cpu = CpuSpec(coreCount = 4, coreSpeed = 3.ghz), memory = MemorySpec(size = 16.gib)),
                                        ),
                                ),
                            ),
                    ),
                ),
            ),
        workload = workload,
        allocationPolicy = PrefabAllocationPolicySpec(),
        id = id,
    )

    private fun unit(
        scenario: ScenarioSpec,
        results: Path,
    ): LaunchUnit {
        val run = results.resolve("${scenario.id}")
        return LaunchUnit(
            scenario = scenario,
            outputs = scenario.exportModel.filesToExport.map { OutputTarget(it.fileName, run.resolve(it.fileName).toUri().toString()) },
            outcome = run.resolve("outcome.json").toUri().toString(),
        )
    }

    private fun awaitFinished(): ExitOutcome {
        while (true) {
            when (val event = events.poll(90, TimeUnit.SECONDS) ?: error("nothing finished")) {
                is PlatformEvent.Started -> continue
                is PlatformEvent.Finished -> return event.outcome
            }
        }
    }

    // Two units in one process, one of which cannot find its input. Its failure is its own: the other
    // unit's results are published and certified, and the process ends normally.
    @Test
    fun `runs a bag to the end when one of its units fails, and certifies each unit separately`() {
        val results = workDir.resolve("results")
        val good = unit(scenario(0, InlineWorkloadSpec(listOf(task()))), results)
        val missing = unit(scenario(1, TraceWorkloadSpec(source = UriReference("inputs/never-staged"))), results)

        dispatcher(mainClass = LAUNCHER_MAIN).launch(request(manifest(good, missing)))
        val outcome = awaitFinished()

        assertEquals(ExitReason.OK, outcome.reason)
        assertEquals(EXIT_UNITS_FAILED, outcome.exitCode)
        assertTrue(decode(results.resolve("0/outcome.json")) is UnitOutcome.Succeeded)
        assertTrue(results.resolve("0/${OutputFileSpec.HOST.fileName}").exists(), "the unit that ran published its files")
        val failed = decode(results.resolve("1/outcome.json"))
        assertTrue(
            failed is UnitOutcome.Failed && failed.failure == UnitFailure.SIMULATION_ERROR,
            "the missing input failed its own unit: $failed",
        )
        val tagged = UNIT_TAG.findAll(outcome.logTail).map { it.groupValues[1] }.toSet()
        assertEquals(setOf("0", "1"), tagged, "each unit's lines carry its tag")
        assertTrue(outcome.peakMemory is PeakMemory.Measured || !Path.of("/proc/self/status").exists())
    }

    @Test
    fun `reports a manifest it could not read as one that no retry can fix`() {
        val dispatcher = dispatcher(mainClass = LAUNCHER_MAIN)
        val broken = Files.writeString(workDir.resolve("broken.json"), "{not a manifest")

        dispatcher.launch(LaunchRequest(UUID.randomUUID(), broken.toUri().toString(), Grant(1, 256, 768, 120)))

        assertEquals(ExitReason.INVALID_SPEC, awaitFinished().reason)
    }

    @Test
    fun `hands back what the process printed, when it ran, and its own account of its memory`() {
        val dispatcher = dispatcher(mainClass = ReportingLauncher::class.java.name)
        val request = request()

        dispatcher.launch(request)

        val started = events.poll(60, TimeUnit.SECONDS)
        assertTrue(started is PlatformEvent.Started, "a start is reported before an end: $started")
        val outcome = awaitFinished()
        val span = outcome.span
        assertTrue(span is PlatformSpan.Ran && !span.endedAt.isBefore(span.startedAt), "it ran, from a start to an end: $span")
        assertTrue(REPORTED_LINE in outcome.logTail)
        assertEquals(PeakMemory.Measured(residentMb = 100.0, liveHeapMb = 50.0), outcome.peakMemory)
        assertFalse(workDir.resolve("executions/${request.executionId}").exists(), "an execution's directory goes once it has ended")
    }

    @Test
    fun `reports no memory when the process left no account of it`() {
        dispatcher(mainClass = SilentLauncher::class.java.name).launch(request())

        assertEquals(PeakMemory.Unmeasured, awaitFinished().peakMemory)
    }

    @Test
    fun `refuses an execution it has nowhere to run`() {
        val occupied = Files.writeString(workDir.resolve("not-a-directory"), "")

        assertTrue(dispatcher(executions = occupied).launch(request()) is Launch.Rejected)
    }

    @Test
    fun `calls an overrun a timeout rather than an unexplained death`() {
        dispatcher().launch(request(timeLimitSeconds = 0))

        assertEquals(ExitReason.TIMEOUT, awaitFinished().reason)
    }

    @Test
    fun `calls a kill it asked for a cancellation, not a failure`() {
        val dispatcher = dispatcher()
        val request = request()

        dispatcher.launch(request)
        dispatcher.cancel(request.executionId)

        assertEquals(ExitReason.CANCELLED, awaitFinished().reason)
    }

    @Test
    fun `admits nothing larger than what a running execution leaves`() {
        val dispatcher = dispatcher()
        val request = request()

        dispatcher.launch(request)

        assertTrue(dispatcher.admits(cores = 3, memoryMb = 3000.0))
        assertTrue(!dispatcher.admits(cores = 4, memoryMb = 1.0), "one core is already spoken for")
        assertTrue(!dispatcher.admits(cores = 1, memoryMb = 3400.0), "768 MB is already spoken for")
        assertEquals(1, dispatcher.capacity().allocatedCores)
        dispatcher.cancel(request.executionId)
    }

    // Otherwise work larger than the machine waits for room that will never appear.
    @Test
    fun `admits work larger than itself when nothing else is running`() {
        assertTrue(dispatcher().admits(cores = 64, memoryMb = 100_000.0))
    }

    @Test
    fun `knows nothing of an execution from an earlier lifetime`() {
        val id = UUID.randomUUID()

        assertEquals(mapOf(id to PlatformVerdict.Unknown), dispatcher().reconcile(listOf(id)))
    }

    @Test
    fun `accepts an execution it already holds without starting it twice`() {
        val dispatcher = dispatcher()
        val request = request()

        assertEquals(Launch.Accepted, dispatcher.launch(request))
        assertEquals(Launch.Accepted, dispatcher.launch(request))
        assertEquals(1, dispatcher.capacity().allocatedCores)
        dispatcher.cancel(request.executionId)
    }

    private fun decode(file: Path): UnitOutcome = SdkJson.json.decodeFromString(UnitOutcome.serializer(), file.readText())

    private fun task(): TaskSpec =
        TaskSpec(
            id = 0,
            submissionTime = 0.ms,
            duration = 60_000.ms,
            cpuCoreCount = 1,
            cpuCapacity = 1000.mhz,
            memory = 0.mib,
            fragments = listOf(TaskFragmentSpec(duration = 60_000.ms, cpuUsage = 1000.mhz)),
        )

    /** Stands in for a launcher that is still working, so a kill is what ends it. */
    object SleepingLauncher {
        @JvmStatic
        fun main(args: Array<String>) {
            Thread.sleep(120_000)
        }
    }

    /** Stands in for a launcher that says something and leaves its memory report where it was started. */
    object ReportingLauncher {
        @JvmStatic
        fun main(args: Array<String>) {
            println(REPORTED_LINE)
            Files.writeString(
                Path.of(PEAK_MEMORY_FILE),
                SdkJson.json.encodeToString(PeakMemory.serializer(), PeakMemory.Measured(100.0, 50.0)),
            )
        }
    }

    /** Stands in for a launcher that exits without leaving anything behind. */
    object SilentLauncher {
        @JvmStatic
        fun main(args: Array<String>) {}
    }

    private companion object {
        const val REPORTED_LINE = "the launcher was here"
    }
}
