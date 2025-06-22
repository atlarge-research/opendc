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
import org.opendc.compute.topology.specs.ClusterSpec
import org.opendc.compute.workload.Task
import org.opendc.simulator.compute.workload.trace.TraceFragment
import java.util.ArrayList

/**
 * Testing suite containing tests that specifically test the FlowDistributor
 */
class GpuTest {
    /**
     * Test the creation of a GPU host with a single GPU, in minimal configuration
     */
    @Test
    fun testGpuHostCreationSingleMinimal() {
        val topology = createTopology("Gpus/single_gpu_no_vendor_no_memory.json")
        assertGpuConfiguration(
            topology,
            coreCount = 1,
            coreSpeed = 2000.0,
            memorySize = -1L,
            memoryBandwidth = -1.0,
            vendor = "unknown",
            modelName = "unknown",
            architecture = "unknown",
            gpuCount = 1,
        )
    }

    /**
     * Test the creation of a GPU host with a single GPU with memory but no vendor
     */
    @Test
    fun testGpuHostCreationSingleWithMemoryNoVendor() {
        val topology = createTopology("Gpus/single_gpu_no_vendor.json")
        assertGpuConfiguration(
            topology,
            coreCount = 1,
            coreSpeed = 2000.0,
            memorySize = 4096L,
            memoryBandwidth = 500.0,
            vendor = "unknown",
            modelName = "unknown",
            architecture = "unknown",
            gpuCount = 1,
        )
    }

    /**
     * Test the creation of a GPU host with a single GPU with no memory but with vendor
     */
    @Test
    fun testGpuHostCreationSingleNoMemoryWithVendor() {
        val topology = createTopology("Gpus/single_gpu_no_memory.json")
        assertGpuConfiguration(
            topology,
            coreCount = 1,
            coreSpeed = 2000.0,
            memorySize = -1L,
            memoryBandwidth = -1.0,
            vendor = "NVIDIA",
            modelName = "Tesla V100",
            architecture = "Volta",
            gpuCount = 1,
        )
    }

    /**
     * Test the creation of a GPU host with a single GPU, in full configuration
     */
    @Test
    fun testGpuHostCreationSingleWithMemoryWithVendor() {
        val topology = createTopology("Gpus/single_gpu_full.json")
        assertGpuConfiguration(
            topology,
            // cuda cores
            coreCount = 5120,
//            coreCount = 640, // tensor cores
            // fictional value
            coreSpeed = 5000.0,
            memorySize = 30517578125,
            memoryBandwidth = 7031250000.0,
            vendor = "NVIDIA",
            modelName = "Tesla V100",
            architecture = "Volta",
            gpuCount = 1,
        )
    }

    /**
     * Test the creation of a GPU host with multiple GPU, in minimal configuration
     */
    @Test
    fun testGpuHostCreationMultiMinimal() {
        val topology = createTopology("Gpus/multi_gpu_no_vendor_no_memory.json")
        val count = 3
        assertGpuConfiguration(
            topology,
            coreCount = 1 * count,
            coreSpeed = 2000.0,
            memorySize = -1L * count,
            memoryBandwidth = -1.0,
            vendor = "unknown",
            modelName = "unknown",
            architecture = "unknown",
            gpuCount = 1,
        )
    }

    /**
     * Test the creation of a GPU host with multiple GPU with memory but no vendor
     */
    @Test
    fun testGpuHostCreationMultiWithMemoryNoVendor() {
        val topology = createTopology("Gpus/multi_gpu_no_vendor.json")
        val count = 100

        assertGpuConfiguration(
            topology,
            coreCount = 1 * count,
            coreSpeed = 2000.0,
            memorySize = 4096L * count,
            memoryBandwidth = 500.0,
            vendor = "unknown",
            modelName = "unknown",
            architecture = "unknown",
            gpuCount = 1,
        )
    }

    /**
     * Test the creation of a GPU host with multiple GPU with no memory but with vendor
     */
    @Test
    fun testGpuHostCreationMultiNoMemoryWithVendor() {
        val topology = createTopology("Gpus/multi_gpu_no_memory.json")
        val count = 2
        assertGpuConfiguration(
            topology,
            coreCount = 1 * count,
            coreSpeed = 2000.0,
            memorySize = -1L * count,
            memoryBandwidth = -1.0,
            vendor = "NVIDIA",
            modelName = "Tesla V100",
            architecture = "Volta",
            gpuCount = 1,
        )
    }

    /**
     * Test the creation of a GPU host with multiple GPU, in full configuration
     */
    @Test
    fun testGpuHostCreationMultiWithMemoryWithVendor() {
        val topology = createTopology("Gpus/multi_gpu_full.json")
        val count = 5
        assertGpuConfiguration(
            topology,
            // cuda cores
            coreCount = 5120 * count,
            // fictional value
            coreSpeed = 5000.0,
            memorySize = 30517578125 * count,
            memoryBandwidth = 7031250000.0,
            vendor = "NVIDIA",
            modelName = "Tesla V100",
            architecture = "Volta",
            gpuCount = 1,
        )
    }

    /**
     * This test checks if the FlowDistributor can handle a workload that requires multiple GPUs.
     * This test assumes that multiple GPUs are concatenated into on single larger GPU.
     */
    @Test
    fun testMultiGpuConcation() {
        val workload: ArrayList<Task> =
            arrayListOf(
                createTestTask(
                    name = "0",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1, 2000.0, 1),
                        ),
                ),
                createTestTask(
                    name = "1",
                    fragments =
                        arrayListOf(
                            TraceFragment(10 * 60 * 1000, 1000.0, 1, 2000.0, 1),
                        ),
                ),
            )
        val topology = createTopology("Gpus/multi_gpu_host.json")

        val monitor = runTest(topology, workload)

        assertAll(
            { assertEquals(10 * 60 * 1000, monitor.maxTimestamp) { "The expected runtime is exceeded" } },
            // CPU
            // task 0
            { assertEquals(1000.0, monitor.taskCpuDemands["0"]?.get(1)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuDemands["0"]?.get(8)) { "The cpu demanded by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["0"]?.get(1)) { "The cpu used by task 0 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["0"]?.get(8)) { "The cpu used by task 0 is incorrect" } },
            // task 1
            { assertEquals(1000.0, monitor.taskCpuDemands["1"]?.get(1)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuDemands["1"]?.get(8)) { "The cpu demanded by task 1 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["1"]?.get(1)) { "The cpu used by task 1 is incorrect" } },
            { assertEquals(1000.0, monitor.taskCpuSupplied["1"]?.get(8)) { "The cpu used by task 1 is incorrect" } },
            // host
            { assertEquals(2000.0, monitor.hostCpuDemands["DualGpuHost"]?.get(1)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuDemands["DualGpuHost"]?.get(9)) { "The cpu demanded by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["DualGpuHost"]?.get(1)) { "The cpu used by the host is incorrect" } },
            { assertEquals(2000.0, monitor.hostCpuSupplied["DualGpuHost"]?.get(9)) { "The cpu used by the host is incorrect" } },
            // GPU
            // task 0
            { assertEquals(2000.0, monitor.taskGpuDemands["0"]?.get(1)?.get(0)) { "The gpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskGpuDemands["0"]?.get(8)?.get(0)) { "The gpu demanded by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskGpuSupplied["0"]?.get(1)?.get(0)) { "The gpu used by task 0 is incorrect" } },
            { assertEquals(2000.0, monitor.taskGpuSupplied["0"]?.get(8)?.get(0)) { "The gpu used by task 0 is incorrect" } },
            // task 1
            { assertEquals(2000.0, monitor.taskGpuDemands["1"]?.get(1)?.get(0)) { "The gpu demanded by task 1 is incorrect" } },
            { assertEquals(2000.0, monitor.taskGpuDemands["1"]?.get(8)?.get(0)) { "The gpu demanded by task 1 is incorrect" } },
            { assertEquals(2000.0, monitor.taskGpuSupplied["1"]?.get(1)?.get(0)) { "The gpu used by task 1 is incorrect" } },
            { assertEquals(2000.0, monitor.taskGpuSupplied["1"]?.get(8)?.get(0)) { "The gpu used by task 1 is incorrect" } },
            // host
            { assertEquals(4000.0, monitor.hostGpuDemands["DualGpuHost"]?.get(1)?.get(0)) { "The gpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostGpuDemands["DualGpuHost"]?.get(9)?.get(0)) { "The gpu demanded by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(1)?.get(0)) { "The gpu used by the host is incorrect" } },
            { assertEquals(4000.0, monitor.hostGpuSupplied["DualGpuHost"]?.get(9)?.get(0)) { "The gpu used by the host is incorrect" } },
        )
    }

    private fun assertGpuConfiguration(
        topology: List<ClusterSpec>,
        coreCount: Int,
        coreSpeed: Double,
        memorySize: Long,
        memoryBandwidth: Double,
        vendor: String,
        modelName: String,
        architecture: String,
        gpuCount: Int,
    ) {
        for (cluster in topology) {
            for (host in cluster.hostSpecs) {
                assert(host.model.gpuModels.size == gpuCount) { "GPU count should be $gpuCount, but is ${host.model.gpuModels.size}" }

                for (gpuModel in host.model.gpuModels) {
                    assert(gpuModel.coreCount == coreCount) { "GPU Core count should be $coreCount, but is ${gpuModel.coreCount}" }
                    assert(gpuModel.coreSpeed == coreSpeed) { "GPU core speed should be $coreSpeed, but is ${gpuModel.coreSpeed}" }
                    assert(gpuModel.memorySize == memorySize) { "GPU memory size should be $memorySize, but is ${gpuModel.memorySize}" }
                    assert(gpuModel.memoryBandwidth == memoryBandwidth) {
                        "GPU memory bandwidth should be $memoryBandwidth, but is ${gpuModel.memoryBandwidth}"
                    }
                    assert(gpuModel.vendor.contentEquals(vendor)) { "GPU vendor should be $vendor, but is ${gpuModel.vendor}" }
                    assert(
                        gpuModel.modelName.contentEquals(modelName),
                    ) { "GPU model name should be $modelName, but is ${gpuModel.modelName}" }
                    assert(
                        gpuModel.architecture.contentEquals(architecture),
                    ) { "GPU architecture should be $architecture, but is ${gpuModel.architecture}" }
                }
            }
        }
    }
}
