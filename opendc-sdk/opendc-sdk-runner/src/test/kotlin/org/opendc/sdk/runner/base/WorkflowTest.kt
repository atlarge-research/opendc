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
import org.opendc.sdk.runner.base.harness.createTestTask
import org.opendc.sdk.runner.base.harness.createTopology
import org.opendc.sdk.runner.base.harness.fragment
import org.opendc.sdk.runner.base.harness.runTest

/**
 * Integration tests ported one-to-one from `opendc-experiments-base`'s `WorkflowTest`,
 * driving the SDK runner and asserting the identical values.
 */
class WorkflowTest {
    @Test
    fun testWorkflow1() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(),
                    children = setOf(1, 2),
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(0),
                    children = setOf(3),
                ),
                createTestTask(
                    id = 2,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(0),
                    children = setOf(3),
                ),
                createTestTask(
                    id = 3,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(1, 2),
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

    @Test
    fun testWorkflow2() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(),
                    children = setOf(1, 2),
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(0),
                    children = setOf(3),
                ),
                createTestTask(
                    id = 2,
                    fragments =
                        listOf(
                            fragment(5 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(0),
                    children = setOf(3),
                ),
                createTestTask(
                    id = 3,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(1, 2),
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

    @Test
    fun testWorkflow3() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(),
                    children = setOf(1, 2),
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(0),
                    children = setOf(3),
                ),
                createTestTask(
                    id = 2,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(0),
                    children = setOf(3),
                ),
                createTestTask(
                    id = 3,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(1, 2),
                    children = emptySet(),
                ),
                createTestTask(
                    id = 5,
                    fragments =
                        listOf(
                            fragment(40 * 60 * 1000, 1000.0),
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

    @Test
    fun testWorkflow4() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(),
                    children = setOf(1, 2),
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(0),
                    children = setOf(3),
                ),
                createTestTask(
                    id = 2,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(0),
                    children = setOf(3),
                ),
                createTestTask(
                    id = 3,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(1, 2),
                    children = emptySet(),
                ),
                createTestTask(
                    id = 5,
                    fragments =
                        listOf(
                            fragment(15 * 60 * 1000, 1000.0),
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

    @Test
    fun testWorkflow5() {
        val workload =
            listOf(
                createTestTask(
                    id = 0,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 10000.0),
                        ),
                    cpuCoreCount = 10,
                    parents = setOf(),
                    children = setOf(1, 2),
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(0),
                    children = setOf(3),
                ),
                createTestTask(
                    id = 2,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(0),
                    children = setOf(3),
                ),
                createTestTask(
                    id = 3,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
                        ),
                    cpuCoreCount = 1,
                    parents = setOf(1, 2),
                    children = emptySet(),
                ),
                createTestTask(
                    id = 5,
                    fragments =
                        listOf(
                            fragment(10 * 60 * 1000, 1000.0),
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
