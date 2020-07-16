package com.atlarge.opendc.runner.web

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
import com.mongodb.client.AggregateIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import java.util.*
import kotlinx.coroutines.launch
import org.bson.Document

/**
 * A helper class that converts the MongoDB topology into an OpenDC environment.
 */
class TopologyParser(private val collection: MongoCollection<Document>, private val id: String) : EnvironmentReader {
    /**
     * Parse the topology with the specified [id].
     */
    override suspend fun construct(dom: Domain): Environment {
        val nodes = mutableListOf<SimpleBareMetalDriver>()
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
                SimpleBareMetalDriver(
                    dom.newDomain(machineId),
                    UUID(random.nextLong(), random.nextLong()),
                    "node-$clusterId-$position",
                    mapOf(NODE_CLUSTER to clusterId),
                    processors,
                    memoryUnits,
                    // For now we assume a simple linear load model with an idle draw of ~200W and a maximum
                    // power draw of 350W.
                    // Source: https://stackoverflow.com/questions/6128960
                    LinearLoadPowerModel(200.0, 350.0)
                )
            )
        }

        val provisioningService = SimpleProvisioningService(dom.newDomain("provisioner"))
        dom.launch {
            for (node in nodes) {
                provisioningService.create(node)
            }
        }

        val serviceRegistry = ServiceRegistry().put(ProvisioningService, provisioningService)

        val platform = Platform(
            UUID.randomUUID(), "opendc-platform", listOf(
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
