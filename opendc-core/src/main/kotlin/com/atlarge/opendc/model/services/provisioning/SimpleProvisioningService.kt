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

package com.atlarge.opendc.model.services.provisioning

import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.StashBuffer
import com.atlarge.odcsim.receiveMessage
import com.atlarge.odcsim.same
import com.atlarge.odcsim.setup
import com.atlarge.odcsim.unhandled
import com.atlarge.odcsim.unsafeCast
import com.atlarge.opendc.model.Zone
import com.atlarge.opendc.model.ZoneMessage
import com.atlarge.opendc.model.ZoneRef
import com.atlarge.opendc.model.ZoneResponse
import com.atlarge.opendc.model.resources.compute.MachineRef
import com.atlarge.opendc.model.services.Service
import com.atlarge.opendc.model.services.resources.HostView
import com.atlarge.opendc.model.services.resources.ResourceManagementMessage
import com.atlarge.opendc.model.services.resources.ResourceManagementResponse
import com.atlarge.opendc.model.services.resources.ResourceManagementService
import com.atlarge.opendc.model.services.resources.ResourceManagerRef
import java.util.ArrayDeque
import java.util.UUID

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
    override fun invoke(zone: Zone, ref: ZoneRef): Behavior<ProvisioningMessage> = setup { ctx ->
        val buffer = StashBuffer<Any>(capacity = 30)
        ctx.send(ref, ZoneMessage.Find(ResourceManagementService, ctx.self.unsafeCast()))

        receiveMessage<Any> { msg ->
            when (msg) {
                is ZoneResponse.Listing -> {
                    val service = msg(ResourceManagementService) ?: throw IllegalStateException("Resource management service not available")
                    buffer.unstashAll(ctx.unsafeCast(), active(zone, service).unsafeCast())
                }
                else -> {
                    buffer.stash(msg)
                    same()
                }
            }
        }.narrow()
    }

    private fun active(zone: Zone, manager: ResourceManagerRef) = setup<ProvisioningMessage> { ctx ->
        val hosts = mutableMapOf<MachineRef, HostView>()
        val available = ArrayDeque<HostView>()
        val leases = mutableSetOf<ProvisioningResponse.Lease>()

        // Subscribe to all machines in the zone
        for (cluster in zone.clusters) {
            for (host in cluster.hosts) {
                ctx.send(manager, ResourceManagementMessage.Lookup(host, ctx.self.unsafeCast()))
            }
        }

        receiveMessage<Any> { msg ->
            when (msg) {
                is ProvisioningMessage.Request -> {
                    ctx.log.info("Provisioning {} hosts", msg.numHosts)
                    val leaseHosts = mutableListOf<HostView>()
                    while (available.isNotEmpty() && leaseHosts.size < msg.numHosts) {
                        leaseHosts += available.poll()
                    }
                    val lease = ProvisioningResponse.Lease(leaseHosts)
                    leases += lease
                    ctx.send(msg.replyTo, lease)
                    same()
                }
                is ProvisioningMessage.Release -> {
                    val lease = msg.lease
                    if (lease in leases) {
                        return@receiveMessage same()
                    }
                    available.addAll(lease.hosts)
                    leases -= lease
                    same()
                }
                is ResourceManagementResponse.Listing -> {
                    if (msg.instance != null) {
                        hosts[msg.instance.ref] = msg.instance
                        available.add(msg.instance)
                    }
                    same()
                }
                else ->
                    unhandled()
            }
        }.narrow()
    }
}
