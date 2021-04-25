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
import org.bson.Document
import org.bson.types.ObjectId
import org.opendc.format.environment.EnvironmentReader
import org.opendc.format.environment.MachineDef
import org.opendc.simulator.compute.SimMachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.LinearPowerModel
import java.util.*

/**
 * A helper class that converts the MongoDB topology into an OpenDC environment.
 */
public class TopologyParser(private val collection: MongoCollection<Document>) {

    /**
     * Parse the topology from the specified [id].
     */
    public fun read(id: ObjectId): EnvironmentReader {
        val nodes = mutableListOf<MachineDef>()
        val random = Random(0)

        for (machine in fetchMachines(id)) {
            val clusterId = machine.get("rack_id").toString()
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

            val energyConsumptionW = machine.getList("cpus", Document::class.java).sumBy { it.getInteger("energyConsumptionW") }.toDouble()

            nodes.add(
                MachineDef(
                    UUID(random.nextLong(), random.nextLong()),
                    "node-$clusterId-$position",
                    mapOf("cluster" to clusterId),
                    SimMachineModel(processors, memoryUnits),
                    LinearPowerModel(2 * energyConsumptionW, energyConsumptionW * 0.5)
                )
            )
        }

        return object : EnvironmentReader {
            override fun read(): List<MachineDef> = nodes
            override fun close() {}
        }
    }

    /**
     * Fetch the metadata of the topology.
     */
    private fun fetchName(id: ObjectId): String {
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
    private fun fetchMachines(id: ObjectId): AggregateIterable<Document> {
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
