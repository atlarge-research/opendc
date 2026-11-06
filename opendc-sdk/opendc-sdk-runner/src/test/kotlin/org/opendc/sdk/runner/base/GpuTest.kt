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

package org.opendc.sdk.runner.base

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.opendc.common.units.DataRate
import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.common.units.Power
import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.sdk.model.topology.BestEffortPolicySpec
import org.opendc.sdk.model.topology.ClusterSpec
import org.opendc.sdk.model.topology.ConstantVirtualizationOverheadSpec
import org.opendc.sdk.model.topology.CpuSpec
import org.opendc.sdk.model.topology.DataCenterSpec
import org.opendc.sdk.model.topology.DistributionPolicySpec
import org.opendc.sdk.model.topology.EqualSharePolicySpec
import org.opendc.sdk.model.topology.FirstFitPolicySpec
import org.opendc.sdk.model.topology.FixedSharePolicySpec
import org.opendc.sdk.model.topology.GpuSpec
import org.opendc.sdk.model.topology.HostSpec
import org.opendc.sdk.model.topology.LinearPowerModelSpec
import org.opendc.sdk.model.topology.MaxMinFairnessPolicySpec
import org.opendc.sdk.model.topology.MemorySpec
import org.opendc.sdk.model.topology.NoVirtualizationOverheadSpec
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.workload.TaskSpec
import org.opendc.sdk.runner.base.harness.TestMetricExporter
import org.opendc.sdk.runner.base.harness.createTestTask
import org.opendc.sdk.runner.base.harness.fragment
import org.opendc.sdk.runner.base.harness.runTest

/**
 * Tests for hosts with GPUs.
 *
 * The first tests check that the GPU of a host is read correctly from JSON, including the defaults of the fields that
 * are left out. The other tests run tasks on hosts with GPUs of one 2 GHz core each (see [gpuTopology]).
 *
 * The GPUs of a host act as one large GPU: with the default max-min fairness policy, the GPU demand of all tasks on a
 * host is spread evenly over its GPUs. The last tests check the other GPU distribution policies on more than four GPUs.
 */
class GpuTest {
    /** The GPU read from JSON that only specifies its cores, with every other field spelled out at its default. */
    private val minimalGpu =
        GpuSpec(
            coreCount = 1,
            coreSpeed = Frequency.ofGHz(2),
            count = 1,
            // A negative memory size and bandwidth mean "unspecified"
            memory = DataSize.ofMiB(-1),
            memoryBandwidth = DataRate.ofKibps(-1),
            vendor = "unknown",
            modelName = "unknown",
            architecture = "unknown",
            virtualizationOverhead = NoVirtualizationOverheadSpec,
        )

    /**
     * A GPU with only its cores specified is a single GPU with unspecified memory, an unknown vendor, and no
     * virtualization overhead.
     */
    @Test
    fun `a GPU with only its cores gets the defaults for all other fields`() {
        val gpu = decodeGpu("""{ "coreCount": 1, "coreSpeed": "2 GHz" }""")

        assertEquals(minimalGpu, gpu)
    }

    /** A GPU with memory but without a vendor gets an unknown vendor. */
    @Test
    fun `a GPU with memory but without a vendor gets the default vendor`() {
        val gpu = decodeGpu("""{ "coreCount": 1, "coreSpeed": "2 GHz", "memory": "4096 MiB", "memoryBandwidth": "500 Kibps" }""")

        assertEquals(minimalGpu.copy(memory = DataSize.ofMiB(4096), memoryBandwidth = DataRate.ofKibps(500)), gpu)
    }

    /** A GPU with a vendor but without memory gets unspecified memory. */
    @Test
    fun `a GPU with a vendor but without memory gets the default memory`() {
        val gpu =
            decodeGpu(
                """{ "coreCount": 1, "coreSpeed": "2 GHz", "vendor": "NVIDIA", "modelName": "Tesla V100", "architecture": "Volta" }""",
            )

        assertEquals(minimalGpu.copy(vendor = "NVIDIA", modelName = "Tesla V100", architecture = "Volta"), gpu)
    }

    /**
     * A fully specified GPU: the eight NVIDIA Tesla V100s of a DGX-1, with 5120 CUDA cores at 1530 MHz, 32 GiB of memory
     * and 900 GB/s of memory bandwidth each.
     * Every field is read, and every quantity is converted from the unit it is written in.
     */
    @Test
    fun `a fully specified GPU is read with all its fields`() {
        val gpu =
            decodeGpu(
                """
                {
                    "count": 8,
                    "coreCount": 5120,
                    "coreSpeed": "1530 MHz",
                    "memory": "32 GiB",
                    "memoryBandwidth": "900 GBps",
                    "vendor": "NVIDIA",
                    "modelName": "Tesla V100",
                    "architecture": "Volta",
                    "virtualizationOverhead": { "type": "constant" }
                }
                """,
            )

        val expected =
            GpuSpec(
                coreCount = 5120,
                coreSpeed = Frequency.ofMHz(1530),
                count = 8,
                memory = DataSize.ofGiB(32),
                memoryBandwidth = DataRate.ofGBps(900),
                vendor = "NVIDIA",
                modelName = "Tesla V100",
                architecture = "Volta",
                virtualizationOverhead = ConstantVirtualizationOverheadSpec(),
            )

        assertEquals(expected, gpu)
    }

    /**
     * Two tasks each demand 2000 MHz of GPU on a host with two GPUs of 2000 MHz.
     * The GPUs act as one large GPU of 4000 MHz, so both tasks get their full demand and both GPUs run at full load.
     * Each task also uses one CPU core at 1000 MHz, so the two 2 GHz cores of the host run at half load.
     */
    @Test
    fun `two tasks share two GPUs as if they were one large GPU`() {
        val workload = listOf(tenMinuteGpuTask(id = 0, gpuUsage = 2000.0), tenMinuteGpuTask(id = 1, gpuUsage = 2000.0))

        val monitor = runTest(gpuTopology(gpuCount = 2, cpuCoreCount = 2), workload)

        assertAll(
            { assertEquals(10 * MINUTE, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertTaskUsage(monitor, taskId = 0, cpu = 1000.0, gpu = 2000.0) },
            { assertTaskUsage(monitor, taskId = 1, cpu = 1000.0, gpu = 2000.0) },
            { assertEquals(2000.0, monitor.hostCpuDemands.getValue(HOST)[0]) { "CPU demand of the host incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied.getValue(HOST)[0]) { "CPU supply of the host incorrect" } },
            { assertEquals(listOf(2000.0, 2000.0), monitor.hostGpuDemands.getValue(HOST)[0]) { "GPU demands of the host incorrect" } },
            { assertEquals(listOf(2000.0, 2000.0), monitor.hostGpuSupplied.getValue(HOST)[0]) { "GPU supplies of the host incorrect" } },
            {
                assertEquals(HALF_LOAD_POWER + 2 * FULL_LOAD_POWER, monitor.hostPowerDraws.getValue(HOST)[0]) {
                    "Power draw of the host incorrect"
                }
            },
            // Once both tasks have finished, the host has nothing left to do
            { assertEquals(0.0, monitor.hostCpuDemands.getValue(HOST).last()) { "Final CPU demand of the host incorrect" } },
            { assertEquals(listOf(0.0, 0.0), monitor.hostGpuDemands.getValue(HOST).last()) { "Final GPU demands of the host incorrect" } },
        )
    }

    /**
     * A single 10 min task demands half of the total GPU capacity of a host, which is 1000 MHz for each of its GPUs.
     * Whatever the number of GPUs, the demand is spread evenly, so every GPU runs at half load for the full 10 min.
     * Together with the CPU, which also runs at half load, the host draws [HALF_LOAD_POWER] for the CPU and every GPU.
     */
    @ParameterizedTest(name = "{0} GPUs")
    @ValueSource(ints = [1, 2, 3, 4, 5, 6, 8, 16])
    fun `the GPU demand of a task is spread evenly over all GPUs of a host`(gpuCount: Int) {
        val gpuDemand = gpuCount * 1000.0

        val monitor = runTest(gpuTopology(gpuCount), listOf(tenMinuteGpuTask(gpuUsage = gpuDemand)))

        val runtime = 10 * MINUTE
        val halfLoadPerGpu = List(gpuCount) { 1000.0 }
        val powerDraw = (1 + gpuCount) * HALF_LOAD_POWER

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertTaskUsage(monitor, taskId = 0, cpu = 1000.0, gpu = gpuDemand) },
            { assertEquals(halfLoadPerGpu, monitor.hostGpuDemands.getValue(HOST)[0]) { "GPU demands of the host incorrect" } },
            { assertEquals(halfLoadPerGpu, monitor.hostGpuSupplied.getValue(HOST)[0]) { "GPU supplies of the host incorrect" } },
            {
                assertEquals(List(gpuCount) { runtime / 2 }, monitor.hostGpuActiveTimes.getValue(HOST).last()) {
                    "GPU active times of the host incorrect"
                }
            },
            { assertEquals(powerDraw, monitor.hostPowerDraws.getValue(HOST)[0]) { "Power draw of the host incorrect" } },
            {
                assertEquals(energy(powerDraw, runtime), monitor.hostEnergyUsages.getValue(HOST).last()) {
                    "Total energy usage of the host incorrect"
                }
            },
        )
    }

    /**
     * A single 10 min task demands 15000 MHz of GPU on a host with 5 GPUs of 2000 MHz, 1.5 times their total capacity.
     * Every GPU is asked for 3000 MHz but runs at full load, so the task gets the total capacity of 10000 MHz.
     * With the default scaling policy, the task still finishes after 10 min.
     */
    @Test
    fun `a task that demands more than the total GPU capacity runs all GPUs at full load`() {
        val monitor = runTest(gpuTopology(gpuCount = 5), listOf(tenMinuteGpuTask(gpuUsage = 15000.0)))

        assertAll(
            { assertEquals(10 * MINUTE, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(15000.0, monitor.taskGpuDemands.getValue(0)[0]) { "GPU demand of the task incorrect" } },
            { assertEquals(10000.0, monitor.taskGpuSupplied.getValue(0)[0]) { "GPU supply of the task incorrect" } },
            { assertEquals(List(5) { 3000.0 }, monitor.hostGpuDemands.getValue(HOST)[0]) { "GPU demands of the host incorrect" } },
            { assertEquals(List(5) { 2000.0 }, monitor.hostGpuSupplied.getValue(HOST)[0]) { "GPU supplies of the host incorrect" } },
            {
                assertEquals(HALF_LOAD_POWER + 5 * FULL_LOAD_POWER, monitor.hostPowerDraws.getValue(HOST)[0]) {
                    "Power draw of the host incorrect"
                }
            },
        )
    }

    /**
     * A single 10 min task that only uses the CPU, at half load, runs on a host with 8 GPUs.
     * The GPUs are not used, but each of them still draws its [IDLE_POWER].
     */
    @Test
    fun `idle GPUs draw their idle power`() {
        val workload = listOf(createTestTask(id = 0, fragments = listOf(fragment(10 * MINUTE, 1000.0))))

        val monitor = runTest(gpuTopology(gpuCount = 8), workload)

        val runtime = 10 * MINUTE
        val powerDraw = HALF_LOAD_POWER + 8 * IDLE_POWER

        assertAll(
            { assertEquals(runtime, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(List(8) { 0.0 }, monitor.hostGpuSupplied.getValue(HOST)[0]) { "GPU supplies of the host incorrect" } },
            { assertEquals(powerDraw, monitor.hostPowerDraws.getValue(HOST)[0]) { "Power draw of the host incorrect" } },
            {
                assertEquals(energy(powerDraw, runtime), monitor.hostEnergyUsages.getValue(HOST).last()) {
                    "Total energy usage of the host incorrect"
                }
            },
        )
    }

    /**
     * Three identical hosts with 5 GPUs each run one 10 min task each, which demands 5000 MHz of GPU. Every host has a
     * single CPU core, so the scheduler places one task on every host.
     * The ids of the GPUs are numbered across all hosts, but every host should spread the demand of its own task evenly
     * over its own 5 GPUs.
     */
    @Test
    fun `every host spreads the GPU demand of its task over its own GPUs`() {
        val workload = List(3) { tenMinuteGpuTask(id = it, gpuUsage = 5000.0) }

        val monitor = runTest(gpuTopology(gpuCount = 5, hostCount = 3), workload)

        val hostAssertions =
            monitor.hostGpuSupplied.map { (host, supplies) ->
                { assertEquals(List(5) { 1000.0 }, supplies[0]) { "GPU supplies of host $host incorrect" } }
            }
        val taskAssertions = List(3) { taskId -> { assertTaskUsage(monitor, taskId, cpu = 1000.0, gpu = 5000.0) } }

        assertAll(
            { assertEquals(10 * MINUTE, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(3, monitor.hostGpuSupplied.size) { "Number of hosts incorrect" } },
            { assertAll("GPU supplies of the hosts", hostAssertions) },
            { assertAll("Usage of the tasks", taskAssertions) },
        )
    }

    /** Max-min fairness spreads the 7000 MHz that both tasks demand evenly over the 5 GPUs, and gives every task its demand. */
    @Test
    fun `max-min fairness spreads the GPU demand evenly over more than four GPUs`() {
        val monitor = runOnFiveGpus(MaxMinFairnessPolicySpec)

        assertGpuSupplies(monitor, gpus = List(5) { 1400.0 }, tasks = listOf(5000.0, 2000.0))
    }

    /** First fit fills the GPUs in order: the 7000 MHz that both tasks demand fills the first 3.5 GPUs. */
    @Test
    fun `first fit fills more than four GPUs in order`() {
        val monitor = runOnFiveGpus(FirstFitPolicySpec)

        assertGpuSupplies(monitor, gpus = listOf(2000.0, 2000.0, 2000.0, 1000.0, 0.0), tasks = listOf(5000.0, 2000.0))
    }

    /**
     * Equal share gives every task the same share of the total GPU capacity, regardless of its demand: both tasks get
     * 5000 MHz, so the task that demands 2000 MHz gets more than it demands. All GPUs run at full load.
     */
    @Test
    fun `equal share gives every task the same share of more than four GPUs`() {
        val monitor = runOnFiveGpus(EqualSharePolicySpec)

        assertGpuSupplies(monitor, gpus = List(5) { 2000.0 }, tasks = listOf(5000.0, 5000.0))
    }

    /**
     * Best effort has enough GPU capacity to give every task its demand. Which GPUs supply it is not part of the policy,
     * so only their total supply is checked.
     */
    @Test
    fun `best effort gives every task its demand from more than four GPUs`() {
        val monitor = runOnFiveGpus(BestEffortPolicySpec())

        assertAll(
            { assertEquals(5000.0, monitor.taskGpuSupplied.getValue(0)[0]) { "GPU supply of task 0 incorrect" } },
            { assertEquals(2000.0, monitor.taskGpuSupplied.getValue(1)[0]) { "GPU supply of task 1 incorrect" } },
            { assertEquals(7000.0, monitor.hostGpuSupplied.getValue(HOST)[0].sum()) { "Total GPU supply of the host incorrect" } },
        )
    }

    /**
     * Fixed share with a share ratio of 0.5 gives both tasks the same, fixed share of the GPUs, regardless of their
     * demand. Whatever that share is, the GPUs should supply exactly what the tasks get.
     *
     * TODO: The fixed share is computed as `shareRatio * capacity / numSuppliers`, and used both as the supply of every
     *  task and as the demand pushed to every GPU. These only match when a host has as many GPUs as tasks: here, both
     *  tasks get 1000 MHz, while each of the 5 GPUs supplies 1000 MHz. `DistributionPoliciesTest` already shows this
     *  with one task on two GPUs, but asserts it as correct. It is also unclear which share is intended: the class
     *  documentation of `FixedShareFlowDistributor` says a share of the total GPU capacity (5000 MHz here), while
     *  `DistributionPoliciesTest` expects a share of a single GPU (1000 MHz here).
     */
    @Disabled("Fixed share takes more from the GPUs than it gives to the tasks, see the TODO")
    @Test
    fun `fixed share gives the tasks exactly what it takes from the GPUs`() {
        val monitor = runOnFiveGpus(FixedSharePolicySpec(shareRatio = 0.5))

        val taskSupplies = listOf(0, 1).map { monitor.taskGpuSupplied.getValue(it)[0] }

        assertAll(
            { assertEquals(taskSupplies[0], taskSupplies[1]) { "GPU supplies of the tasks should be equal" } },
            {
                assertEquals(taskSupplies.sum(), monitor.hostGpuSupplied.getValue(HOST)[0].sum()) {
                    "Total GPU supply of the host should equal the total GPU supply of the tasks"
                }
            },
        )
    }

    /**
     * Runs two 10 min tasks that demand 5000 and 2000 MHz of GPU, 7000 of the 10000 MHz in total, on a host with 2 CPU
     * cores and 5 GPUs that uses [gpuPolicy] to distribute the GPUs over the tasks.
     */
    private fun runOnFiveGpus(gpuPolicy: DistributionPolicySpec): TestMetricExporter {
        val workload = listOf(tenMinuteGpuTask(id = 0, gpuUsage = 5000.0), tenMinuteGpuTask(id = 1, gpuUsage = 2000.0))

        return runTest(gpuTopology(gpuCount = 5, cpuCoreCount = 2, gpuPolicy = gpuPolicy), workload)
    }

    /**
     * A topology with [hostCount] identical hosts, named after [HOST], that have [cpuCoreCount] CPU cores of 2 GHz and
     * [gpuCount] GPUs of one 2 GHz core each, distributed over their tasks by [gpuPolicy]. The CPU and every GPU have a
     * linear power model from 100 to 200 W.
     */
    private fun gpuTopology(
        gpuCount: Int,
        cpuCoreCount: Int = 1,
        hostCount: Int = 1,
        gpuPolicy: DistributionPolicySpec = MaxMinFairnessPolicySpec,
    ): TopologySpec {
        val powerModel = LinearPowerModelSpec(idlePower = Power.ofWatts(100.0), maxPower = Power.ofWatts(200.0))

        return TopologySpec(
            datacenters =
                listOf(
                    DataCenterSpec(
                        clusters =
                            listOf(
                                ClusterSpec(
                                    name = "C01",
                                    hosts =
                                        listOf(
                                            HostSpec(
                                                name = HOST,
                                                count = hostCount,
                                                cpu = CpuSpec(coreCount = cpuCoreCount, coreSpeed = Frequency.ofGHz(2)),
                                                memory = MemorySpec(size = DataSize.ofGB(128)),
                                                gpu = GpuSpec(coreCount = 1, coreSpeed = Frequency.ofGHz(2), count = gpuCount),
                                                cpuPowerModel = powerModel,
                                                gpuPowerModel = powerModel,
                                                gpuDistribution = gpuPolicy,
                                            ),
                                        ),
                                ),
                            ),
                    ),
                ),
        )
    }

    /** A task that runs for 10 min on one CPU core at 1000 MHz, and on the GPUs at [gpuUsage] MHz. */
    private fun tenMinuteGpuTask(
        id: Int = 0,
        gpuUsage: Double,
    ): TaskSpec = createTestTask(id = id, fragments = listOf(fragment(10 * MINUTE, 1000.0, gpuUsage)), gpuCoreCount = 1)

    /** Reads a [GpuSpec] from [json], written as the `gpu` of a host in a topology file. */
    private fun decodeGpu(json: String): GpuSpec = SdkJson.json.decodeFromString(json)

    /** The energy in J used when drawing [power] W for [time] ms. */
    private fun energy(
        power: Double,
        time: Long,
    ): Double = power * time / SECOND

    /**
     * Asserts that task [taskId] demanded and was supplied [cpu] MHz of CPU and [gpu] MHz of GPU in its first sample, and
     * nothing in its last sample, after it finished.
     */
    private fun assertTaskUsage(
        monitor: TestMetricExporter,
        taskId: Int,
        cpu: Double,
        gpu: Double,
    ) {
        assertAll(
            "Usage of task $taskId",
            { assertEquals(cpu, monitor.taskCpuDemands.getValue(taskId)[0]) { "CPU demand incorrect" } },
            { assertEquals(cpu, monitor.taskCpuSupplied.getValue(taskId)[0]) { "CPU supply incorrect" } },
            { assertEquals(gpu, monitor.taskGpuDemands.getValue(taskId)[0]) { "GPU demand incorrect" } },
            { assertEquals(gpu, monitor.taskGpuSupplied.getValue(taskId)[0]) { "GPU supply incorrect" } },
            { assertEquals(0.0, monitor.taskCpuDemands.getValue(taskId).last()) { "Final CPU demand incorrect" } },
            { assertEquals(0.0, monitor.taskCpuSupplied.getValue(taskId).last()) { "Final CPU supply incorrect" } },
            { assertEquals(0.0, monitor.taskGpuDemands.getValue(taskId).last()) { "Final GPU demand incorrect" } },
            { assertEquals(0.0, monitor.taskGpuSupplied.getValue(taskId).last()) { "Final GPU supply incorrect" } },
        )
    }

    /** Asserts the GPU supply in MHz of every GPU of [HOST], and of every task, in their first sample. */
    private fun assertGpuSupplies(
        monitor: TestMetricExporter,
        gpus: List<Double>,
        tasks: List<Double>,
    ) {
        val taskSupplies = tasks.indices.map { monitor.taskGpuSupplied.getValue(it)[0] }

        assertAll(
            "GPU supplies",
            { assertEquals(gpus, monitor.hostGpuSupplied.getValue(HOST)[0]) { "GPU supplies of the host incorrect" } },
            { assertEquals(tasks, taskSupplies) { "GPU supplies of the tasks incorrect" } },
        )
    }

    private companion object {
        /** All times in these tests are in milliseconds. */
        const val SECOND = 1_000L
        const val MINUTE = 60 * SECOND

        /** The host in [gpuTopology], or the first one if it has more. */
        const val HOST = "H01"

        /** The power draw of an idle CPU or GPU of [HOST]. Their linear power models go from 100 to 200 W. */
        const val IDLE_POWER = 100.0

        /** The power draw of a CPU or GPU of [HOST] at half its capacity. */
        const val HALF_LOAD_POWER = 150.0

        /** The power draw of a CPU or GPU of [HOST] at its full capacity. */
        const val FULL_LOAD_POWER = 200.0
    }
}
