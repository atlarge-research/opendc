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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.compute.simulator.provisioner.Provisioner
import org.opendc.compute.simulator.provisioner.registerComputeMonitor
import org.opendc.compute.simulator.provisioner.setupComputeService
import org.opendc.compute.simulator.provisioner.setupHosts
import org.opendc.compute.simulator.scheduler.FilterScheduler
import org.opendc.compute.simulator.scheduler.filters.ComputeFilter
import org.opendc.compute.simulator.scheduler.filters.RamFilter
import org.opendc.compute.simulator.scheduler.filters.VCpuFilter
import org.opendc.compute.simulator.scheduler.weights.CoreRamWeigher
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.simulator.telemetry.ComputeMonitor
import org.opendc.compute.simulator.telemetry.table.HostTableReader
import org.opendc.compute.simulator.telemetry.table.TaskTableReader
import org.opendc.compute.topology.clusterTopology
import org.opendc.compute.topology.specs.ClusterSpec
import org.opendc.compute.workload.ComputeWorkloadLoader
import org.opendc.compute.workload.Task
import org.opendc.experiments.base.runner.replay
import org.opendc.simulator.compute.workload.TraceFragment
import org.opendc.simulator.compute.workload.TraceWorkload
import org.opendc.simulator.kotlin.runSimulation
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.ArrayList
import java.util.UUID

/**
 * Testing suite containing tests that specifically test the FlowDistributor
 */
class FlowDistributorTest {
    /**
     * The monitor used to keep track of the metrics.
     */
    private lateinit var monitor: TestComputeMonitor

    /**
     * The [FilterScheduler] to use for all experiments.
     */
    private lateinit var computeScheduler: FilterScheduler

    /**
     * The [ComputeWorkloadLoader] responsible for loading the traces.
     */
    private lateinit var workloadLoader: ComputeWorkloadLoader

    private val basePath = "src/test/resources/FlowDistributor"

    /**
     * Set up the experimental environment.
     */
    @BeforeEach
    fun setUp() {
        monitor = TestComputeMonitor()
        computeScheduler =
            FilterScheduler(
                filters = listOf(ComputeFilter(), VCpuFilter(16.0), RamFilter(1.0)),
                weighers = listOf(CoreRamWeigher(multiplier = 1.0)),
            )
        workloadLoader = ComputeWorkloadLoader(File("$basePath/traces"), 0L, 0L, 0.0)
    }

    private fun createTestTask(
        name: String,
        cpuCount: Int = 1,
        cpuCapacity: Double = 0.0,
        memCapacity: Long = 0L,
        submissionTime: String = "1970-01-01T00:00",
        duration: Long = 0L,
        fragments: ArrayList<TraceFragment>,
    ): Task {
        return Task(
            UUID.nameUUIDFromBytes(name.toByteArray()),
            name,
            cpuCount,
            cpuCapacity,
            memCapacity,
            1800000.0,
            LocalDateTime.parse(submissionTime).atZone(ZoneId.systemDefault()).toInstant(),
            duration,
            TraceWorkload(
                fragments,
            ),
        )
    }

    private fun runTest(
        topology: List<ClusterSpec>,
        workload: ArrayList<Task>,
    ): TestComputeMonitor {
        runSimulation {
            val monitor = monitor
            val seed = 0L
            Provisioner(dispatcher, seed).use { provisioner ->
                provisioner.runSteps(
                    setupComputeService(serviceDomain = "compute.opendc.org", { computeScheduler }),
                    registerComputeMonitor(serviceDomain = "compute.opendc.org", monitor, exportInterval = Duration.ofMinutes(1)),
                    setupHosts(serviceDomain = "compute.opendc.org", topology),
                )

                val service = provisioner.registry.resolve("compute.opendc.org", ComputeService::class.java)!!
                service.replay(timeSource, workload)
            }
        }
        return monitor
    }

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

        monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuDemands["0"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuDemands[1]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuDemands[10]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuSupplied[1]) { "The cpu used by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied[10]) { "The cpu used by the host is incorrect" } },
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

        monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(3000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(4000.0, monitor.taskCpuDemands["0"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuDemands[1]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands[10]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied[1]) { "The cpu used by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied[10]) { "The cpu used by the host is incorrect" } },
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

        monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(4000.0, monitor.taskCpuDemands["0"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuDemands[1]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands[10]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuSupplied[1]) { "The cpu used by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied[10]) { "The cpu used by the host is incorrect" } },
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

        monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(4000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuDemands["0"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["0"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands[1]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuDemands[10]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied[1]) { "The cpu used by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuSupplied[10]) { "The cpu used by the host is incorrect" } },
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

        monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(4000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuDemands["0"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands[1]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuDemands[10]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied[1]) { "The cpu used by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied[10]) { "The cpu used by the host is incorrect" } },
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

        monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(1000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands["0"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuSupplied["0"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuDemands["1"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuDemands["1"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(3000.0, monitor.taskCpuSupplied["1"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["1"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands[1]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands[10]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied[1]) { "The cpu used by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied[10]) { "The cpu used by the host is incorrect" } },
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

        monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(6000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(5000.0, monitor.taskCpuDemands["0"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["0"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(5000.0, monitor.taskCpuDemands["1"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(6000.0, monitor.taskCpuDemands["1"]?.get(10)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["1"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskCpuSupplied["1"]?.get(10)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(11000.0, monitor.hostCpuDemands[1]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(11000.0, monitor.hostCpuDemands[10]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied[1]) { "The cpu used by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied[10]) { "The cpu used by the host is incorrect" } },
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

        monitor = runTest(topology, workload)

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
            { assertEquals(1000.0, monitor.hostCpuDemands[1]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuDemands[5]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands[9]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuDemands[14]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(1000.0, monitor.hostCpuSupplied[1]) { "The cpu used by the host is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuSupplied[5]) { "The cpu used by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied[9]) { "The cpu used by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied[14]) { "The cpu used by the host is incorrect" } },
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

        monitor = runTest(topology, workload)

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
            { assertEquals(3000.0, monitor.hostCpuDemands[1]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4500.0, monitor.hostCpuDemands[5]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuDemands[14]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuSupplied[1]) { "The cpu used by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied[5]) { "The cpu used by the host is incorrect" } },
            { assertEquals(3000.0, monitor.hostCpuSupplied[14]) { "The cpu used by the host is incorrect" } },
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

        monitor = runTest(topology, workload)

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
            { assertEquals(4000.0, monitor.hostCpuDemands[1]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4500.0, monitor.hostCpuDemands[5]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(5500.0, monitor.hostCpuDemands[9]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuDemands[14]) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied[1]) { "The cpu used by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied[5]) { "The cpu used by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied[9]) { "The cpu used by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostCpuSupplied[14]) { "The cpu used by the host is incorrect" } },
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
                            repeat(10) { this.add(TraceFragment(20 * 60 * 1000, 3000.0, 1)) }
                        },
                ),
            )
        val topology = createTopology("single_5000_2000.json")

        monitor = runTest(topology, workload)
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

        monitor = runTest(topology, workload)
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

        monitor = runTest(topology, workload)
    }

    /**
     * Obtain the topology factory for the test.
     */
    private fun createTopology(name: String): List<ClusterSpec> {
        val stream = checkNotNull(object {}.javaClass.getResourceAsStream("/FlowDistributor/topologies/$name"))
        return stream.use { clusterTopology(stream) }
    }

    class TestComputeMonitor : ComputeMonitor {
        var hostCpuDemands = ArrayList<Double>()
        var hostCpuSupplied = ArrayList<Double>()

        override fun record(reader: HostTableReader) {
            hostCpuDemands.add(reader.cpuDemand)
            hostCpuSupplied.add(reader.cpuUsage)
        }

        var taskCpuDemands = mutableMapOf<String, ArrayList<Double>>()
        var taskCpuSupplied = mutableMapOf<String, ArrayList<Double>>()

        override fun record(reader: TaskTableReader) {
            val taskName: String = reader.taskInfo.name

            if (taskName in taskCpuDemands) {
                taskCpuDemands[taskName]?.add(reader.cpuDemand)
                taskCpuSupplied[taskName]?.add(reader.cpuUsage)
            } else {
                taskCpuDemands[taskName] = arrayListOf(reader.cpuDemand)
                taskCpuSupplied[taskName] = arrayListOf(reader.cpuUsage)
            }
        }
    }
}
