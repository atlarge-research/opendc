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

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import org.bson.Document
import org.bson.types.ObjectId
import java.time.Instant

/**
 * Manages the queue of scenarios that need to be processed.
 */
public class ScenarioManager(private val collection: MongoCollection<Document>) {
    /**
     * Find the next scenario that the simulator needs to process.
     */
    public fun findNext(): Document? {
        return collection
            .find(Filters.eq("simulation.state", "QUEUED"))
            .first()
    }

    /**
     * Claim the scenario in the database with the specified id.
     */
    public fun claim(id: ObjectId): Boolean {
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
    public fun heartbeat(id: ObjectId) {
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
    public fun fail(id: ObjectId) {
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
    public fun finish(id: ObjectId, results: List<WebExperimentMonitor.Result>) {
        collection.findOneAndUpdate(
            Filters.eq("_id", id),
            Updates.combine(
                Updates.set("simulation.state", "FINISHED"),
                Updates.unset("simulation.time"),
                Updates.set("results.total_requested_burst", results.map { it.totalRequestedBurst }),
                Updates.set("results.total_granted_burst", results.map { it.totalGrantedBurst }),
                Updates.set("results.total_overcommitted_burst", results.map { it.totalOvercommittedBurst }),
                Updates.set("results.total_interfered_burst", results.map { it.totalInterferedBurst }),
                Updates.set("results.mean_cpu_usage", results.map { it.meanCpuUsage }),
                Updates.set("results.mean_cpu_demand", results.map { it.meanCpuDemand }),
                Updates.set("results.mean_num_deployed_images", results.map { it.meanNumDeployedImages }),
                Updates.set("results.max_num_deployed_images", results.map { it.maxNumDeployedImages }),
                Updates.set("results.total_power_draw", results.map { it.totalPowerDraw }),
                Updates.set("results.total_failure_slices", results.map { it.totalFailureSlices }),
                Updates.set("results.total_failure_vm_slices", results.map { it.totalFailureVmSlices }),
                Updates.set("results.total_vms_submitted", results.map { it.totalVmsSubmitted }),
                Updates.set("results.total_vms_queued", results.map { it.totalVmsQueued }),
                Updates.set("results.total_vms_finished", results.map { it.totalVmsFinished }),
                Updates.set("results.total_vms_failed", results.map { it.totalVmsFailed })
            )
        )
    }
}
