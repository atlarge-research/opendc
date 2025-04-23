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

import org.junit.jupiter.api.Test
import org.opendc.compute.topology.specs.ClusterSpec

/**
 * Testing suite containing tests that specifically test the FlowDistributor
 */
class GpuTest {
    /**
     * Test the creation of a GPU host with a single GPU, in minimal cofiguration
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
            gpuCount = 1
        )
    }

    /**
     * Test the creation of a GPU host with a single GPU, in full configuration
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
            gpuCount = 1
        )
    }

    /**
     * Test the creation of a GPU host with a single GPU, in full configuration
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
            gpuCount = 1
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
            coreCount = 5120, // cuda cores
//            coreCount = 640, // tensor cores
            coreSpeed = 5000.0, // fictional value
            memorySize = 30517578125,
            memoryBandwidth = 7031250000.0,
            vendor = "NVIDIA",
            modelName = "Tesla V100",
            architecture = "Volta",
            gpuCount = 1
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
        gpuCount: Int

    ) {
        for (cluster in topology) {
            for(host in cluster.hostSpecs){

                assert(host.model.gpuModels.size == gpuCount) { "GPU count should be $gpuCount, but is ${host.model.gpuModels.size}" }

                for (gpuModel in host.model.gpuModels){
                    assert(gpuModel.coreCount == coreCount) { "GPU Core count should be $coreCount, but is ${gpuModel.coreCount}" }
                    assert(gpuModel.coreSpeed == coreSpeed) { "GPU core speed should be $coreSpeed, but is ${gpuModel.coreSpeed}" }
                    assert(gpuModel.memorySize == memorySize) { "GPU memory size should be $memorySize, but is ${gpuModel.memorySize}" }
                    assert(gpuModel.memoryBandwidth == memoryBandwidth) { "GPU memory bandwidth should be $memoryBandwidth, but is ${gpuModel.memoryBandwidth}" }
                    assert(gpuModel.vendor.contentEquals(vendor)) { "GPU vendor should be $vendor, but is ${gpuModel.vendor}" }
                    assert(gpuModel.modelName.contentEquals(modelName)) { "GPU model name should be $modelName, but is ${gpuModel.modelName}" }
                    assert(gpuModel.architecture.contentEquals(architecture)) { "GPU architecture should be $architecture, but is ${gpuModel.architecture}" }
                }
            }
        }

    }

}


