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

package com.atlarge.opendc.core.services

import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.Channel
import com.atlarge.odcsim.ProcessContext
import com.atlarge.odcsim.SendRef
import com.atlarge.opendc.core.Identity
import com.atlarge.opendc.core.Zone
import com.atlarge.opendc.core.ZoneMessage
import java.util.UUID

/**
 * An abstract representation of a cloud service implementation provided by a cloud platform.
 */
interface ServiceProvider : Identity {
    /**
     * The unique identifier of the service implementation.
     */
    override val uid: UUID

    /**
     * The name of the service implementation.
     */
    override val name: String

    /**
     * The set of services provided by this [ServiceProvider].
     */
    val provides: Set<Service<*>>

    /**
     * The dependencies of the service implementation.
     */
    val dependencies: Set<Service<*>>

    /**
     * Build the runtime [Behavior] for this service.
     *
     * @param zone The zone model for which the service should be build.
     * @param zoneRef The runtime reference to the zone's actor for communication.
     * @param main The channel on which the service should listen.
     */
    suspend operator fun invoke(ctx: ProcessContext, zone: Zone, zoneRef: SendRef<ZoneMessage>, main: Channel<Any>)
}
