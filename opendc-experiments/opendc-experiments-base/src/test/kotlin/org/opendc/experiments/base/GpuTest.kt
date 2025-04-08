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
import org.opendc.experiments.base.experiment.specs.TraceBasedFailureModelSpec
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
            { assertEquals(300.0, monitor.hostPowerDraws["H01"]?.get(25)) { "Incorrect energy usage at time 25" } },
        )
    }

    /**
     * Injecting a failure after 5 minutes. Failure lasts 5 minutes. No checkpointing
     */
    @Test
    fun testGpuFailure() {
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

        val failureModelSpec =
            TraceBasedFailureModelSpec(
                "src/test/resources/failureTraces/single_failure.parquet",
                repeat = false,
            )

        val topology = createTopology("gpu/single_1_2000.json")

        val monitor = runTest(topology, workload, failureModelSpec)

        assertAll(
            { assertEquals(20 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(300.0, monitor.hostPowerDraws["H01"]?.get(0)) { "Incorrect energy usage at 0" } },
            { assertEquals(300.0, monitor.hostPowerDraws["H01"]?.get(12)) { "Incorrect energy usage at 9" } },
        )
    }

    @Test
    fun testGpuCheckpoint() {
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
                    checkpointInterval = 60 * 1000L,
                    checkpointDuration = 1000L,
                ),
            )

        val failureModelSpec =
            TraceBasedFailureModelSpec(
                "src/test/resources/failureTraces/single_failure.parquet",
                repeat = false,
            )

        val topology = createTopology("gpu/single_1_2000.json")

        val monitor = runTest(topology, workload, failureModelSpec)

        assertAll(
            // Task run time + Time node is in failed state + checkpoint time + time waiting to be scheduled
            {
                assertEquals(
                    (10 * 60 * 1000) + (5 * 60 * 1000) + (9 * 1000) + (56 * 1000),
                    monitor.maxTimestamp,
                ) { "Total runtime incorrect" }
            },
            { assertEquals(300.0, monitor.hostPowerDraws["H01"]?.get(0)) { "Incorrect power draw at 0" } },
            { assertEquals(300.0, monitor.hostPowerDraws["H01"]?.get(12)) { "Incorrect power draw at 9" } },
        )
    }
}
