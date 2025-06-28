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
            {assertEquals(DistributionPolicy.MAX_MIN_FAIRNESS, maxMinFairnessGpuTopology[0].hostSpecs[0].model.gpuDistributionStrategy, "MaxMinFairnessDistributionPolicy should be used")},
            {assertEquals(DistributionPolicy.EQUAL_SHARE, equalShareGpuTopology[0].hostSpecs[0].model.gpuDistributionStrategy, "EqualShareDistributionPolicy should be used")},
            {assertEquals(DistributionPolicy.FIXED_SHARE, fixedShareGpuTopology[0].hostSpecs[0].model.gpuDistributionStrategy, "FixedShareDistributionPolicy should be used")},
            {assertEquals(0.5, fixedShareGpuTopology[0].hostSpecs[0].model.gpuDistributionStrategy.getProperty("shareRatio"), "FixedShareDistributionPolicy should have a share ratio of 0.5")},
        )
    }


    /**
     * This test verifies that the [EqualShareDistributionPolicy] correctly distributes supply according to the number of suppliers.
     * The supply is divided equally among all suppliers.
     */
    @Test
    fun equalShareDistributionPolicyTest() {
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

        val single_gpu_topology = createTopology("DistributionPolicies/equalShare/dual_core_gpu_host.json")
        val double_gpu_topology = createTopology("DistributionPolicies/equalShare/multi_gpu_host.json")

        val single_monitor = runTest(single_gpu_topology, workload)
        val double_monitor = runTest(double_gpu_topology, workload)

        assertAll(
            // single gpu
            {assertEquals(2000.0, single_monitor.taskGpuDemands["0"]?.get(1), "Single GPU demand should be 1000.0")},
            {assertEquals(2000.0, single_monitor.taskGpuSupplied["0"]?.get(1), "Single GPU demand should be 1000.0")},
            {assertEquals(2000.0, single_monitor.hostGpuSupplied["0"]?.get(1)?.get(0), "Single GPU demand should be 1000.0")},
            {assertEquals(22000.0, single_monitor.hostGpuDemands["0"]?.get(1)?.get(0), "Single GPU demand should be 1000.0")},

            // double gpu
            {assertEquals(2000.0, double_monitor.taskGpuDemands["0"]?.get(1), "Double GPU demand should be 1000.0")},
            {assertEquals(2000.0, double_monitor.taskGpuSupplied["0"]?.get(1), "Double GPU demand should be 1000.0")},
            {assertEquals(1000.0, double_monitor.hostGpuSupplied["0"]?.get(1)?.get(0), "Double GPU demand for GPU 0 should be 500.0")},
            {assertEquals(1000.0, double_monitor.hostGpuDemands["0"]?.get(1)?.get(0), "Double GPU demand for GPU 0 should be 500.0")},
            {assertEquals(1000.0, double_monitor.hostGpuSupplied["0"]?.get(1)?.get(1), "Double GPU demand for GPU 1 should be 500.0")},
            {assertEquals(1000.0, double_monitor.hostGpuDemands["0"]?.get(1)?.get(1), "Double GPU demand for GPU 1 should be 500.0")},
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
