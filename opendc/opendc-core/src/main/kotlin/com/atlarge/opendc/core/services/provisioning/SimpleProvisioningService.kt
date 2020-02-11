/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.core.services.provisioning

import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.Channel
import com.atlarge.odcsim.ProcessContext
import com.atlarge.odcsim.SendRef
import com.atlarge.odcsim.ask
import com.atlarge.odcsim.sendOnce
import com.atlarge.opendc.core.Zone
import com.atlarge.opendc.core.ZoneMessage
import com.atlarge.opendc.core.find
import com.atlarge.opendc.core.resources.compute.MachineRef
import com.atlarge.opendc.core.services.Service
import com.atlarge.opendc.core.services.resources.HostView
import com.atlarge.opendc.core.services.resources.ResourceManagementMessage
import com.atlarge.opendc.core.services.resources.ResourceManagementResponse
import com.atlarge.opendc.core.services.resources.ResourceManagementService
import java.util.ArrayDeque
import java.util.UUID
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * A cloud platform service that provisions the native resources on the platform.
 *
 * This service assumes control over all hosts in its [Zone].
 */
object SimpleProvisioningService : ProvisioningService() {
    override val uid: UUID = UUID.randomUUID()
    override val name: String = "simple-provisioner"
    override val dependencies: Set<Service<*>> = setOf(ResourceManagementService)

    /**
     * Build the runtime [Behavior] for the resource provisioner, responding to messages of shape [ProvisioningMessage].
     */
    override suspend fun invoke(ctx: ProcessContext, zone: Zone, zoneRef: SendRef<ZoneMessage>, main: Channel<Any>) {
        val inlet = ctx.listen(main.receive)
        val manager = zoneRef.find(ResourceManagementService)

        delay(10)

        val hosts = mutableMapOf<MachineRef, HostView>()
        val available = ArrayDeque<HostView>()
        val leases = mutableSetOf<ProvisioningResponse.Lease>()

        // Subscribe to all machines in the zone
        for (cluster in zone.clusters) {
            for (host in cluster.hosts) {
                val msg: ResourceManagementResponse.Listing = manager.ask { ResourceManagementMessage.Lookup(host, it) }
                if (msg.instance != null) {
                    hosts[msg.instance.ref] = msg.instance
                    available.add(msg.instance)
                }
            }
        }

        coroutineScope {
            while (isActive) {
                when (val msg = inlet.receive()) {
                    is ProvisioningMessage.Request -> {
                        println("Provisioning ${msg.numHosts} hosts")
                        val leaseHosts = mutableListOf<HostView>()
                        while (available.isNotEmpty() && leaseHosts.size < msg.numHosts) {
                            leaseHosts += available.poll()
                        }
                        val lease = ProvisioningResponse.Lease(leaseHosts)
                        leases += lease
                        msg.replyTo.sendOnce(lease)
                    }
                    is ProvisioningMessage.Release -> {
                        val lease = msg.lease
                        if (lease in leases) {
                            return@coroutineScope
                        }
                        available.addAll(lease.hosts)
                        leases -= lease
                    }
                }
            }
        }
    }
}
