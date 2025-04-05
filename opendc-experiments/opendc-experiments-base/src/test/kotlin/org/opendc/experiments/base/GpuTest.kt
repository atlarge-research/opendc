package org.opendc.experiments.base

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.compute.workload.Task
import org.opendc.simulator.compute.workload.trace.TraceFragment
import java.util.ArrayList

class GpuTest {

    @Test
    fun testGpuOnly() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    accelFragments =
                    arrayListOf(
                        TraceFragment(10 * 60 * 1000, 1000.0, 1),
                    ),
                ),
            )

        val topology = createTopology("gpu/single_1_2000.json")

        val monitor = runTest(topology, workload)

        // Power usage is 150 from gpu + 100 from idle cpu in the topology
        assertAll(
            { assertEquals(10 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(250.0, monitor.hostPowerDraws["H01"]?.get(0)) { "Incorrect energy usage" } },
            { assertEquals(250.0, monitor.hostPowerDraws["H01"]?.get(9)) { "Incorrect energy usage" } },
        )
    }

    @Test
    fun testCpuAndGpu() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                    arrayListOf(
                        TraceFragment(10 * 60 * 1000, 1000.0, 1),
                    ),
                    accelFragments =
                    arrayListOf(
                        TraceFragment(10 * 60 * 1000, 1000.0, 1),
                    ),
                ),
            )

        val topology = createTopology("gpu/single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(10 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(300.0, monitor.hostPowerDraws["H01"]?.get(0)) { "Incorrect energy usage" } },
            { assertEquals(300.0, monitor.hostPowerDraws["H01"]?.get(9)) { "Incorrect energy usage" } },
        )
    }

    @Test
    fun testCpuAndGpuDiffDuration() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                    arrayListOf(
                        TraceFragment(10 * 60 * 1000, 1000.0, 1),
                    ),
                    accelFragments =
                    arrayListOf(
                        TraceFragment(20 * 60 * 1000, 1000.0, 1),
                    ),
                ),
            )

        val topology = createTopology("gpu/single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(20 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(300.0, monitor.hostPowerDraws["H01"]?.get(0)) { "Incorrect energy usage at time 0" } },
            { assertEquals(300.0, monitor.hostPowerDraws["H01"]?.get(8)) { "Incorrect energy usage at time 8" } },
            { assertEquals(250.0, monitor.hostPowerDraws["H01"]?.get(13)) { "Incorrect energy usage at time 13" } },
        )
    }

    @Test
    fun testCpuAndGpuMultipleTasks() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                    arrayListOf(
                        TraceFragment(10 * 60 * 1000, 1000.0, 1),
                    ),
                    accelFragments =
                    arrayListOf(
                        TraceFragment(20 * 60 * 1000, 1000.0, 1),
                    ),
                ),
                createTestTask(
                    name = "1",
                    fragments =
                    arrayListOf(
                        TraceFragment(10 * 60 * 1000, 1000.0, 1),
                    ),
                    accelFragments =
                    arrayListOf(
                        TraceFragment(10 * 60 * 1000, 1000.0, 1),
                    ),
                ),
            )

        val topology = createTopology("gpu/single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(30 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(300.0, monitor.hostPowerDraws["H01"]?.get(0)) { "Incorrect energy usage at time 0" } },
            { assertEquals(300.0, monitor.hostPowerDraws["H01"]?.get(8)) { "Incorrect energy usage at time 8" } },
            { assertEquals(250.0, monitor.hostPowerDraws["H01"]?.get(13)) { "Incorrect energy usage at time 13" } },
            { assertEquals(300.0, monitor.hostPowerDraws["H01"]?.get(25)) { "Incorrect energy usage at time 18" } },
        )
    }
}
