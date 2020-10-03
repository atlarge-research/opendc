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

package org.opendc.format.environment.sc20

import kotlinx.coroutines.CoroutineScope
import org.opendc.compute.metal.NODE_CLUSTER
import org.opendc.compute.metal.driver.SimBareMetalDriver
import org.opendc.compute.metal.power.LinearLoadPowerModel
import org.opendc.compute.metal.service.ProvisioningService
import org.opendc.compute.metal.service.SimpleProvisioningService
import org.opendc.core.Environment
import org.opendc.core.Platform
import org.opendc.core.Zone
import org.opendc.core.services.ServiceRegistry
import org.opendc.format.environment.EnvironmentReader
import org.opendc.simulator.compute.SimMachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.time.Clock
import java.util.*

/**
 * A [EnvironmentReader] for the internal environment format.
 *
 * @param environmentFile The file describing the physical cluster.
 */
public class Sc20ClusterEnvironmentReader(
    private val input: InputStream
) : EnvironmentReader {

    public constructor(file: File) : this(FileInputStream(file))

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun construct(coroutineScope: CoroutineScope, clock: Clock): Environment {
        var clusterIdCol = 0
        var speedCol = 0
        var numberOfHostsCol = 0
        var memoryPerHostCol = 0
        var coresPerHostCol = 0

        var clusterIdx: Int = 0
        var clusterId: String
        var speed: Double
        var numberOfHosts: Int
        var memoryPerHost: Long
        var coresPerHost: Int

        val nodes = mutableListOf<SimBareMetalDriver>()
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
                            SimBareMetalDriver(
                                coroutineScope,
                                clock,
                                UUID(random.nextLong(), random.nextLong()),
                                "node-$clusterId-$it",
                                mapOf(NODE_CLUSTER to clusterId),
                                SimMachineModel(
                                    List(coresPerHost) { coreId ->
                                        ProcessingUnit(unknownProcessingNode, coreId, speed)
                                    },
                                    listOf(unknownMemoryUnit)
                                ),
                                // For now we assume a simple linear load model with an idle draw of ~200W and a maximum
                                // power draw of 350W.
                                // Source: https://stackoverflow.com/questions/6128960
                                LinearLoadPowerModel(200.0, 350.0)
                            )
                        )
                    }
                }
        }

        val provisioningService = SimpleProvisioningService()
        for (node in nodes) {
            provisioningService.create(node)
        }

        val serviceRegistry = ServiceRegistry().put(ProvisioningService, provisioningService)

        val platform = Platform(
            UUID.randomUUID(),
            "sc20-platform",
            listOf(
                Zone(UUID.randomUUID(), "zone", serviceRegistry)
            )
        )

        return Environment("SC20 Environment", null, listOf(platform))
    }

    override fun close() {}
}
