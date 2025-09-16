/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.experiments.base

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.compute.simulator.scheduler.MemorizingScheduler
import org.opendc.compute.simulator.scheduler.filters.ComputeFilter
import org.opendc.compute.simulator.scheduler.filters.RamFilter
import org.opendc.compute.simulator.scheduler.filters.VCpuFilter
import org.opendc.compute.workload.Task
import org.opendc.simulator.compute.workload.trace.TraceFragment
import java.util.ArrayList

/**
 * An integration test suite for the Scenario experiments.
 */
class ScenarioRunnerTest {
    /**
     * Scenario test 1: Single Task
     * In this test, a single task is scheduled that takes 10 minutes to run.
     *
     * There should be no problems running the task, so the total runtime should be 10 min.
     *
     * The task is using 50% of the available CPU capacity.
     * This means that half of the time is active, and half is idle.
     * When the task is failed, all time is idle.
     */
    @Test
    fun testScenario1() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCount = 1,
                ),
            )

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

    /**
     * Scenario test 1: Two Tasks
     * In this test, two tasks are scheduled.
     *
     * There should be no problems running the task, so the total runtime should be 15 min.
     *
     * The first task is using 50% of the available CPU capacity.
     * The second task is using 100% of the available CPU capacity.
     */
    @Test
    fun testScenario2() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCount = 1,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(5 * 60 * 1000, 2000.0),
                        ),
                    cpuCount = 1,
                ),
            )

        val topology = createTopology("single_1_2000.json")

        val monitor =
            runTest(
                topology,
                workload,
                computeScheduler =
                    MemorizingScheduler(
                        filters = listOf(ComputeFilter(), VCpuFilter(1.0), RamFilter(1.0)),
                    ),
            )

        assertAll(
            { assertEquals(15 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((10 * 30000)).toLong(), monitor.hostCpuIdleTimes["H01"]?.sum()) { "Idle time incorrect" } },
            { assertEquals(((10 * 30000) + (5 * 60000)).toLong(), monitor.hostCpuActiveTimes["H01"]?.sum()) { "Active time incorrect" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect energy usage" } },
            { assertEquals((600 * 150.0) + (300 * 200.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect energy usage" } },
            { assertEquals((600 * 150.0) + (300 * 200.0), monitor.energyUsages.sum()) { "Incorrect energy usage" } },
        )
    }

    /**
     * Scenario test 3: Two Tasks, one scheduled later
     * In this test, two tasks are scheduled.
     *
     * There should be no problems running the task, so the total runtime should be 15 min.
     *
     * The first task is using 50% of the available CPU capacity.
     * The second task is using 100% of the available CPU capacity.
     */
    @Test
    fun testScenario3() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCount = 1,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCount = 1,
                ),
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

    /**
     * Scenario test 4: Two Tasks, one scheduled later
     * In this test, two tasks are scheduled.
     *
     * There should be no problems running the task, so the total runtime should be 15 min.
     *
     * The first task is using 50% of the available CPU capacity.
     * The second task is using 100% of the available CPU capacity.
     */
    @Test
    fun testScenario4() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCount = 1,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(5 * 60 * 1000, 2000.0),
                        ),
                    cpuCount = 1,
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

    /**
     * Scenario test 5: One Task purely running on GPU
     *
     * In this test, a single task is scheduled that takes 10 minutes to run. It solely uses the GPU.
     */
    @Test
    fun testScenario5() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 1000.0),
                        ),
                    cpuCount = 0,
                    gpuCount = 1,
                ),
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
                    monitor.hostGpuIdleTimes["H01"]?.fold(0, { acc, iterator -> acc + iterator[0] }),
                ) { "GPU Idle time incorrect" }
            },
            {
                assertEquals(
                    ((10 * 30000)).toLong(),
                    monitor.hostGpuActiveTimes["H01"]?.fold(0, { acc, iterator -> acc + iterator[0] }),
                ) { "GPU Active time incorrect" }
            },
            // double, as CPU and GPU both use power
            // higher power usage, as default GPU power model is used range [200, 400]
            { assertEquals(2 * 12000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect host energy usage at timestamp 0" } },
            { assertEquals((600 * 100.0) + (600 * 300.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect host energy usage" } },
            { assertEquals((600 * 100.0) + (600 * 300.0), monitor.energyUsages.sum()) { "Incorrect total energy usage" } },
        )
    }

    /**
     * Scenario test 6: One Task running on CPU & GPU
     *
     * In this test, a single task is scheduled that takes 10 minutes to run. CPU & GPU are used and have the same runtime.
     */
    @Test
    fun testScenario6() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1000.0),
                        ),
                    cpuCount = 1,
                    gpuCount = 1,
                ),
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
                    monitor.hostGpuIdleTimes["H01"]?.fold(0, { acc, iterator -> acc + iterator[0] }),
                ) { "GPU Idle time incorrect" }
            },
            {
                assertEquals(
                    ((10 * 30000)).toLong(),
                    monitor.hostGpuActiveTimes["H01"]?.fold(0, { acc, iterator -> acc + iterator[0] }),
                ) { "GPU Active time incorrect" }
            },
            // double, as CPU and GPU both use power
            { assertEquals(27000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect host energy usage at timestamp 0" } },
            { assertEquals((600 * 150.0) + (600 * 300.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect host energy usage" } },
            { assertEquals((600 * 150.0) + (600 * 300.0), monitor.energyUsages.sum()) { "Incorrect total energy usage" } },
        )
    }

    /**
     * Scenario test 7: One Task running on CPU & GPU
     *
     * In this test, a single task is scheduled that takes 10 minutes to run. CPU & GPU are used. CPU will finish way ahead of the GPU.
     */
    @Test
    fun testScenario7() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 2000.0),
                        ),
                    cpuCount = 1,
                    gpuCount = 1,
                ),
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
                    monitor.hostGpuIdleTimes["H01"]?.fold(0, { acc, iterator -> acc + iterator[0] }),
                ) { "GPU Idle time incorrect" }
            },
            {
                assertEquals(
                    ((10 * 60000)).toLong(),
                    monitor.hostGpuActiveTimes["H01"]?.fold(0, { acc, iterator -> acc + iterator[0] }),
                ) { "GPU Active time incorrect" }
            },
            // double, as CPU and GPU both use power
            { assertEquals(33000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect host energy usage at timestamp 0" } },
            { assertEquals((600 * 150.0) + (600 * 400.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect host energy usage" } },
            { assertEquals((600 * 150.0) + (600 * 400.0), monitor.energyUsages.sum()) { "Incorrect total energy usage" } },
        )
    }

    /**
     * Scenario test 8: One Task running on CPU & GPU
     *
     * In this test, a single task is scheduled that takes 10 minutes to run. CPU & GPU are used. GPU will finish way ahead of the CPU.
     */
    @Test
    fun testScenario8() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 2000.0, 1000.0),
                        ),
                    cpuCount = 1,
                    gpuCount = 1,
                ),
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
                    monitor.hostGpuIdleTimes["H01"]?.fold(0, { acc, iterator -> acc + iterator[0] }),
                ) { "GPU Idle time incorrect" }
            },
            {
                assertEquals(
                    ((10 * 30000)).toLong(),
                    monitor.hostGpuActiveTimes["H01"]?.fold(0, { acc, iterator -> acc + iterator[0] }),
                ) { "GPU Active time incorrect" }
            },
            // double, as CPU and GPU both use power
            { assertEquals(30000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect host energy usage at timestamp 0" } },
            { assertEquals((600 * 200.0) + (600 * 300.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect host energy usage" } },
            { assertEquals((600 * 200.0) + (600 * 300.0), monitor.energyUsages.sum()) { "Incorrect total energy usage" } },
        )
    }

    /**
     * Scenario test 9: Two tasks running on CPU & GPU
     *
     * In this test, two tasks are scheduled at the same time that takes 10 minutes to run. CPU & GPU are used. Both resources will finish at the same time.
     */
    @Test
    fun testScenario9() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1000.0),
                        ),
                    cpuCount = 1,
                    gpuCount = 1,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1000.0),
                        ),
                    cpuCount = 1,
                    gpuCount = 1,
                ),
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
                    monitor.hostGpuIdleTimes["H01"]?.fold(0, { acc, iterator -> acc + iterator[0] }),
                ) { "GPU Idle time incorrect" }
            },
            {
                assertEquals(
                    ((10 * 60000)).toLong(),
                    monitor.hostGpuActiveTimes["H01"]?.fold(0, { acc, iterator -> acc + iterator[0] }),
                ) { "GPU Active time incorrect" }
            },
            // double, as CPU and GPU both use power
            { assertEquals(27000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect host energy usage at timestamp 0" } },
            { assertEquals(2 * ((600 * 150.0) + (600 * 300.0)), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect host energy usage" } },
            { assertEquals(2 * ((600 * 150.0) + (600 * 300.0)), monitor.energyUsages.sum()) { "Incorrect total energy usage" } },
        )
    }

    /**
     * Scenario test 10: Two tasks running on CPU & GPU
     *
     * In this test, two tasks are scheduled at the same time that takes 10 minutes to run. One task purely uses CPU, one purely GPU.
     */
    @Test
    fun testScenario10() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 0.0),
                        ),
                    cpuCount = 1,
                    gpuCount = 0,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 1000.0),
                        ),
                    cpuCount = 0,
                    gpuCount = 1,
                ),
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
                    monitor.hostGpuIdleTimes["H01"]?.fold(0, { acc, iterator -> acc + iterator[0] }),
                ) { "GPU Idle time incorrect" }
            },
            {
                assertEquals(
                    ((10 * 30000)).toLong(),
                    monitor.hostGpuActiveTimes["H01"]?.fold(0, { acc, iterator -> acc + iterator[0] }),
                ) { "GPU Active time incorrect" }
            },
            // double, as CPU and GPU both use power
            { assertEquals(27000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect host energy usage at timestamp 0" } },
            { assertEquals((600 * 150.0) + (600 * 300.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect host energy usage" } },
            { assertEquals((600 * 150.0) + (600 * 300.0), monitor.energyUsages.sum()) { "Incorrect total energy usage" } },
        )
    }
}
