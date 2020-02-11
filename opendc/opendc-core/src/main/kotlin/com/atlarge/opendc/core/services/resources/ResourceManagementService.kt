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

package com.atlarge.opendc.core.services.resources

import com.atlarge.odcsim.Channel
import com.atlarge.odcsim.ProcessContext
import com.atlarge.odcsim.SendRef
import com.atlarge.odcsim.sendOnce
import com.atlarge.opendc.core.Zone
import com.atlarge.opendc.core.ZoneMessage
import com.atlarge.opendc.core.resources.compute.MachineRef
import com.atlarge.opendc.core.resources.compute.MachineStatus
import com.atlarge.opendc.core.resources.compute.host.Host
import com.atlarge.opendc.core.resources.compute.supervision.MachineSupervisionEvent
import com.atlarge.opendc.core.services.Service
import com.atlarge.opendc.core.services.ServiceProvider
import java.util.UUID
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

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
    override suspend fun invoke(ctx: ProcessContext, zone: Zone, zoneRef: SendRef<ZoneMessage>, main: Channel<Any>) {
        // Launch the clusters of the zone
        for (cluster in zone.clusters) {
            ctx.spawn({ cluster(it, main.send) }, name = "${cluster.name}-${cluster.uid}")
        }

        val hosts = mutableMapOf<MachineRef, HostView>()
        val inlet = ctx.listen(main.receive)

        coroutineScope {
            while (isActive) {
                when (val msg = inlet.receive()) {
                    is MachineSupervisionEvent.Announce -> {
                        val host = msg.machine as? Host
                        if (host != null) {
                            hosts[msg.ref] = HostView(host, msg.ref)
                        }
                    }
                    is MachineSupervisionEvent.Up -> {
                        hosts.computeIfPresent(msg.ref) { _, value ->
                            value.copy(status = MachineStatus.RUNNING)
                        }
                    }
                    is ResourceManagementMessage.Lookup -> {
                        msg.replyTo.sendOnce(ResourceManagementResponse.Listing(hosts.values.find { it.host == msg.host }))
                    }
                }
            }
        }
    }
}

/**
 * A reference to the resource manager of a zone.
 */
typealias ResourceManagerRef = SendRef<ResourceManagementMessage>

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
        val replyTo: SendRef<ResourceManagementResponse.Listing>
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
