package org.opendc.experiments.base

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.compute.simulator.scheduler.FilterScheduler
import org.opendc.compute.simulator.scheduler.MemorizingScheduler
import org.opendc.compute.simulator.scheduler.filters.ComputeFilter
import org.opendc.compute.simulator.scheduler.filters.RamFilter
import org.opendc.compute.simulator.scheduler.filters.VCpuFilter
import org.opendc.compute.simulator.scheduler.weights.CoreRamWeigher
import org.opendc.compute.workload.Task
import org.opendc.simulator.compute.workload.TraceFragment
import java.util.ArrayList

class SchedulerTest {

    @Test
    fun testSimulator4Memorizing() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                    arrayListOf(
                        TraceFragment(10 * 60 * 1000, 1000.0, 1),
                    ),
                ),
                createTestTask(
                    name = "1",
                    fragments =
                    arrayListOf(
                        TraceFragment(5 * 60 * 1000, 2000.0, 1),
                    ),
                    submissionTime = "1970-01-01T00:20",
                ),
            )

        val topology = createTopology("single_1_2000.json")

        val computeScheduler =
            MemorizingScheduler(
                filters = listOf(ComputeFilter(), VCpuFilter(1.0), RamFilter(1.0)),
            )
        val monitor = runTestWithScheduler(topology, workload, computeScheduler)

        assertAll(
            { assertEquals(25 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((10 * 30000) + (10 * 60000)).toLong(), monitor.hostIdleTimes["H01"]?.sum()) { "Idle time incorrect" } },
            { assertEquals(((10 * 30000) + (5 * 60000)).toLong(), monitor.hostActiveTimes["H01"]?.sum()) { "Active time incorrect" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect energy usage" } },
            {
                assertEquals(
                    (600 * 150.0) + (600 * 100.0) + (300 * 200.0),
                    monitor.hostEnergyUsages["H01"]?.sum(),
                ) { "Incorrect energy usage" }
            },
        )
    }
}
