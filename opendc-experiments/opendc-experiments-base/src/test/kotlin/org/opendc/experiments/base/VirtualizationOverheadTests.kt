/*
 * Copyright (c) 2024 AtLarge Research
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
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.compute.workload.Task
import org.opendc.simulator.compute.virtualization.OverheadModels.ConstantVirtualizationOverhead
import org.opendc.simulator.compute.virtualization.OverheadModels.NoVirtualizationOverHead
import org.opendc.simulator.compute.virtualization.OverheadModels.ShareBasedVirtualizationOverhead
import org.opendc.simulator.compute.workload.trace.TraceFragment
import java.util.ArrayList

class VirtualizationOverheadTests {
    /**
     * Test that the different virtualization overhead models are loaded correctly from a topology file.
     */
    @Test
    fun loadsVirtualizationOverheadModelCorrectly() {
        val noModelTopology = createTopology("virtualizationOverhead/single_gpu_no_model.json")
        val noOverHeadTopology = createTopology("virtualizationOverhead/single_gpu_no_overhead.json")
        val constantOverHeadTopology = createTopology("virtualizationOverhead/single_gpu_constant_overhead.json")
        val customConstantOverHeadTopology = createTopology("virtualizationOverhead/single_gpu_custom_constant_overhead.json")
        val shareBasedOverheadTopology = createTopology("virtualizationOverhead/single_gpu_share_based_overhead.json")

        assertAll(
            {
                assertInstanceOf(
                    NoVirtualizationOverHead::class.java,
                    noModelTopology[0].hostSpecs[0].model.gpuModels[0].virtualizationOverheadModel,
                    "Did not load default model correctly, when no model was given.",
                )
            },
            // no overhead
            {
                assertInstanceOf(
                    NoVirtualizationOverHead::class.java,
                    noOverHeadTopology[0].hostSpecs[0].model.gpuModels[0].virtualizationOverheadModel,
                    "Did not load no overhead model correctly.",
                )
            },
            // default constant overhead
            {
                assertInstanceOf(
                    ConstantVirtualizationOverhead::class.java,
                    constantOverHeadTopology[0].hostSpecs[0].model.gpuModels[0].virtualizationOverheadModel,
                    "Did not load constant overhead model correctly.",
                )
            },
            {
                assertEquals(
                    0.05,
                    (
                        constantOverHeadTopology[0].hostSpecs[0].model.gpuModels[0].virtualizationOverheadModel
                            as ConstantVirtualizationOverhead
                    ).percentageOverhead,
                    "Constant overhead should have 5% overhead",
                )
            },
            // custom constant overhead
            {
                assertInstanceOf(
                    ConstantVirtualizationOverhead::class.java,
                    customConstantOverHeadTopology[0].hostSpecs[0].model.gpuModels[0].virtualizationOverheadModel,
                    "Did not load constant overhead model correctly, when overhead factor was given.",
                )
            },
            {
                assertEquals(
                    0.25,
                    (
                        customConstantOverHeadTopology[0].hostSpecs[0].model.gpuModels[0].virtualizationOverheadModel
                            as ConstantVirtualizationOverhead
                    ).percentageOverhead,
                    "Custom constant overhead should have 25% overhead",
                )
            },
            // share-based overhead
            {
                assertInstanceOf(
                    ShareBasedVirtualizationOverhead::class.java,
                    shareBasedOverheadTopology[0].hostSpecs[0].model.gpuModels[0].virtualizationOverheadModel,
                    "Did not load shared based overhead model correctly",
                )
            },
        )
    }

    /**
     * Test that the NoVirtualizationOverhead model does not apply any overhead.
     */
    @Test
    fun noVirtualizationOverheadModelTest() {
        val topology = createTopology("virtualizationOverhead/single_gpu_no_overhead.json")
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1000.0),
                        ),
                    cpuCount = 1,
                    gpuCount = 1,
                ),
            )

        val monitor = runTest(topology, workload)
        assertEquals(1000.0, monitor.taskGpuDemands[0]?.get(1), "Task 0 should have gpu demand 1000.0")
        assertEquals(1000.0, monitor.taskGpuSupplied[0]?.get(1), "Task 0 should have gpu supplied 1000.0 ")
        assertEquals(1000.0, monitor.hostGpuDemands["H01"]?.get(1)?.get(0), "Host H01 should have gpu demand 1000.0")
        assertEquals(1000.0, monitor.hostGpuSupplied["H01"]?.get(1)?.get(0), "Host H01 should have gpu supply 1000.0")
    }

    /**
     * Test that the constant overhead model does apply the correct amount of overhead.
     */
    @Test
    fun constantVirtualizationOverheadModelTest() {
        val topology = createTopology("virtualizationOverhead/single_gpu_constant_overhead.json")
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1000.0),
                        ),
                    cpuCount = 1,
                    gpuCount = 1,
                ),
            )

        val monitor = runTest(topology, workload)
        assertAll(
            { assertEquals(1000.0, monitor.taskGpuDemands[0]?.get(1), "Task 0 should have gpu demand 1000.0") },
            { assertEquals(0.95 * 1000.0, monitor.taskGpuSupplied[0]?.get(1), "Task 0 should have gpu supplied 950.0 ") },
            { assertEquals(1000.0, monitor.hostGpuDemands["H01"]?.get(1)?.get(0), "Host H01 should have gpu demand 1000.0") },
            { assertEquals(0.95 * 1000.0, monitor.hostGpuSupplied["H01"]?.get(1)?.get(0), "Host H01 should have gpu supply 950.0") },
        )
    }

    /**
     * Test that the custom constant overhead model does not apply the correct amount of overhead.
     */
    @Test
    fun customConstantVirtualizationOverheadModelTest() {
        val topology = createTopology("virtualizationOverhead/single_gpu_custom_constant_overhead.json")
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1000.0),
                        ),
                    cpuCount = 1,
                    gpuCount = 1,
                ),
            )

        val monitor = runTest(topology, workload)
        assertAll(
            { assertEquals(1000.0, monitor.taskGpuDemands[0]?.get(1), "Task 0 should have gpu demand 1000.0") },
            { assertEquals(0.75 * 1000.0, monitor.taskGpuSupplied[0]?.get(1), "Task 0 should have gpu supplied 750.0 ") },
            { assertEquals(1000.0, monitor.hostGpuDemands["H01"]?.get(1)?.get(0), "Host H01 should have gpu demand 1000.0") },
            { assertEquals(0.75 * 1000.0, monitor.hostGpuSupplied["H01"]?.get(1)?.get(0), "Host H01 should have gpu supply 750.0") },
        )
    }

    /**
     * Test that the share-based overhead model does not apply the correct amount of overhead, depending on the number of VMs.
     */
    @Test
    fun shareBasedVirtualizationOverheadModelTest() {
        val topology = createTopology("virtualizationOverhead/single_gpu_share_based_overhead.json")
        val workload1: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1000.0),
                        ),
                    cpuCount = 1,
                    gpuCount = 1,
                ),
            )

        val workload2: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 1000.0),
                        ),
                    cpuCount = 0,
                    gpuCount = 1,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 1000.0),
                        ),
                    cpuCount = 0,
                    gpuCount = 1,
                ),
            )

        val workload3: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    id = 0,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 1000.0),
                        ),
                    cpuCount = 0,
                    gpuCount = 1,
                ),
                createTestTask(
                    id = 1,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 1000.0),
                        ),
                    cpuCount = 0,
                    gpuCount = 1,
                ),
                createTestTask(
                    id = 2,
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 0.0, 1000.0),
                        ),
                    cpuCount = 0,
                    gpuCount = 1,
                ),
            )

        val monitor1 = runTest(topology, workload1)
        val monitor2 = runTest(topology, workload2)
        val monitor3 = runTest(topology, workload3)

        assertAll(
            // Test with one VM
            { assertEquals(1000.0, monitor1.taskGpuDemands[0]?.get(1), "Task 0 should have gpu demand 1000.0") },
            { assertEquals(1000.0, monitor1.taskGpuSupplied[0]?.get(1), "Task 0 should have gpu supplied 1000.0 ") },
            { assertEquals(1000.0, monitor1.hostGpuDemands["H01"]?.get(1)?.get(0), "Host H01 should have gpu demand 1000.0") },
            { assertEquals(1000.0, monitor1.hostGpuSupplied["H01"]?.get(1)?.get(0), "Host H01 should have gpu supply 1000.0") },
            // Test with two VMs
            { assertEquals(1000.0, monitor2.taskGpuDemands[0]?.get(1), "Task 0 should have gpu demand 1000.0") },
            { assertEquals(500.0, monitor2.taskGpuSupplied[0]?.get(1), "Task 0 should have gpu supplied 500.0") },
            { assertEquals(1000.0, monitor2.taskGpuDemands[1]?.get(1), "Task 0 should have gpu demand 1000.0") },
            { assertEquals(500.0, monitor2.taskGpuSupplied[1]?.get(1), "Task 0 should have gpu supplied 500.0") },
            { assertEquals(2000.0, monitor2.hostGpuDemands["H01"]?.get(1)?.get(0), "Host H01 should have gpu demand 2000.0") },
            { assertEquals(1000.0, monitor2.hostGpuSupplied["H01"]?.get(1)?.get(0), "Host H01 should have gpu supply 1000.0") },
            // Test with three VMs
            { assertEquals(1000.0, monitor3.taskGpuDemands[0]?.get(1), "Task 0 should have gpu demand 1000.0") },
            { assertEquals(333.3, monitor3.taskGpuSupplied[0]?.get(1) ?: 0.0, 0.05, "Task 0 should have gpu supplied 333.3 ") },
            { assertEquals(1000.0, monitor3.taskGpuDemands[1]?.get(1), "Task 0 should have gpu demand 1000.0") },
            { assertEquals(333.3, monitor3.taskGpuSupplied[1]?.get(1) ?: 0.0, 0.05, "Task 0 should have gpu supplied 333.3 ") },
            { assertEquals(1000.0, monitor3.taskGpuDemands[2]?.get(1), "Task 0 should have gpu demand 1000.0") },
            { assertEquals(333.3, monitor3.taskGpuSupplied[2]?.get(1) ?: 0.0, 0.05, "Task 0 should have gpu supplied 333.3 ") },
            { assertEquals(3000.0, monitor3.hostGpuDemands["H01"]?.get(1)?.get(0), "Host H01 should have gpu demand 3000.0") },
            { assertEquals(1000.0, monitor3.hostGpuSupplied["H01"]?.get(1)?.get(0), "Host H01 should have gpu supply 700.0") },
        )
    }
}
