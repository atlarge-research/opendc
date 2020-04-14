/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.format.environment.sc20

import com.atlarge.odcsim.Domain
import com.atlarge.opendc.compute.core.MemoryUnit
import com.atlarge.opendc.compute.core.ProcessingNode
import com.atlarge.opendc.compute.core.ProcessingUnit
import com.atlarge.opendc.compute.metal.NODE_CLUSTER
import com.atlarge.opendc.compute.metal.driver.SimpleBareMetalDriver
import com.atlarge.opendc.compute.metal.power.LinearLoadPowerModel
import com.atlarge.opendc.compute.metal.service.ProvisioningService
import com.atlarge.opendc.compute.metal.service.SimpleProvisioningService
import com.atlarge.opendc.core.Environment
import com.atlarge.opendc.core.Platform
import com.atlarge.opendc.core.Zone
import com.atlarge.opendc.core.services.ServiceRegistry
import com.atlarge.opendc.format.environment.EnvironmentReader
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.Random
import java.util.UUID

/**
 * A [EnvironmentReader] for the internal environment format.
 *
 * @param environmentFile The file describing the physical cluster.
 */
class Sc20ClusterEnvironmentReader(
    private val environmentFile: File
) : EnvironmentReader {
    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun construct(dom: Domain): Environment {
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

        val nodes = mutableListOf<SimpleBareMetalDriver>()
        val random = Random(0)

        BufferedReader(FileReader(environmentFile)).use { reader ->
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
                            SimpleBareMetalDriver(
                                dom.newDomain("node-$clusterId-$it"),
                                UUID(random.nextLong(), random.nextLong()),
                                "node-$clusterId-$it",
                                mapOf(NODE_CLUSTER to clusterId),
                                List(coresPerHost) { coreId ->
                                    ProcessingUnit(unknownProcessingNode, coreId, speed)
                                },
                                // For now we assume a simple linear load model with an idle draw of ~200W and a maximum
                                // power draw of 350W.
                                // Source: https://stackoverflow.com/questions/6128960
                                listOf(unknownMemoryUnit),
                                LinearLoadPowerModel(200.0, 350.0)
                            )
                        )
                    }
                }
        }

        val provisioningService = SimpleProvisioningService(dom.newDomain("provisioner"))
        for (node in nodes) {
            provisioningService.create(node)
        }

        val serviceRegistry = ServiceRegistry().put(ProvisioningService, provisioningService)

        val platform = Platform(
            UUID.randomUUID(), "sc20-platform", listOf(
                Zone(UUID.randomUUID(), "zone", serviceRegistry)
            )
        )

        return Environment("SC20 Environment", null, listOf(platform))
    }

    override fun close() {}
}
