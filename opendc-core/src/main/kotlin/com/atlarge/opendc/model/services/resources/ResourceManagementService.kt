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

package com.atlarge.opendc.model.services.resources

import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.receiveMessage
import com.atlarge.odcsim.same
import com.atlarge.odcsim.setup
import com.atlarge.odcsim.unhandled
import com.atlarge.opendc.model.Zone
import com.atlarge.opendc.model.ZoneRef
import com.atlarge.opendc.model.resources.compute.MachineRef
import com.atlarge.opendc.model.resources.compute.MachineStatus
import com.atlarge.opendc.model.resources.compute.host.Host
import com.atlarge.opendc.model.resources.compute.supervision.MachineSupervisionEvent
import com.atlarge.opendc.model.services.Service
import com.atlarge.opendc.model.services.ServiceProvider
import java.util.UUID

/**
 * A cloud platform service that manages the native resources on the platform.
 *
 * This service assumes control over all hosts in its [Zone].
 */
object ResourceManagementService : ServiceProvider, Service<ResourceManagementMessage> {
    override val uid: UUID = UUID.randomUUID()
    override val name: String = "resource-manager"
    override val provides: Set<Service<*>> = setOf(ResourceManagementService)
    override val dependencies: Set<Service<*>> = emptySet()

    /**
     * Build the runtime behavior of the [ResourceManagementService].
     */
    override fun invoke(zone: Zone, ref: ZoneRef): Behavior<ResourceManagementMessage> = setup { ctx ->
        // Launch the clusters of the zone
        for (cluster in zone.clusters) {
            ctx.spawn(cluster(ctx.self), name = "${cluster.name}-${cluster.uid}")
        }

        val hosts = mutableMapOf<MachineRef, HostView>()
        receiveMessage<Any> { msg ->
            when (msg) {
                is MachineSupervisionEvent.Announce -> {
                    val host = msg.machine as? Host
                    if (host != null) {
                        hosts[msg.ref] = HostView(host, msg.ref)
                    }
                    same()
                }
                is MachineSupervisionEvent.Up -> {
                    hosts.computeIfPresent(msg.ref) { _, value ->
                        value.copy(status = MachineStatus.RUNNING)
                    }
                    same()
                }
                is ResourceManagementMessage.Lookup -> {
                    ctx.send(msg.replyTo, ResourceManagementResponse.Listing(hosts.values.find { it.host == msg.host }))
                    same()
                }
                else ->
                    unhandled()
            }
        }.narrow()
    }
}

/**
 * A reference to the resource manager of a zone.
 */
typealias ResourceManagerRef = ActorRef<ResourceManagementMessage>

/**
 * A message protocol for communicating to the resource manager.
 */
sealed class ResourceManagementMessage {
    /**
     * Lookup the specified [Host].
     *
     * @property host The host to lookup.
     * @property replyTo The address to sent the response to.
     */
    data class Lookup(
        val host: Host,
        val replyTo: ActorRef<ResourceManagementResponse.Listing>
    ) : ResourceManagementMessage()
}

/**
 * A message protocol used by the resource manager to respond to [ResourceManagementMessage]s.
 */
sealed class ResourceManagementResponse {
    /**
     * A response to a [ResourceManagementMessage.Lookup] request.
     *
     * @property instance The instance that was found or `null` if it does not exist.
     */
    data class Listing(val instance: HostView?) : ResourceManagementResponse()
}
