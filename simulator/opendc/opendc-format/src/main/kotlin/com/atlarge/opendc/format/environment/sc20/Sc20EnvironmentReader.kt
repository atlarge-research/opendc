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
import com.atlarge.opendc.compute.metal.driver.SimpleBareMetalDriver
import com.atlarge.opendc.compute.metal.power.LinearLoadPowerModel
import com.atlarge.opendc.compute.metal.service.ProvisioningService
import com.atlarge.opendc.compute.metal.service.SimpleProvisioningService
import com.atlarge.opendc.core.Environment
import com.atlarge.opendc.core.Platform
import com.atlarge.opendc.core.Zone
import com.atlarge.opendc.core.services.ServiceRegistry
import com.atlarge.opendc.format.environment.EnvironmentReader
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream
import java.util.UUID

/**
 * A parser for the JSON experiment setup files used for the SC20 paper.
 *
 * @param input The input stream to read from.
 * @param mapper The Jackson object mapper to use.
 */
class Sc20EnvironmentReader(input: InputStream, mapper: ObjectMapper = jacksonObjectMapper()) : EnvironmentReader {
    /**
     * The environment that was read from the file.
     */
    private val setup: Setup = mapper.readValue(input)

    override suspend fun construct(dom: Domain): Environment {
        var counter = 0
        val nodes = setup.rooms.flatMap { room ->
            room.objects.flatMap { roomObject ->
                when (roomObject) {
                    is RoomObject.Rack -> {
                        roomObject.machines.map { machine ->
                            val cores = machine.cpus.flatMap { id ->
                                when (id) {
                                    1 -> {
                                        val node = ProcessingNode("Intel", "Core(TM) i7-6920HQ", "amd64", 4)
                                        List(node.coreCount) { ProcessingUnit(node, it, 4100.0) }
                                    }
                                    2 -> {
                                        val node = ProcessingNode("Intel", "Core(TM) i7-6920HQ", "amd64", 2)
                                        List(node.coreCount) { ProcessingUnit(node, it, 3500.0) }
                                    }
                                    else -> throw IllegalArgumentException("The cpu id $id is not recognized")
                                }
                            }
                            val memories = machine.memories.map { id ->
                                when (id) {
                                    1 -> MemoryUnit("Samsung", "PC DRAM K4A4G045WD", 1600.0, 4_000L)
                                    else -> throw IllegalArgumentException("The cpu id $id is not recognized")
                                }
                            }
                            SimpleBareMetalDriver(
                                dom.newDomain("node-$counter"),
                                UUID.randomUUID(),
                                "node-${counter++}",
                                emptyMap(),
                                cores,
                                // For now we assume a simple linear load model with an idle draw of ~200W and a maximum
                                // power draw of 350W.
                                // Source: https://stackoverflow.com/questions/6128960
                                memories,
                                LinearLoadPowerModel(200.0, 350.0)
                            )
                        }
                    }
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

        return Environment(setup.name, null, listOf(platform))
    }

    override fun close() {}
}
