package com.atlarge.opendc.runner.web

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import java.time.Instant
import org.bson.Document

/**
 * Manages the queue of scenarios that need to be processed.
 */
class ScenarioManager(private val collection: MongoCollection<Document>) {
    /**
     * Find the next scenario that the simulator needs to process.
     */
    fun findNext(): Document? {
        return collection
            .find(Filters.eq("simulation.state", "QUEUED"))
            .first()
    }

    /**
     * Claim the scenario in the database with the specified id.
     */
    fun claim(id: String): Boolean {
        val res = collection.findOneAndUpdate(
            Filters.and(
                Filters.eq("_id", id),
                Filters.eq("simulation.state", "QUEUED")
            ),
            Updates.combine(
                Updates.set("simulation.state", "RUNNING"),
                Updates.set("simulation.time", Instant.now())
            )
        )
        return res != null
    }

    /**
     * Update the heartbeat of the specified scenario.
     */
    fun heartbeat(id: String) {
        collection.findOneAndUpdate(
            Filters.and(
                Filters.eq("_id", id),
                Filters.eq("simulation.state", "RUNNING")
            ),
            Updates.set("simulation.time", Instant.now())
        )
    }

    /**
     * Mark the scenario as failed.
     */
    fun fail(id: String) {
        collection.findOneAndUpdate(
            Filters.and(
                Filters.eq("_id", id),
                Filters.eq("simulation.state", "FAILED")
            ),
            Updates.set("simulation.time", Instant.now())
        )
    }

    /**
     * Mark the scenario as finished.
     */
    fun finish(id: String) {
        collection.findOneAndUpdate(
            Filters.and(
                Filters.eq("_id", id),
                Filters.eq("simulation.state", "FINISHED")
            ),
            Updates.set("simulation.time", Instant.now())
        )
    }
}
