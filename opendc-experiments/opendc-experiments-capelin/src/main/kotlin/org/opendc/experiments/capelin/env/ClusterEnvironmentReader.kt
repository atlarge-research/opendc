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

package org.opendc.experiments.capelin.env

import org.opendc.compute.workload.env.MachineDef
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.LinearPowerModel
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.*

/**
 * A [EnvironmentReader] for the internal environment format.
 *
 * @param input The input stream describing the physical cluster.
 */
class ClusterEnvironmentReader(private val input: InputStream) : EnvironmentReader {
    /**
     * Construct a [ClusterEnvironmentReader] for the specified [file].
     */
    constructor(file: File) : this(FileInputStream(file))

    override fun read(): List<MachineDef> {
        var clusterIdCol = 0
        var speedCol = 0
        var numberOfHostsCol = 0
        var memoryPerHostCol = 0
        var coresPerHostCol = 0

        var clusterIdx = 0
        var clusterId: String
        var speed: Double
        var numberOfHosts: Int
        var memoryPerHost: Long
        var coresPerHost: Int

        val nodes = mutableListOf<MachineDef>()
        val random = Random(0)

        input.bufferedReader().use { reader ->
            reader.lineSequence()
                .filter { line ->
                    // Ignore comments in the file
                    !line.startsWith("#") && line.isNotBlank()
                }
                .forEachIndexed { idx, line ->
                    val values = line.split(";")

                    if (idx == 0) {
                        val header = values.mapIndexed { col, name -> Pair(name.trim(), col) }.toMap()
                        clusterIdCol = header["ClusterID"]!!
                        speedCol = header["Speed"]!!
                        numberOfHostsCol = header["numberOfHosts"]!!
                        memoryPerHostCol = header["memoryCapacityPerHost"]!!
                        coresPerHostCol = header["coreCountPerHost"]!!
                        return@forEachIndexed
                    }

                    clusterIdx++
                    clusterId = values[clusterIdCol].trim()
                    speed = values[speedCol].trim().toDouble() * 1000.0
                    numberOfHosts = values[numberOfHostsCol].trim().toInt()
                    memoryPerHost = values[memoryPerHostCol].trim().toLong() * 1000L
                    coresPerHost = values[coresPerHostCol].trim().toInt()

                    val unknownProcessingNode = ProcessingNode("unknown", "unknown", "unknown", coresPerHost)
                    val unknownMemoryUnit = MemoryUnit("unknown", "unknown", -1.0, memoryPerHost)

                    repeat(numberOfHosts) {
                        nodes.add(
                            MachineDef(
                                UUID(random.nextLong(), random.nextLong()),
                                "node-$clusterId-$it",
                                mapOf("cluster" to clusterId),
                                MachineModel(
                                    List(coresPerHost) { coreId ->
                                        ProcessingUnit(unknownProcessingNode, coreId, speed)
                                    },
                                    listOf(unknownMemoryUnit)
                                ),
                                // For now we assume a simple linear load model with an idle draw of ~200W and a maximum
                                // power draw of 350W.
                                // Source: https://stackoverflow.com/questions/6128960
                                LinearPowerModel(350.0, idlePower = 200.0)
                            )
                        )
                    }
                }
        }

        return nodes
    }

    override fun close() {
        input.close()
    }
}
