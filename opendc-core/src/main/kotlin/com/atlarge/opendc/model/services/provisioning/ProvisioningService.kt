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

import com.atlarge.odcsim.ActorRef
import com.atlarge.opendc.model.Zone
import com.atlarge.opendc.model.services.AbstractService
import com.atlarge.opendc.model.services.Service
import com.atlarge.opendc.model.services.ServiceProvider
import com.atlarge.opendc.model.services.resources.HostView
import java.util.UUID

/**
 * A cloud platform service that provisions the native resources on the platform.
 *
 * This service assumes control over all hosts in its [Zone].
 */
abstract class ProvisioningService : ServiceProvider {
    override val provides: Set<Service<*>> = setOf(ProvisioningService)

    /**
     * The service key of the provisioner service.
     */
    companion object : AbstractService<ProvisioningMessage>(UUID.randomUUID(), "provisioner")
}

/**
 * A message protocol for communicating to the resource provisioner.
 */
sealed class ProvisioningMessage {
    /**
     * Request the specified number of resources from the provisioner.
     *
     * @property numHosts The number of hosts to request from the provisioner.
     * @property replyTo The actor to reply to.
     */
    data class Request(val numHosts: Int, val replyTo: ActorRef<ProvisioningResponse.Lease>) : ProvisioningMessage()

    /**
     * Release the specified resource [ProvisioningResponse.Lease].
     *
     * @property lease The lease to release.
     */
    data class Release(val lease: ProvisioningResponse.Lease) : ProvisioningMessage()
}

/**
 * A message protocol used by the resource provisioner to respond to [ProvisioningMessage]s.
 */
sealed class ProvisioningResponse {
    /**
     * A lease for the specified hosts.
     */
    data class Lease(val hosts: List<HostView>) : ProvisioningResponse()
}
