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

package org.opendc.compute.virt.service

import kotlinx.coroutines.flow.Flow
import org.opendc.compute.core.Flavor
import org.opendc.compute.core.Server
import org.opendc.compute.core.image.Image
import org.opendc.compute.virt.driver.VirtDriver
import org.opendc.compute.virt.service.allocation.AllocationPolicy

/**
 * A service for VM provisioning on a cloud.
 */
public interface VirtProvisioningService {
    /**
     * The policy used for allocating a VM on the available hypervisors.
     */
    public val allocationPolicy: AllocationPolicy

    /**
     * The events emitted by the service.
     */
    public val events: Flow<VirtProvisioningEvent>

    /**
     * Obtain the active hypervisors for this provisioner.
     */
    public suspend fun drivers(): Set<VirtDriver>

    /**
     * Submit the specified [Image] to the provisioning service.
     *
     * @param name The name of the server to deploy.
     * @param image The image to be deployed.
     * @param flavor The flavor of the machine instance to run this [image] on.
     */
    public suspend fun deploy(
        name: String,
        image: Image,
        flavor: org.opendc.compute.core.Flavor
    ): Server

    /**
     * Terminate the provisioning service releasing all the leased bare-metal machines.
     */
    public suspend fun terminate()
}
