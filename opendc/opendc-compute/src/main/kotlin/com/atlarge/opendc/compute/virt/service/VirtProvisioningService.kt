package com.atlarge.opendc.compute.virt.service

import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.image.Image
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import com.atlarge.opendc.compute.virt.service.allocation.AllocationPolicy

/**
 * A service for VM provisioning on a cloud.
 */
interface VirtProvisioningService {
    /**
     * The policy used for allocating a VM on the available hypervisors.
     */
    val allocationPolicy: AllocationPolicy

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
    public suspend fun deploy(name: String, image: Image, flavor: Flavor): Server

    /**
     * Terminate the provisioning service releasing all the leased bare-metal machines.
     */
    public suspend fun terminate()
}
