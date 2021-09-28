/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.web.runner

import org.opendc.web.client.ApiClient
import org.opendc.web.client.model.Job
import org.opendc.web.client.model.SimulationState

/**
 * Manages the queue of scenarios that need to be processed.
 */
public class ScenarioManager(private val client: ApiClient) {
    /**
     * Find the next job that the simulator needs to process.
     */
    public suspend fun findNext(): Job? {
        return client.getJobs().firstOrNull()
    }

    /**
     * Claim the simulation job with the specified id.
     */
    public suspend fun claim(id: String): Boolean {
        return client.updateJob(id, SimulationState.CLAIMED)
    }

    /**
     * Update the heartbeat of the specified scenario.
     */
    public suspend fun heartbeat(id: String) {
        client.updateJob(id, SimulationState.RUNNING)
    }

    /**
     * Mark the scenario as failed.
     */
    public suspend fun fail(id: String) {
        client.updateJob(id, SimulationState.FAILED)
    }

    /**
     * Persist the specified results.
     */
    public suspend fun finish(id: String, results: List<WebComputeMetricExporter.Result>) {
        client.updateJob(
            id, SimulationState.FINISHED,
            mapOf(
                "total_requested_burst" to results.map { it.totalActiveTime + it.totalIdleTime },
                "total_granted_burst" to results.map { it.totalActiveTime },
                "total_overcommitted_burst" to results.map { it.totalStealTime },
                "total_interfered_burst" to results.map { it.totalLostTime },
                "mean_cpu_usage" to results.map { it.meanCpuUsage },
                "mean_cpu_demand" to results.map { it.meanCpuDemand },
                "mean_num_deployed_images" to results.map { it.meanNumDeployedImages },
                "max_num_deployed_images" to results.map { it.maxNumDeployedImages },
                "total_power_draw" to results.map { it.totalPowerDraw },
                "total_failure_slices" to results.map { it.totalFailureSlices },
                "total_failure_vm_slices" to results.map { it.totalFailureVmSlices },
                "total_vms_submitted" to results.map { it.totalVmsSubmitted },
                "total_vms_queued" to results.map { it.totalVmsQueued },
                "total_vms_finished" to results.map { it.totalVmsFinished },
                "total_vms_failed" to results.map { it.totalVmsFailed }
            )
        )
    }
}
