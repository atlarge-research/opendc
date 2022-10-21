/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.experiments.tf20.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.CpuPowerModels
import java.io.InputStream
import java.util.UUID

/**
 * An environment reader for the TensorFlow experiments.
 */
public class MLEnvironmentReader {
    /**
     * The [ObjectMapper] to convert the format.
     */
    private val mapper = jacksonObjectMapper()

    public fun readEnvironment(input: InputStream): List<MachineDef> {
        val setup: Setup = mapper.readValue(input)
        var counter = 0
        return setup.rooms.flatMap { room ->
            room.objects.flatMap { roomObject ->
                when (roomObject) {
                    is RoomObject.Rack -> {
                        roomObject.machines.map { machine ->
                            var isGpuFlag = true
                            var maxPower = 350.0
                            var minPower = 200.0
                            val cores = machine.cpus.flatMap { id ->
                                when (id) {
                                    1 -> {
                                        // ref: https://www.guru3d.com/articles-pages/nvidia-geforce-gtx-titan-x-review,8.html#:~:text=GeForce%20GTX%20Titan%20X%20%2D%20On,power%20supply%20unit%20as%20minimum.
                                        maxPower = 334.0
                                        minPower = 90.0
                                        val node = ProcessingNode("NVidia", "TITAN X", "Pascal", 4992)
                                        List(node.coreCount) { ProcessingUnit(node, it, 824.0) }
                                    }
                                    2 -> {
                                        // ref: https://www.microway.com/hpc-tech-tips/nvidia-tesla-p100-pci-e-16gb-gpu-accelerator-pascal-gp100-close/
                                        maxPower = 250.0
                                        minPower = 125.0
                                        val node = ProcessingNode("NVIDIA", "Tesla P100", "Pascal", 3584)
                                        List(node.coreCount) { ProcessingUnit(node, it, 1190.0) }
                                    }
                                    3 -> {
                                        // ref: https://www.anandtech.com/show/10923/openpower-saga-tyans-1u-power8-gt75/7
                                        minPower = 84.0
                                        maxPower = 135.0
                                        val node = ProcessingNode("Intel", "E5-2690v3 Haswell24", "amd64", 24)
                                        isGpuFlag = false
                                        List(node.coreCount) { ProcessingUnit(node, it, 3498.0) }
                                    }
                                    4 -> {
                                        minPower = 130.0
                                        maxPower = 190.0
                                        val node = ProcessingNode("IBM", "POWER8", "RISC", 10)
                                        isGpuFlag = false
                                        List(node.coreCount) { ProcessingUnit(node, it, 143000.0) } // 28600.0 3690
                                    }
                                    else -> throw IllegalArgumentException("The cpu id $id is not recognized")
                                }
                            }
                            val memories = machine.memories.map { id ->
                                when (id) {
                                    1 -> MemoryUnit("NVidia", "GDDR5X", 480.0, 24L)
                                    2 -> MemoryUnit("NVidia", "GDDR5X", 720.0, 16L)
                                    3 -> MemoryUnit("IBM", "GDDR5X", 115.0, 160L)
                                    4 -> MemoryUnit("Inter", "GDDR5X", 68.0, 512L)
                                    else -> throw IllegalArgumentException("The cpu id $id is not recognized")
                                }
                            }

                            MachineDef(
                                UUID(0, counter.toLong()),
                                "node-${counter++}",
                                mapOf("gpu" to isGpuFlag),
                                MachineModel(cores, memories),
                                CpuPowerModels.linear(maxPower, minPower)
                            )
                        }
                    }
                }
            }
        }
    }
}
