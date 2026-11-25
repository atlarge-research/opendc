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

package org.opendc.sdk.python

import org.apache.parquet.hadoop.ParquetFileReader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.common.units.TimeDelta
import org.opendc.sdk.model.carbon.loader.CarbonTraceLoader
import org.opendc.sdk.model.checkpoint.CheckpointModelSpec
import org.opendc.sdk.model.experiment.ExperimentSpec
import org.opendc.sdk.model.experiment.expand
import org.opendc.sdk.model.failure.TraceBasedFailureSpec
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.resource.UriReference
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicySpec
import org.opendc.sdk.model.scheduler.SchedulerPrefabSpec
import org.opendc.sdk.model.topology.ClusterSpec
import org.opendc.sdk.model.topology.CpuSpec
import org.opendc.sdk.model.topology.DataCenterSpec
import org.opendc.sdk.model.topology.EqualSharePolicySpec
import org.opendc.sdk.model.topology.FixedSharePolicySpec
import org.opendc.sdk.model.topology.HostSpec
import org.opendc.sdk.model.topology.MemorySpec
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.workload.InlineWorkloadSpec
import org.opendc.sdk.model.workload.NO_DEADLINE
import org.opendc.sdk.model.workload.TaskFragmentSpec
import org.opendc.sdk.model.workload.TaskSpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec
import org.opendc.sdk.model.workload.loader.WorkloadLoader
import org.opendc.sdk.runner.OpenDC
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.sdk.runner.telemetry.table.host.HostSample
import org.opendc.trace.formats.workload.parquet.TASK_SCHEMA
import org.opendc.trace.parquet.LocalInputFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

/**
 * Calls the bridge with arguments shaped as JPype passes them from Python: ints as `Long`, floats as `Double`, lists as
 * `List` and dicts as `Map`.
 */
class PythonInteropTest {
    private val task =
        TaskSpec(
            id = 1,
            submissionTime = TimeDelta.ofMin(0),
            duration = TimeDelta.ofMin(10),
            cpuCoreCount = 4,
            cpuCapacity = Frequency.ofGHz(2),
            memory = DataSize.ofGiB(8),
            fragments = listOf(TaskFragmentSpec(TimeDelta.ofMin(10), Frequency.ofGHz(1))),
            parents = intArrayOf(0),
        )

    private val host = HostSpec(cpu = CpuSpec(8, Frequency.ofGHz(3)), memory = MemorySpec(DataSize.ofGiB(64)))

    private val experiment =
        ExperimentSpec(
            topologies = setOf(TopologySpec(listOf(DataCenterSpec(listOf(ClusterSpec(hosts = listOf(host))))))),
            workloads = setOf(InlineWorkloadSpec(listOf(task))),
        )

    @Test
    fun `constructs a class from named arguments and applies the Kotlin defaults`() {
        val built = PythonInterop.construct(CpuSpec::class.java, emptyList(), mapOf("core_count" to 8L, "coreSpeed" to Frequency.ofGHz(3)))

        assertEquals(CpuSpec(coreCount = 8, coreSpeed = Frequency.ofGHz(3)), built)
    }

    @Test
    fun `binds positional arguments in declaration order`() {
        assertEquals(FixedSharePolicySpec(0.5), PythonInterop.construct(FixedSharePolicySpec::class.java, listOf(0.5), emptyMap()))
    }

    @Test
    fun `narrows Python ints to the Short and IntArray parameters of a task`() {
        val built =
            PythonInterop.construct(
                TaskSpec::class.java,
                emptyList(),
                mapOf(
                    "id" to 1L,
                    "submission_time" to TimeDelta.ofMin(0),
                    "duration" to TimeDelta.ofMin(10),
                    "cpu_core_count" to 4L,
                    "cpu_capacity" to Frequency.ofGHz(2),
                    "memory" to DataSize.ofGiB(8),
                    "fragments" to listOf(TaskFragmentSpec(TimeDelta.ofMin(10), Frequency.ofGHz(1))),
                    "parents" to listOf(0L),
                ),
            )

        assertEquals(task, built)
    }

    @Test
    fun `rejects an unknown parameter and names the known ones`() {
        val failure =
            assertFailsWith<ArgumentBindingException> {
                PythonInterop.construct(CpuSpec::class.java, emptyList(), mapOf("cores" to 8L, "coreSpeed" to Frequency.ofGHz(3)))
            }

        assertContains(failure.message.orEmpty(), "coreCount")
    }

    @Test
    fun `rejects a call that misses a required parameter`() {
        val failure = assertFailsWith<ArgumentBindingException> { PythonInterop.construct(CpuSpec::class.java, listOf(8L), emptyMap()) }

        assertContains(failure.message.orEmpty(), "missing 'coreSpeed'")
    }

    @Test
    fun `rejects None for a unit instead of letting it become zero`() {
        assertFailsWith<ArgumentBindingException> {
            PythonInterop.construct(CpuSpec::class.java, emptyList(), mapOf("coreCount" to 8L, "coreSpeed" to null))
        }
    }

    @Test
    fun `rejects a bare number for a unit`() {
        assertFailsWith<ArgumentBindingException> { PythonInterop.construct(CpuSpec::class.java, listOf(8L, 3000.0), emptyMap()) }
    }

    @Test
    fun `rejects a fraction for an integer and an integer out of range`() {
        assertFailsWith<ArgumentBindingException> {
            PythonInterop.construct(CpuSpec::class.java, listOf(8.5, Frequency.ofGHz(3)), emptyMap())
        }

        assertFailsWith<ArgumentBindingException> {
            PythonInterop.construct(
                TaskSpec::class.java,
                listOf(1L, TimeDelta.ofMin(0), TimeDelta.ofMin(10), 100_000L, Frequency.ofGHz(2), DataSize.ofGiB(8), emptyList<Any>()),
                emptyMap(),
            )
        }
    }

    @Test
    fun `keeps null and the order of a set whose elements are nullable`() {
        val built =
            PythonInterop.construct(
                ExperimentSpec::class.java,
                emptyList(),
                mapOf(
                    "topologies" to experiment.topologies.toList(),
                    "workloads" to experiment.workloads.toList(),
                    "checkpointModels" to listOf(null, CheckpointModelSpec()),
                ),
            )

        assertEquals(listOf(null, CheckpointModelSpec()), (built as ExperimentSpec).checkpointModels.toList())
    }

    @Test
    fun `coerces the elements of a parameter typed as a concrete collection`() {
        val built = PythonInterop.construct(HostSample::class.java, emptyList(), mapOf("gpu_usages" to listOf(1L, 2L)))

        assertEquals(arrayListOf(1.0, 2.0), (built as HostSample).gpuUsages)
    }

    @Test
    fun `refuses to construct an abstract class`() {
        assertFailsWith<ArgumentBindingException> { PythonInterop.construct(WorkloadLoader::class.java, emptyList(), emptyMap()) }
    }

    @Test
    fun `builds a file from a string`() {
        assertIs<CarbonTraceLoader>(PythonInterop.construct(CarbonTraceLoader::class.java, listOf("carbon.parquet"), emptyMap()))
    }

    @Test
    fun `applies the default arguments a unit inherits from its interface`() {
        val approx = PythonInterop.call(Frequency.ofGHz(1), "approx", listOf(Frequency.ofGHz(1)), mapOf("min_epsilon" to 0.5))

        assertEquals(true, approx)
    }

    @Test
    fun `calls the members of a private class through its public interface`(
        @TempDir root: Path,
    ) {
        val resource = FileSystemResourceProvisioner(root).provision(NamedReference("."))

        assertEquals(Attribute.Value(root.resolve(".")), PythonInterop.attribute(resource, "path"))
    }

    @Test
    fun `reads an enum constant from its name`() {
        val built = PythonInterop.construct(PrefabAllocationPolicySpec::class.java, emptyList(), mapOf("prefab_name" to "core_mem"))

        assertEquals(PrefabAllocationPolicySpec(SchedulerPrefabSpec.CoreMem), built)
    }

    @Test
    fun `resolves a folded name but refuses names that differ only in case`() {
        assertEquals(3000.0, PythonInterop.call(Frequency.ofGHz(3), "to_mhz", emptyList(), emptyMap()))

        val failure = assertFailsWith<ArgumentBindingException> { PythonInterop.call(DataSize.ofGiB(4), "to_mib", emptyList(), emptyMap()) }
        assertContains(failure.message.orEmpty(), "toMiB")

        assertEquals(4096.0, PythonInterop.call(DataSize.ofGiB(4), "toMiB", emptyList(), emptyMap()))
    }

    @Test
    fun `prefers the overload that needs no default arguments`() {
        assertEquals("one", PythonInterop.call(Overloads(), "pick", listOf(1L), emptyMap()))
        assertEquals("two", PythonInterop.call(Overloads(), "pick", listOf(1L, 3L), emptyMap()))
    }

    @Test
    fun `calls an extension function of the SDK on its receiver`() {
        assertEquals(experiment.expand(), PythonInterop.call(experiment, "expand", emptyList(), emptyMap()))
    }

    @Test
    fun `resolves classes, objects and top-level values of the SDK by name`() {
        assertEquals(Attribute.Type(HostSpec::class.java), PythonInterop.sdkMember("HostSpec"))
        assertEquals(Attribute.Value(EqualSharePolicySpec), PythonInterop.sdkMember("EqualSharePolicySpec"))
        assertEquals(Attribute.Value(NO_DEADLINE), PythonInterop.sdkMember("NO_DEADLINE"))
        assertEquals(Attribute.TopLevelFunction("generateTopology"), PythonInterop.sdkMember("generate_topology"))
        assertEquals(Attribute.Type(OpenDC::class.java), PythonInterop.sdkMember("OpenDC"))
    }

    @Test
    fun `resolves enum constants, nested classes and companion members of a class`() {
        assertEquals(Attribute.Value(SchedulerPrefabSpec.CoreMem), PythonInterop.classAttribute(SchedulerPrefabSpec::class.java, "CoreMem"))
        assertEquals(
            Attribute.MemberFunction(Frequency.Companion, "ofGHz"),
            PythonInterop.classAttribute(Frequency::class.java, "of_ghz"),
        )
        assertEquals(Attribute.Type(OpenDC.Builder::class.java), PythonInterop.classAttribute(OpenDC::class.java, "Builder"))
        assertEquals(Attribute.MemberFunction(OpenDC.Companion, "builder"), PythonInterop.classAttribute(OpenDC::class.java, "builder"))
    }

    @Test
    fun `describes a class whose companion object is private`() {
        assertEquals("hosts", PythonInterop.describe(ClusterSpec::class.java).constructorParameters[2].name)
        assertFailsWith<ArgumentBindingException> { PythonInterop.classAttribute(ClusterSpec::class.java, "logger") }
    }

    @Test
    fun `reads a property and resolves a function of an object`() {
        assertEquals(Attribute.Value(Frequency.ofGHz(3)), PythonInterop.attribute(host.cpu, "core_speed"))
        assertEquals(Attribute.MemberFunction(host, "copy"), PythonInterop.attribute(host, "copy"))
    }

    @Test
    fun `converts a string to a path`() {
        val builder = OpenDC.builder()

        assertSame(builder, PythonInterop.call(builder, "output", listOf("opendc-output"), emptyMap()))
    }

    @Test
    fun `rethrows what the called function throws`() {
        val failure =
            assertFailsWith<IllegalArgumentException> { PythonInterop.call(OpenDC.builder(), "parallelism", listOf(0L), emptyMap()) }

        // Not an ArgumentBindingException, which Python would raise as a TypeError
        assertEquals(IllegalArgumentException::class, failure::class)
        assertContains(failure.message.orEmpty(), "parallelism must be between")
    }

    @Test
    fun `describes the constructor and the members Python lists`() {
        val members = PythonInterop.describe(HostSpec::class.java)

        assertEquals(listOf("name", "count", "cpu", "memory"), members.constructorParameters.take(4).map { it.name })
        assertEquals(listOf(true, true, false, false), members.constructorParameters.take(4).map { it.isOptional })
        assertContains(members.instanceAttributes, "copy")
        assertEquals(false, "component1" in members.instanceAttributes)
    }

    @Test
    fun `provisions a staged trace by its name or URI`(
        @TempDir root: Path,
    ) {
        val staged = root.resolve("trace-0")
        val inPlace = root.resolve("carbon.parquet")
        val passed = mapOf("bitbrains" to staged.toString(), "file:///traces/carbon.parquet" to inPlace.toString())
        val traces = StagedTraces(passed, FileSystemResourceProvisioner(root))

        assertEquals(staged, traces.provision(NamedReference("bitbrains")).path)
        assertEquals(inPlace, traces.provision(UriReference("file:///traces/carbon.parquet")).path)
    }

    @Test
    fun `provisions a trace that was not passed from the input root`(
        @TempDir root: Path,
    ) {
        val onDisk = Files.createDirectory(root.resolve("surf"))
        val traces = StagedTraces(mapOf("bitbrains" to root.resolve("trace-0").toString()), FileSystemResourceProvisioner(root))

        assertEquals(onDisk, traces.provision(NamedReference("surf")).path)

        val failure = assertFailsWith<IllegalArgumentException> { traces.provision(NamedReference("missing")) }
        assertContains(failure.message.orEmpty(), root.resolve("missing").toString())
        assertContains(failure.message.orEmpty(), "bitbrains")
    }

    @Test
    fun `renames the references to passed traces to their library names`() {
        val workload = TraceWorkloadSpec(NamedReference("local"))
        val failures = TraceBasedFailureSpec(UriReference("file:///failures.parquet"))
        val kept = TraceWorkloadSpec(NamedReference("bitbrains-small"))
        val local = experiment.copy(workloads = setOf(workload, kept), failureModels = setOf(failures))

        val renamed = local.withLibraryNames(mapOf("local" to "alice/local-1a2b", "file:///failures.parquet" to "alice/failures-3c4d"))

        assertEquals(
            listOf(NamedReference("alice/local-1a2b"), NamedReference("bitbrains-small"), NamedReference("alice/failures-3c4d")),
            renamed.references().map { it.reference },
        )
    }

    @Test
    fun `writes an empty table whose schema reads back as the canonical one`(
        @TempDir root: Path,
    ) {
        val target = root.resolve("tasks.parquet")

        TraceTable.TASK.writeEmpty(target)

        val schema = ParquetFileReader.open(LocalInputFile(target)).use { it.footer.fileMetaData.schema }
        assertEquals(TASK_SCHEMA, schema)
    }
}

/** Two overloads that a call with one argument both fit; Kotlin picks the one without default arguments. */
class Overloads {
    fun pick(value: Int): String = "one"

    fun pick(
        value: Int,
        scale: Int = 2,
    ): String = "two"
}
