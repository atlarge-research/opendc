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
import org.opendc.simulator.compute.workload.TraceFragment
import java.util.ArrayList

/**
 * An integration test suite for the Scenario experiments.
 */
class FailuresAndCheckpointingTest {
    /**
     * Failure test 1: Single Task, Single Failure
     * In this test, a single task is scheduled that is interrupted by a failure after 5 min.
     * Because there is no checkpointing, the full task has to be rerun.
     *
     * This means the final runtime is 20 minutes
     *
     * When the task is running, it is using 50% of the cpu.
     * This means that half of the time is active, and half is idle.
     * When the task is failed, all time is idle.
     */
    @Test
    fun testFailures1() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
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

        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload, failureModelSpec)

        assertAll(
            { assertEquals(20 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((15 * 30000) + (5 * 60000)).toLong(), monitor.hostIdleTimes["H01"]?.sum()) { "Idle time incorrect" } },
            { assertEquals((15 * 30000).toLong(), monitor.hostActiveTimes["H01"]?.sum()) { "Active time incorrect" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect energy usage" } },
            { assertEquals(6000.0, monitor.hostEnergyUsages["H01"]?.get(5)) { "Incorrect energy usage" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(10)) { "Incorrect energy usage" } },
            { assertEquals((15 * 60 * 150.0) + (5 * 60 * 100.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect energy usage" } },
        )
    }

    /**
     * Failure test 2: Single Task, Failure much later
     * In this test, a single task is scheduled, with a failure trace.
     *
     * However, the first failure occurs after 500 min and should thus not affect the Task.
     */
    @Test
    fun testFailures2() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                        ),
                ),
            )

        val failureModelSpec =
            TraceBasedFailureModelSpec(
                "src/test/resources/failureTraces/single_failure_2.parquet",
                repeat = false,
            )

        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload, failureModelSpec)

        assertAll(
            { assertEquals(10 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals((10 * 30000).toLong(), monitor.hostIdleTimes["H01"]?.sum()) { "Idle time incorrect" } },
            { assertEquals((10 * 30000).toLong(), monitor.hostActiveTimes["H01"]?.sum()) { "Active time incorrect" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect energy usage" } },
            { assertEquals((600 * 150.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect energy usage" } },
        )
    }

    /**
     * Failure test 3: Single Task, Single Failure
     * In this test, a single task is scheduled that is interrupted by a failure after 5 min.
     * Because there is no checkpointing, the full task has to be rerun.
     *
     * This means the final runtime is 20 minutes
     *
     * When the task is running, it is using 50% of the cpu.
     * This means that half of the time is active, and half is idle.
     * When the task is failed, all time is idle.
     */
    @Test
    fun testFailures3() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                        ),
                ),
            )

        val failureModelSpec =
            TraceBasedFailureModelSpec(
                "src/test/resources/failureTraces/two_failures.parquet",
                repeat = false,
            )

        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload, failureModelSpec)

        assertAll(
            { assertEquals(37 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((22 * 30000) + (15 * 60000)).toLong(), monitor.hostIdleTimes["H01"]?.sum()) { "Idle time incorrect" } },
            { assertEquals((22 * 30000).toLong(), monitor.hostActiveTimes["H01"]?.sum()) { "Active time incorrect" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect energy usage" } },
            { assertEquals(6000.0, monitor.hostEnergyUsages["H01"]?.get(5)) { "Incorrect energy usage" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(10)) { "Incorrect energy usage" } },
            { assertEquals((22 * 60 * 150.0) + (15 * 60 * 100.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect energy usage" } },
        )
    }

    /**
     * Failure test 4: Single Task, repeated failure
     * In this test, a single task is scheduled that is interrupted by a failure after 5 min.
     * Because there is no checkpointing, the full task has to be rerun.
     *
     * This means the final runtime is 20 minutes
     *
     * When the task is running, it is using 50% of the cpu.
     * This means that half of the time is active, and half is idle.
     * When the task is failed, all time is idle.
     */
    @Test
    fun testFailures4() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                        ),
                ),
            )

        val failureModelSpec =
            TraceBasedFailureModelSpec(
                "src/test/resources/failureTraces/single_failure.parquet",
                repeat = true,
            )

        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload, failureModelSpec)

        assertAll(
            { assertEquals(95 * 60000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((50 * 60000) + (20 * 60000)).toLong(), monitor.hostIdleTimes["H01"]?.sum()) { "Idle time incorrect" } },
            { assertEquals((25 * 60000).toLong(), monitor.hostActiveTimes["H01"]?.sum()) { "Active time incorrect" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect energy usage" } },
            { assertEquals(6000.0, monitor.hostEnergyUsages["H01"]?.get(5)) { "Incorrect energy usage" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(10)) { "Incorrect energy usage" } },
            { assertEquals(6000.0, monitor.hostEnergyUsages["H01"]?.get(15)) { "Incorrect energy usage" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(20)) { "Incorrect energy usage" } },
            { assertEquals(6000.0, monitor.hostEnergyUsages["H01"]?.get(25)) { "Incorrect energy usage" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(30)) { "Incorrect energy usage" } },
            { assertEquals(6000.0, monitor.hostEnergyUsages["H01"]?.get(35)) { "Incorrect energy usage" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(40)) { "Incorrect energy usage" } },
            { assertEquals(6000.0, monitor.hostEnergyUsages["H01"]?.get(45)) { "Incorrect energy usage" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(50)) { "Incorrect energy usage" } },
            { assertEquals(6000.0, monitor.hostEnergyUsages["H01"]?.get(55)) { "Incorrect energy usage" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(60)) { "Incorrect energy usage" } },
            { assertEquals(6000.0, monitor.hostEnergyUsages["H01"]?.get(65)) { "Incorrect energy usage" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(70)) { "Incorrect energy usage" } },
            { assertEquals(6000.0, monitor.hostEnergyUsages["H01"]?.get(75)) { "Incorrect energy usage" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(80)) { "Incorrect energy usage" } },
            { assertEquals(6000.0, monitor.hostEnergyUsages["H01"]?.get(85)) { "Incorrect energy usage" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(90)) { "Incorrect energy usage" } },
            { assertEquals(0.0, monitor.hostEnergyUsages["H01"]?.get(95)) { "Incorrect energy usage" } },
            { assertEquals((10 * 300 * 150.0) + (9 * 300 * 100.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect energy usage" } },
        )
    }

    /**
     * Failure test 1: Single Task with checkpointing
     * In this test, a single task is scheduled that is interrupted by a failure after 5 min.
     * Because there is no checkpointing, the full task has to be rerun.
     *
     * This means the final runtime is 20 minutes
     *
     * When the task is running, it is using 50% of the cpu.
     * This means that half of the time is active, and half is idle.
     * When the task is failed, all time is idle.
     */
    @Test
    fun testCheckpoints1() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                        ),
                    checkpointInterval = 60 * 1000L,
                    checkpointDuration = 1000L,
                ),
            )

        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals((10 * 60000) + (9 * 1000), monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((10 * 30000)).toLong(), monitor.hostIdleTimes["H01"]?.sum()) { "Idle time incorrect" } },
            { assertEquals(((10 * 30000) + (9 * 1000)).toLong(), monitor.hostActiveTimes["H01"]?.sum()) { "Active time incorrect" } },
            { assertEquals((10 * 60 * 150.0) + (9 * 200.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect energy usage" } },
        )
    }

    /**
     * Failure test 2: Single Task with scaling checkpointing
     * In this test, a single task is scheduled that is interrupted by a failure after 5 min.
     * Because there is no checkpointing, the full task has to be rerun.
     *
     * This means the final runtime is 20 minutes
     *
     * When the task is running, it is using 50% of the cpu.
     * This means that half of the time is active, and half is idle.
     * When the task is failed, all time is idle.
     */
    @Test
    fun testCheckpoints2() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                        ),
                    checkpointInterval = 60 * 1000L,
                    checkpointDuration = 1000L,
                    checkpointIntervalScaling = 1.5,
                ),
            )

        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals((10 * 60000) + (4 * 1000), monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((10 * 30000)).toLong(), monitor.hostIdleTimes["H01"]?.sum()) { "Idle time incorrect" } },
            { assertEquals(((10 * 30000) + (4 * 1000)).toLong(), monitor.hostActiveTimes["H01"]?.sum()) { "Active time incorrect" } },
            { assertEquals((10 * 60 * 150.0) + (4 * 200.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect energy usage" } },
        )
    }

    /**
     * Checkpoint test 3: Single Task, single failure with checkpointing
     * In this test, a single task is scheduled that is interrupted by a failure after 5 min.
     * Because there is no checkpointing, the full task has to be rerun.
     *
     */
    @Test
    fun testCheckpoints3() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
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

        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload, failureModelSpec)

        assertAll(
            { assertEquals((960 * 1000) + 5000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            {
                assertEquals(
                    ((300 * 1000) + (296 * 500) + (360 * 500)).toLong(),
                    monitor.hostIdleTimes["H01"]?.sum(),
                ) { "Idle time incorrect" }
            },
            {
                assertEquals(
                    ((296 * 500) + 4000 + (360 * 500) + 5000).toLong(),
                    monitor.hostActiveTimes["H01"]?.sum(),
                ) { "Active time incorrect" }
            },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect energy usage" } },
            { assertEquals(6000.0, monitor.hostEnergyUsages["H01"]?.get(5)) { "Incorrect energy usage" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(10)) { "Incorrect energy usage" } },
            {
                assertEquals(
                    (296 * 150.0) + (4 * 200.0) + (300 * 100.0) +
                        (360 * 150.0) + (5 * 200.0),
                    monitor.hostEnergyUsages["H01"]?.sum(),
                ) { "Incorrect energy usage" }
            },
        )
    }

    /**
     * Checkpoint test 4: Single Task, repeated failure with checkpointing
     * In this test, a single task is scheduled that is interrupted by a failure after 5 min.
     * Because there is no checkpointing, the full task has to be rerun.
     *
     */
    @Test
    fun testCheckpoints4() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
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
                repeat = true,
            )

        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload, failureModelSpec)

        assertAll(
            { assertEquals((22 * 60000) + 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            {
                assertEquals(
                    ((10 * 60000) + (2 * 296 * 500) + (120 * 500)).toLong(),
                    monitor.hostIdleTimes["H01"]?.sum(),
                ) { "Idle time incorrect" }
            },
            {
                assertEquals(
                    ((2 * 296 * 500) + 8000 + (120 * 500) + 1000).toLong(),
                    monitor.hostActiveTimes["H01"]?.sum(),
                ) { "Active time incorrect" }
            },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect energy usage" } },
            { assertEquals(6000.0, monitor.hostEnergyUsages["H01"]?.get(5)) { "Incorrect energy usage" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(10)) { "Incorrect energy usage" } },
            {
                assertEquals(
                    (2 * 296 * 150.0) + (8 * 200.0) + (600 * 100.0) +
                        (120 * 150.0) + (200.0),
                    monitor.hostEnergyUsages["H01"]?.sum(),
                ) { "Incorrect energy usage" }
            },
        )
    }
}
