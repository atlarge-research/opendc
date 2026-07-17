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
import org.opendc.sdk.model.workload.TaskFragmentSpec
import org.opendc.sdk.model.workload.TaskSpec
import org.opendc.sdk.runner.base.harness.createTestTask
import org.opendc.sdk.runner.base.harness.createTopology
import org.opendc.sdk.runner.base.harness.fragment
import org.opendc.sdk.runner.base.harness.runTest

/**
 * Integration tests ported one-to-one from `opendc-experiments-base`'s `FlowDistributorTest`,
 * driving the SDK runner and asserting the identical values.
 */
class FlowDistributorTest {
    @Test
    fun testFlowDistributor1() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                            fragment(10 * 60 * 1000, 2000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1000.0, monitor.taskCpuDemands[0]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuDemands[0]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied[0]?.get(1)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[0]?.get(10)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu supplied to the host is incorrect" } },
        )
    }

    @Test
    fun testFlowDistributor2() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 3000.0),
                            fragment(10 * 60 * 1000, 4000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(3000.0, monitor.taskCpuDemands[0]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(4000.0, monitor.taskCpuDemands[0]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[0]?.get(1)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[0]?.get(10)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu supplied to the host is incorrect" } },
        )
    }

    @Test
    fun testFlowDistributor3() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                            fragment(10 * 60 * 1000, 4000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1000.0, monitor.taskCpuDemands[0]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(4000.0, monitor.taskCpuDemands[0]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied[0]?.get(1)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[0]?.get(10)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu supplied to the host is incorrect" } },
        )
    }

    @Test
    fun testFlowDistributor4() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 4000.0),
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(4000.0, monitor.taskCpuDemands[0]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuDemands[0]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[0]?.get(1)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied[0]?.get(10)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu supplied to the host is incorrect" } },
        )
    }

    @Test
    fun testFlowDistributor5() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 4000.0),
                            fragment(10 * 60 * 1000, 2000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(4000.0, monitor.taskCpuDemands[0]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuDemands[0]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[0]?.get(1)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[0]?.get(10)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu supplied to the host is incorrect" } },
        )
    }

    @Test
    fun testFlowDistributor6() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                            fragment(10 * 60 * 1000, 3000.0),
                        ),
                    cpuCoreCount = 1,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 3000.0),
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("single_2_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1000.0, monitor.taskCpuDemands[0]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands[0]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied[0]?.get(1)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuSupplied[0]?.get(10)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands[1]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuDemands[1]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuSupplied[1]?.get(1)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied[1]?.get(10)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu supplied to the host is incorrect" } },
        )
    }

    @Test
    fun testFlowDistributor7() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 6000.0),
                            fragment(10 * 60 * 1000, 5000.0),
                        ),
                    cpuCoreCount = 1,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 5000.0),
                            fragment(10 * 60 * 1000, 6000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("single_2_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(6000.0, monitor.taskCpuDemands[0]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(5000.0, monitor.taskCpuDemands[0]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[0]?.get(1)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[0]?.get(10)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(5000.0, monitor.taskCpuDemands[1]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(6000.0, monitor.taskCpuDemands[1]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[1]?.get(1)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[1]?.get(10)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(11000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(11000.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu supplied to the host is incorrect" } },
        )
    }

    @Test
    fun testFlowDistributor8() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    submissionTime = "2024-02-01T10:00",
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                            fragment(10 * 60 * 1000, 2000.0),
                        ),
                    cpuCoreCount = 1,
                ),
                createTestTask(
                    id = 1,
                    submissionTime = "2024-02-01T10:05",
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 2000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("single_2_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1000.0, monitor.taskCpuDemands[0]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuDemands[0]?.get(5)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuDemands[0]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuDemands[0]?.get(14)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied[0]?.get(1)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied[0]?.get(5)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[0]?.get(9)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[0]?.get(14)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuDemands[1]?.get(1)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuDemands[1]?.get(6)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[1]?.get(1)) { "The cpu supplied to task 1 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[1]?.get(6)) { "The cpu supplied to task 1 is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuDemands["H01"]?.get(5)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(9)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuDemands["H01"]?.get(14)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuSupplied["H01"]?.get(5)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(9)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["H01"]?.get(14)) { "The cpu supplied to the host is incorrect" } },
        )
    }

    @Test
    fun testFlowDistributor9() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    submissionTime = "2024-02-01T10:00",
                    fragments =
                        listOf(
                            fragment(20 * 60 * 1000, 3000.0),
                        ),
                    cpuCoreCount = 1,
                ),
                createTestTask(
                    id = 1,
                    submissionTime = "2024-02-01T10:05",
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1500.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("single_2_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(3000.0, monitor.taskCpuDemands[0]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands[0]?.get(5)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands[0]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands[0]?.get(14)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuSupplied[0]?.get(1)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2500.0, monitor.taskCpuSupplied[0]?.get(5)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2500.0, monitor.taskCpuSupplied[0]?.get(9)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuSupplied[0]?.get(14)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1500.0, monitor.taskCpuDemands[1]?.get(1)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(1500.0, monitor.taskCpuDemands[1]?.get(6)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(1500.0, monitor.taskCpuSupplied[1]?.get(1)) { "The cpu supplied to task 1 is incorrect" } },
            { assertEquals(1500.0, monitor.taskCpuSupplied[1]?.get(6)) { "The cpu supplied to task 1 is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4500.0, monitor.hostCpuDemands["H01"]?.get(5)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuDemands["H01"]?.get(14)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(5)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuSupplied["H01"]?.get(14)) { "The cpu supplied to the host is incorrect" } },
        )
    }

    @Test
    fun testFlowDistributor10() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(5 * 60 * 1000, 1000.0),
                            fragment(5 * 60 * 1000, 1500.0),
                            fragment(5 * 60 * 1000, 2500.0),
                            fragment(5 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        listOf(
                            fragment(20 * 60 * 1000, 3000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("single_2_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1000.0, monitor.taskCpuDemands[0]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1500.0, monitor.taskCpuDemands[0]?.get(5)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2500.0, monitor.taskCpuDemands[0]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuDemands[0]?.get(14)) { "The cpu demanded is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied[0]?.get(1)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1500.0, monitor.taskCpuSupplied[0]?.get(5)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[0]?.get(9)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied[0]?.get(14)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands[1]?.get(1)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands[1]?.get(5)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands[1]?.get(9)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands[1]?.get(14)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuSupplied[1]?.get(1)) { "The cpu supplied to task 1 is incorrect" } },
            { assertEquals(2500.0, monitor.taskCpuSupplied[1]?.get(5)) { "The cpu supplied to task 1 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[1]?.get(9)) { "The cpu supplied to task 1 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuSupplied[1]?.get(14)) { "The cpu supplied to task 1 is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4500.0, monitor.hostCpuDemands["H01"]?.get(5)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(5500.0, monitor.hostCpuDemands["H01"]?.get(9)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands["H01"]?.get(14)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(5)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(9)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied["H01"]?.get(14)) { "The cpu supplied to the host is incorrect" } },
        )
    }

    @Test
    fun testFlowDistributor11() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf<TaskFragmentSpec>().apply {
                            repeat(1) { this.add(fragment(10 * 60 * 1000, 3000.0)) }
                        },
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("single_5000_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(10 * 60 * 1000, monitor.maxTimestamp) { "The expected runtime is exceeded" } },
        )
    }

    @Test
    fun testFlowDistributor12() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf<TaskFragmentSpec>().apply {
                            repeat(1000) { this.add(fragment(10 * 60 * 1000, 2000.0)) }
                        },
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1000 * 10 * 60 * 1000, monitor.maxTimestamp) { "The expected runtime is exceeded" } },
        )
    }

    @Test
    fun testFlowDistributor13() {
        val numTasks = 1000

        val workload =
            arrayListOf<TaskSpec>().apply {
                repeat(numTasks) {
                    this.add(
                        createTestTask(
                            id = 0,
                            fragments =
                                listOf(fragment(10 * 60 * 1000, 2000.0)),
                            cpuCoreCount = 1,
                        ),
                    )
                }
            }
        val topology = createTopology("single_1_2000.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(numTasks * 10 * 60 * 1000L, monitor.maxTimestamp) { "The expected runtime is exceeded" } },
        )
    }

    @Test
    fun testFlowDistributor14() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 0.0, 1000.0),
                            fragment(10 * 60 * 1000, 0.0, 2000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )

        val topology = createTopology("Gpus/single_gpu_no_vendor_no_memory.json")

        val monitor = runTest(topology, workload)

        assertAll(
            // CPU
            // task
            { assertEquals(0.0, monitor.taskCpuDemands[0]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuDemands[0]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuSupplied[0]?.get(1)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuSupplied[0]?.get(10)) { "The cpu supplied to task 0 is incorrect" } },
            // host
            { assertEquals(0.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(0.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(0.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(0.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu supplied to the host is incorrect" } },
            // GPU
            // task
            { assertEquals(1000.0, monitor.taskGpuDemands[0]?.get(1)) { "The gpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskGpuDemands[0]?.get(10)) { "The gpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuSupplied[0]?.get(1)) { "The gpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskGpuSupplied[0]?.get(10)) { "The gpu supplied to task 0 is incorrect" } },
            // host
            { assertEquals(1000.0, monitor.hostGpuDemands["H01"]?.get(1)?.get(0)) { "The gpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostGpuDemands["H01"]?.get(10)?.get(0)) { "The gpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostGpuSupplied["H01"]?.get(1)?.get(0)) { "The gpu supplied to the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostGpuSupplied["H01"]?.get(10)?.get(0)) { "The gpu supplied to the host is incorrect" } },
        )
    }

    @Test
    fun testFlowDistributor15() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0, 1000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )

        val topology = createTopology("Gpus/single_gpu_no_vendor_no_memory.json")

        val monitor = runTest(topology, workload)

        assertAll(
            // CPU
            // task
            { assertEquals(1000.0, monitor.taskCpuDemands[0]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuDemands[0]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied[0]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuSupplied[0]?.get(9)) { "The cpu supplied to task 0 is incorrect" } },
            // host
            { assertEquals(1000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(0.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(0.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu supplied to the host is incorrect" } },
            // GPU
            // task
            { assertEquals(1000.0, monitor.taskGpuDemands[0]?.get(0)) { "The gpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuDemands[0]?.get(9)) { "The gpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuSupplied[0]?.get(0)) { "The gpu supplied to task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuSupplied[0]?.get(9)) { "The gpu supplied to task 0 is incorrect" } },
            // host
            { assertEquals(1000.0, monitor.hostGpuDemands["H01"]?.get(1)?.get(0)) { "The gpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostGpuSupplied["H01"]?.get(1)?.get(0)) { "The gpu supplied to the host is incorrect" } },
            { assertEquals(0.0, monitor.hostGpuDemands["H01"]?.get(10)?.get(0)) { "The gpu demanded by the host is incorrect" } },
            { assertEquals(0.0, monitor.hostGpuSupplied["H01"]?.get(10)?.get(0)) { "The gpu supplied to the host is incorrect" } },
        )
    }

    @Test
    fun testFlowDistributor16() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0, 2000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )

        val topology = createTopology("Gpus/single_gpu_no_vendor_no_memory.json")

        val monitor = runTest(topology, workload)

        assertAll(
            // CPU
            // task
            { assertEquals(1000.0, monitor.taskCpuDemands[0]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuDemands[0]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied[0]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuSupplied[0]?.get(9)) { "The cpu supplied to task 0 is incorrect" } },
            // host
            { assertEquals(1000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(0.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(0.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu supplied to the host is incorrect" } },
            // GPU
            // task
            { assertEquals(2000.0, monitor.taskGpuDemands[0]?.get(0)) { "The gpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskGpuDemands[0]?.get(9)) { "The gpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskGpuSupplied[0]?.get(0)) { "The gpu supplied to task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskGpuSupplied[0]?.get(9)) { "The gpu supplied to task 0 is incorrect" } },
            // host
            { assertEquals(2000.0, monitor.hostGpuDemands["H01"]?.get(1)?.get(0)) { "The gpu demanded by the host is incorrect" } },
            { assertEquals(0.0, monitor.hostGpuDemands["H01"]?.get(10)?.get(0)) { "The gpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostGpuSupplied["H01"]?.get(1)?.get(0)) { "The gpu supplied to the host is incorrect" } },
            { assertEquals(0.0, monitor.hostGpuSupplied["H01"]?.get(10)?.get(0)) { "The gpu supplied to the host is incorrect" } },
        )
    }

    @Test
    fun testFlowDistributor17() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 2000.0, 1000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )
        val topology = createTopology("Gpus/single_gpu_no_vendor_no_memory.json")
        val monitor = runTest(topology, workload)

        assertAll(
            // CPU
            // task
            { assertEquals(2000.0, monitor.taskCpuDemands[0]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuDemands[0]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied[0]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuSupplied[0]?.get(9)) { "The cpu supplied to task 0 is incorrect" } },
            // host
            { assertEquals(2000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(0.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(0.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu supplied to the host is incorrect" } },
            // GPU
            // task
            { assertEquals(1000.0, monitor.taskGpuDemands[0]?.get(1)) { "The gpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuDemands[0]?.get(9)) { "The gpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuSupplied[0]?.get(1)) { "The gpu supplied to task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuSupplied[0]?.get(9)) { "The gpu supplied to task 0 is incorrect" } },
            // host
            { assertEquals(1000.0, monitor.hostGpuDemands["H01"]?.get(1)?.get(0)) { "The gpu demanded by the host is incorrect" } },
            { assertEquals(0.0, monitor.hostGpuDemands["H01"]?.get(10)?.get(0)) { "The gpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostGpuSupplied["H01"]?.get(1)?.get(0)) { "The gpu supplied to the host is incorrect" } },
            { assertEquals(0.0, monitor.hostGpuSupplied["H01"]?.get(10)?.get(0)) { "The gpu supplied to the host is incorrect" } },
        )
    }

    @Test
    fun testFlowDistributor18() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0, 1000.0),
                        ),
                    cpuCoreCount = 1,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0, 1000.0),
                        ),
                    cpuCoreCount = 1,
                ),
            )

        val topology = createTopology("Gpus/single_gpu_no_vendor_no_memory.json")
        val monitor = runTest(topology, workload)
        assertAll(
            // CPU
            // task 0
            { assertEquals(1000.0, monitor.taskCpuDemands[0]?.get(0)) { "The cpu demanded by task 0 at t=0 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuDemands[0]?.get(9)) { "The cpu demanded by task 0 at t=0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied[0]?.get(0)) { "The cpu supplied to task 0 at t=0 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuSupplied[0]?.get(9)) { "The cpu supplied to task 0 at t=0 is incorrect" } },
            // task 1
            { assertEquals(0.0, monitor.taskCpuDemands[1]?.get(1)) { "The cpu demanded by task 1 at t=1 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuDemands[1]?.get(10)) { "The cpu demanded by task 1 at t=10 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuDemands[1]?.get(19)) { "The cpu demanded by task 1 at t=19 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuSupplied[1]?.get(1)) { "The cpu supplied to task 1 at t=1 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied[1]?.get(10)) { "The cpu supplied to task 1 at t=10 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuSupplied[1]?.get(19)) { "The cpu supplied to task 1 at t=9 is incorrect" } },
            // host
            { assertEquals(1000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host at t=1 is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host at t=10 is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu supplied to the host at t=1 is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu supplied to the host at t=10 is incorrect" } },
            // GPU
            // task 0
            { assertEquals(1000.0, monitor.taskGpuDemands[0]?.get(0)) { "The gpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuDemands[0]?.get(9)) { "The gpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuSupplied[0]?.get(0)) { "The gpu supplied to task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuSupplied[0]?.get(9)) { "The gpu supplied to task 0 is incorrect" } },
            // task 1
            { assertEquals(0.0, monitor.taskGpuDemands[1]?.get(0)) { "The gpu demanded by task 1 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuDemands[1]?.get(10)) { "The gpu demanded by task 1 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuDemands[1]?.get(19)) { "The gpu supplied to task 1 is incorrect" } },
            { assertEquals(0.0, monitor.taskGpuSupplied[1]?.get(0)) { "The gpu supplied to task 1 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuSupplied[1]?.get(10)) { "The gpu supplied to task 1 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuSupplied[1]?.get(19)) { "The gpu supplied to task 1 is incorrect" } },
            // host
            { assertEquals(1000.0, monitor.hostGpuDemands["H01"]?.get(1)?.get(0)) { "The gpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostGpuDemands["H01"]?.get(10)?.get(0)) { "The gpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostGpuSupplied["H01"]?.get(1)?.get(0)) { "The gpu supplied to the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostGpuSupplied["H01"]?.get(10)?.get(0)) { "The gpu supplied to the host is incorrect" } },
        )
    }

    @Test
    fun testFlowDistributor19() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0, 0.0),
                        ),
                    cpuCoreCount = 1,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 0.0, 1000.0),
                        ),
                    cpuCoreCount = 0,
                    gpuCoreCount = 1,
                ),
            )

        val topology = createTopology("Gpus/single_gpu_no_vendor_no_memory.json")
        val monitor = runTest(topology, workload)

        assertAll(
            // CPU
            // task 0
            { assertEquals(1000.0, monitor.taskCpuDemands[0]?.get(0)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuDemands[0]?.get(9)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied[0]?.get(0)) { "The cpu supplied to task 0 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuSupplied[0]?.get(9)) { "The cpu supplied to task 0 is incorrect" } },
            // task 1
            { assertEquals(0.0, monitor.taskCpuDemands[1]?.get(0)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuDemands[1]?.get(9)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuSupplied[1]?.get(0)) { "The cpu supplied to task 1 is incorrect" } },
            { assertEquals(0.0, monitor.taskCpuSupplied[1]?.get(9)) { "The cpu supplied to task 1 is incorrect" } },
            // host
            { assertEquals(1000.0, monitor.hostCpuDemands["H01"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(0.0, monitor.hostCpuDemands["H01"]?.get(10)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuSupplied["H01"]?.get(1)) { "The cpu supplied to the host is incorrect" } },
            { assertEquals(0.0, monitor.hostCpuSupplied["H01"]?.get(10)) { "The cpu supplied to the host is incorrect" } },
            // GPU
            // task 0
            { assertEquals(0.0, monitor.taskGpuDemands[0]?.get(0)) { "The gpu demanded by task 0 is incorrect" } },
            { assertEquals(0.0, monitor.taskGpuDemands[0]?.get(9)) { "The gpu demanded by task 0 is incorrect" } },
            { assertEquals(0.0, monitor.taskGpuSupplied[0]?.get(0)) { "The gpu supplied to task 0 is incorrect" } },
            { assertEquals(0.0, monitor.taskGpuSupplied[0]?.get(9)) { "The gpu supplied to task 0 is incorrect" } },
            // task 1
            { assertEquals(1000.0, monitor.taskGpuDemands[1]?.get(0)) { "The gpu demanded by task 1 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuDemands[1]?.get(9)) { "The gpu demanded by task 1 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuSupplied[1]?.get(0)) { "The gpu supplied to task 1 is incorrect" } },
            { assertEquals(1000.0, monitor.taskGpuSupplied[1]?.get(9)) { "The gpu supplied to task 1 is incorrect" } },
            // host
            { assertEquals(1000.0, monitor.hostGpuDemands["H01"]?.get(1)?.get(0)) { "The gpu demanded by the host is incorrect" } },
            { assertEquals(0.0, monitor.hostGpuDemands["H01"]?.get(10)?.get(0)) { "The gpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostGpuSupplied["H01"]?.get(1)?.get(0)) { "The gpu supplied to the host is incorrect" } },
            { assertEquals(0.0, monitor.hostGpuSupplied["H01"]?.get(10)?.get(0)) { "The gpu supplied to the host is incorrect" } },
        )
    }
}
