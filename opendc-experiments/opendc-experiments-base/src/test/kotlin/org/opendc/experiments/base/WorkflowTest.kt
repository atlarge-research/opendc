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
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.compute.workload.Task
import org.opendc.simulator.compute.workload.trace.TraceFragment
import java.util.ArrayList

/**
 * An integration test suite for the Scenario experiments.
 */
class WorkflowTest {
    /**
     * Scenario test 1: simple workflow
     * In this test, a simple workflow with 4 tasks is executed on a single host with 2 CPUs.
     * The tasks are arranged in a diamond shape, where task 0 is the root task, tasks 1 and 2 are the children of task 0,
     * and task 3 is the child of tasks 1 and 2.
     *
     * There should be no problems running the task, so the total runtime should be 30 min because 1 and 2 can be executed
     * at the same time.
     *
     */
    @Test
    fun testWorkflow1() {
        val workload: ArrayList<ServiceTask> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = ArrayList<Int>(),
                    children = mutableSetOf(1, 2),
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = ArrayList<Int>(),
                    children = mutableSetOf(3),
                ),
                createTestTask(
                    id = 2,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = ArrayList<Int>(),
                    children = mutableSetOf(3),
                ),
                createTestTask(
                    id = 3,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = arrayListOf<Int>(1,2),
                    children = emptySet(),
                ),
            )

        val topology = createTopology("single_2_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(3 * 10 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            {
                assertEquals(
                    ((10 * 45000) + (10 * 30000) + (10 * 45000)).toLong(),
                    monitor.hostCpuIdleTimes["H01"]?.sum(),
                ) { "Idle time incorrect" }
            },
            {
                assertEquals(
                    ((10 * 15000) + (10 * 30000) + (10 * 15000)).toLong(),
                    monitor.hostCpuActiveTimes["H01"]?.sum(),
                ) { "Active time incorrect" }
            },
            { assertEquals(7500.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect host energy usage at timestamp 0" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(10)) { "Incorrect host energy usage at timestamp 0" } },
            { assertEquals(7500.0, monitor.hostEnergyUsages["H01"]?.get(20)) { "Incorrect host energy usage at timestamp 0" } },
            {
                assertEquals(
                    600 * 125.0 + 600 * 150.0 + 600 * 125.0,
                    monitor.hostEnergyUsages["H01"]?.sum(),
                ) { "Incorrect host energy usage" }
            },
            { assertEquals(600 * 125.0 + 600 * 150.0 + 600 * 125.0, monitor.energyUsages.sum()) { "Incorrect total energy usage" } },
        )
    }

    /**
     * Scenario test 2: simple workflow
     * In this test, a simple workflow with 4 tasks is executed on a single host with 2 CPUs.
     * The tasks are arranged in a diamond shape, where task 0 is the root task, tasks 1 and 2 are the children of task 0,
     * and task 3 is the child of tasks 1 and 2. However, task 2 has a shorter duration than task 1.
     *
     * There should be no problems running the task, so the total runtime should still be 30 min because
     * 3 can only be executed after 1 is finished.
     *
     */
    @Test
    fun testWorkflow2() {
        val workload: ArrayList<ServiceTask> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = ArrayList<Int>(),
                    children = mutableSetOf(1, 2),
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = ArrayList<Int>(),
                    children = mutableSetOf(3),
                ),
                createTestTask(
                    id = 2,
                    fragments =
                        arrayListOf(
                            TraceFragment(5 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = ArrayList<Int>(),
                    children = mutableSetOf(3),
                ),
                createTestTask(
                    id = 3,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = arrayListOf(1,2),
                    children = emptySet(),
                ),
            )

        val topology = createTopology("single_2_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(3 * 10 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            {
                assertEquals(
                    ((10 * 45000) + (5 * 30000) + (15 * 45000)).toLong(),
                    monitor.hostCpuIdleTimes["H01"]?.sum(),
                ) { "Idle time incorrect" }
            },
            {
                assertEquals(
                    ((10 * 15000) + (5 * 30000) + (15 * 15000)).toLong(),
                    monitor.hostCpuActiveTimes["H01"]?.sum(),
                ) { "Active time incorrect" }
            },
            {
                assertEquals(
                    7500.0,
                    monitor.hostEnergyUsages["H01"]?.get(0),
                ) { "Incorrect host energy usage at timestamp 0" }
            },
            {
                assertEquals(
                    9000.0,
                    monitor.hostEnergyUsages["H01"]?.get(10),
                ) { "Incorrect host energy usage at timestamp 0" }
            },
            {
                assertEquals(
                    7500.0,
                    monitor.hostEnergyUsages["H01"]?.get(20),
                ) { "Incorrect host energy usage at timestamp 0" }
            },
            {
                assertEquals(
                    600 * 125.0 + 300 * 150.0 + 900 * 125.0,
                    monitor.hostEnergyUsages["H01"]?.sum(),
                ) { "Incorrect host energy usage" }
            },
            {
                assertEquals(
                    600 * 125.0 + 300 * 150.0 + 900 * 125.0,
                    monitor.energyUsages.sum(),
                ) { "Incorrect total energy usage" }
            },
        )
    }

    /**
     * Scenario test 3: simple workflow with unconnected extra task
     * In this test, a simple workflow with 4 tasks is executed on a single host with 2 CPUs.
     * However, there is also another task that is not connected to the workflow running.
     *
     * This means that the workflow cannot be parallelized as the extra task will take up one CPU for 40 minutes.
     *
     * The total runtime should therefore be 40 minutes.
     */
    @Test
    fun testWorkflow3() {
        val workload: ArrayList<ServiceTask> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = ArrayList<Int>(),
                    children = mutableSetOf(1, 2),
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = ArrayList<Int>(),
                    children = mutableSetOf(3),
                ),
                createTestTask(
                    id = 2,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = ArrayList<Int>(),
                    children = mutableSetOf(3),
                ),
                createTestTask(
                    id = 3,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = arrayListOf(1,2),
                    children = emptySet(),
                ),
                createTestTask(
                    id = 5,
                    fragments =
                        arrayListOf(
                            TraceFragment(40 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )

        val topology = createTopology("single_2_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(4 * 10 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            {
                assertEquals(
                    ((40 * 30000)).toLong(),
                    monitor.hostCpuIdleTimes["H01"]?.sum(),
                ) { "Idle time incorrect" }
            },
            {
                assertEquals(
                    ((40 * 30000)).toLong(),
                    monitor.hostCpuActiveTimes["H01"]?.sum(),
                ) { "Active time incorrect" }
            },
            {
                assertEquals(
                    9000.0,
                    monitor.hostEnergyUsages["H01"]?.get(0),
                ) { "Incorrect host energy usage at timestamp 0" }
            },
            {
                assertEquals(
                    9000.0,
                    monitor.hostEnergyUsages["H01"]?.get(10),
                ) { "Incorrect host energy usage at timestamp 0" }
            },
            {
                assertEquals(
                    9000.0,
                    monitor.hostEnergyUsages["H01"]?.get(20),
                ) { "Incorrect host energy usage at timestamp 0" }
            },
            {
                assertEquals(
                    2400 * 150.0,
                    monitor.hostEnergyUsages["H01"]?.sum(),
                ) { "Incorrect host energy usage" }
            },
            {
                assertEquals(
                    2400 * 150.0,
                    monitor.energyUsages.sum(),
                ) { "Incorrect total energy usage" }
            },
        )
    }

    /**
     * Scenario test 4: simple workflow with unconnected extra task
     * In this test, a simple workflow with 4 tasks is executed on a single host with 2 CPUs.
     * However, there is also another task that is not connected to the workflow running.
     *
     * This means that the workflow cannot be parallelized for the first 15 minutes as the extra task will take up one CPU.
     * After that, the workflow can be parallelized as the extra task is finished.
     *
     * The total runtime should therefore be 35 minutes.
     */
    @Test
    fun testWorkflow4() {
        val workload: ArrayList<ServiceTask> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = ArrayList<Int>(),
                    children = mutableSetOf(1, 2),
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = ArrayList<Int>(),
                    children = mutableSetOf(3),
                ),
                createTestTask(
                    id = 2,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = ArrayList<Int>(),
                    children = mutableSetOf(3),
                ),
                createTestTask(
                    id = 3,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = arrayListOf(1,2),
                    children = emptySet(),
                ),
                createTestTask(
                    id = 5,
                    fragments =
                        arrayListOf(
                            TraceFragment(15 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )

        val topology = createTopology("single_2_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(35 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            {
                assertEquals(
                    ((20 * 30000) + (15 * 45000)).toLong(),
                    monitor.hostCpuIdleTimes["H01"]?.sum(),
                ) { "Idle time incorrect" }
            },
            {
                assertEquals(
                    ((20 * 30000) + (15 * 15000)).toLong(),
                    monitor.hostCpuActiveTimes["H01"]?.sum(),
                ) { "Active time incorrect" }
            },
            {
                assertEquals(
                    9000.0,
                    monitor.hostEnergyUsages["H01"]?.get(0),
                ) { "Incorrect host energy usage at timestamp 0" }
            },
            {
                assertEquals(
                    9000.0,
                    monitor.hostEnergyUsages["H01"]?.get(10),
                ) { "Incorrect host energy usage at timestamp 0" }
            },
            {
                assertEquals(
                    7500.0,
                    monitor.hostEnergyUsages["H01"]?.get(20),
                ) { "Incorrect host energy usage at timestamp 0" }
            },
            {
                assertEquals(
                    1200 * 150.0 + 900 * 125.0,
                    monitor.hostEnergyUsages["H01"]?.sum(),
                ) { "Incorrect host energy usage" }
            },
            {
                assertEquals(
                    1200 * 150.0 + 900 * 125.0,
                    monitor.energyUsages.sum(),
                ) { "Incorrect total energy usage" }
            },
        )
    }

    /**
     * Scenario test 3: workflow for which the first task cannot be scheduled.
     * In this test, a simple workflow with 4 tasks is executed on a single host with 2 CPUs.
     * However, the first task can not be scheduled on the host due to its high CPU requirement.
     * The whole workflow is therefore terminated.
     *
     * The single unrelated task should still be executed
     *
     * The total runtime should therefore be 10 minutes.
     */
    @Test
    fun testWorkflow5() {
        val workload: ArrayList<ServiceTask> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 10000.0),
                        ),
                    cpuCoreCount = 10,
                    parents = ArrayList<Int>(),
                    children = mutableSetOf(1, 2),
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = ArrayList<Int>(),
                    children = mutableSetOf(3),
                ),
                createTestTask(
                    id = 2,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = ArrayList<Int>(),
                    children = mutableSetOf(3),
                ),
                createTestTask(
                    id = 3,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = arrayListOf(1,2),
                    children = emptySet(),
                ),
                createTestTask(
                    id = 5,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )

        val topology = createTopology("single_2_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(10 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            {
                assertEquals(
                    ((10 * 45000)).toLong(),
                    monitor.hostCpuIdleTimes["H01"]?.sum(),
                ) { "Idle time incorrect" }
            },
            {
                assertEquals(
                    ((10 * 15000)).toLong(),
                    monitor.hostCpuActiveTimes["H01"]?.sum(),
                ) { "Active time incorrect" }
            },
            {
                assertEquals(
                    7500.0,
                    monitor.hostEnergyUsages["H01"]?.get(0),
                ) { "Incorrect host energy usage at timestamp 0" }
            },
            {
                assertEquals(
                    600 * 125.0,
                    monitor.hostEnergyUsages["H01"]?.sum(),
                ) { "Incorrect host energy usage" }
            },
            {
                assertEquals(
                    600 * 125.0,
                    monitor.energyUsages.sum(),
                ) { "Incorrect total energy usage" }
            },
        )
    }
}
