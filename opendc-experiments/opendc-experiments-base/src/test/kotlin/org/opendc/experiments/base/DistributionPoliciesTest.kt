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
import org.opendc.compute.workload.Task
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
        )
    }

    /**
     * This test verifies that the [EqualShareDistributionPolicy] correctly distributes supply according to the number of suppliers.
     * The supply is divided equally among all suppliers.
     */
    @Test
    fun equalShareDistributionPolicyTest1() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 0, 2000.0, 1),
                        ),
                ),
            )

        val singleGpuTopology = createTopology("DistributionPolicies/equalShare/dual_core_gpu_host.json")
        val doubleGpuTopology = createTopology("DistributionPolicies/equalShare/multi_gpu_host.json")

        val singleMonitor = runTest(singleGpuTopology, workload)
        val doubleMonitor = runTest(doubleGpuTopology, workload)

        assertAll(
            // single gpu
            { assertEquals(2000.0, singleMonitor.taskGpuDemands["0"]?.get(1), "Single GPU demand in task \"0\" should be 2000.0") },
            { assertEquals(4000.0, singleMonitor.taskGpuSupplied["0"]?.get(1), "Single GPU demand in task \"0\" should be 2000.0") },
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
            { assertEquals(2000.0, doubleMonitor.taskGpuDemands["0"]?.get(1), "Double GPU demand in task \"0\" should be 2000.0") },
            { assertEquals(4000.0, doubleMonitor.taskGpuSupplied["0"]?.get(1), "Double GPU supplied in task \"0\" should be 4000.0") },
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
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 0, 4000.0, 2),
                        ),
                ),
                createTestTask(
                    name = "1",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 0, 4000.0, 2),
                        ),
                ),
            )

        val singleGpuTopology = createTopology("DistributionPolicies/equalShare/dual_core_gpu_host.json")
        val doubleGpuTopology = createTopology("DistributionPolicies/equalShare/multi_gpu_host.json")

        val singleMonitor = runTest(singleGpuTopology, workload)
        val doubleMonitor = runTest(doubleGpuTopology, workload)

        assertAll(
            // single gpu
            { assertEquals(4000.0, singleMonitor.taskGpuDemands["0"]?.get(1), "Single GPU demand in task \"0\" should be 4000.0") },
            { assertEquals(2000.0, singleMonitor.taskGpuSupplied["0"]?.get(1), "Single GPU supplied in task \"0\" should be 2000.0") },
            { assertEquals(4000.0, singleMonitor.taskGpuDemands["1"]?.get(1), "Single GPU demand in task \"0\" should be 4000.0") },
            { assertEquals(2000.0, singleMonitor.taskGpuSupplied["1"]?.get(1), "Single GPU supplied in task \"0\" should be 2000.0") },
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
            { assertEquals(4000.0, doubleMonitor.taskGpuDemands["0"]?.get(1), "Double GPU demand in task \"0\" should be 4000.0") },
            { assertEquals(2000.0, doubleMonitor.taskGpuSupplied["0"]?.get(1), "Double GPU supply in task \"0\" should be 2000.0") },
            { assertEquals(4000.0, doubleMonitor.taskGpuDemands["1"]?.get(1), "Double GPU demand in task \"0\" should be 4000.0") },
            { assertEquals(2000.0, doubleMonitor.taskGpuSupplied["1"]?.get(1), "Double GPU supply in task \"0\" should be 2000.0") },
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
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 0, 1000.0, 1),
                            TraceFragment(10 * 60 * 1000, 0.0, 0, 2000.0, 1),
                        ),
                ),
            )

        val topology = createTopology("DistributionPolicies/fixedShare/multi_gpu_host.json")

        val monitor = runTest(topology, workload)
    }
}
