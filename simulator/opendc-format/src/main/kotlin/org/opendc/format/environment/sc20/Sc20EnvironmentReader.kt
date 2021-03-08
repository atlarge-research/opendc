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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.opendc.compute.simulator.power.models.LinearPowerModel
import org.opendc.format.environment.EnvironmentReader
import org.opendc.format.environment.MachineDef
import org.opendc.simulator.compute.SimMachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import java.io.InputStream
import java.util.*

/**
 * A parser for the JSON experiment setup files used for the SC20 paper.
 *
 * @param input The input stream to read from.
 * @param mapper The Jackson object mapper to use.
 */
public class Sc20EnvironmentReader(input: InputStream, mapper: ObjectMapper = jacksonObjectMapper()) : EnvironmentReader {
    /**
     * The environment that was read from the file.
     */
    private val setup: Setup = mapper.readValue(input)

    /**
     * Read the environment.
     */
    public override fun read(): List<MachineDef> {
        var counter = 0
        return setup.rooms.flatMap { room ->
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
                            MachineDef(
                                UUID(0L, counter++.toLong()),
                                "node-$counter",
                                emptyMap(),
                                SimMachineModel(cores, memories),
                                // For now we assume a simple linear load model with an idle draw of ~200W and a maximum
                                // power draw of 350W.
                                // Source: https://stackoverflow.com/questions/6128960
                                LinearPowerModel(350.0, 200 / 350.0)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun close() {}
}
