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

package org.opendc.compute.service.scheduler

import mu.KotlinLogging
import org.opendc.compute.api.Server
import org.opendc.compute.service.HostView

/**
 * Policy replaying VM-cluster assignment.
 *
 * Within each cluster, the active servers on each node determine which node gets
 * assigned the VM image.
 */
public class ReplayScheduler(private val vmPlacements: Map<String, String>) : ComputeScheduler {
    private val logger = KotlinLogging.logger {}

    /**
     * The pool of hosts available to the scheduler.
     */
    private val hosts = mutableListOf<HostView>()

    override fun addHost(host: HostView) {
        hosts.add(host)
    }

    override fun removeHost(host: HostView) {
        hosts.remove(host)
    }

    override fun select(server: Server): HostView? {
        val clusterName = vmPlacements[server.name]
            ?: throw IllegalStateException("Could not find placement data in VM placement file for VM ${server.name}")
        val machinesInCluster = hosts.filter { it.host.name.contains(clusterName) }

        if (machinesInCluster.isEmpty()) {
            logger.info { "Could not find any machines belonging to cluster $clusterName for image ${server.name}, assigning randomly." }
            return hosts.maxByOrNull { it.availableMemory }
        }

        return machinesInCluster.maxByOrNull { it.availableMemory }
            ?: throw IllegalStateException("Cloud not find any machine and could not randomly assign")
    }
}
