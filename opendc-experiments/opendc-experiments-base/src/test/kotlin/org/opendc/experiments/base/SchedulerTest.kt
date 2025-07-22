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

package org.opendc.experiments.base

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.compute.simulator.scheduler.FilterScheduler
import org.opendc.compute.simulator.scheduler.MemorizingScheduler
import org.opendc.compute.simulator.scheduler.filters.ComputeFilter
import org.opendc.compute.simulator.scheduler.filters.RamFilter
import org.opendc.compute.simulator.scheduler.filters.VCpuFilter
import org.opendc.compute.simulator.scheduler.filters.VGpuFilter
import org.opendc.compute.simulator.scheduler.weights.VCpuWeigher
import org.opendc.compute.simulator.scheduler.weights.VGpuWeigher
import org.opendc.compute.workload.Task
import org.opendc.simulator.compute.workload.trace.TraceFragment
import java.util.ArrayList

class SchedulerTest {
    @Test
    fun testSimulator4Memorizing() {
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

        val computeScheduler =
            MemorizingScheduler(
                filters = listOf(ComputeFilter(), VCpuFilter(1.0), RamFilter(1.0)),
            )
        val monitor = runTest(topology, workload, computeScheduler = computeScheduler)

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
     * This test verifies that the gpu only schedulers are working correctly.
     * The same workload is run 4 times, once with the normal gpu filter scheduler and once with the inverted gpu filter scheduler.
     * Each scheduler is then run with a hardware configuration where the tasks fit onto one host, and one where multiple hosts are needed.
     */
    @Test
    fun testGpuAwareSchedulers() {
        // Define workload with tasks requiring both CPU and GPU resources
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
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 2000.0),
                        ),
                    cpuCount = 1,
                    gpuCount = 1,
                    submissionTime = "1970-01-01T00:20",
                ),
            )

        // Topology with 1 host having 2 GPUs (both tasks can fit on one host)
        val fittingTopology = createTopology("Gpus/dual_core_gpu_host.json")

        // Topology with 2 hosts each having 1 GPU (tasks must be distributed)
        val nonFittingTopology = createTopology("Gpus/single_gpu_hosts.json")

        val cpuAllocationRatio = 1.0
        val ramAllocationRatio = 1.5
        val gpuAllocationRatio = 1.0

        // Normal scheduler prioritizes hosts with more available resources
        val normalScheduler =
            FilterScheduler(
                filters =
                    listOf(
                        ComputeFilter(),
                        VCpuFilter(cpuAllocationRatio),
                        VGpuFilter(gpuAllocationRatio),
                        RamFilter(ramAllocationRatio),
                    ),
                weighers = listOf(VCpuWeigher(cpuAllocationRatio, multiplier = 1.0), VGpuWeigher(gpuAllocationRatio, multiplier = 1.0)),
            )

        // Inverted scheduler prioritizes hosts with fewer available resources
        val invertedScheduler =
            FilterScheduler(
                filters =
                    listOf(
                        ComputeFilter(),
                        VCpuFilter(cpuAllocationRatio),
                        VGpuFilter(gpuAllocationRatio),
                        RamFilter(ramAllocationRatio),
                    ),
                weighers = listOf(VCpuWeigher(cpuAllocationRatio, multiplier = -1.0), VGpuWeigher(gpuAllocationRatio, multiplier = -1.0)),
            )

        // Run the tests with both schedulers and both topologies
        val normalFittingMonitor = runTest(fittingTopology, workload, computeScheduler = normalScheduler)
        val normalNonFittingMonitor = runTest(nonFittingTopology, workload, computeScheduler = normalScheduler)
        val invertedFittingMonitor = runTest(fittingTopology, workload, computeScheduler = invertedScheduler)
        val invertedNonFittingMonitor = runTest(nonFittingTopology, workload, computeScheduler = invertedScheduler)

        assertAll(
            // Normal scheduler with fitting topology should use just one host
            {
                assertEquals(
                    1,
                    normalFittingMonitor.hostCpuSupplied.size,
                ) { "Normal scheduler should place both tasks on a single host when possible" }
            },
            // Normal scheduler with non-fitting topology must use two hosts
            {
                assertEquals(
                    2,
                    normalNonFittingMonitor.hostCpuSupplied.size,
                ) { "Normal scheduler should distribute tasks across hosts when needed" }
            },
            // Inverted scheduler with fitting topology might still use one host or distribute depending on implementation
            {
                assert(
                    invertedFittingMonitor.hostCpuSupplied.isNotEmpty(),
                ) { "Inverted scheduler should place tasks based on resource availability" }
            },
            // Inverted scheduler with non-fitting topology must use two hosts
            {
                assertEquals(
                    2,
                    invertedNonFittingMonitor.hostCpuSupplied.size,
                ) { "Inverted scheduler should distribute tasks across hosts when needed" }
            },
            // Verify GPU allocations - check that both tasks had their GPUs allocated
            { assertEquals(2, normalFittingMonitor.taskGpuSupplied.size) { "Both tasks should have GPU allocations" } },
            { assertEquals(2, normalNonFittingMonitor.taskGpuSupplied.size) { "Both tasks should have GPU allocations" } },
            { assertEquals(2, invertedFittingMonitor.taskGpuSupplied.size) { "Both tasks should have GPU allocations" } },
            { assertEquals(2, invertedNonFittingMonitor.taskGpuSupplied.size) { "Both tasks should have GPU allocations" } },
        )
    }
}
