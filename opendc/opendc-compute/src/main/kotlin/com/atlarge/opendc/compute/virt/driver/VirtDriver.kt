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
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.core.services.AbstractServiceKey
import java.util.UUID

/**
 * A driver interface for a hypervisor running on some host server and communicating with the central compute service to
 * provide virtualization for that particular resource.
 */
public interface VirtDriver {
    /**
     * Spawn the given [Image] on the compute resource of this driver.
     *
     * @param image The image to deploy.
     * @param monitor The monitor to use for the deployment of this particular image.
     * @param flavor The flavor of the server which this driver is controlling.
     * @return The virtual server spawned by this method.
     */
    public suspend fun spawn(image: Image, monitor: ServerMonitor, flavor: Flavor): Server

    /**
     * Adds the given [VirtDriverMonitor] to the list of monitors to keep informed on the state of this driver.
     *
     * @param monitor The monitor to keep informed.
     */
    public suspend fun addMonitor(monitor: VirtDriverMonitor)

    /**
     * Removes the given [VirtDriverMonitor] from the list of monitors.
     *
     * @param monitor The monitor to unsubscribe
     */
    public suspend fun removeMonitor(monitor: VirtDriverMonitor)

    companion object Key : AbstractServiceKey<VirtDriver>(UUID.randomUUID(), "virtual-driver")
}
