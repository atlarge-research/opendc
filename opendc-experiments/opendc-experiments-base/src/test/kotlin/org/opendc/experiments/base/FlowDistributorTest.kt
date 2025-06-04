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
import org.opendc.simulator.compute.workload.trace.TraceFragment
import java.util.ArrayList

/**
 * Testing suite containing tests that specifically test the FlowDistributor
 */
class FlowDistributorTest {
    /**
     * FlowDistributor test 1: A single fitting task
     * In this test, a single task is scheduled that should fit the FlowDistributor
     * We check if both the host and the Task show the correct cpu usage and demand during the two fragments.
     */
    @Test
    fun testFlowDistributor1() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                            TraceFragment(10 * 60 * 1000, 2000.0, 1),
                        ),
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuDemands["0"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu used by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu used by the host is incorrect" } },
        )
    }

    /**
     * FlowDistributor test 2: A single overloaded task
     * In this test, a single task is scheduled that does not fit the FlowDistributor
     * In this test we expect the usage to be lower than the demand.
     * We check if both the host and the Task show the correct cpu usage and demand during the two fragments.
     */
    @Test
    fun testFlowDistributor2() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 3000.0, 1),
                            TraceFragment(10 * 60 * 1000, 4000.0, 1),
                        ),
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(3000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(4000.0, monitor.taskCpuDemands["0"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu used by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu used by the host is incorrect" } },
        )
    }

    /**
     * FlowDistributor test 3: A single task transition fit to overloaded
     * In this test, a single task is scheduled where the first fragment fits, but the second does not.
     * For the first fragment, we expect the usage of the second fragment to be lower than the demand
     * We check if both the host and the Task show the correct cpu usage and demand during the two fragments.
     */
    @Test
    fun testFlowDistributor3() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                            TraceFragment(10 * 60 * 1000, 4000.0, 1),
                        ),
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(4000.0, monitor.taskCpuDemands["0"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu used by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu used by the host is incorrect" } },
        )
    }

    /**
     * FlowDistributor test 4: A single task transition overload to fit
     * In this test, a single task is scheduled where the first fragment does not fit, and the second does.
     * For the first fragment, we expect the usage of the first fragment to be lower than the demand
     * We check if both the host and the Task show the correct cpu usage and demand during the two fragments.
     */
    @Test
    fun testFlowDistributor4() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 4000.0, 1),
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                        ),
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(4000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuDemands["0"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["0"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu used by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu used by the host is incorrect" } },
        )
    }

    /**
     * FlowDistributor test 5: A single task transition overload to perfect fit
     * In this test, a single task is scheduled where the first fragment does not fit, and the second does perfectly for the available CPU.
     * For the first fragment, we expect the usage of the first fragment to be lower than the demand
     * We check if both the host and the Task show the correct cpu usage and demand during the two fragments.
     */
    @Test
    fun testFlowDistributor5() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 4000.0, 1),
                            TraceFragment(10 * 60 * 1000, 2000.0, 1),
                        ),
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(4000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuDemands["0"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu used by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu used by the host is incorrect" } },
        )
    }

    /**
     * FlowDistributor test 6: Two task, same time, both fit
     * In this test, two tasks are scheduled, and they fit together on the host . The tasks start and finish at the same time
     * This test shows how the FlowDistributor handles two tasks that can fit and no redistribution is required.
     * We check if both the host and the Tasks show the correct cpu usage and demand during the two fragments.
     */
    @Test
    fun testFlowDistributor6() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                            TraceFragment(10 * 60 * 1000, 3000.0, 1),
                        ),
                ),
                createTestTask(
                    name = "1",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 3000.0, 1),
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                        ),
                ),
            )
        val topology = createTopology("single_2_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands["0"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuSupplied["0"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands["1"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuDemands["1"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuSupplied["1"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["1"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu used by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu used by the host is incorrect" } },
        )
    }

    /**
     * FlowDistributor test 7: Two task, same time, can not fit
     * In this test, two tasks are scheduled, and they can not both fit. The tasks start and finish at the same time
     * This test shows how the FlowDistributor handles two tasks that both do not fit and redistribution is required.
     * We check if both the host and the Tasks show the correct cpu usage and demand during the two fragments.
     */
    @Test
    fun testFlowDistributor7() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 6000.0, 1),
                            TraceFragment(10 * 60 * 1000, 5000.0, 1),
                        ),
                ),
                createTestTask(
                    name = "1",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 5000.0, 1),
                            TraceFragment(10 * 60 * 1000, 6000.0, 1),
                        ),
                ),
            )
        val topology = createTopology("single_2_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(6000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(5000.0, monitor.taskCpuDemands["0"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(5000.0, monitor.taskCpuDemands["1"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(6000.0, monitor.taskCpuDemands["1"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["1"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["1"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(11000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(11000.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu used by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu used by the host is incorrect" } },
        )
    }

    /**
     * FlowDistributor test 8: Two task, both fit, second task is delayed
     * In this test, two tasks are scheduled, the second task is delayed.
     * We check if both the host and the Tasks show the correct cpu usage and demand during the two fragments.
     */
    @Test
    fun testFlowDistributor8() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    submissionTime = "2024-02-01T10:00",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                            TraceFragment(10 * 60 * 1000, 2000.0, 1),
                        ),
                ),
                createTestTask(
                    name = "1",
                    submissionTime = "2024-02-01T10:05",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 2000.0, 1),
                        ),
                ),
            )
        val topology = createTopology("single_2_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuDemands["0"]?.get(5)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuDemands["0"]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuDemands["0"]?.get(14)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["0"]?.get(5)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(9)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(14)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuDemands["1"]?.get(1)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuDemands["1"]?.get(6)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["1"]?.get(1)) { "The cpu used by task 1 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["1"]?.get(6)) { "The cpu used by task 1 is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuDemands["H01"]?.get(5)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(9)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuDemands["H01"]?.get(14)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu used by the host is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuSupplied["H01"]?.get(5)) { "The cpu used by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(9)) { "The cpu used by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["H01"]?.get(14)) { "The cpu used by the host is incorrect" } },
        )
    }

    /**
     * FlowDistributor test 9: Two task, both fit on their own but not together, second task is delayed
     * In this test, two tasks are scheduled, the second task is delayed.
     * We check if both the host and the Tasks show the correct cpu usage and demand during the two fragments.
     * When the second task comes in, the host is overloaded.
     * This test shows how the FlowDistributor can handle redistribution when a new task comes in.
     * We check if both the host and the Tasks show the correct cpu usage and demand during the two fragments.
     */
    @Test
    fun testFlowDistributor9() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    submissionTime = "2024-02-01T10:00",
                    fragments =
                        arrayListOf(
                            TraceFragment(20 * 60 * 1000, 3000.0, 1),
                        ),
                ),
                createTestTask(
                    name = "1",
                    submissionTime = "2024-02-01T10:05",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1500.0, 1),
                        ),
                ),
            )
        val topology = createTopology("single_2_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(3000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands["0"]?.get(5)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands["0"]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands["0"]?.get(14)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2500.0, monitor.taskCpuSupplied["0"]?.get(5)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2500.0, monitor.taskCpuSupplied["0"]?.get(9)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuSupplied["0"]?.get(14)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(1500.0, monitor.taskCpuDemands["1"]?.get(1)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(1500.0, monitor.taskCpuDemands["1"]?.get(6)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(1500.0, monitor.taskCpuSupplied["1"]?.get(1)) { "The cpu used by task 1 is incorrect" } },
            { assertEquals(1500.0, monitor.taskCpuSupplied["1"]?.get(6)) { "The cpu used by task 1 is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4500.0, monitor.hostCpuDemands["H01"]?.get(5)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuDemands["H01"]?.get(14)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu used by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(5)) { "The cpu used by the host is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuSupplied["H01"]?.get(14)) { "The cpu used by the host is incorrect" } },
        )
    }

    /**
     * FlowDistributor test 10: Two task, one changes demand, causing overload
     * In this test, two tasks are scheduled, and they can both fit.
     * However, task 0 increases its demand which overloads the FlowDistributor.
     * This test shows how the FlowDistributor handles transition from fitting to overloading when multiple tasks are running.
     * We check if both the host and the Tasks show the correct cpu usage and demand during the two fragments.
     */
    @Test
    fun testFlowDistributor10() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(5 * 60 * 1000, 1000.0, 1),
                            TraceFragment(5 * 60 * 1000, 1500.0, 1),
                            TraceFragment(5 * 60 * 1000, 2500.0, 1),
                            TraceFragment(5 * 60 * 1000, 1000.0, 1),
                        ),
                ),
                createTestTask(
                    name = "1",
                    fragments =
                        arrayListOf(
                            TraceFragment(20 * 60 * 1000, 3000.0, 1),
                        ),
                ),
            )
        val topology = createTopology("single_2_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1500.0, monitor.taskCpuDemands["0"]?.get(5)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2500.0, monitor.taskCpuDemands["0"]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuDemands["0"]?.get(14)) { "The cpu demanded is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(1500.0, monitor.taskCpuSupplied["0"]?.get(5)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(9)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["0"]?.get(14)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands["1"]?.get(1)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands["1"]?.get(5)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands["1"]?.get(9)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands["1"]?.get(14)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuSupplied["1"]?.get(1)) { "The cpu used by task 1 is incorrect" } },
            { assertEquals(2500.0, monitor.taskCpuSupplied["1"]?.get(5)) { "The cpu used by task 1 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["1"]?.get(9)) { "The cpu used by task 1 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuSupplied["1"]?.get(14)) { "The cpu used by task 1 is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4500.0, monitor.hostCpuDemands["H01"]?.get(5)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(5500.0, monitor.hostCpuDemands["H01"]?.get(9)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(14)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu used by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(5)) { "The cpu used by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(9)) { "The cpu used by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(14)) { "The cpu used by the host is incorrect" } },
        )
    }

    /**
     * FlowDistributor test 11: 5000 hosts. This tests the performance of the distributor
     * In this test, two tasks are scheduled, and they can both fit.
     * However, task 0 increases its demand which overloads the FlowDistributor.
     * This test shows how the FlowDistributor handles transition from fitting to overloading when multiple tasks are running.
     * We check if both the host and the Tasks show the correct cpu usage and demand during the two fragments.
     */
    @Test
    fun testFlowDistributor11() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf<TraceFragment>().apply {
                            repeat(1) { this.add(TraceFragment(10 * 60 * 1000, 3000.0, 1)) }
                        },
                ),
            )
        val topology = createTopology("single_5000_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(10 * 60 * 1000, monitor.maxTimestamp) { "The expected runtime is exceeded" } },
        )
    }

    /**
     * FlowDistributor test 12: 1000 fragments. This tests the performance of the distributor
     * In this test, two tasks are scheduled, and they can both fit.
     * However, task 0 increases its demand which overloads the FlowDistributor.
     * This test shows how the FlowDistributor handles transition from fitting to overloading when multiple tasks are running.
     * We check if both the host and the Tasks show the correct cpu usage and demand during the two fragments.
     */
    @Test
    fun testFlowDistributor12() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf<TraceFragment>().apply {
                            repeat(1000) { this.add(TraceFragment(10 * 60 * 1000, 2000.0, 1)) }
                        },
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1000 * 10 * 60 * 1000, monitor.maxTimestamp) { "The expected runtime is exceeded" } },
        )
    }

    /**
     * FlowDistributor test 13: 1000 tasks. This tests the performance
     * In this test, two tasks are scheduled, and they can both fit.
     * However, task 0 increases its demand which overloads the FlowDistributor.
     * This test shows how the FlowDistributor handles transition from fitting to overloading when multiple tasks are running.
     * We check if both the host and the Tasks show the correct cpu usage and demand during the two fragments.
     */
    @Test
    fun testFlowDistributor13() {
        val workload: ArrayList<Task> =
            arrayListOf<Task>().apply {
                repeat(1000) {
                    this.add(
                        createTestTask(
                            name = "0",
                            fragments =
                                arrayListOf(TraceFragment(10 * 60 * 1000, 2000.0, 1)),
                        ),
                    )
                }
            }
        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1000 * 10 * 60 * 1000, monitor.maxTimestamp) { "The expected runtime is exceeded" } },
        )
    }

    /**
     * FlowDistributor test 14: A single fitting GPU task
     * In this test, a single task is scheduled that should fit the FlowDistributor
     * We check if both the host and the Task show the correct cpu and gpu usage and demand during the two fragments.
     */
    @Test
    fun testFlowDistributor14() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0,0,1000.0, 1),
                            TraceFragment(10 * 60 * 1000, 0.0,0,2000.0, 1),
                        ),
                ),
            )

        val topology = createTopology("Gpus/single_gpu_no_vendor_no_memory.json")

        val monitor = runTest(topology, workload)

        assertAll(
            // CPU
            // task
            { assertEquals(0.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuDemands["0"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuSupplied["0"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            // host
            { assertEquals(0.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(0.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(0.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu used by the host is incorrect" } },
            { assertEquals(0.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu used by the host is incorrect" } },
            //GPU
            // task
            { assertEquals(1000.0, monitor.taskGpuDemands["0"]?.get(1)?.get(0)) { "The gpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskGpuDemands["0"]?.get(10)?.get(0)) { "The gpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuSupplied["0"]?.get(1)?.get(0)) { "The gpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskGpuSupplied["0"]?.get(10)?.get(0)) { "The gpu used by task 0 is incorrect" } },
            // host
            { assertEquals(1000.0, monitor.hostGpuDemands["H01"]?.get(1)?.get(0)) { "The gpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostGpuDemands["H01"]?.get(10)?.get(0)) { "The gpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostGpuSupplied["H01"]?.get(1)?.get(0)) { "The gpu used by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostGpuSupplied["H01"]?.get(10)?.get(0)) { "The gpu used by the host is incorrect" } },
        )
    }
}
