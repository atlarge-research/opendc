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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicy
import org.opendc.sdk.model.scheduler.SchedulerName
import org.opendc.sdk.runner.base.harness.createTestTask
import org.opendc.sdk.runner.base.harness.createTopology
import org.opendc.sdk.runner.base.harness.fragment
import org.opendc.sdk.runner.base.harness.runTest

/**
 * Integration tests ported one-to-one from `opendc-experiments-base`'s `ScenarioRunnerTest`,
 * driving the SDK runner and asserting the identical values.
 */
class ScenarioRunnerTest {
    @Test
    fun testScenario1() {
        val workload = listOf(createTestTask(id = 0, fragments = listOf(fragment(10 * 60 * 1000, 1000.0)), cpuCoreCount = 1))
        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(10 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((10 * 30000)).toLong(), monitor.hostCpuIdleTimes["H01"]?.sum()) { "Idle time incorrect" } },
            { assertEquals((10 * 30000).toLong(), monitor.hostCpuActiveTimes["H01"]?.sum()) { "Active time incorrect" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect host energy usage at timestamp 0" } },
            { assertEquals(600 * 150.0, monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect host energy usage" } },
            { assertEquals(600 * 150.0, monitor.energyUsages.sum()) { "Incorrect total energy usage" } },
        )
    }

    @Test
    fun testScenario2() {
        val workload =
            listOf(
                createTestTask(id = 0, fragments = listOf(fragment(10 * 60 * 1000, 1000.0)), cpuCoreCount = 1),
                createTestTask(id = 1, fragments = listOf(fragment(5 * 60 * 1000, 2000.0)), cpuCoreCount = 1),
            )
        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload, allocationPolicy = PrefabAllocationPolicy(SchedulerName.TaskNumMemorizing))

        assertAll(
            { assertEquals(15 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((10 * 30000)).toLong(), monitor.hostCpuIdleTimes["H01"]?.sum()) { "Idle time incorrect" } },
            { assertEquals(((10 * 30000) + (5 * 60000)).toLong(), monitor.hostCpuActiveTimes["H01"]?.sum()) { "Active time incorrect" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect energy usage" } },
            { assertEquals((600 * 150.0) + (300 * 200.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect energy usage" } },
            { assertEquals((600 * 150.0) + (300 * 200.0), monitor.energyUsages.sum()) { "Incorrect energy usage" } },
        )
    }

    @Test
    fun testScenario3() {
        val workload =
            listOf(
                createTestTask(id = 0, fragments = listOf(fragment(10 * 60 * 1000, 1000.0)), cpuCoreCount = 1),
                createTestTask(id = 1, fragments = listOf(fragment(10 * 60 * 1000, 1000.0)), cpuCoreCount = 1),
            )
        val topology = createTopology("single_2_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(10 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((10 * 30000)).toLong(), monitor.hostCpuIdleTimes["H01"]?.sum()) { "Idle time incorrect" } },
            { assertEquals(((10 * 30000)).toLong(), monitor.hostCpuActiveTimes["H01"]?.sum()) { "Active time incorrect" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect energy usage" } },
            { assertEquals((600 * 150.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect energy usage" } },
            { assertEquals((600 * 150.0), monitor.energyUsages.sum()) { "Incorrect energy usage" } },
        )
    }

    @Test
    fun testScenario4() {
        val workload =
            listOf(
                createTestTask(id = 0, fragments = listOf(fragment(10 * 60 * 1000, 1000.0)), cpuCoreCount = 1),
                createTestTask(
                    id = 1,
                    fragments = listOf(fragment(5 * 60 * 1000, 2000.0)),
                    cpuCoreCount = 1,
                    submissionTime = "1970-01-01T00:20",
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(25 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((10 * 30000) + (10 * 60000)).toLong(), monitor.hostCpuIdleTimes["H01"]?.sum()) { "Idle time incorrect" } },
            { assertEquals(((10 * 30000) + (5 * 60000)).toLong(), monitor.hostCpuActiveTimes["H01"]?.sum()) { "Active time incorrect" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect energy usage" } },
            {
                assertEquals(
                    (600 * 150.0) + (600 * 100.0) + (300 * 200.0),
                    monitor.hostEnergyUsages["H01"]?.sum(),
                ) { "Incorrect energy usage" }
            },
        )
    }

    @Test
    fun testScenario5() {
        val workload =
            listOf(
                createTestTask(id = 0, fragments = listOf(fragment(10 * 60 * 1000, 0.0, 1000.0)), cpuCoreCount = 0, gpuCoreCount = 1),
            )
        val topology = createTopology("Gpus/single_gpu_no_vendor_no_memory.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(10 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((10 * 60 * 1000)).toLong(), monitor.hostCpuIdleTimes["H01"]?.sum()) { "CPU Idle time incorrect" } },
            { assertEquals(0L, monitor.hostCpuActiveTimes["H01"]?.sum()) { "CPU Active time incorrect" } },
            {
                assertEquals(
                    ((10 * 30000)).toLong(),
                    monitor.hostGpuIdleTimes["H01"]?.fold(0) { acc, it -> acc + it[0] },
                ) { "GPU Idle time incorrect" }
            },
            {
                assertEquals(
                    ((10 * 30000)).toLong(),
                    monitor.hostGpuActiveTimes["H01"]?.fold(0) { acc, it -> acc + it[0] },
                ) { "GPU Active time incorrect" }
            },
            { assertEquals(2 * 12000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect host energy usage at timestamp 0" } },
            { assertEquals((600 * 100.0) + (600 * 300.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect host energy usage" } },
            { assertEquals((600 * 100.0) + (600 * 300.0), monitor.energyUsages.sum()) { "Incorrect total energy usage" } },
        )
    }

    @Test
    fun testScenario6() {
        val workload =
            listOf(
                createTestTask(id = 0, fragments = listOf(fragment(10 * 60 * 1000, 1000.0, 1000.0)), cpuCoreCount = 1, gpuCoreCount = 1),
            )
        val topology = createTopology("Gpus/single_gpu_no_vendor_no_memory.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(10 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((10 * 30000)).toLong(), monitor.hostCpuIdleTimes["H01"]?.sum()) { "CPU Idle time incorrect" } },
            { assertEquals(((10 * 30000)).toLong(), monitor.hostCpuActiveTimes["H01"]?.sum()) { "CPU Active time incorrect" } },
            {
                assertEquals(
                    ((10 * 30000)).toLong(),
                    monitor.hostGpuIdleTimes["H01"]?.fold(0) { acc, it -> acc + it[0] },
                ) { "GPU Idle time incorrect" }
            },
            {
                assertEquals(
                    ((10 * 30000)).toLong(),
                    monitor.hostGpuActiveTimes["H01"]?.fold(0) { acc, it -> acc + it[0] },
                ) { "GPU Active time incorrect" }
            },
            { assertEquals(27000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect host energy usage at timestamp 0" } },
            { assertEquals((600 * 150.0) + (600 * 300.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect host energy usage" } },
            { assertEquals((600 * 150.0) + (600 * 300.0), monitor.energyUsages.sum()) { "Incorrect total energy usage" } },
        )
    }

    @Test
    fun testScenario7() {
        val workload =
            listOf(
                createTestTask(id = 0, fragments = listOf(fragment(10 * 60 * 1000, 1000.0, 2000.0)), cpuCoreCount = 1, gpuCoreCount = 1),
            )
        val topology = createTopology("Gpus/single_gpu_no_vendor_no_memory.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(10 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((10 * 30000)).toLong(), monitor.hostCpuIdleTimes["H01"]?.sum()) { "CPU Idle time incorrect" } },
            { assertEquals(((10 * 30000)).toLong(), monitor.hostCpuActiveTimes["H01"]?.sum()) { "CPU Active time incorrect" } },
            {
                assertEquals(
                    0L,
                    monitor.hostGpuIdleTimes["H01"]?.fold(0) { acc, it -> acc + it[0] },
                ) { "GPU Idle time incorrect" }
            },
            {
                assertEquals(
                    ((10 * 60000)).toLong(),
                    monitor.hostGpuActiveTimes["H01"]?.fold(0) { acc, it -> acc + it[0] },
                ) { "GPU Active time incorrect" }
            },
            { assertEquals(33000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect host energy usage at timestamp 0" } },
            { assertEquals((600 * 150.0) + (600 * 400.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect host energy usage" } },
            { assertEquals((600 * 150.0) + (600 * 400.0), monitor.energyUsages.sum()) { "Incorrect total energy usage" } },
        )
    }

    @Test
    fun testScenario8() {
        val workload =
            listOf(
                createTestTask(id = 0, fragments = listOf(fragment(10 * 60 * 1000, 2000.0, 1000.0)), cpuCoreCount = 1, gpuCoreCount = 1),
            )
        val topology = createTopology("Gpus/single_gpu_no_vendor_no_memory.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(10 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(0L, monitor.hostCpuIdleTimes["H01"]?.sum()) { "CPU Idle time incorrect" } },
            { assertEquals(((10 * 60000)).toLong(), monitor.hostCpuActiveTimes["H01"]?.sum()) { "CPU Active time incorrect" } },
            {
                assertEquals(
                    ((10 * 30000)).toLong(),
                    monitor.hostGpuIdleTimes["H01"]?.fold(0) { acc, it -> acc + it[0] },
                ) { "GPU Idle time incorrect" }
            },
            {
                assertEquals(
                    ((10 * 30000)).toLong(),
                    monitor.hostGpuActiveTimes["H01"]?.fold(0) { acc, it -> acc + it[0] },
                ) { "GPU Active time incorrect" }
            },
            { assertEquals(30000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect host energy usage at timestamp 0" } },
            { assertEquals((600 * 200.0) + (600 * 300.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect host energy usage" } },
            { assertEquals((600 * 200.0) + (600 * 300.0), monitor.energyUsages.sum()) { "Incorrect total energy usage" } },
        )
    }

    @Test
    fun testScenario9() {
        val workload =
            listOf(
                createTestTask(id = 0, fragments = listOf(fragment(10 * 60 * 1000, 1000.0, 1000.0)), cpuCoreCount = 1, gpuCoreCount = 1),
                createTestTask(id = 1, fragments = listOf(fragment(10 * 60 * 1000, 1000.0, 1000.0)), cpuCoreCount = 1, gpuCoreCount = 1),
            )
        val topology = createTopology("Gpus/single_gpu_no_vendor_no_memory.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(2 * (10 * 60 * 1000), monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((10 * 60000)).toLong(), monitor.hostCpuIdleTimes["H01"]?.sum()) { "CPU Idle time incorrect" } },
            { assertEquals(((10 * 60000)).toLong(), monitor.hostCpuActiveTimes["H01"]?.sum()) { "CPU Active time incorrect" } },
            {
                assertEquals(
                    ((10 * 60000)).toLong(),
                    monitor.hostGpuIdleTimes["H01"]?.fold(0) { acc, it -> acc + it[0] },
                ) { "GPU Idle time incorrect" }
            },
            {
                assertEquals(
                    ((10 * 60000)).toLong(),
                    monitor.hostGpuActiveTimes["H01"]?.fold(0) { acc, it -> acc + it[0] },
                ) { "GPU Active time incorrect" }
            },
            { assertEquals(27000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect host energy usage at timestamp 0" } },
            { assertEquals(2 * ((600 * 150.0) + (600 * 300.0)), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect host energy usage" } },
            { assertEquals(2 * ((600 * 150.0) + (600 * 300.0)), monitor.energyUsages.sum()) { "Incorrect total energy usage" } },
        )
    }

    @Test
    fun testScenario10() {
        val workload =
            listOf(
                createTestTask(id = 0, fragments = listOf(fragment(10 * 60 * 1000, 1000.0, 0.0)), cpuCoreCount = 1, gpuCoreCount = 0),
                createTestTask(id = 1, fragments = listOf(fragment(10 * 60 * 1000, 0.0, 1000.0)), cpuCoreCount = 0, gpuCoreCount = 1),
            )
        val topology = createTopology("Gpus/single_gpu_no_vendor_no_memory.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(10 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((10 * 30000)).toLong(), monitor.hostCpuIdleTimes["H01"]?.sum()) { "CPU Idle time incorrect" } },
            { assertEquals(((10 * 30000)).toLong(), monitor.hostCpuActiveTimes["H01"]?.sum()) { "CPU Active time incorrect" } },
            {
                assertEquals(
                    ((10 * 30000)).toLong(),
                    monitor.hostGpuIdleTimes["H01"]?.fold(0) { acc, it -> acc + it[0] },
                ) { "GPU Idle time incorrect" }
            },
            {
                assertEquals(
                    ((10 * 30000)).toLong(),
                    monitor.hostGpuActiveTimes["H01"]?.fold(0) { acc, it -> acc + it[0] },
                ) { "GPU Active time incorrect" }
            },
            { assertEquals(27000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect host energy usage at timestamp 0" } },
            { assertEquals((600 * 150.0) + (600 * 300.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect host energy usage" } },
            { assertEquals((600 * 150.0) + (600 * 300.0), monitor.energyUsages.sum()) { "Incorrect total energy usage" } },
        )
    }
}
