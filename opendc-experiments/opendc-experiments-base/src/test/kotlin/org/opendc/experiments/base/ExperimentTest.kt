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
import org.opendc.simulator.compute.workload.TraceFragment
import java.util.ArrayList

/**
 * An integration test suite for the Scenario experiments.
 */
class ExperimentTest {
    /**
     * Simulator test 1: Single Task
     * In this test, a single task is scheduled that takes 10 minutes to run.
     *
     * There should be no problems running the task, so the total runtime should be 10 min.
     *
     * The task is using 50% of the available CPU capacity.
     * This means that half of the time is active, and half is idle.
     * When the task is failed, all time is idle.
     */
    @Test
    fun testSimulator1() {
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

        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(10 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((10 * 30000)).toLong(), monitor.hostIdleTimes["H01"]?.sum()) { "Idle time incorrect" } },
            { assertEquals((10 * 30000).toLong(), monitor.hostActiveTimes["H01"]?.sum()) { "Active time incorrect" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect energy usage" } },
            { assertEquals(600 * 150.0, monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect energy usage" } },
        )
    }

    /**
     * Simulator test 1: Two Tasks
     * In this test, two tasks are scheduled.
     *
     * There should be no problems running the task, so the total runtime should be 15 min.
     *
     * The first task is using 50% of the available CPU capacity.
     * The second task is using 100% of the available CPU capacity.
     */
    @Test
    fun testSimulator2() {
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
                ),
            )

        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(15 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((10 * 30000)).toLong(), monitor.hostIdleTimes["H01"]?.sum()) { "Idle time incorrect" } },
            { assertEquals(((10 * 30000) + (5 * 60000)).toLong(), monitor.hostActiveTimes["H01"]?.sum()) { "Active time incorrect" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect energy usage" } },
            { assertEquals((600 * 150.0) + (300 * 200.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect energy usage" } },
        )
    }

    /**
     * Simulator test 3: Two Tasks, one scheduled later
     * In this test, two tasks are scheduled.
     *
     * There should be no problems running the task, so the total runtime should be 15 min.
     *
     * The first task is using 50% of the available CPU capacity.
     * The second task is using 100% of the available CPU capacity.
     */
    @Test
    fun testSimulator3() {
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
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                        ),
                ),
            )

        val topology = createTopology("single_2_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(10 * 60 * 1000, monitor.maxTimestamp) { "Total runtime incorrect" } },
            { assertEquals(((10 * 30000)).toLong(), monitor.hostIdleTimes["H01"]?.sum()) { "Idle time incorrect" } },
            { assertEquals(((10 * 30000)).toLong(), monitor.hostActiveTimes["H01"]?.sum()) { "Active time incorrect" } },
            { assertEquals(9000.0, monitor.hostEnergyUsages["H01"]?.get(0)) { "Incorrect energy usage" } },
            { assertEquals((600 * 150.0), monitor.hostEnergyUsages["H01"]?.sum()) { "Incorrect energy usage" } },
            { assertEquals((600 * 150.0), monitor.energyUsages.sum()) { "Incorrect energy usage" } },
        )
    }

    /**
     * Simulator test 4: Two Tasks, one scheduled later
     * In this test, two tasks are scheduled.
     *
     * There should be no problems running the task, so the total runtime should be 15 min.
     *
     * The first task is using 50% of the available CPU capacity.
     * The second task is using 100% of the available CPU capacity.
     */
    @Test
    fun testSimulator4() {
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

        val monitor = runTest(topology, workload)

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

//    /**
//     * Test a small simulation setup.
//     */
//    @Test
//    fun testSingleTask() =
//        runSimulation {
//            val seed = 1L
//            val workload = createTestWorkload("single_task", 1.0, seed)
//            val topology = createTopology("single.json")
//            val monitor = monitor
//
//            Provisioner(dispatcher, seed).use { provisioner ->
//                provisioner.runSteps(
//                    setupComputeService(serviceDomain = "compute.opendc.org", { computeScheduler }),
//                    registerComputeMonitor(serviceDomain = "compute.opendc.org", monitor),
//                    setupHosts(serviceDomain = "compute.opendc.org", topology),
//                )
//
//                val service = provisioner.registry.resolve("compute.opendc.org", ComputeService::class.java)!!
//                service.replay(timeSource, workload, seed = seed)
//            }
//
//            println(
//                "Scheduler " +
//                    "Success=${monitor.attemptsSuccess} " +
//                    "Failure=${monitor.attemptsFailure} " +
//                    "Error=${monitor.attemptsError} " +
//                    "Pending=${monitor.tasksPending} " +
//                    "Active=${monitor.tasksActive}",
//            )
//
//            // Note that these values have been verified beforehand
//            assertAll(
//                { assertEquals(0, monitor.idleTime) { "Idle time incorrect" } },
//                { assertEquals(3000000, monitor.activeTime) { "Active time incorrect" } },
//                { assertEquals(0, monitor.stealTime) { "Steal time incorrect" } },
//                { assertEquals(0, monitor.lostTime) { "Lost time incorrect" } },
//                { assertEquals(1200000.0, monitor.hostEnergyUsage.sum(), 1E4) { "Incorrect energy usage" } },
//            )
//        }
//
//    /**
//     * Test a small simulation setup.
//     */
//    @Test
//    fun testSmall() =
//        runSimulation {
//            val seed = 1L
//            val workload = createTestWorkload("bitbrains-small", 0.25, seed)
//            val topology = createTopology("single.json")
//            val monitor = monitor
//
//            Provisioner(dispatcher, seed).use { provisioner ->
//                provisioner.runSteps(
//                    setupComputeService(serviceDomain = "compute.opendc.org", { computeScheduler }),
//                    registerComputeMonitor(serviceDomain = "compute.opendc.org", monitor),
//                    setupHosts(serviceDomain = "compute.opendc.org", topology),
//                )
//
//                val service = provisioner.registry.resolve("compute.opendc.org", ComputeService::class.java)!!
//                service.replay(timeSource, workload, seed = seed)
//            }
//
//            println(
//                "Scheduler " +
//                    "Success=${monitor.attemptsSuccess} " +
//                    "Failure=${monitor.attemptsFailure} " +
//                    "Error=${monitor.attemptsError} " +
//                    "Pending=${monitor.tasksPending} " +
//                    "Active=${monitor.tasksActive}",
//            )
//
//            // Note that these values have been verified beforehand
//            assertAll(
//                { assertEquals(1803918435, monitor.idleTime) { "Idle time incorrect" } },
//                { assertEquals(787181565, monitor.activeTime) { "Active time incorrect" } },
//                { assertEquals(0, monitor.stealTime) { "Steal time incorrect" } },
//                { assertEquals(0, monitor.lostTime) { "Lost time incorrect" } },
//                { assertEquals(6.7565629E8, monitor.hostEnergyUsage.sum(), 1E4) { "Incorrect energy usage" } },
//            )
//        }
//
//    /**
//     * Test a large simulation setup.
//     */
//    @Test
//    fun testLarge() =
//        runSimulation {
//            val seed = 0L
//            val workload = createTestWorkload("bitbrains-small", 1.0, seed)
//            val topology = createTopology("multi.json")
//            val monitor = monitor
//
//            Provisioner(dispatcher, seed).use { provisioner ->
//                provisioner.runSteps(
//                    setupComputeService(serviceDomain = "compute.opendc.org", { computeScheduler }),
//                    registerComputeMonitor(serviceDomain = "compute.opendc.org", monitor),
//                    setupHosts(serviceDomain = "compute.opendc.org", topology),
//                )
//
//                val service = provisioner.registry.resolve("compute.opendc.org", ComputeService::class.java)!!
//                service.replay(timeSource, workload, seed = seed)
//            }
//
//            println(
//                "Scheduler " +
//                    "Success=${monitor.attemptsSuccess} " +
//                    "Failure=${monitor.attemptsFailure} " +
//                    "Error=${monitor.attemptsError} " +
//                    "Pending=${monitor.tasksPending} " +
//                    "Active=${monitor.tasksActive}",
//            )
//
//            // Note that these values have been verified beforehand
//            assertAll(
//                { assertEquals(50, monitor.attemptsSuccess, "The scheduler should schedule 50 VMs") },
//                { assertEquals(50, monitor.tasksCompleted, "The scheduler should schedule 50 VMs") },
//                { assertEquals(0, monitor.tasksTerminated, "The scheduler should schedule 50 VMs") },
//                { assertEquals(0, monitor.tasksActive, "All VMs should finish after a run") },
//                { assertEquals(0, monitor.attemptsFailure, "No VM should be unscheduled") },
//                { assertEquals(0, monitor.tasksPending, "No VM should not be in the queue") },
//                { assertEquals(43101787496, monitor.idleTime) { "Incorrect idle time" } },
//                { assertEquals(3489412504, monitor.activeTime) { "Incorrect active time" } },
//                { assertEquals(0, monitor.stealTime) { "Incorrect steal time" } },
//                { assertEquals(0, monitor.lostTime) { "Incorrect lost time" } },
//                { assertEquals(6.914184592181973E9, monitor.hostEnergyUsage.sum(), 1E4) { "Incorrect energy usage" } },
//            )
//        }
}
