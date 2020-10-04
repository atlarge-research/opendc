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

package org.opendc.runner.web

import com.mongodb.client.AggregateIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bson.Document
import org.opendc.compute.core.metal.NODE_CLUSTER
import org.opendc.compute.core.metal.service.ProvisioningService
import org.opendc.compute.core.metal.service.SimpleProvisioningService
import org.opendc.compute.simulator.SimBareMetalDriver
import org.opendc.compute.simulator.power.LinearLoadPowerModel
import org.opendc.core.Environment
import org.opendc.core.Platform
import org.opendc.core.Zone
import org.opendc.core.services.ServiceRegistry
import org.opendc.format.environment.EnvironmentReader
import org.opendc.simulator.compute.SimMachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import java.time.Clock
import java.util.*

/**
 * A helper class that converts the MongoDB topology into an OpenDC environment.
 */
public class TopologyParser(private val collection: MongoCollection<Document>, private val id: String) : EnvironmentReader {
    /**
     * Parse the topology with the specified [id].
     */
    override suspend fun construct(coroutineScope: CoroutineScope, clock: Clock): Environment {
        val nodes = mutableListOf<SimBareMetalDriver>()
        val random = Random(0)

        for (machine in fetchMachines(id)) {
            val machineId = machine.getString("_id")
            val clusterId = machine.getString("rack_id")
            val position = machine.getInteger("position")

            val processors = machine.getList("cpus", Document::class.java).flatMap { cpu ->
                val cores = cpu.getInteger("numberOfCores")
                val speed = cpu.get("clockRateMhz", Number::class.java).toDouble()
                // TODO Remove hardcoding of vendor
                val node = ProcessingNode("Intel", "amd64", cpu.getString("name"), cores)
                List(cores) { coreId ->
                    ProcessingUnit(node, coreId, speed)
                }
            }
            val memoryUnits = machine.getList("memories", Document::class.java).map { memory ->
                MemoryUnit(
                    "Samsung",
                    memory.getString("name"),
                    memory.get("speedMbPerS", Number::class.java).toDouble(),
                    memory.get("sizeMb", Number::class.java).toLong()
                )
            }
            nodes.add(
                SimBareMetalDriver(
                    coroutineScope,
                    clock,
                    UUID(random.nextLong(), random.nextLong()),
                    "node-$clusterId-$position",
                    mapOf(NODE_CLUSTER to clusterId),
                    SimMachineModel(processors, memoryUnits),
                    // For now we assume a simple linear load model with an idle draw of ~200W and a maximum
                    // power draw of 350W.
                    // Source: https://stackoverflow.com/questions/6128960
                    LinearLoadPowerModel(200.0, 350.0)
                )
            )
        }

        val provisioningService = SimpleProvisioningService()
        coroutineScope.launch {
            for (node in nodes) {
                provisioningService.create(node)
            }
        }

        val serviceRegistry = ServiceRegistry().put(ProvisioningService, provisioningService)

        val platform = Platform(
            UUID.randomUUID(),
            "opendc-platform",
            listOf(
                Zone(UUID.randomUUID(), "zone", serviceRegistry)
            )
        )

        return Environment(fetchName(id), null, listOf(platform))
    }

    override fun close() {}

    /**
     * Fetch the metadata of the topology.
     */
    private fun fetchName(id: String): String {
        return collection.aggregate(
            listOf(
                Aggregates.match(Filters.eq("_id", id)),
                Aggregates.project(Projections.include("name"))
            )
        )
            .first()!!
            .getString("name")
    }

    /**
     * Fetch a topology from the database with the specified [id].
     */
    private fun fetchMachines(id: String): AggregateIterable<Document> {
        return collection.aggregate(
            listOf(
                Aggregates.match(Filters.eq("_id", id)),
                Aggregates.project(Projections.fields(Document("racks", "\$rooms.tiles.rack"))),
                Aggregates.unwind("\$racks"),
                Aggregates.unwind("\$racks"),
                Aggregates.replaceRoot("\$racks"),
                Aggregates.addFields(Field("machines.rack_id", "\$_id")),
                Aggregates.unwind("\$machines"),
                Aggregates.replaceRoot("\$machines")
            )
        )
    }
}
