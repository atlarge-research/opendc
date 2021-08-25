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

package org.opendc.experiments.capelin.monitor

import org.opendc.compute.api.Server
import org.opendc.compute.api.ServerState
import org.opendc.compute.service.driver.Host
import org.opendc.compute.service.driver.HostState

/**
 * A monitor watches the events of an experiment.
 */
public interface ExperimentMonitor : AutoCloseable {
    /**
     * This method is invoked when the state of a VM changes.
     */
    public fun reportVmStateChange(time: Long, server: Server, newState: ServerState) {}

    /**
     * This method is invoked when the state of a host changes.
     */
    public fun reportHostStateChange(time: Long, host: Host, newState: HostState) {}

    /**
     * This method is invoked for a host for each slice that is finishes.
     */
    public fun reportHostSlice(
        time: Long,
        requestedBurst: Long,
        grantedBurst: Long,
        overcommissionedBurst: Long,
        interferedBurst: Long,
        cpuUsage: Double,
        cpuDemand: Double,
        powerDraw: Double,
        numberOfDeployedImages: Int,
        host: Host
    ) {}

    /**
     * This method is invoked for a provisioner event.
     */
    public fun reportProvisionerMetrics(
        time: Long,
        totalHostCount: Int,
        availableHostCount: Int,
        totalVmCount: Int,
        activeVmCount: Int,
        inactiveVmCount: Int,
        waitingVmCount: Int,
        failedVmCount: Int
    ) {}
}
