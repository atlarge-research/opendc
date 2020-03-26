/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.compute.virt.driver

import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.virt.HypervisorEvent
import com.atlarge.opendc.core.services.AbstractServiceKey
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * A driver interface for a hypervisor running on some host server and communicating with the central compute service to
 * provide virtualization for that particular resource.
 */
public interface VirtDriver {
    /**
     * The events emitted by the driver.
     */
    public val events: Flow<HypervisorEvent>

    /**
     * Spawn the given [Image] on the compute resource of this driver.
     *
     * @param name The name of the server to spawn.
     * @param image The image to deploy.
     * @param flavor The flavor of the server which this driver is controlling.
     * @return The virtual server spawned by this method.
     */
    public suspend fun spawn(name: String, image: Image, flavor: Flavor): Server

    companion object Key : AbstractServiceKey<VirtDriver>(UUID.randomUUID(), "virtual-driver")
}
