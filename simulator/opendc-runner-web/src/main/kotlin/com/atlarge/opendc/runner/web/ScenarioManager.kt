package com.atlarge.opendc.runner.web

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import org.bson.Document
import java.time.Instant

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
                Updates.set("simulation.heartbeat", Instant.now())
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
            Updates.set("simulation.heartbeat", Instant.now())
        )
    }

    /**
     * Mark the scenario as failed.
     */
    fun fail(id: String) {
        collection.findOneAndUpdate(
            Filters.eq("_id", id),
            Updates.combine(
                Updates.set("simulation.state", "FAILED"),
                Updates.set("simulation.heartbeat", Instant.now())
            )
        )
    }

    /**
     * Persist the specified results.
     */
    fun finish(id: String, result: ResultProcessor.Result) {
        collection.findOneAndUpdate(
            Filters.eq("_id", id),
            Updates.combine(
                Updates.set("simulation.state", "FINISHED"),
                Updates.unset("simulation.time"),
                Updates.set("results.total_requested_burst", result.totalRequestedBurst),
                Updates.set("results.total_granted_burst", result.totalGrantedBurst),
                Updates.set("results.total_overcommitted_burst", result.totalOvercommittedBurst),
                Updates.set("results.total_interfered_burst", result.totalInterferedBurst),
                Updates.set("results.mean_cpu_usage", result.meanCpuUsage),
                Updates.set("results.mean_cpu_demand", result.meanCpuDemand),
                Updates.set("results.mean_num_deployed_images", result.meanNumDeployedImages),
                Updates.set("results.max_num_deployed_images", result.maxNumDeployedImages),
                Updates.set("results.max_num_deployed_images", result.maxNumDeployedImages),
                Updates.set("results.total_power_draw", result.totalPowerDraw),
                Updates.set("results.total_failure_slices", result.totalFailureSlices),
                Updates.set("results.total_failure_vm_slices", result.totalFailureVmSlices),
                Updates.set("results.total_vms_submitted", result.totalVmsSubmitted),
                Updates.set("results.total_vms_queued", result.totalVmsQueued),
                Updates.set("results.total_vms_finished", result.totalVmsFinished),
                Updates.set("results.total_vms_failed", result.totalVmsFailed)
            )
        )
    }
}
