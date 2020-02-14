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

package com.atlarge.opendc.format.environment.sc18

import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.ProcessingUnit
import com.atlarge.opendc.compute.metal.driver.FakeBareMetalDriver
import com.atlarge.opendc.compute.metal.service.ProvisioningService
import com.atlarge.opendc.compute.metal.service.SimpleProvisioningService
import com.atlarge.opendc.core.Environment
import com.atlarge.opendc.core.Platform
import com.atlarge.opendc.core.Zone
import com.atlarge.opendc.core.services.ServiceRegistryImpl
import com.atlarge.opendc.format.environment.EnvironmentReader
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.runBlocking

/**
 * A parser for the JSON experiment setup files used for the SC18 paper: "A Reference Architecture for Datacenter
 * Schedulers".
 *
 * @param input The input stream to read from.
 * @param mapper The Jackson object mapper to use.
 */
class Sc18EnvironmentReader(input: InputStream, mapper: ObjectMapper = jacksonObjectMapper()) : EnvironmentReader {
    /**
     * The environment that was read from the file.
     */
    private val environment: Environment

    init {
        val setup = mapper.readValue<Setup>(input)
        var counter = 0
        val nodes = setup.rooms.flatMap { room ->
            room.objects.flatMap { roomObject ->
                when (roomObject) {
                    is RoomObject.Rack -> {
                        roomObject.machines.map { machine ->
                            val cores = machine.cpus.map { id ->
                                when (id) {
                                    1 -> ProcessingUnit("Intel", "Core(TM) i7-6920HQ", "amd64", 4100.0, 4)
                                    2 -> ProcessingUnit("Intel", "Core(TM) I7-6920HQ", "amd64", 3500.0, 2)
                                    else -> throw IllegalArgumentException("The cpu id $id is not recognized")
                                }
                            }
                            val flavor = Flavor(cores)
                            FakeBareMetalDriver(UUID.randomUUID(), "node-${counter++}", flavor)
                        }
                    }
                }
            }
        }

        val provisioningService = SimpleProvisioningService()
        runBlocking {
            for (node in nodes) {
                provisioningService.create(node)
            }
        }

        val serviceRegistry = ServiceRegistryImpl()
        serviceRegistry[ProvisioningService.Key] = provisioningService

        val platform = Platform(UUID.randomUUID(), "sc18-platform", listOf(
            Zone(UUID.randomUUID(), "zone", serviceRegistry)
        ))

        environment = Environment(setup.name, null, listOf(platform))
    }

    override fun read(): Environment = environment

    override fun close() {}
}
