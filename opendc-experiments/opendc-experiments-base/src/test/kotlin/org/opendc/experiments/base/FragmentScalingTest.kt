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
import org.opendc.simulator.compute.workload.trace.scaling.NoDelayScaling
import org.opendc.simulator.compute.workload.trace.scaling.PerfectScaling
import java.util.ArrayList

/**
 * Testing suite containing tests that specifically test the scaling of trace fragments
 */
class FragmentScalingTest {
    /**
     * Scaling test 1: A single fitting task
     * In this test, a single task is scheduled that should fit the system.
     * This means nothing will be delayed regardless of the scaling policy
     */
    @Test
    fun testScaling1() {
        val workloadNoDelay: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 2000.0, 1),
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                        ),
                    scalingPolicy = NoDelayScaling(),
                ),
            )

        val workloadPerfect: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 2000.0, 1),
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                        ),
                    scalingPolicy = PerfectScaling(),
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitorNoDelay = runTest(topology, workloadNoDelay)
        val monitorPerfect = runTest(topology, workloadPerfect)

        assertAll(
            { assertEquals(1200000, monitorNoDelay.maxTimestamp) { "The workload took longer to finish than expected." } },
            { assertEquals(1200000, monitorPerfect.maxTimestamp) { "The workload took longer to finish than expected." } },
            { assertEquals(2000.0, monitorNoDelay.taskCpuDemands["0"]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitorPerfect.taskCpuDemands["0"]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitorNoDelay.taskCpuSupplied["0"]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitorPerfect.taskCpuSupplied["0"]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1000.0, monitorNoDelay.taskCpuDemands["0"]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitorPerfect.taskCpuDemands["0"]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitorNoDelay.taskCpuSupplied["0"]?.get(9)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1000.0, monitorPerfect.taskCpuSupplied["0"]?.get(9)) { "The cpu supplied to task 0 is incorrect" } },
        )
    }

    /**
     * Scaling test 2: A single task with a single non-fitting fragment
     * In this test, a single task is scheduled that should not fit.
     * This means the Task is getting only 2000 Mhz while it was demanding 4000 Mhz
     *
     * For the NoDelay scaling policy, the task should take the planned 10 min.
     * For the Perfect scaling policy, the task should be slowed down by 50% resulting in a runtime of 20 min.
     */
    @Test
    fun testScaling2() {
        val workloadNoDelay: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 4000.0, 1),
                        ),
                    scalingPolicy = NoDelayScaling(),
                ),
            )

        val workloadPerfect: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 4000.0, 1),
                        ),
                    scalingPolicy = PerfectScaling(),
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitorNoDelay = runTest(topology, workloadNoDelay)
        val monitorPerfect = runTest(topology, workloadPerfect)

        assertAll(
            { assertEquals(600000, monitorNoDelay.maxTimestamp) { "The workload took longer to finish than expected." } },
            { assertEquals(1200000, monitorPerfect.maxTimestamp) { "The workload took longer to finish than expected." } },
            { assertEquals(4000.0, monitorNoDelay.taskCpuDemands["0"]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(4000.0, monitorPerfect.taskCpuDemands["0"]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitorNoDelay.taskCpuSupplied["0"]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitorPerfect.taskCpuSupplied["0"]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
        )
    }

    /**
     * Scaling test 3: A single task that switches between fitting and not fitting
     * In this test, a single task is scheduled has one fragment that does not fit
     * This means the second fragment is getting only 2000 Mhz while it was demanding 4000 Mhz
     *
     * For the NoDelay scaling policy, the task should take the planned 30 min.
     * For the Perfect scaling policy, the second fragment should be slowed down by 50% resulting in a runtime of 20 min,
     * and a total runtime of 40 min.
     */
    @Test
    fun testScaling3() {
        val workloadNoDelay: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                            TraceFragment(10 * 60 * 1000, 4000.0, 1),
                            TraceFragment(10 * 60 * 1000, 1500.0, 1),
                        ),
                    scalingPolicy = NoDelayScaling(),
                ),
            )

        val workloadPerfect: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                            TraceFragment(10 * 60 * 1000, 4000.0, 1),
                            TraceFragment(10 * 60 * 1000, 1500.0, 1),
                        ),
                    scalingPolicy = PerfectScaling(),
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitorNoDelay = runTest(topology, workloadNoDelay)
        val monitorPerfect = runTest(topology, workloadPerfect)

        assertAll(
            { assertEquals(1800000, monitorNoDelay.maxTimestamp) { "The workload took longer to finish than expected." } },
            { assertEquals(2400000, monitorPerfect.maxTimestamp) { "The workload took longer to finish than expected." } },
            { assertEquals(1000.0, monitorNoDelay.taskCpuDemands["0"]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitorPerfect.taskCpuDemands["0"]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitorNoDelay.taskCpuSupplied["0"]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1000.0, monitorPerfect.taskCpuSupplied["0"]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(4000.0, monitorNoDelay.taskCpuDemands["0"]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(4000.0, monitorPerfect.taskCpuDemands["0"]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitorNoDelay.taskCpuSupplied["0"]?.get(9)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitorPerfect.taskCpuSupplied["0"]?.get(9)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1500.0, monitorNoDelay.taskCpuDemands["0"]?.get(19)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(4000.0, monitorPerfect.taskCpuDemands["0"]?.get(19)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1500.0, monitorNoDelay.taskCpuSupplied["0"]?.get(19)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitorPerfect.taskCpuSupplied["0"]?.get(19)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1500.0, monitorPerfect.taskCpuDemands["0"]?.get(29)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1500.0, monitorPerfect.taskCpuSupplied["0"]?.get(29)) { "The cpu supplied to task 0 is incorrect" } },
        )
    }

    /**
     * Scaling test 4: Two tasks, that both fit
     * In this test, two tasks are scheduled that both fit
     *
     * For both scaling policies, the tasks should run without delay.
     */
    @Test
    fun testScaling4() {
        val workloadNoDelay: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                        ),
                    scalingPolicy = NoDelayScaling(),
                ),
                createTestTask(
                    name = "1",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 3000.0, 1),
                        ),
                    scalingPolicy = NoDelayScaling(),
                ),
            )

        val workloadPerfect: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1),
                        ),
                    scalingPolicy = PerfectScaling(),
                ),
                createTestTask(
                    name = "1",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 3000.0, 1),
                        ),
                    scalingPolicy = PerfectScaling(),
                ),
            )
        val topology = createTopology("single_2_2000.json")

        val monitorNoDelay = runTest(topology, workloadNoDelay)
        val monitorPerfect = runTest(topology, workloadPerfect)

        assertAll(
            { assertEquals(600000, monitorNoDelay.maxTimestamp) { "The workload took longer to finish than expected." } },
            { assertEquals(600000, monitorPerfect.maxTimestamp) { "The workload took longer to finish than expected." } },
            { assertEquals(1000.0, monitorNoDelay.taskCpuDemands["0"]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitorNoDelay.taskCpuDemands["1"]?.get(0)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(1000.0, monitorPerfect.taskCpuDemands["0"]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitorPerfect.taskCpuDemands["1"]?.get(0)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(1000.0, monitorNoDelay.taskCpuSupplied["0"]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(3000.0, monitorNoDelay.taskCpuSupplied["1"]?.get(0)) { "The cpu supplied to task 1 is incorrect" } },
            { assertEquals(1000.0, monitorPerfect.taskCpuSupplied["0"]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(3000.0, monitorPerfect.taskCpuSupplied["1"]?.get(0)) { "The cpu supplied to task 1 is incorrect" } },
        )
    }

    /**
     * Scaling test 5: Two tasks, that don't fit together
     * In this test, two tasks are scheduled that do not fit together
     * This means the Task_1 is getting only 2000 Mhz while it was demanding 4000 Mhz
     *
     * For the NoDelay scaling policy, the tasks should complete in 10 min
     * For the Perfect scaling policy, task_1 is delayed while task_0 is still going.
     * In the first 10 min (while Task_0 is still running), Task_1 is running at 50%.
     * This means that after Task_0 is done, Task_1 still needs to run for 5 minutes, making the total runtime 15 min.
     */
    @Test
    fun testScaling5() {
        val workloadNoDelay: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 2000.0, 1),
                        ),
                    scalingPolicy = NoDelayScaling(),
                ),
                createTestTask(
                    name = "1",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 4000.0, 1),
                        ),
                    scalingPolicy = NoDelayScaling(),
                ),
            )

        val workloadPerfect: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 2000.0, 1),
                        ),
                    scalingPolicy = PerfectScaling(),
                ),
                createTestTask(
                    name = "1",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 4000.0, 1),
                        ),
                    scalingPolicy = PerfectScaling(),
                ),
            )
        val topology = createTopology("single_2_2000.json")

//        val monitorNoDelay = runTest(topology, workloadNoDelay)
        val monitorPerfect = runTest(topology, workloadPerfect)

//        assertAll(
//            { assertEquals(600000, monitorNoDelay.maxTimestamp) { "The workload took longer to finish than expected." } },
//            { assertEquals(900000, monitorPerfect.maxTimestamp) { "The workload took longer to finish than expected." } },
//
//            { assertEquals(1000.0, monitorNoDelay.taskCpuDemands["0"]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
//            { assertEquals(3000.0, monitorNoDelay.taskCpuDemands["1"]?.get(0)) { "The cpu demanded by task 1 is incorrect" } },
//            { assertEquals(1000.0, monitorPerfect.taskCpuDemands["0"]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
//            { assertEquals(3000.0, monitorPerfect.taskCpuDemands["1"]?.get(0)) { "The cpu demanded by task 1 is incorrect" } },
//
//            { assertEquals(1000.0, monitorNoDelay.taskCpuSupplied["0"]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
//            { assertEquals(3000.0, monitorNoDelay.taskCpuSupplied["1"]?.get(0)) { "The cpu supplied to task 1 is incorrect" } },
//            { assertEquals(1000.0, monitorPerfect.taskCpuSupplied["0"]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
//            { assertEquals(3000.0, monitorPerfect.taskCpuSupplied["1"]?.get(0)) { "The cpu supplied to task 1 is incorrect" } },
//        )
    }
}
