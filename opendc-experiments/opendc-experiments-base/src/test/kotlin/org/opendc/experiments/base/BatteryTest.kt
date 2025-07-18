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
import org.opendc.compute.workload.Task
import org.opendc.experiments.base.experiment.specs.TraceBasedFailureModelSpec
import org.opendc.simulator.compute.workload.trace.TraceFragment
import java.util.ArrayList

/**
 * Testing suite containing tests that specifically test the FlowDistributor
 */
class BatteryTest {
    /**
     * Battery test 1: One static task High Carbon, Empty battery
     */
    @Test
    fun testBattery1() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    submissionTime = "2022-01-01T00:00",
                ),
            )

        val topology = createTopology("batteries/experiment1.json")
        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(150.0, monitor.powerDraws[0]) { "The power usage at timestamp 0 is not correct" } },
            { assertEquals(10 * 60 * 150.0, monitor.energyUsages.sum()) { "The total power usage is not correct" } },
        )
    }

    /**
     * Battery test 2: One static task Low Carbon, Empty battery
     */
    @Test
    fun testBattery2() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    submissionTime = "2022-01-01T00:00",
                ),
            )

        val topology = createTopology("batteries/experiment2.json")
        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1150.0, monitor.powerDraws[0]) { "The power usage at timestamp 0 is not correct" } },
            { assertEquals(150.0, monitor.powerDraws[5]) { "The power usage at timestamp 0 is not correct" } },
            { assertEquals(10 * 60 * 150.0 + 360000, monitor.energyUsages.sum()) { "The total power usage is not correct" } },
        )
    }

    /**
     * Battery test 3: One static task Low Carbon followed by High Carbon, Empty battery
     */
    @Test
    fun testBattery3() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(20 * 60 * 1000, 1000.0),
                        ),
                    submissionTime = "2022-01-01T00:00",
                ),
            )

        val topology = createTopology("batteries/experiment3.json")
        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1150.0, monitor.powerDraws[0]) { "The power usage at timestamp 0 is not correct" } },
            { assertEquals(150.0, monitor.powerDraws[5]) { "The power usage at timestamp 0 is not correct" } },
            { assertEquals(0.0, monitor.powerDraws[9]) { "The power usage at timestamp 0 is not correct" } },
            { assertEquals(72000.0 + 12 * 60 * 150, monitor.energyUsages.sum()) { "The total power usage is not correct" } },
        )
    }

    /**
     * Battery test 4: One static task High Carbon followed by Low Carbon, Empty battery
     */
    @Test
    fun testBattery4() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(30 * 60 * 1000, 1000.0),
                        ),
                    submissionTime = "2022-01-01T00:00",
                ),
            )

        val topology = createTopology("batteries/experiment3.json")
        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1150.0, monitor.powerDraws[0]) { "The power usage at timestamp 0 is not correct" } },
            { assertEquals(150.0, monitor.powerDraws[5]) { "The power usage at timestamp 0 is not correct" } },
            { assertEquals(3 * 60 * 1000.0 + 10 * 60 * 150, monitor.energyUsages.sum()) { "The total power usage is not correct" } },
        )
    }

    /**
     * Battery test 5: One static task Alternating Low / High battery, battery never charges fully
     */
    @Test
    fun testBattery5() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(30 * 60 * 1000, 1000.0),
                        ),
                    submissionTime = "2022-01-01T00:00",
                ),
            )

        val topology = createTopology("batteries/experiment4.json")
        val monitor = runTest(topology, workload)

        val topologyBat = createTopology("batteries/experiment3.json")
        val monitorBat = runTest(topologyBat, workload)

        assertAll(
            { assertEquals(9000.0, monitor.energyUsages[0]) { "The power usage at timestamp 0 is not correct" } },
            { assertEquals(69000.0, monitorBat.energyUsages[0]) { "The power usage at timestamp 0 is not correct" } },
            { assertEquals(9000.0, monitor.energyUsages[2]) { "The power usage at timestamp 2 is not correct" } },
            { assertEquals(9000.0, monitorBat.energyUsages[2]) { "The power usage at timestamp 2 is not correct" } },
            { assertEquals(9000.0, monitor.energyUsages[10]) { "The power usage at timestamp 2 is not correct" } },
            { assertEquals(0.0, monitorBat.energyUsages[10]) { "The power usage at timestamp 2 is not correct" } },
            { assertEquals(9000.0, monitor.energyUsages[18]) { "The power usage at timestamp 2 is not correct" } },
            { assertEquals(9000.0, monitorBat.energyUsages[18]) { "The power usage at timestamp 2 is not correct" } },
            { assertEquals(30 * 60 * 150.0, monitor.energyUsages.sum()) { "The total power usage is not correct" } },
            { assertEquals(30 * 60 * 150.0, monitorBat.energyUsages.sum()) { "The total power usage is not correct" } },
            { assertEquals(8.0, monitor.carbonEmissions.sum(), 1e-2) { "The total power usage is not correct" } },
            { assertEquals(7.2, monitorBat.carbonEmissions.sum(), 1e-2) { "The total power usage is not correct" } },
        )
    }

    /**
     * Battery test 6: One static task Alternating Low / High battery, battery never charges fully
     */
    @Test
    fun testBattery6() {
        val numTasks = 1000

        val workload: ArrayList<Task> =
            arrayListOf<Task>().apply {
                repeat(numTasks) {
                    this.add(
                        createTestTask(
                            id = 0,
                            fragments =
                                arrayListOf(TraceFragment(10 * 60 * 1000, 1000.0)),
                            submissionTime = "2022-01-01T00:00",
                        ),
                    )
                }
            }

        val topologyBat = createTopology("batteries/experiment3.json")
        val monitorBat = runTest(topologyBat, workload)

        assertAll(
            { assertEquals(10L * 60 * 1000 * numTasks, monitorBat.maxTimestamp) { "The power usage at timestamp 0 is not correct" } },
        )
    }

    /**
     * Battery test 7: One static task High Carbon, Empty battery with failures
     */
    @Test
    fun testBattery7() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    submissionTime = "2022-01-01T00:00",
                ),
            )

        val failureModelSpec =
            TraceBasedFailureModelSpec(
                "src/test/resources/failureTraces/single_failure.parquet",
                repeat = false,
            )

        val topology = createTopology("batteries/experiment1.json")
        val monitor = runTest(topology, workload, failureModelSpec = failureModelSpec)

        assertAll(
            { assertEquals(20 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(150.0, monitor.powerDraws[0]) { "The power usage at timestamp 0 is not correct" } },
            { assertEquals(15 * 60 * 150.0 + 5 * 60 * 100.0, monitor.energyUsages.sum()) { "The total power usage is not correct" } },
        )
    }

    /**
     * Battery test 8: One static task High Carbon, Empty battery with failures and checkpointing
     */
    @Test
    fun testBattery8() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    checkpointInterval = 60 * 1000L,
                    checkpointDuration = 1000L,
                    submissionTime = "2022-01-01T00:00",
                ),
            )

        val failureModelSpec =
            TraceBasedFailureModelSpec(
                "src/test/resources/failureTraces/single_failure.parquet",
                repeat = false,
            )

        val topology = createTopology("batteries/experiment1.json")
        val monitor = runTest(topology, workload, failureModelSpec = failureModelSpec)

        println(monitor.hostEnergyUsages["H01"])

        assertAll(
            { assertEquals((960 * 1000) + 5000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            {
                assertEquals(
                    (665 * 150.0) + (300 * 100.0),
                    monitor.hostEnergyUsages["H01"]?.sum(),
                ) { "Incorrect energy usage" }
            },
        )
    }
}
