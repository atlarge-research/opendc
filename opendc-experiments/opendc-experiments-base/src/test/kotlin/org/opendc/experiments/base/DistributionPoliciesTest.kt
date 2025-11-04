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
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.simulator.compute.workload.trace.TraceFragment
import org.opendc.simulator.engine.graph.distributionPolicies.FlowDistributorFactory.DistributionPolicy
import java.util.ArrayList

class DistributionPoliciesTest {
    /**
     * This test verifies that the DistributionPolicies are correctly loaded from the configuration.
     */
    @Test
    fun distributionPoliciesTest() {
        val maxMinFairnessGpuTopology = createTopology("DistributionPolicies/maxMinFairness/multi_gpu_host.json")
        val equalShareGpuTopology = createTopology("DistributionPolicies/equalShare/dual_core_gpu_host.json")
        val fixedShareGpuTopology = createTopology("DistributionPolicies/fixedShare/multi_gpu_host.json")
        val bestEffortGpuTopology = createTopology("DistributionPolicies/bestEffort/multi_gpu_host.json")

        assertAll(
            {
                assertEquals(
                    DistributionPolicy.MAX_MIN_FAIRNESS,
                    maxMinFairnessGpuTopology[0].hostSpecs[0].model.gpuDistributionStrategy,
                    "MaxMinFairnessDistributionPolicy should be used",
                )
            },
            {
                assertEquals(
                    DistributionPolicy.EQUAL_SHARE,
                    equalShareGpuTopology[0].hostSpecs[0].model.gpuDistributionStrategy,
                    "EqualShareDistributionPolicy should be used",
                )
            },
            {
                assertEquals(
                    DistributionPolicy.FIXED_SHARE,
                    fixedShareGpuTopology[0].hostSpecs[0].model.gpuDistributionStrategy,
                    "FixedShareDistributionPolicy should be used",
                )
            },
            {
                assertEquals(
                    0.5,
                    fixedShareGpuTopology[0].hostSpecs[0].model.gpuDistributionStrategy.getProperty("shareRatio"),
                    "FixedShareDistributionPolicy should have a share ratio of 0.5",
                )
            },
            {
                assertEquals(
                    DistributionPolicy.BEST_EFFORT,
                    bestEffortGpuTopology[0].hostSpecs[0].model.gpuDistributionStrategy,
                    "BestEffortDistributionPolicy should be used",
                )
            },
        )
    }

    /**
     * This test verifies that the [EqualShareDistributionPolicy] correctly distributes supply according to the number of suppliers.
     * The supply is divided equally among all suppliers.
     */
    @Test
    fun equalShareDistributionPolicyTest1() {
        val workload: ArrayList<ServiceTask> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 2000.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 1,
                ),
            )

        val singleGpuTopology = createTopology("DistributionPolicies/equalShare/dual_core_gpu_host.json")
        val doubleGpuTopology = createTopology("DistributionPolicies/equalShare/multi_gpu_host.json")

        val singleMonitor = runTest(singleGpuTopology, workload)
        val doubleMonitor = runTest(doubleGpuTopology, workload)

        assertAll(
            // single gpu
            { assertEquals(2000.0, singleMonitor.taskGpuDemands[0]?.get(1), "Single GPU demand in task \"0\" should be 2000.0") },
            { assertEquals(4000.0, singleMonitor.taskGpuSupplied[0]?.get(1), "Single GPU supplied in task \"0\" should be 4000.0") },
            {
                assertEquals(
                    4000.0,
                    singleMonitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(0),
                    "Single GPU demand at host \"DualGpuHost\" should be 2000.0",
                )
            },
            {
                assertEquals(
                    4000.0,
                    singleMonitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(0),
                    "Single GPU supply at host \"DualGpuHost\" should be 2000.0",
                )
            },
            // double gpu
            { assertEquals(2000.0, doubleMonitor.taskGpuDemands[0]?.get(1), "Double GPU demand in task \"0\" should be 2000.0") },
            { assertEquals(4000.0, doubleMonitor.taskGpuSupplied[0]?.get(1), "Double GPU supplied in task \"0\" should be 4000.0") },
            {
                assertEquals(
                    2000.0,
                    doubleMonitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(0),
                    "Double GPU demand for GPU 0 at host \"DualGpuHost\" should be 2000.0",
                )
            },
            {
                assertEquals(
                    2000.0,
                    doubleMonitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(0),
                    "Double GPU supplied for GPU 0 at host \"DualGpuHost\" should be 2000.0",
                )
            },
            {
                assertEquals(
                    2000.0,
                    doubleMonitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(1),
                    "Double GPU demand for GPU 1 at host \"DualGpuHost\" should be 2000.0",
                )
            },
            {
                assertEquals(
                    2000.0,
                    doubleMonitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(1),
                    "Double GPU supplied for GPU 1 at host \"DualGpuHost\" should be 2000.0",
                )
            },
        )
    }

    /**
     * This test verifies that the [EqualShareDistributionPolicy] correctly distributes supply according to the number of suppliers.
     * The supply is divided equally among all suppliers.
     */
    @Test
    fun equalShareDistributionPolicyTest2() {
        val workload: ArrayList<ServiceTask> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 4000.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 2,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 4000.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 2,
                ),
            )

        val singleGpuTopology = createTopology("DistributionPolicies/equalShare/dual_core_gpu_host.json")
        val doubleGpuTopology = createTopology("DistributionPolicies/equalShare/multi_gpu_host.json")

        val singleMonitor = runTest(singleGpuTopology, workload)
        val doubleMonitor = runTest(doubleGpuTopology, workload)

        assertAll(
            // single gpu
            // task 0
            { assertEquals(4000.0, singleMonitor.taskGpuDemands[0]?.get(1), "Single GPU demand in task \"0\" should be 4000.0") },
            { assertEquals(2000.0, singleMonitor.taskGpuSupplied[0]?.get(1), "Single GPU supplied in task \"0\" should be 2000.0") },
            // task 1
            { assertEquals(4000.0, singleMonitor.taskGpuDemands[1]?.get(1), "Single GPU demand in task \"0\" should be 4000.0") },
            { assertEquals(2000.0, singleMonitor.taskGpuSupplied[1]?.get(1), "Single GPU supplied in task \"0\" should be 2000.0") },
            // host
            {
                assertEquals(
                    4000.0,
                    singleMonitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(0),
                    "Single GPU demand at host \"DualGpuHost\" should be 4000.0",
                )
            },
            {
                assertEquals(
                    4000.0,
                    singleMonitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(0),
                    "Single GPU supply at host \"DualGpuHost\" should be 4000.0",
                )
            },
            // double gpu
            // task 0
            { assertEquals(4000.0, doubleMonitor.taskGpuDemands[0]?.get(1), "Double GPU demand in task \"0\" should be 4000.0") },
            { assertEquals(2000.0, doubleMonitor.taskGpuSupplied[0]?.get(1), "Double GPU supply in task \"0\" should be 2000.0") },
            // task 1
            { assertEquals(4000.0, doubleMonitor.taskGpuDemands[1]?.get(1), "Double GPU demand in task \"0\" should be 4000.0") },
            { assertEquals(2000.0, doubleMonitor.taskGpuSupplied[1]?.get(1), "Double GPU supply in task \"0\" should be 2000.0") },
            // host
            {
                assertEquals(
                    2000.0,
                    doubleMonitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(0),
                    "Double GPU demand for GPU 0 at host \"DualGpuHost\" should be 2000.0",
                )
            },
            {
                assertEquals(
                    2000.0,
                    doubleMonitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(0),
                    "Double GPU supply for GPU 0 at host \"DualGpuHost\" should be 2000.0",
                )
            },
            {
                assertEquals(
                    2000.0,
                    doubleMonitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(1),
                    "Double GPU demand for GPU 1 at host \"DualGpuHost\" should be 2000.0",
                )
            },
            {
                assertEquals(
                    2000.0,
                    doubleMonitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(1),
                    "Double GPU supply for GPU 1 at host \"DualGpuHost\" should be 2000.0",
                )
            },
        )
    }

    /**
     * This test verifies that the [FixedShareDistributionPolicy] correctly distributes supply according to the fixed share.
     * The supply is divided according to the fixed share defined for each supplier.
     */
    @Test
    fun fixedShareDistributionPolicyTest() {
        val workload: ArrayList<ServiceTask> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 4000.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 2,
                ),
            )

        val topology = createTopology("DistributionPolicies/fixedShare/multi_gpu_host.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(4000.0, monitor.taskGpuDemands[0]?.get(1), "Task GPU demand should be 4000.0") },
            { assertEquals(1000.0, monitor.taskGpuSupplied[0]?.get(1), "Task GPU supplied should be 1000.0") },
            // Host
            {
                assertEquals(
                    1000.0,
                    monitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(0),
                    "GPU 0 demand at host should be 1000.0 (50% of the capacity)",
                )
            },
            { assertEquals(1000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(0), "GPU 0 supplied at host should be 1000.0") },
            {
                assertEquals(
                    1000.0,
                    monitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(1),
                    "GPU 1 demand at host should be 1000.0 (50% of the capacity)",
                )
            },
            { assertEquals(1000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(1), "GPU 1 supplied at host should be 1000.0") },
        )
    }

    /**
     * This test verifies that the [FixedShareDistributionPolicy] correctly handles resource contention.
     * When total demand exceeds available supply, resources should be distributed according to the fixed share ratio.
     */
    @Test
    fun fixedShareDistributionPolicyContentionTest() {
        val workload: ArrayList<ServiceTask> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 6000.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 2,
                ),
            )

        val topology = createTopology("DistributionPolicies/fixedShare/multi_gpu_host.json")

        val monitor = runTest(topology, workload)

        // With demand of 6000.0 but total GPU capacity of 4000.0 (2 GPUs * 2000.0 each)
        // Fixed share ratio of 0.5 means each GPU gets 50% of available capacity = 2000.0 each
        // Total supplied should be 4000.0 (limited by total capacity)
        assertAll(
            { assertEquals(6000.0, monitor.taskGpuDemands[0]?.get(1), "Task GPU demand should be 6000.0") },
            { assertEquals(1000.0, monitor.taskGpuSupplied[0]?.get(1), "Task GPU supplied should be 1000.0 (limited by the capacity)") },
            // Host
            {
                assertEquals(
                    1000.0,
                    monitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(0),
                    "GPU 0 demand at host should be 1000.0 (50% of the gpu capacity)",
                )
            },
            {
                assertEquals(
                    1000.0,
                    monitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(0),
                    "GPU 0 supplied at host should be 1000.0 (limited by GPU capacity)",
                )
            },
            {
                assertEquals(
                    1000.0,
                    monitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(1),
                    "GPU 1 demand at host should be 1000.0 (50% of the gpu capacity)",
                )
            },
            {
                assertEquals(
                    1000.0,
                    monitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(1),
                    "GPU 1 supplied at host should be 1000.0 (limited by GPU capacity)",
                )
            },
        )
    }

    /**
     * This test verifies that the [FixedShareDistributionPolicy] correctly handles multiple tasks competing for resources.
     * Resources should be distributed proportionally according to the fixed share ratio among all tasks.
     */
    @Test
    fun fixedShareDistributionPolicyMultipleTasksTest() {
        val workload: ArrayList<ServiceTask> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 3000.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 2,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 3000.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 2,
                ),
            )

        val topology = createTopology("DistributionPolicies/fixedShare/multi_gpu_host.json")

        val monitor = runTest(topology, workload)

        // Total demand: 6000.0 (3000.0 from each task)
        // Total capacity: 4000.0 (2 GPUs * 2000.0 each)
        // So each task gets 1000.0, distributed as 1000.0 per GPU (50% share ratio)
        assertAll(
            // Task 0
            { assertEquals(3000.0, monitor.taskGpuDemands[0]?.get(1), "Task 0 GPU demand should be 3000.0") },
            { assertEquals(1000.0, monitor.taskGpuSupplied[0]?.get(1), "Task 0 GPU supplied should be 1000.0") },
            // Task 1
            { assertEquals(3000.0, monitor.taskGpuDemands[1]?.get(1), "Task 1 GPU demand should be 3000.0") },
            { assertEquals(1000.0, monitor.taskGpuSupplied[1]?.get(1), "Task 1 GPU supplied should be 1000.0") },
            // Host
            { assertEquals(1000.0, monitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(0), "GPU 0 total demand at host should be 1000.0") },
            {
                assertEquals(
                    1000.0,
                    monitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(0),
                    "GPU 0 total supplied at host should be 1000.0",
                )
            },
            { assertEquals(1000.0, monitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(1), "GPU 1 total demand at host should be 1000.0") },
            {
                assertEquals(
                    1000.0,
                    monitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(1),
                    "GPU 1 total supplied at host should be 1000.0",
                )
            },
        )
    }

    /**
     * This test verifies that the [BestEffortDistributionPolicy] correctly distributes supply based on demand
     * when resources are abundant. It should satisfy all demands and distribute remaining capacity optimally.
     */
    @Test
    fun bestEffortDistributionPolicyBasicTest() {
        val workload: ArrayList<ServiceTask> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 1500.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 1,
                ),
            )

        val singleGpuTopology = createTopology("DistributionPolicies/bestEffort/dual_core_gpu_host.json")
        val doubleGpuTopology = createTopology("DistributionPolicies/bestEffort/multi_gpu_host.json")

        val singleMonitor = runTest(singleGpuTopology, workload)
        val doubleMonitor = runTest(doubleGpuTopology, workload)

        assertAll(
            // single gpu - should satisfy demand and utilize remaining capacity
            { assertEquals(1500.0, singleMonitor.taskGpuDemands[0]?.get(1), "Single GPU demand in task \"0\" should be 1500.0") },
            { assertEquals(1500.0, singleMonitor.taskGpuSupplied[0]?.get(1)) { "Single GPU should supply the demanded 1500.0" } },
            // Host
            {
                assertEquals(
                    1500.0,
                    singleMonitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(0),
                    "Single GPU demand at host \"DualGpuHost\" should be 1500.0",
                )
            },
            {
                assertEquals(
                    1500.0,
                    singleMonitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(0),
                    "Single GPU supply at host \"DualGpuHost\" should be 1500.0",
                )
            },
            // double gpu - should distribute across both GPUs and utilize remaining capacity
            { assertEquals(1500.0, doubleMonitor.taskGpuDemands[0]?.get(1), "Double GPU demand in task \"0\" should be 1500.0") },
            { assertEquals(1500.0, doubleMonitor.taskGpuSupplied[0]?.get(1), "Double GPU should supply the demanded 1500.0") },
            // Host
            {
                assertEquals(
                    1500.0,
                    doubleMonitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(0),
                    "GPU 0 demand at host \"DualGpuHost\" should be 750.0",
                )
            },
            {
                assertEquals(
                    1500.0,
                    doubleMonitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(0),
                    "GPU 0 supply at host \"DualGpuHost\" should be 750.0",
                )
            },
            {
                assertEquals(
                    0.0,
                    doubleMonitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(1),
                    "GPU 1 demand at host \"DualGpuHost\" should be 750.0",
                )
            },
            {
                assertEquals(
                    0.0,
                    doubleMonitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(1),
                    "GPU 1 supply at host \"DualGpuHost\" should be 750.0",
                )
            },
        )
    }

    /**
     * This test verifies that the [BestEffortDistributionPolicy] correctly handles resource contention
     * by using round-robin distribution when demand exceeds supply.
     */
    @Test
    fun bestEffortDistributionPolicyContentionTest() {
        val workload: ArrayList<ServiceTask> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 3000.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 2,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 2500.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 2,
                ),
            )

        val topology = createTopology("DistributionPolicies/bestEffort/multi_gpu_host.json")
        val monitor = runTest(topology, workload)

        // Total demand: 5500.0 (3000.0 + 2500.0)
        // Total capacity: 4000.0 (2 GPUs * 2000.0 each)
        // Best effort should distribute proportionally based on demand while using round-robin
        assertAll(
            // Task 0
            { assertEquals(3000.0, monitor.taskGpuDemands[0]?.get(0), "Task 0 GPU demand should be 3000.0") },
            { assertEquals(3000.0, monitor.taskGpuSupplied[0]?.get(0), "Task 0 GPU supply should be 1000.0") },
            // Task 1
            { assertEquals(2500.0, monitor.taskGpuDemands[1]?.get(0), "Task 1 GPU demand should be 2500.0") },
            { assertEquals(1000.0, monitor.taskGpuSupplied[1]?.get(0), "Task 1 GPU supply should be 1000.0") },
            // Host
            { assertEquals(2750.0, monitor.hostGpuDemands["DualGpuHost"]?.get(0)?.get(0), "GPU 0 demand at host should be 2000.0") },
            { assertEquals(2000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(0)?.get(0), "GPU 0 supplied at host should be 2000.0") },
            { assertEquals(2750.0, monitor.hostGpuDemands["DualGpuHost"]?.get(0)?.get(1), "GPU 1 demand at host should be 2000.0") },
            { assertEquals(2000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(0)?.get(1), "GPU 1 supplied at host should be 2000.0") },
        )
    }

    /**
     * This test verifies that the [BestEffortDistributionPolicy] prioritizes already utilized resources
     * when supply exceeds demand, demonstrating the efficiency optimization principle.
     */
    @Test
    fun bestEffortDistributionPolicyUtilizationOptimizationTest() {
        val workload: ArrayList<ServiceTask> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 1000.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 1,
                ),
            )

        val doubleGpuTopology = createTopology("DistributionPolicies/bestEffort/multi_gpu_host.json")
        val monitor = runTest(doubleGpuTopology, workload)

        // With low demand (1000.0) and high capacity (4000.0), best effort should:
        // 1. Satisfy the demand
        // 2. Utilize remaining capacity efficiently
        assertAll(
            { assertEquals(1000.0, monitor.taskGpuDemands[0]?.get(1), "Task GPU demand should be 1000.0") },
            { assertEquals(1000.0, monitor.taskGpuSupplied[0]?.get(1), "Task GPU supplied should be 1000.0") },
            // host
            { assertEquals(1000.0, monitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(0), "GPU 0 demand at host should be 1000.0") },
            { assertEquals(1000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(0), "GPU 0 supplied at host should be 1000.0") },
            { assertEquals(0.0, monitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(1), "GPU 1 demand at host should be 0.0") },
            { assertEquals(0.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(1), "GPU 1 supplied at host should be 0.0") },
        )
    }

    /**
     * This test verifies that the [BestEffortDistributionPolicy] handles varying demands correctly
     * and does not distribute the resources equally.
     */
    @Test
    fun bestEffortDistributionPolicyVaryingDemandsTest() {
        val workload: ArrayList<ServiceTask> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 3500.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 2,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 500.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 2,
                ),
            )

        val topology = createTopology("DistributionPolicies/bestEffort/multi_gpu_host.json")

        val monitor = runTest(topology, workload)

        // Best effort should prioritize the high-demand task differently than equal share
        assertAll(
            // Best effort should allocate more to high-demand task compared to equal share
            { assertEquals(3500.0, monitor.taskGpuDemands[0]?.get(1), "Task 0 demand should be 3500.0") },
            { assertEquals(3500.0, monitor.taskGpuDemands[0]?.get(1), "Task 0 supply should be 3500.0") },
            { assertEquals(500.0, monitor.taskGpuDemands[1]?.get(1), "Task 1 demand should be 500.0") },
            { assertEquals(500.0, monitor.taskGpuSupplied[1]?.get(1), "Task 1 supply should be 500.0") },
            // Host
            { assertEquals(2000.0, monitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(0), "GPU 0 demand at host should be 2000.0") },
            { assertEquals(2000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(0), "GPU 0 supplied at host should be 2000.0") },
            { assertEquals(2000.0, monitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(1), "GPU 1 demand at host should be 2000.0") },
            { assertEquals(2000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(1), "GPU 1 supplied at host should be 2000.0") },
        )
    }

    /**
     * This test verifies that the [BestEffortDistributionPolicy] maintains fairness over time
     * through its round-robin mechanism when resources are constrained.
     */
    @Test
    fun bestEffortDistributionPolicyFairnessTest() {
        val workload: ArrayList<ServiceTask> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 2000.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 2,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 2000.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 2,
                ),
                createTestTask(
                    id = 2,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 2000.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 2,
                ),
            )

        val topology = createTopology("DistributionPolicies/bestEffort/multi_gpu_host.json")
        val monitor = runTest(topology, workload)

        // With equal demands (2000.0 each) and limited capacity (4000.0 total)
        // Best effort should distribute fairly among all tasks in a round-robin manner
        assertAll(
            // Task Demands at start
            { assertEquals(2000.0, monitor.taskGpuDemands[0]?.get(0), "Task 0 demand should be 2000.0") },
            { assertEquals(2000.0, monitor.taskGpuDemands[1]?.get(0), "Task 1 demand should be 2000.0") },
            { assertEquals(2000.0, monitor.taskGpuDemands[2]?.get(0), "Task 2 demand should be 2000.0") },
            // Task supplies at start
            { assertEquals(2000.0, monitor.taskGpuSupplied[0]?.get(0), "Task 0 supply at the start should be 2000.0") },
            { assertEquals(2000.0, monitor.taskGpuSupplied[1]?.get(0), "Task 1 supply at the start  should be 0.0") },
            { assertEquals(0.0, monitor.taskGpuSupplied[2]?.get(0), "Task 2 supply at the start  should be 2000.0") },
            // Task supplies second step
            { assertEquals(0.0, monitor.taskGpuSupplied[0]?.get(1), "Task 0 supply at the second step should be 2000.0") },
            { assertEquals(2000.0, monitor.taskGpuSupplied[1]?.get(1), "Task 1 supply at the second step should be 0.0") },
            { assertEquals(2000.0, monitor.taskGpuSupplied[2]?.get(1), "Task 2 supply at the second step should be 2000.0") },
            // Task supplies third step
            { assertEquals(2000.0, monitor.taskGpuSupplied[0]?.get(2), "Task 0 supply at the third step should be 2000.0") },
            { assertEquals(0.0, monitor.taskGpuSupplied[1]?.get(2), "Task 1 supply at the third step should be 0.0") },
            { assertEquals(2000.0, monitor.taskGpuSupplied[2]?.get(2), "Task 2 supply at the third step should be 2000.0") },
            // Host
            // At start
            { assertEquals(3000.0, monitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(0), "GPU 0 demand at host should be 2000.0") },
            { assertEquals(2000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(0), "GPU 0 supplied at host should be 2000.0") },
            { assertEquals(3000.0, monitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(1), "GPU 1 demand at host should be 2000.0") },
            { assertEquals(2000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(1), "GPU 1 supplied at host should be 2000.0") },
            // Next Round
            { assertEquals(3000.0, monitor.hostGpuDemands["DualGpuHost"]?.get(2)?.get(0), "GPU 0 demand at host should be 2000.0") },
            { assertEquals(2000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(2)?.get(0), "GPU 0 supplied at host should be 2000.0") },
            { assertEquals(3000.0, monitor.hostGpuDemands["DualGpuHost"]?.get(2)?.get(1), "GPU 1 demand at host should be 2000.0") },
            { assertEquals(2000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(2)?.get(1), "GPU 1 supplied at host should be 2000.0") },
            // Next Round
            { assertEquals(3000.0, monitor.hostGpuDemands["DualGpuHost"]?.get(3)?.get(0), "GPU 0 demand at host should be 2000.0") },
            { assertEquals(2000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(3)?.get(0), "GPU 0 supplied at host should be 2000.0") },
            { assertEquals(3000.0, monitor.hostGpuDemands["DualGpuHost"]?.get(3)?.get(1), "GPU 1 demand at host should be 2000.0") },
            { assertEquals(2000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(3)?.get(1), "GPU 1 supplied at host should be 2000.0") },
        )
    }

    /**
     * This test verifies that the [FirstFitDistributionPolicy] places workloads on the first GPU
     * before utilizing the second GPU, demonstrating the First Fit allocation strategy.
     * All tasks should be satisfied as total demand is within available capacity.
     */
    @Test
    fun firstFitDistributionPolicyGpuPlacementTest() {
        val workload: ArrayList<ServiceTask> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 1500.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 2,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 1000.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 2,
                ),
            )

        val topology = createTopology("DistributionPolicies/firstFit/multi_gpu_host.json")
        val monitor = runTest(topology, workload)

        // With First Fit policy, tasks should be placed on first GPU before second GPU
        // Total demand (2500.0) is less than total capacity (4000.0), so all should be satisfied
        assertAll(
            // Task demands should remain as requested
            { assertEquals(1500.0, monitor.taskGpuDemands[0]?.get(1), "Task 0 GPU demand should be 1500.0") },
            { assertEquals(1000.0, monitor.taskGpuDemands[1]?.get(1), "Task 1 GPU demand should be 1000.0") },
            // All tasks should be fully satisfied
            { assertEquals(1500.0, monitor.taskGpuSupplied[0]?.get(1), "Task 0 GPU supply should be 1500.0") },
            { assertEquals(1000.0, monitor.taskGpuSupplied[1]?.get(1), "Task 1 GPU supply should be 1000.0") },
            // First GPU should handle both tasks (total 2500.0, within its 2000.0 capacity limit per task)
            { assertEquals(2000.0, monitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(0), "GPU 0 demand should be 2000.0") },
            { assertEquals(2000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(0), "GPU 0 supply should be 2000.0") },
            // Second GPU should have remaining demand
            { assertEquals(500.0, monitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(1), "GPU 1 demand should be 500.0") },
            { assertEquals(500.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(1), "GPU 1 supply should be 500.0") },
        )
    }

    /**
     * This test verifies that the [FirstFitDistributionPolicy] correctly handles scenarios
     * where overall demand exceeds total available supply. Some tasks should receive no supply
     * if they cannot be satisfied by a single GPU.
     */
    @Test
    fun firstFitDistributionPolicyOverdemandTest() {
        val workload: ArrayList<ServiceTask> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 2000.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 1,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 2000.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 1,
                ),
                createTestTask(
                    id = 2,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 1500.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 1,
                ),
            )

        val topology = createTopology("DistributionPolicies/firstFit/multi_gpu_host.json")
        val monitor = runTest(topology, workload)

        // With First Fit policy and total demand (5500.0) > total capacity (4000.0),
        // only tasks that can fit on individual GPUs should be satisfied
        assertAll(
            // Task demands should remain as requested
            { assertEquals(2000.0, monitor.taskGpuDemands[0]?.get(1), "Task 0 GPU demand should be 2000.0") },
            { assertEquals(2000.0, monitor.taskGpuDemands[1]?.get(1), "Task 1 GPU demand should be 2000.0") },
            { assertEquals(1500.0, monitor.taskGpuDemands[2]?.get(1), "Task 2 GPU demand should be 1500.0") },
            // First two tasks should be satisfied (each fits on one GPU)
            { assertEquals(2000.0, monitor.taskGpuSupplied[0]?.get(1), "Task 0 should be fully satisfied") },
            { assertEquals(2000.0, monitor.taskGpuSupplied[1]?.get(1), "Task 1 should be fully satisfied") },
            // Third task should receive no supply as no single GPU can satisfy it after first two are allocated
            { assertEquals(0.0, monitor.taskGpuSupplied[2]?.get(1), "Task 2 should receive no supply") },
            // Both GPUs should be fully utilized by the first two tasks
            { assertEquals(2000.0, monitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(0), "GPU 0 should have 2000.0 demand") },
            { assertEquals(2000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(0), "GPU 0 should supply 2000.0") },
            { assertEquals(2000.0, monitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(1), "GPU 1 should have 2000.0 demand") },
            { assertEquals(2000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(1), "GPU 1 should supply 2000.0") },
        )
    }
}
