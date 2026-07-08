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

package org.opendc.sdk.runner.base
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.sdk.model.workload.ScalingPolicy
import org.opendc.sdk.runner.base.harness.createTestTask
import org.opendc.sdk.runner.base.harness.createTopology
import org.opendc.sdk.runner.base.harness.fragment
import org.opendc.sdk.runner.base.harness.runTest

/**
 * Integration tests ported one-to-one from `opendc-experiments-base`'s `FragmentScalingTest`,
 * driving the SDK runner and asserting the identical values.
 */
class FragmentScalingTest {
    @Test
    fun testScaling1() {
        val workloadNoDelay =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 2000.0),
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )

        val workloadPerfect =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 2000.0),
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitorPerfect = runTest(topology, workloadPerfect, scalingPolicy = ScalingPolicy.Perfect)
        val monitorNoDelay = runTest(topology, workloadNoDelay)

        assertAll(
            { assertEquals(1200000, monitorNoDelay.maxTimestamp) { "The workload took longer to finish than expected." } },
            { assertEquals(1200000, monitorPerfect.maxTimestamp) { "The workload took longer to finish than expected." } },
            { assertEquals(2000.0, monitorNoDelay.taskCpuDemands[0]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitorPerfect.taskCpuDemands[0]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitorNoDelay.taskCpuSupplied[0]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitorPerfect.taskCpuSupplied[0]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1000.0, monitorNoDelay.taskCpuDemands[0]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitorPerfect.taskCpuDemands[0]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitorNoDelay.taskCpuSupplied[0]?.get(9)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1000.0, monitorPerfect.taskCpuSupplied[0]?.get(9)) { "The cpu supplied to task 0 is incorrect" } },
        )
    }

    @Test
    fun testScaling2() {
        val workloadNoDelay =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 4000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )

        val workloadPerfect =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 4000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitorNoDelay = runTest(topology, workloadNoDelay)
        val monitorPerfect = runTest(topology, workloadPerfect, scalingPolicy = ScalingPolicy.Perfect)

        assertAll(
            { assertEquals(600000, monitorNoDelay.maxTimestamp) { "The workload took longer to finish than expected." } },
            { assertEquals(1200000, monitorPerfect.maxTimestamp) { "The workload took longer to finish than expected." } },
            { assertEquals(4000.0, monitorNoDelay.taskCpuDemands[0]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(4000.0, monitorPerfect.taskCpuDemands[0]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitorNoDelay.taskCpuSupplied[0]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitorPerfect.taskCpuSupplied[0]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
        )
    }

    @Test
    fun testScaling3() {
        val workloadNoDelay =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                            fragment(10 * 60 * 1000, 4000.0),
                            fragment(10 * 60 * 1000, 1500.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )

        val workloadPerfect =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                            fragment(10 * 60 * 1000, 4000.0),
                            fragment(10 * 60 * 1000, 1500.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitorNoDelay = runTest(topology, workloadNoDelay)
        val monitorPerfect = runTest(topology, workloadPerfect, scalingPolicy = ScalingPolicy.Perfect)

        assertAll(
            { assertEquals(1800000, monitorNoDelay.maxTimestamp) { "The workload took longer to finish than expected." } },
            { assertEquals(2400000, monitorPerfect.maxTimestamp) { "The workload took longer to finish than expected." } },
            { assertEquals(1000.0, monitorNoDelay.taskCpuDemands[0]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitorPerfect.taskCpuDemands[0]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitorNoDelay.taskCpuSupplied[0]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1000.0, monitorPerfect.taskCpuSupplied[0]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(4000.0, monitorNoDelay.taskCpuDemands[0]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(4000.0, monitorPerfect.taskCpuDemands[0]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitorNoDelay.taskCpuSupplied[0]?.get(9)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitorPerfect.taskCpuSupplied[0]?.get(9)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1500.0, monitorNoDelay.taskCpuDemands[0]?.get(19)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(4000.0, monitorPerfect.taskCpuDemands[0]?.get(19)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1500.0, monitorNoDelay.taskCpuSupplied[0]?.get(19)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitorPerfect.taskCpuSupplied[0]?.get(19)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1500.0, monitorPerfect.taskCpuDemands[0]?.get(29)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1500.0, monitorPerfect.taskCpuSupplied[0]?.get(29)) { "The cpu supplied to task 0 is incorrect" } },
        )
    }

    @Test
    fun testScaling4() {
        val workloadNoDelay =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 3000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )

        val workloadPerfect =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 3000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("single_2_2000.json")

        val monitorNoDelay = runTest(topology, workloadNoDelay)
        val monitorPerfect = runTest(topology, workloadPerfect, scalingPolicy = ScalingPolicy.Perfect)

        assertAll(
            { assertEquals(600000, monitorNoDelay.maxTimestamp) { "The workload took longer to finish than expected." } },
            { assertEquals(600000, monitorPerfect.maxTimestamp) { "The workload took longer to finish than expected." } },
            { assertEquals(1000.0, monitorNoDelay.taskCpuDemands[0]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitorNoDelay.taskCpuDemands[1]?.get(0)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(1000.0, monitorPerfect.taskCpuDemands[0]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitorPerfect.taskCpuDemands[1]?.get(0)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(1000.0, monitorNoDelay.taskCpuSupplied[0]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(3000.0, monitorNoDelay.taskCpuSupplied[1]?.get(0)) { "The cpu supplied to task 1 is incorrect" } },
            { assertEquals(1000.0, monitorPerfect.taskCpuSupplied[0]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(3000.0, monitorPerfect.taskCpuSupplied[1]?.get(0)) { "The cpu supplied to task 1 is incorrect" } },
        )
    }

    @Test
    fun testScaling5() {
        val workloadNoDelay =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 2000.0),
                        ),
                    cpuCoreCount = 1,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 4000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )

        val workloadPerfect =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 2000.0),
                        ),
                    cpuCoreCount = 1,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 4000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("single_2_2000.json")

//        val monitorNoDelay = runTest(topology, workloadNoDelay)
        val monitorPerfect = runTest(topology, workloadPerfect, scalingPolicy = ScalingPolicy.Perfect)

//        assertAll(
//            { assertEquals(600000, monitorNoDelay.maxTimestamp) { "The workload took longer to finish than expected." } },
//            { assertEquals(900000, monitorPerfect.maxTimestamp) { "The workload took longer to finish than expected." } },
//
//            { assertEquals(1000.0, monitorNoDelay.taskCpuDemands[0]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
//            { assertEquals(3000.0, monitorNoDelay.taskCpuDemands[1]?.get(0)) { "The cpu demanded by task 1 is incorrect" } },
//            { assertEquals(1000.0, monitorPerfect.taskCpuDemands[0]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
//            { assertEquals(3000.0, monitorPerfect.taskCpuDemands[1]?.get(0)) { "The cpu demanded by task 1 is incorrect" } },
//
//            { assertEquals(1000.0, monitorNoDelay.taskCpuSupplied[0]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
//            { assertEquals(3000.0, monitorNoDelay.taskCpuSupplied[1]?.get(0)) { "The cpu supplied to task 1 is incorrect" } },
//            { assertEquals(1000.0, monitorPerfect.taskCpuSupplied[0]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
//            { assertEquals(3000.0, monitorPerfect.taskCpuSupplied[1]?.get(0)) { "The cpu supplied to task 1 is incorrect" } },
//        )
    }
}
